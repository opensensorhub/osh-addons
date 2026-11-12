/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research, LLC. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.process.gpmf;

import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Vector;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.sensorhub.impl.process.gpmf.parser.GpmfGps;
import org.sensorhub.impl.process.gpmf.parser.GpmfGpsFix;
import org.sensorhub.impl.process.gpmf.parser.GpmfPayload;
import org.sensorhub.impl.process.gpmf.parser.GpmfStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataArrayImpl;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import java.util.List;

/**
 * Process that decodes GoPro GPMF telemetry packets.
 * <p>
 * The input is one undecoded GPMF packet, as the raw bytes a GoPro records on its telemetry track. Each
 * packet describes a set of sensor streams for the interval it covers -- accelerometer, gyroscope, GPS,
 * camera state, and whatever else the model records -- and this process publishes one telemetry record per
 * stream, with the camera's own scale divisors already applied, plus one position record per GPS fix.
 * <p>
 * Which streams appear depends on the camera model, its firmware, and the capture settings, so the
 * telemetry record is self-describing rather than shaped around a fixed set of sensors: every record names
 * its stream and units as the camera reported them, and carries that stream's samples.
 * <p>
 * Feed it from a demuxed data track -- for instance the {@code data} output of the FFmpeg driver attached
 * to a GoPro recording's {@code gpmd} track.
 *
 * @see GpmfPayload
 */
public class GpmfProcess extends ExecutableProcessImpl {
    public static final OSHProcessInfo INFO = new OSHProcessInfo(
            "gpmf:GpmfDecoder",
            "GPMF Telemetry Decoder",
            "Decodes GoPro GPMF telemetry packets into per-stream telemetry records and GPS position fixes",
            GpmfProcess.class);

    protected static final String INPUT_NAME = "gpmfPacket";
    protected static final String TELEMETRY_OUTPUT_NAME = "gpmf";
    protected static final String GPS_OUTPUT_NAME = "gps";

    private static final String VALUE_COUNT_ID = "valueCountID";
    private static final Logger logger = LoggerFactory.getLogger(GpmfProcess.class.getSimpleName());

    /**
     * Flat index of the variable-size value array within a telemetry record's data block. The ten
     * preceding atoms are the scalar fields, the last of which is the array's own size component.
     */
    private static final int VALUES_INDEX = 10;

    private final DataArray gpmfPacket;
    private final DataRecord telemetryRecord;
    private final DataArrayImpl valueArray;
    private final DataRecord gpsRecord;
    private final Vector location;
    private final Quantity altitude;
    private final net.opengis.swe.v20.Boolean requireGpsFix;
    private final net.opengis.swe.v20.Text altitudeDatumParam;

    /**
     * The datum the GPS record is currently described on, which the altitude datum parameter sets at
     * initialization.
     */
    private AltitudeDatum altitudeDatum = AltitudeDatum.MEAN_SEA_LEVEL;

    /**
     * Set once the camera's declared altitude system has been checked against the configured datum, so
     * that a mismatch is reported once rather than for every packet.
     */
    private boolean altitudeSystemChecked;

    public GpmfProcess() {
        super(INFO);

        GeoPosHelper sweFactory = new GeoPosHelper();

        // Input: one undecoded GPMF packet
        inputData.add(INPUT_NAME, gpmfPacket = sweFactory.createArray()
                .label("GPMF Packet")
                .description("Raw bytes of one GPMF telemetry packet, exactly as they were demuxed")
                .withElement("byte", sweFactory.createCount()
                        .label("Byte")
                        .definition(SWEConstants.DEF_DN)
                        .dataType(DataType.BYTE))
                .build());

        // Parameters
        paramData.add("requireGpsFix", requireGpsFix = sweFactory.createBoolean()
                .label("Require GPS Fix")
                .description("Only publish position records the receiver had a lock for. A GoPro reports " +
                        "coordinates even with no fix, and those coordinates are meaningless, so clearing " +
                        "this will scatter the track with bad positions. Unaffected either way, the telemetry " +
                        "output always reports the GPS streams as recorded.")
                .value(true)
                .build());

        paramData.add("altitudeDatum", altitudeDatumParam = sweFactory.createText()
                .label("GPS Altitude Datum")
                .description("Reference surface the camera measures altitude from, used only when the camera " +
                        "does not declare it. The HERO8, MAX, and newer declare it in the telemetry; the HERO7 " +
                        "and earlier do not and report height above the WGS84 ellipsoid. The process publishes " +
                        "the camera's readings unchanged and uses this only to describe them correctly.")
                .addAllowedValues(AltitudeDatum.class)
                .value(AltitudeDatum.MEAN_SEA_LEVEL.name())
                .build());

        // Telemetry output, one record per sensor stream in a packet
        outputData.add(TELEMETRY_OUTPUT_NAME, telemetryRecord = sweFactory.createRecord()
                .label("GPMF Telemetry")
                .description("GoPro GPMF telemetry")
                .definition(SWEHelper.getPropertyUri("TelemetryStream"))
                .addField("sampleTime", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("Time of data collection"))
                .addField("streamTime", sweFactory.createQuantity()
                        .label("Stream Time")
                        .description("Offset of these samples from the start of the recording, as reported by the camera. NaN if the camera did not report one.")
                        .uomCode("s")
                        .dataType(DataType.DOUBLE))
                .addField("deviceName", sweFactory.createText()
                        .label("Device Name")
                        .description("Name of the device that recorded this stream, for example Hero11 Black"))
                .addField("streamKey", sweFactory.createText()
                        .label("Stream Key")
                        .description("Four-character GPMF key identifying the stream, for example ACCL, GYRO, or GPS5"))
                .addField("streamName", sweFactory.createText()
                        .label("Stream Name")
                        .description("Human-readable stream name as the camera reported it, for example Accelerometer (z,x,y)"))
                .addField("units", sweFactory.createText()
                        .label("Units")
                        .description("Units of the values, as the camera reported them, for example m/s2"))
                .addField("elementCount", sweFactory.createCount()
                        .label("Element Count")
                        .description("Number of values in one sample, for example 3 for a three-axis sensor")
                        .dataType(DataType.INT))
                .addField("sampleCount", sweFactory.createCount()
                        .label("Sample Count")
                        .description("Number of samples in this record")
                        .dataType(DataType.INT))
                .addField("textValue", sweFactory.createText()
                        .label("Text Value")
                        .description("Content of a text-valued stream, such as the GPS UTC timestamp. Empty for numeric streams."))
                .addField("valueCount", sweFactory.createCount()
                        .id(VALUE_COUNT_ID)
                        .label("Value Count")
                        .description("Total number of values in this record, equal to the sample count times the element count")
                        .dataType(DataType.INT))
                .addField("values", sweFactory.createArray()
                        .withVariableSize(VALUE_COUNT_ID)
                        .label("Values")
                        .description("Samples of this stream in order, each holding element count values, with the camera's scale divisors applied")
                        .withElement("value", sweFactory.createQuantity()
                                .label("Value")
                                .definition(SWEConstants.DEF_DN)
                                .dataType(DataType.DOUBLE)))
                .build());

        valueArray = (DataArrayImpl) telemetryRecord.getComponent("values");

        // Position output, one record per GPS fix. The datum is applied in init() once the parameter is
        // known; mean sea level is the default the structure is built on.
        outputData.add(GPS_OUTPUT_NAME, gpsRecord = sweFactory.createRecord()
                .label("GPS")
                .description("GoPro GPS fixes from GPMF telemetry")
                .definition(SWEHelper.getPropertyUri("GeoPosition"))
                .addField("sampleTime", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("Time of data collection"))
                .addField("gpsTime", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("GPS Time")
                        .description("Time of the fix as reported by the GPS receiver. NaN if the camera did not report one."))
                .addField("location", sweFactory.createLocationVectorLLA_MSL()
                        .label("Location")
                        .description("Position of the camera"))
                .addField("speed2D", sweFactory.createQuantity()
                        .label("Ground Speed")
                        .description("Speed over the ground. NaN if the camera did not report one.")
                        .definition(SWEHelper.getPropertyUri("GroundSpeed"))
                        .uomCode("m/s")
                        .dataType(DataType.DOUBLE))
                .addField("speed3D", sweFactory.createQuantity()
                        .label("3D Speed")
                        .description("Speed including the vertical component. NaN if the camera did not report one.")
                        .definition(SWEHelper.getPropertyUri("Speed"))
                        .uomCode("m/s")
                        .dataType(DataType.DOUBLE))
                .addField("fixType", sweFactory.createCount()
                        .label("Fix Type")
                        .description("GPS fix quality: 0 for no fix, 2 for a 2D fix, 3 for a 3D fix, -1 if the camera did not report one")
                        .dataType(DataType.INT))
                .addField("dop", sweFactory.createQuantity()
                        .label("Dilution of Precision")
                        .description("Dilution of precision of the fix, lower being better. NaN if the camera did not report one.")
                        .uomCode("1")
                        .dataType(DataType.DOUBLE))
                .build());

        location = (Vector) gpsRecord.getComponent("location");
        altitude = (Quantity) location.getComponent("alt");
    }

    @Override
    public void init() throws ProcessException {
        super.init();

        applyAltitudeDatum(readAltitudeDatum());

        logger.debug("GPMF process initialized with {} altitude.", altitudeDatum);
    }

    @Override
    public void notifyParamChange() {
        AltitudeDatum datum = readAltitudeDatum();

        if (datum != altitudeDatum) {
            applyAltitudeDatum(datum);
            altitudeSystemChecked = false;
            logger.info("GPS altitude datum changed to {}.", altitudeDatum);
        }
    }

    @Override
    public void execute() throws ProcessException {
        // The packet carries no wall-clock time of its own, so collection time is when it was decoded
        long timestamp = System.currentTimeMillis();
        byte[] data = readPacketBytes();

        if (data.length == 0) {
            logger.debug("Skipping empty GPMF packet.");
            return;
        }

        GpmfPayload payload;
        try {
            payload = GpmfPayload.parse(data);
        } catch (RuntimeException e) {
            // One malformed packet should not stop the telemetry that follows it
            logger.warn("Dropping malformed GPMF payload of {} bytes: {}", data.length, e.getMessage());
            return;
        }

        if (payload.isEmpty()) {
            logger.debug("GPMF payload carried no sensor streams.");
            return;
        }

        try {
            publishTelemetry(payload, timestamp);
            publishGpsFixes(payload, timestamp);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProcessException("Interrupted while publishing GPMF telemetry", e);
        }
    }

    /**
     * Publishing is done record by record from {@link GpmfProcess#execute()}, since one packet yields a
     * record per sensor stream and one per GPS fix rather than the single record per execution that the
     * base class publishes. Letting the base class publish as well would re-send the last record of each
     * output, and would re-send a stale one for an output this packet produced nothing for.
     */
    @Override
    protected void publishData() {
        // deliberately empty, see above
    }

    /**
     * Publishes one telemetry record per sensor stream in the payload.
     *
     * @param payload   The decoded packet.
     * @param timestamp Collection time of the packet, in milliseconds.
     */
    private void publishTelemetry(GpmfPayload payload, long timestamp) throws InterruptedException {
        for (GpmfStream stream : payload.getStreams()) {
            double[] values = stream.getValues();

            // The value array is variable-size, so it has to be resized before the block is allocated
            valueArray.updateSize(values.length);
            DataBlock dataBlock = telemetryRecord.createDataBlock();

            int index = 0;
            dataBlock.setDoubleValue(index++, timestamp / 1000d);
            dataBlock.setDoubleValue(index++, stream.getTimestamp());
            dataBlock.setStringValue(index++, orEmpty(stream.getDeviceName()));
            dataBlock.setStringValue(index++, orEmpty(stream.getKey()));
            dataBlock.setStringValue(index++, orEmpty(stream.getName()));
            dataBlock.setStringValue(index++, orEmpty(stream.getUnits()));
            dataBlock.setIntValue(index++, stream.getElementCount());
            dataBlock.setIntValue(index++, stream.getSampleCount());
            dataBlock.setStringValue(index++, orEmpty(stream.getText()));
            dataBlock.setIntValue(index, values.length);

            for (int i = 0; i < values.length; i++) {
                dataBlock.setDoubleValue(VALUES_INDEX + i, values[i]);
            }

            telemetryRecord.setData(dataBlock);
            publishData(TELEMETRY_OUTPUT_NAME);
        }
    }

    /**
     * Publishes one position record per GPS fix in the payload.
     *
     * @param payload   The decoded packet.
     * @param timestamp Collection time of the packet, in milliseconds.
     */
    private void publishGpsFixes(GpmfPayload payload, long timestamp) throws InterruptedException {
        List<GpmfGpsFix> fixes = GpmfGps.extract(payload);

        if (fixes.isEmpty()) {
            return;
        }

        checkAltitudeSystem(fixes.get(0));

        boolean requireFix = requireGpsFix.getData().getBooleanValue();
        int droppedCount = 0;

        for (GpmfGpsFix fix : fixes) {
            // A GoPro keeps reporting coordinates with no lock, and those coordinates are meaningless:
            // they land far from the camera and would scatter the published track with bad positions.
            if (requireFix && !fix.hasLock()) {
                droppedCount++;
                continue;
            }

            // A fresh block per fix, since the previous one has already been handed to the connections
            DataBlock dataBlock = gpsRecord.createDataBlock();

            int index = 0;
            dataBlock.setDoubleValue(index++, timestamp / 1000d);
            dataBlock.setDoubleValue(index++, fix.utcTime());
            dataBlock.setDoubleValue(index++, fix.latitude());
            dataBlock.setDoubleValue(index++, fix.longitude());
            dataBlock.setDoubleValue(index++, fix.altitude());
            dataBlock.setDoubleValue(index++, fix.speed2D());
            dataBlock.setDoubleValue(index++, fix.speed3D());
            dataBlock.setIntValue(index++, fix.fixType());
            dataBlock.setDoubleValue(index, fix.dop());

            gpsRecord.setData(dataBlock);
            publishData(GPS_OUTPUT_NAME);
        }

        if (droppedCount > 0) {
            logger.debug("Dropped {} of {} GPS fix(es) with no lock.", droppedCount, fixes.size());
        }
    }

    /**
     * Reads the packet bytes from the input port.
     *
     * @return The bytes of the packet, which is empty if the port carries no data.
     */
    private byte[] readPacketBytes() {
        DataBlock dataBlock = gpmfPacket.getData();

        if (dataBlock == null) {
            return new byte[0];
        }

        // The usual case: the connection handed over the byte array the upstream module published
        if (dataBlock.getUnderlyingObject() instanceof byte[] bytes && bytes.length == dataBlock.getAtomCount()) {
            return bytes;
        }

        // Otherwise the block is backed by something else, such as a wider primitive array or a view
        // into a larger block, and has to be read out value by value
        byte[] bytes = new byte[dataBlock.getAtomCount()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = dataBlock.getByteValue(i);
        }

        return bytes;
    }

    /**
     * Reads the altitude datum parameter, falling back to the default for a value that was never set or
     * is not one of the known datums.
     */
    private AltitudeDatum readAltitudeDatum() {
        DataBlock dataBlock = altitudeDatumParam.getData();
        String value = dataBlock != null ? dataBlock.getStringValue() : null;

        if (value == null || value.isEmpty()) {
            return AltitudeDatum.MEAN_SEA_LEVEL;
        }

        try {
            return AltitudeDatum.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown GPS altitude datum '{}', describing altitudes on {} instead.",
                    value, AltitudeDatum.MEAN_SEA_LEVEL);
            return AltitudeDatum.MEAN_SEA_LEVEL;
        }
    }

    /**
     * Describes the position output's altitudes on the given datum.
     * <p>
     * Only the coordinate reference system and the altitude field's semantics change; the record carries
     * the same three coordinates either way, so this leaves any existing connections valid. The camera's
     * readings are published unchanged regardless.
     *
     * @param datum The reference surface the camera measures altitude from.
     */
    private void applyAltitudeDatum(AltitudeDatum datum) {
        altitudeDatum = datum;

        if (datum == AltitudeDatum.MEAN_SEA_LEVEL) {
            location.setReferenceFrame(SWEConstants.REF_FRAME_WGS84_MSL);
            altitude.setDefinition(GeoPosHelper.DEF_ALTITUDE_MSL);
            altitude.setLabel("MSL Height");
            altitude.setDescription("Altitude above mean sea level (approximate datum)");
        } else {
            location.setReferenceFrame(SWEConstants.REF_FRAME_WGS84_HAE);
            altitude.setDefinition(GeoPosHelper.DEF_ALTITUDE_ELLIPSOID);
            altitude.setLabel("Ellipsoidal Height");
            altitude.setDescription("Altitude above WGS84 ellipsoid");
        }

        location.setDescription("Position of the camera, with altitude on the " + datum.getLabel() + " datum");
    }

    /**
     * Compares the altitude system the camera declared against the datum this process is configured for.
     * <p>
     * The record structure declares its coordinate reference system before any telemetry has arrived, so a
     * camera that turns out to disagree cannot be accommodated after the fact. Reporting the mismatch tells
     * the operator exactly what to change, rather than leaving the altitudes quietly mislabelled by the
     * local geoid offset.
     */
    private void checkAltitudeSystem(GpmfGpsFix fix) {
        if (altitudeSystemChecked) {
            return;
        }

        AltitudeDatum declared = AltitudeDatum.fromGpsAltitudeSystem(fix.altitudeSystem());
        if (declared == null) {
            // Older cameras declare nothing, which is what the configured datum is for
            altitudeSystemChecked = fix.altitudeSystem() != null;
            return;
        }

        altitudeSystemChecked = true;

        if (declared != altitudeDatum) {
            logger.warn("Camera reports altitude as '{}' ({}) but this process is configured for {}. " +
                            "Altitudes will be off by the local geoid offset until the altitude datum " +
                            "parameter is changed to {}.",
                    fix.altitudeSystem(), declared.getLabel(), altitudeDatum.getLabel(), declared);
        } else {
            logger.info("Camera reports altitude as '{}', matching the configured {} datum.",
                    fix.altitudeSystem(), altitudeDatum.getLabel());
        }
    }

    /**
     * Substitutes an empty string for a value the payload did not carry, since the telemetry record's text
     * fields are always written.
     */
    private static String orEmpty(String value) {
        return value != null ? value : "";
    }

    /**
     * @return The component carrying the undecoded GPMF packet.
     */
    public DataComponent getPacketInput() {
        return gpmfPacket;
    }
}