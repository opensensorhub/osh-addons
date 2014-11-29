/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Sensia Software LLC. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.axis;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.common.CommandStatus;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.data.DataRecordImpl;


public class AxisPtzControl extends AbstractSensorControl<AxisCameraDriver>
{
	DataComponent commandData;
    
    
    protected AxisPtzControl(AxisCameraDriver driver)
    {
        super(driver);
    }
    
    
    @Override
    public String getName()
    {
        return commandData.getName();
    }
    
    
    protected void init()
    {
        // first check for taskable parameters (e.g. PTZ, zoom, etc.)
    	    	
    	
    	
        // build command message structure from IP queries
        this.commandData = new DataRecordImpl(6);
        commandData.setName("ptzControl");
        
    }


    @Override
    public DataComponent getCommandDescription()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public CommandStatus execCommand(DataBlock command) throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }

}
