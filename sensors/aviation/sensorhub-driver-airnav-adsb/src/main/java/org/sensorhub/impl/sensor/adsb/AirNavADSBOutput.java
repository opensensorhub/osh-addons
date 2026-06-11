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

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.utils.aero.AeroHelper;
import org.vast.swe.SWEHelper;

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
        AeroHelper aeroHelper = new AeroHelper();

        dataStruct = aeroHelper.createRecord()
                .name(SENSOR_OUTPUT_NAME)
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("sampleTime", aeroHelper.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("Time of data collection"))
                .addField("icaoAddress", aeroHelper.createText()
                        .label("ICAO Address")
                        .definition(SWEHelper.getPropertyUri("IcaoAddress"))
                        .description("24-bit ICAO hex transponder address uniquely identifying the aircraft"))
                .addField("callsign", aeroHelper.createCallSign())
                .addField("pos", aeroHelper.createAircraftLocation())
                .addField("alt_baro", aeroHelper.createBaroAlt())
//                .addField("alt_geo", aeroHelper.createGeomAlt())
                .addField("gs", aeroHelper.createGroundSpeed())
                .addField("track", aeroHelper.createTrueTrack())
                .addField("alt_rate", aeroHelper.createVerticalRate())
                .addField("squawk", aeroHelper.createText()
                        .label("Squawk Code")
                        .definition(SWEHelper.getPropertyUri( "SquawkCode"))
                        .description("4-digit octal ATC transponder code"))
                .addField("alert", aeroHelper.createBoolean()
                        .label("Alert Flag")
                        .definition(SWEHelper.getPropertyUri("AlertFlag"))
                        .description("Indicates squawk code has changed"))
                .addField("emergency", aeroHelper.createBoolean()
                        .label("Emergency Flag")
                        .definition(SWEHelper.getPropertyUri("EmergencyFlag"))
                        .description("Indicates aircraft is in emergency status"))
                .addField("isOnGround", aeroHelper.createBoolean()
                        .label("Is On Ground")
                        .definition(SWEHelper.getPropertyUri("IsOnGround"))
                        .description("Indicates aircraft is on the ground"))
                .build();

        dataEncoding = aeroHelper.newTextEncoding(",", "\n");
    }

    protected synchronized void publishAircraftState(AircraftState state) {
        var foiUID = parentSensor.addFoi(state.icao, state.callsign);

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
        dataBlock.setDoubleValue(idx++, state.altFt);
//        dataBlock.setDoubleValue(idx++, Double.NaN);  // alt_geo not available from SBS
        dataBlock.setDoubleValue(idx++, state.groundSpeed);
        dataBlock.setDoubleValue(idx++, state.track);
        dataBlock.setDoubleValue(idx++, state.verticalRate);
        dataBlock.setStringValue(idx++, state.squawk != null ? state.squawk : "");
        dataBlock.setBooleanValue(idx++, state.alert);
        dataBlock.setBooleanValue(idx++, state.emergency);
        dataBlock.setBooleanValue(idx++, state.isOnGround);

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