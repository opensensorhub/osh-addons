/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.flir.dh390;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.sensor.PositionConfig.EulerOrientation;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.data.TextEncodingImpl;
import org.vast.swe.SWEConstants;
import org.vast.swe.helper.GeoPosHelper;

/**
 * Sensor output for the FLIR DH-390 camera orientation.
 */
public class DH390OrientationOutput extends AbstractSensorOutput<DH390Driver> {
    private static final String SENSOR_OUTPUT_NAME = "sensorOrientation";

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    /**
     * Constructs a new sensor output for orientation data.
     */
    public DH390OrientationOutput(DH390Driver parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    /**
     * Initializes the output and its data structure.
     */
    public void doInit() {
        GeoPosHelper geoPosHelper = new GeoPosHelper();

        dataStruct = geoPosHelper.createRecord()
                .name(getName())
                .label("Sensor Orientation")
                .addSamplingTimeIsoUTC("time")
                .addField("orientation", geoPosHelper.createVector()
                        .from(geoPosHelper.newEulerOrientationNED(SWEConstants.DEF_SENSOR_ORIENT)))
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");
    }

    /**
     * Sets the orientation data.
     *
     * @param orientation The euler orientation to set.
     */
    public void setOrientation(EulerOrientation orientation) {
        dataBlock = (latestRecord == null) ? dataStruct.createDataBlock() : latestRecord.renew();

        latestRecordTime = System.currentTimeMillis() / 1000;

        dataBlock.setLongValue(0, latestRecordTime);
        dataBlock.setDoubleValue(1, orientation.heading);
        dataBlock.setDoubleValue(2, orientation.pitch);
        dataBlock.setDoubleValue(3, orientation.roll);

        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));
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
        return Double.NaN;
    }
}
