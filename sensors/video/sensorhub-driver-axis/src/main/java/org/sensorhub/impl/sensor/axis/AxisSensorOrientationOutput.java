/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.axis;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEConstants;
import org.vast.swe.helper.GeoPosHelper;

public class AxisSensorOrientationOutput extends AbstractSensorOutput<AxisCameraDriver> {

    private static final String OUTPUT_NAME = "sensorOrientationPtz";

    DataRecord outputStruct;
    DataEncoding outputEncoding;

    public AxisSensorOrientationOutput(AxisCameraDriver driver, String sensorFrameID){
        super(OUTPUT_NAME, driver);

        GeoPosHelper fac = new GeoPosHelper();

        outputStruct = fac.createRecord()
            .name(OUTPUT_NAME)
            .label("Sensor Orientation")
            .description("Real-time orientation of the camera sensor computed from the current PTZ position. " +
                    "Heading and pitch are derived by applying pan and tilt offsets to the platform mount " +
                    "orientation. Updated each time the PTZ output polls the camera.")
            .addSamplingTimeIsoUTC("time")
            .addField("orientation", fac.createVector()
                    .from(fac.newEulerOrientationNED(SWEConstants.DEF_SENSOR_ORIENT))
                    .localFrame('#' + sensorFrameID))
                .build();

        outputEncoding = fac.newTextEncoding();
    }

    void updateOrientation(double heading, double pitch, double roll) {
        double time = System.currentTimeMillis() / 1000.0;

        DataBlock dataBlock = outputStruct.createDataBlock();
        dataBlock.setDoubleValue(0, time);
        dataBlock.setDoubleValue(1, heading);
        dataBlock.setDoubleValue(2, pitch);
        dataBlock.setDoubleValue(3, roll);

        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));
    }


    @Override
    public DataComponent getRecordDescription() {
        return outputStruct;
    }


    @Override
    public DataEncoding getRecommendedEncoding() {
        return outputEncoding;
    }


    @Override
    public double getAverageSamplingPeriod() {
        return 1.0;
    }



    
}