/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.ffmpeg.outputs;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.TextEncodingImpl;
import org.vast.swe.helper.GeoPosHelper;

public class LocationOutput<FFMPEGConfig> extends AbstractSensorOutput<FFMPEGSensor> {
    private static final String SENSOR_OUTPUT_NAME = "Location";

    private static final Logger logger = LoggerFactory.getLogger(LocationOutput.class.getSimpleName());

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    public LocationOutput(FFMPEGSensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    protected void init() {
        GeoPosHelper geoPosHelper = new GeoPosHelper();

        dataStruct = geoPosHelper.createRecord()
                .name(getName())
                .label("Location")
                .definition(GeoPosHelper.getPropertyUri("location-output"))
                .addField("Sampling Time", geoPosHelper.createTime()
                        .asSamplingTimeIsoUTC()
                        .name("time")
                        .description("time stamp: when the message was received"))
                .addField("Sensor Location", geoPosHelper.createLocationVectorLLA())
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");
    }

    public void setLocation(LLALocation gpsLocation) {
        if (latestRecord == null) {
            dataBlock = dataStruct.createDataBlock();
        } else {
            dataBlock = latestRecord.renew();
        }

        latestRecordTime = System.currentTimeMillis() / 1000;

        dataBlock.setLongValue(0, latestRecordTime);
        dataBlock.setDoubleValue(1, gpsLocation.lat);
        dataBlock.setDoubleValue(2, gpsLocation.lon);
        dataBlock.setDoubleValue(3, gpsLocation.alt);

        eventHandler.publish(new DataEvent(latestRecordTime, LocationOutput.this, dataBlock));
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
        return 0;
    }
}
