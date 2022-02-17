/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Botts Innovative Research, Inc. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.avl;

import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.vast.ogc.om.MovingFeature;
import org.vast.util.Asserts;


/**
 * <p>
 * Driver implementation supporting the Automated Vehicle Location (AVL) data according 
 * to the Intergraph 911 System data format, although this should be able to be generalized.
 * </p>
 *
 * @author Mike Botts
 * @since September 10, 2015
 */
public class AVLDriver extends AbstractSensorModule<AVLConfig>
{
	static final String UID_PREFIX = "urn:osh:sensor:avl:";
    	
	ICommProvider<?> commProvider;
    AVLOutput dataInterface;
	
    
    public AVLDriver()
    {        
    }
    
    
    @Override
    protected void doInit() throws SensorHubException
    {
        super.doInit();
        
        Asserts.checkNotNullOrEmpty(config.agencyName, "agencyName");
        Asserts.checkNotNullOrEmpty(config.fleetID, "fleetID");
        
        // generate IDs
        generateUniqueID(UID_PREFIX, config.fleetID);
        generateXmlID("AVL_", config.fleetID);
        
        // init main data interface
        dataInterface = new AVLOutput(this);
        addOutput(dataInterface, false);
        dataInterface.init();
    }


    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
            super.updateSensorDescription();
            sensorDescription.setDescription("AVL data for " + config.agencyName);
        }
    }
	

    @Override
    protected void doStart() throws SensorHubException
    {
        // init comm provider
        if (commProvider == null)
        {
            try
            {
                if (config.commSettings == null)
                    throw new SensorHubException("No communication settings specified");
                
                // start comm provider
                var moduleReg = getParentHub().getModuleRegistry();
                commProvider = (ICommProvider<?>)moduleReg.loadSubModule(config.commSettings, true);
                commProvider.start();
                if (commProvider.getCurrentError() != null)
                    throw (SensorHubException)commProvider.getCurrentError();
            }
            catch (Exception e)
            {
                commProvider = null;
                throw e;
            }
        }
        
        // start measurement stream
        dataInterface.start(commProvider);
    }
    

    @Override
    protected void doStop() throws SensorHubException
    {
        // stop comm provider
        if (commProvider != null)
        {
            commProvider.stop();
            commProvider = null;
        }
        
        // stop output interface
        if (dataInterface != null)
            dataInterface.stop();
    }
    

    @Override
    public void cleanup() throws SensorHubException
    {
       
    }
    
    
    @Override
    public boolean isConnected()
    {
        return (commProvider != null);
    }
    
    
    String addFoi(double recordTime, String vehicleID)
    {
        String foiUID = UID_PREFIX + config.fleetID + ':' + vehicleID;
        
        if (!foiMap.containsKey(foiUID))
        {
            // generate small SensorML for FOI (in this case the system is the FOI)
            MovingFeature foi = new MovingFeature();
            foi.setId(vehicleID);
            foi.setUniqueIdentifier(foiUID);
            foi.setName(vehicleID);
            foi.setDescription("Vehicle " + vehicleID + " from " + config.agencyName);
            
            // register it
            addFoi(foi);
            
            getLogger().debug("New vehicle added as FOI: {}", foiUID);
        }
        
        return foiUID;
    }

}
