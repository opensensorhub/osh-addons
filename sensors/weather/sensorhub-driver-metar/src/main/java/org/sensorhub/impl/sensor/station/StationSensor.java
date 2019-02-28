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

package org.sensorhub.impl.sensor.station;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;


/**
 * <p>
 * </p>
 *
 * @author Tony Cook
 * @since Dec 12, 2014
 */
public class StationSensor extends AbstractSensorModule<StationConfig>
{
    StationOutput dataInterface;
//    StationDataPoller stationDataPoller;
    
    public StationSensor()
    {
    }
    
    
    @Override
    public void init() throws SensorHubException
    {
        super.init();
        
        // generate IDs
        this.uniqueID = "urn:osh:sensor:metar:network";
        this.xmlID = "METAR_NETWORK";
        
        dataInterface = new StationOutput(this);
        addOutput(dataInterface, false);
        dataInterface.init();
    }
    
    
    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
            super.updateSensorDescription();
            sensorDescription.setDescription("Generic weather station");
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
