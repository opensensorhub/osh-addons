/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.adsb;

import net.opengis.gml.v32.Point;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

public class AirNavADSBOutput extends AbstractSensorOutput<AirNavADSBSensor> {
    private static final String SENSOR_OUTPUT_NAME = "adsbOutput";
    private static final String SENSOR_OUTPUT_LABEL = "ADS-B Aircraft";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Aircraft position and identification data decoded from ADS-B transmissions";

    private static final int AVERAGE_SAMPLING_PERIOD = 1;

    DataRecord dataStruct;
    DataEncoding dataEncoding;

    public AirNavADSBOutput(AirNavADSBSensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    protected void init() {
        GeoPosHelper sweFactory = new GeoPosHelper();

        dataStruct = sweFactory.createRecord()
                .name(SENSOR_OUTPUT_NAME)
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("sampleTime", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("Time of data collection"))
                .addField("icaoAddress", sweFactory.createText()
                        .label("ICAO Address")
                        .definition(SWEHelper.getPropertyUri("aero/ICAOAddress"))
                        .description("Unique 24-bit ICAO aircraft identifier expressed in hexadecimal"))
                .addField("callsign", sweFactory.createText()
                        .label("Callsign")
                        .definition(SWEHelper.getPropertyUri("aero/Callsign"))
                        .description("8 Character ATC callsign or flight number broadcast by the aircraft"))
                .addField("location", sweFactory.createLocationVectorLLA())
                .addField("groundSpeed", sweFactory.createQuantity()
                        .label("Ground Speed")
                        .definition(SWEHelper.getPropertyUri("aero/GroundSpeed"))
                        .description("Horizontal speed over the ground")
                        .uomCode("[kn_i]"))
                .addField("heading", sweFactory.createQuantity()
                        .label("Heading")
                        .definition(SWEHelper.getPropertyUri("aero/Heading"))
                        .description("Direction relative to true north")
                        .uom("deg"))
                .addField("verticalRate", sweFactory.createQuantity()
                        .label("Vertical Rate")
                        .definition(SWEHelper.getPropertyUri("aero/VerticalRate"))
                        .description("Rate of climb or descent")
                        .uom("ft/min"))
                .addField("squawk", sweFactory.createText()
                        .label("Squawk Code")
                        .definition(SWEHelper.getPropertyUri("aero/SquawkCode"))
                        .description("4-digit octal ATC transponder code"))
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");
    }

    protected synchronized void publishAircraftState(AircraftState state) {
        Point geometry = parentSensor.getGeometry(state.lat, state.lon);
        var foiUID = parentSensor.addFoi(state.icao, state.callsign, geometry);

        DataBlock dataBlock;
        if (latestRecord == null)
            dataBlock = dataStruct.createDataBlock();
        else
            dataBlock = latestRecord.renew();

        int idx = 0;
        dataBlock.setDoubleValue(idx++, System.currentTimeMillis() / 1000.0);
        dataBlock.setStringValue(idx++, state.icao);
        dataBlock.setStringValue(idx++, state.callsign != null ? state.callsign : "");
        dataBlock.setDoubleValue(idx++, state.lat);
        dataBlock.setDoubleValue(idx++, state.lon);
        dataBlock.setDoubleValue(idx++, Double.isNaN(state.altFt) ? 0.0 : state.altFt * 0.3048); // ft to meters
        dataBlock.setDoubleValue(idx++, Double.isNaN(state.groundSpeed) ? 0.0 : state.groundSpeed);
        dataBlock.setDoubleValue(idx++, Double.isNaN(state.heading) ? 0.0 : state.heading);
        dataBlock.setDoubleValue(idx++, Double.isNaN(state.verticalRate) ? 0.0 : state.verticalRate);
        dataBlock.setStringValue(idx++, state.squawk != null ? state.squawk : "");

        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publish(new DataEvent(latestRecordTime, this, foiUID, dataBlock));
    }

    public double getAverageSamplingPeriod() {
        return AVERAGE_SAMPLING_PERIOD;
    }

    @Override
    public DataComponent getRecordDescription() {
        return dataStruct;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }
}
