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

package org.sensorhub.impl.sensor.trupulse;

import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.module.RobustConnection;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


/**
 * <p>
 * Driver implementation supporting the Laser Technology TruPulse 360 Laser Rangefinder.
 * The TruPulse 360 includes GeoSpatial Orientation ("azimuth"), as well as inclination
 * and direct distance. When combined with a sensor that measures GPS location of the 
 * TruPulse sensor, one can calculate the geospatial position of the target.
 * </p>
 *
 * @author Mike Botts
 * @author Alex Robin
 * @since June 8, 2015
 */
public class TruPulseSensor extends AbstractSensorModule<TruPulseConfig>
{
    static final Logger log = LoggerFactory.getLogger(TruPulseSensor.class);
    
    ICommProvider<?> commProvider;
    TruPulseOutput rangeOutput;
    RobustConnection connection;
    
    public TruPulseSensor()
    {        
    }
    
    
    @Override
    protected void doInit() throws SensorHubException
    {
        super.doInit();
        
        // generate identifiers: use serial number from config or first characters of local ID
        generateUniqueID("urn:lasertech:trupulse360:", config.serialNumber);
        generateXmlID("TRUPULSE_", config.serialNumber);
        
        // init main data interface
        rangeOutput = new TruPulseOutput(this);
        addOutput(rangeOutput, false);
        rangeOutput.init();
    }


    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
            super.updateSensorDescription();
            sensorDescription.setDescription("Laser range finder for determining distance, inclination, and azimuth");
        }
    }


    @Override
    protected void doStart() throws SensorHubException
    {
        // init comm provider
        if(commProvider == null){
            try{

                if(config.commSettings == null) log.error("No communication settings specified");

                connection = new RobustConnection(this, config.connection, "TruPulse Laser Range Finder") {
                    @Override
                    public boolean tryConnect() throws IOException {

                        try {
                            // start comm provider
                            var moduleReg = getParentHub().getModuleRegistry();
                            commProvider = (ICommProvider<?>)moduleReg.loadSubModule(config.commSettings, true);
                            commProvider.start();

                            return true;

                        } catch (SensorHubException e){
                            reportError("Cannot connect to TruPulse Laser Range Finder", e,  true);
                            return false;
                        }
                    }
                };

                connection.waitForConnection();
            }catch (SensorHubException e){
                commProvider = null;
                throw new SensorHubException("Cannot connect to TruPulse Laser Range Finder", e);
            }
        }
        
        // start measurement stream
        rangeOutput.start(commProvider);
    }
    

    @Override
    protected void doStop() throws SensorHubException
    {
        if (rangeOutput != null)
            rangeOutput.stop();
        
        if (commProvider != null)
        {
            commProvider.stop();
            commProvider = null;
        }

        if(connection != null) {
            connection.cancel();
            connection = null;
        }

    }


    @Override
    public void cleanup() throws SensorHubException
    {

    }

    
    @Override
    public boolean isConnected()
    {
        if(connection == null) return false;

        return connection.isConnected();
    }
}