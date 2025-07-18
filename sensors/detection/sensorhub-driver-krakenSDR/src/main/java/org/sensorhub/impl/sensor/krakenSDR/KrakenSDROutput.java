/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.krakenSDR;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEBuilders;
import org.vast.swe.SWEHelper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;


/**
 * Output specification and provider for {@link KrakenSDRSensor}.
 */
public class KrakenSDROutput extends AbstractSensorOutput<KrakenSDRSensor> {
    static final String SENSOR_OUTPUT_NAME = "krakenSDR";
    static final String SENSOR_OUTPUT_LABEL = "krakenSDR";
    static final String SENSOR_OUTPUT_DESCRIPTION = "This is the output for the krakenSDR";

    // myNote:
    // Added Variables because there were in other templates
    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private int setCount = 0;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();
    private long lastSetTimeMillis = System.currentTimeMillis();

    private static final Logger logger = LoggerFactory.getLogger(KrakenSDROutput.class);

    private DataRecord dataStruct;
    private DataEncoding dataEncoding;

    /**
     * Creates a new output for the sensor driver.
     *
     * @param parentSensor Sensor driver providing this output.
     */
    public KrakenSDROutput(KrakenSDRSensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering, and data types.
     */
    public void doInit() {
        logger.info("Initializing krakenSDR-Output");
        // Get an instance of SWE Factory suitable to build components
        SWEHelper sweFactory = new SWEHelper();

        // Create the data record description
        SWEBuilders.DataRecordBuilder recordBuilder = sweFactory.createRecord()
                .name(SENSOR_OUTPUT_NAME)
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("collectionTime", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("KrakenSDR Collection Time")
                        .description("Timestamp of when KrakenSDR reading was generated")
                        .definition(SWEHelper.getPropertyUri("collectionTime")))
                .addField("DoA", sweFactory.createQuantity()
                        .uom("deg")
                        .label("DoA")
                        .description("Max DoA Angle in Degrees in Comas Convention")
                        .definition(SWEHelper.getPropertyUri("DoA")))
                .addField("Confidence", sweFactory.createQuantity()
                        .label("Confidence")
                        .description("Confidence Value (0-99). The higher the better")
                        .definition(SWEHelper.getPropertyUri("Confidence")))
                .addField("RSSI_power", sweFactory.createQuantity()
                        .uom("dB")
                        .label("RSSI Power")
                        .description("0 dB is max power")
                        .definition(SWEHelper.getPropertyUri("RSSI_power")))
                .addField("frequency", sweFactory.createQuantity()
                        .uom("Hz")
                        .label("Channel Frequency")
                        .description("Channel Frequency in Hz")
                        .definition(SWEHelper.getPropertyUri("frequency")))
                .addField("antenna_arrangement", sweFactory.createText()
                        .label("Antenna Arrangement")
                        .description("Antenna Array Arrangement : (\"UCA\"/\"ULA\"/\"Custom\")")
                        .definition(SWEHelper.getPropertyUri("antenna_arrangement")))
                .addField("latency", sweFactory.createQuantity()
                        .uom("ms")
                        .label("Latency")
                        .description("Latency in ms : (Time from signal arrival at antenna, to result. NOT including network latency.)")
                        .definition(SWEHelper.getPropertyUri("latency")))
                .addField("krakenID", sweFactory.createText()
                        .label("Station ID")
                        .description("Name of the KrakenSDR station inputted in the Station Information box in the Web GUI")
                        .definition(SWEHelper.getPropertyUri("krakenID")))
                .addField("lat", sweFactory.createQuantity()
                        .label("Latitude")
                        .description("Latitude")
                        .definition(SWEHelper.getPropertyUri("lat")))
                .addField("lon", sweFactory.createQuantity()
                        .label("Longitude")
                        .description("Longitude")
                        .definition(SWEHelper.getPropertyUri("lon")))
                .addField("gps_heading", sweFactory.createText()
                        .label("GPS Heading")
                        .description("GPS Heading")
                        .definition(SWEHelper.getPropertyUri("gps_heading")))
                .addField("compass_heading", sweFactory.createText()
                        .label("Compass Heading")
                        .description("Compass Heading (if available)")
                        .definition(SWEHelper.getPropertyUri("compass_heading")))
                .addField("", sweFactory.createText()
                        .label("Main Heading Sensor Used")
                        .description("Main Heading Sensor Used (\"GPS\"/\"Compass\")")
                        .definition(SWEHelper.getPropertyUri("")))
                ;

        dataStruct = recordBuilder.build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");
    }

    @Override
    public DataComponent getRecordDescription() {
        return dataStruct;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {
        long accumulator = 0;
        synchronized (histogramLock) {
            for (int idx = 0; idx < MAX_NUM_TIMING_SAMPLES; ++idx) {
                accumulator += timingHistogram[idx];
            }
        }
        return accumulator / (double) MAX_NUM_TIMING_SAMPLES;
    }

    public void SetData(String DoA_csv) {
               DataBlock dataBlock;
        try {
            if (latestRecord == null) {
                dataBlock = dataStruct.createDataBlock();
            } else {
                dataBlock = latestRecord.renew();
            }
            synchronized (histogramLock) {
                int setIndex = setCount % MAX_NUM_TIMING_SAMPLES;
                // Get a sampling time for latest set based on previous set sampling time
                timingHistogram[setIndex] = System.currentTimeMillis() - lastSetTimeMillis;
                // Set latest sampling time to now
                lastSetTimeMillis = timingHistogram[setIndex];
            }
            ++setCount;

            String[] DOA_Array = DoA_csv.split(",");

            // Convert Unix Epoch time provided by GPS to OffsetDateTime
            Instant krakenTimeInstant = Instant.ofEpochSecond(Long.parseLong(DOA_Array[0]));
            OffsetDateTime odt = krakenTimeInstant.atOffset(ZoneOffset.UTC);

            dataBlock.setDateTime(0,odt);

            dataBlock.setDoubleValue(1, Double.parseDouble(DOA_Array[1]));
            dataBlock.setDoubleValue(2, Double.parseDouble(DOA_Array[2]));
            dataBlock.setDoubleValue(3, Double.parseDouble(DOA_Array[3]));
            dataBlock.setDoubleValue(4, Double.parseDouble(DOA_Array[4]));
            dataBlock.setStringValue(5, DOA_Array[5]);
            dataBlock.setDoubleValue(6, Double.parseDouble(DOA_Array[1]));
            dataBlock.setStringValue(7, DOA_Array[7]);
            dataBlock.setDoubleValue(8, Double.parseDouble(DOA_Array[8]));
            dataBlock.setDoubleValue(9, Double.parseDouble(DOA_Array[9]));
            dataBlock.setStringValue(10, DOA_Array[10]);
            dataBlock.setStringValue(11, DOA_Array[11]);
            dataBlock.setStringValue(12, DOA_Array[12]);

            latestRecord = dataBlock;
            latestRecordTime = System.currentTimeMillis();

            eventHandler.publish(new DataEvent(latestRecordTime, KrakenSDROutput.this, dataBlock));

        } catch (Exception e) {
            System.err.println("Error reading from krakenSDR: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
