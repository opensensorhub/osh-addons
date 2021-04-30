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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import net.opengis.sensorml.v20.PhysicalSystem;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.FoiEvent;
import org.sensorhub.api.data.IDataProducer;
import org.sensorhub.api.data.IMultiSourceDataProducer;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.utils.SWEDataUtils;
import org.vast.sensorML.SMLHelper;


/**
 * <p>
 * Driver implementation supporting the Automated Vehicle Location (AVL) data according 
 * to the Intergraph 911 System data format, although this should be able to be generalized.
 * </p>
 *
 * @author Mike Botts
 * @since September 10, 2015
 */
public class AVLDriver extends AbstractSensorModule<AVLConfig> implements IMultiSourceDataProducer
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
                commProvider = config.commSettings.getProvider();
                commProvider.start();
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
            String name = vehicleID;            
            String description = "Vehicle " + vehicleID + " from " + config.agencyName;            
            SMLHelper smlFac = new SMLHelper();
            
            // generate small SensorML for FOI (in this case the system is the FOI)
            PhysicalSystem foi = smlFac.newPhysicalSystem();
            foi.setId(vehicleID);
            foi.setUniqueIdentifier(foiUID);
            foi.setName(name);
            foi.setDescription(description);
            /*ContactList contacts = smlFac.newContactList();
            CIResponsibleParty contact = smlFac.newResponsibleParty();
            contact.setOrganisationName(config.agencyName);
            contact.setRole(new CodeListValueImpl("operator"));
            contacts.addContact(contact);
            foi.addContacts(contacts);*/
            
            // add to map
            foiMap.put(foiUID, foi);
            
            // send event
            long now = System.currentTimeMillis();
            eventHandler.publish(new FoiEvent(now, this, foi, SWEDataUtils.toInstant(recordTime)));
            
            getLogger().debug("New vehicle added as FOI: {}", foiUID);
        }
        
        return foiUID;
    }


    @Override
    public Collection<String> getProceduresWithFoi(String foiUID)
    {
        return Arrays.asList(this.uniqueID);
    }


    @Override
    public Map<String, ? extends IDataProducer> getMembers()
    {
        return Collections.emptyMap();
    }

}
