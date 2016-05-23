/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.angel;

import org.sensorhub.impl.sensor.AbstractSensorOutput;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.vast.swe.SWEHelper;


public class ActivityOutput extends AbstractSensorOutput<AngelSensor>
{
    DataComponent dataStruct;
    DataEncoding dataEnc;
    
    
    public ActivityOutput(AngelSensor parentSensor)
    {
        super(parentSensor);
    }


    @Override
    public String getName()
    {
        return "activityData";
    }


    protected void init()
    {
        SWEHelper fac = new SWEHelper();
        
        // build SWE Common record structure
        dataStruct = fac.newDataRecord();
        dataStruct.setName(getName());
        
        // time stamp
        dataStruct.addComponent("time", fac.newTimeStampIsoUTC());
        
        // health measurement
        dataStruct.addComponent("stepCount", fac.newCount("http://sensorml.com/ont/swe/property/StepCount", "Step Count", "Step count since last reset"));        
        dataStruct.addComponent("fallDetect", fac.newBoolean("http://sensorml.com/ont/swe/property/FallDetection", "Fall Detection", null));        
                
        // also generate encoding definition as text block
        dataEnc = fac.newTextEncoding(",", "\n");        
    }


    @Override
    public double getAverageSamplingPeriod()
    {
    	return 1.0;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return dataStruct;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return dataEnc;
    }
}
