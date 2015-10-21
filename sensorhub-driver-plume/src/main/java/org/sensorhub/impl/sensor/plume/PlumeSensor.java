/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.plume;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;


/**
 * <p>
 * </p>
 *
 * @author Tony Cook
 * @since Sep 22, 2015
 */
public class PlumeSensor extends AbstractSensorModule<PlumeConfig>
{
    PlumeOutput dataInterface;
    static final String PLUME_UID = "urn:test:sensors:model:plume";
    
    public PlumeSensor()
    {
    }
    
    @Override
	public void init(PlumeConfig config) throws SensorHubException
    {
        super.init(config);
        
        this.dataInterface = new PlumeOutput(this);        
        addOutput(dataInterface, false);
        dataInterface.init();        
    }
    
    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescription)
        {
            super.updateSensorDescription();
            sensorDescription.setId("PLUME_MODEL");
            sensorDescription.setUniqueIdentifier(PLUME_UID);
            sensorDescription.setDescription("Sensor representing output from Lagrangian Plume Model");
        }
    }


    @Override
    public void start() throws SensorHubException
    {
        dataInterface.start();        
    }
    

    @Override
    public void stop() throws SensorHubException
    {
        dataInterface.stop();
    }
    

    @Override
    public void cleanup() throws SensorHubException
    {
       
    }
    
    
    @Override
    public boolean isConnected()
    {
        return true;
    }
}
