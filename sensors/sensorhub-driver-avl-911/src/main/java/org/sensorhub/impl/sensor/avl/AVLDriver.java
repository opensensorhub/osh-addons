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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.sensorml.v20.PhysicalSystem;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.FoiEvent;
import org.sensorhub.api.data.IMultiSourceDataProducer;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.sensorML.SMLHelper;



/**
 * <p>
 * Driver implementation supporting the Automated Vehicle Location (AVL) data according 
 * to the Intergraph 911 System data format, although this should be able to be generalized.
 * </p>
 *
 * @author Mike Botts <mike.botts@botts-inc.com>
 * @since September 10, 2015
 */
public class AVLDriver extends AbstractSensorModule<AVLConfig> implements IMultiSourceDataProducer
{
	static final Logger log = LoggerFactory.getLogger(AVLDriver.class);

	static final String SENSOR_UID_PREFIX = "urn:osh:sensor:avl:";
    
    Set<String> foiIDs;
    Map<String, AbstractFeature> vehicleFois;
    	
	ICommProvider<?> commProvider;
    AVLOutput dataInterface;
	
    
    public AVLDriver()
    {        
    }
    
    
    @Override
    public void init() throws SensorHubException
    {
        super.init();
        
        // generate IDs
        generateUniqueID(SENSOR_UID_PREFIX, config.fleetID);
        generateXmlID("AVL_", config.fleetID);            
        
        // create foi maps
        this.foiIDs = new LinkedHashSet<String>();
        this.vehicleFois = new LinkedHashMap<String, AbstractFeature>();
        
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
    public void start() throws SensorHubException
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
    public void stop() throws SensorHubException
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
    
    
    void addFoi(double recordTime, String vehicleID)
    {
        if (!vehicleFois.containsKey(vehicleID))
        {
            String name = vehicleID;
            String uid = SENSOR_UID_PREFIX + config.fleetID + ':' + vehicleID;
            String description = "Vehicle " + vehicleID + " from " + config.agencyName;
            
            SMLHelper smlFac = new SMLHelper();
            
            // generate small SensorML for FOI (in this case the system is the FOI)
            PhysicalSystem foi = smlFac.newPhysicalSystem();
            foi.setId(vehicleID);
            foi.setUniqueIdentifier(uid);
            foi.setName(name);
            foi.setDescription(description);
            /*ContactList contacts = smlFac.newContactList();
            CIResponsibleParty contact = smlFac.newResponsibleParty();
            contact.setOrganisationName(config.agencyName);
            contact.setRole(new CodeListValueImpl("operator"));
            contacts.addContact(contact);
            foi.addContacts(contacts);*/
            
            // update maps
            foiIDs.add(uid);
            vehicleFois.put(vehicleID, foi);
            
            // send event
            long now = System.currentTimeMillis();
            eventHandler.publishEvent(new FoiEvent(now, vehicleID, this, foi, recordTime));
            
            log.debug("New vehicle added as FOI: {}", uid);
        }
    }


    @Override
    public Collection<String> getEntityIDs()
    {
        return Collections.unmodifiableSet(vehicleFois.keySet());
    }
    
    
    @Override
    public AbstractFeature getCurrentFeatureOfInterest()
    {
        return null;
    }


    @Override
    public AbstractProcess getCurrentDescription(String entityID)
    {
        return null;
    }


    @Override
    public double getLastDescriptionUpdate(String entityID)
    {
        return 0;
    }


    @Override
    public AbstractFeature getCurrentFeatureOfInterest(String entityID)
    {
        return vehicleFois.get(entityID);
    }


    @Override
    public Collection<? extends AbstractFeature> getFeaturesOfInterest()
    {
        return Collections.unmodifiableCollection(vehicleFois.values());
    }
    
    
    @Override
    public Collection<String> getFeaturesOfInterestIDs()
    {
        return Collections.unmodifiableSet(foiIDs);
    }


    @Override
    public Collection<String> getEntitiesWithFoi(String foiID)
    {
        if (!foiIDs.contains(foiID))
            return Collections.EMPTY_SET;
        
        String entityID = foiID.substring(foiID.lastIndexOf(':')+1);
        return Arrays.asList(entityID);
    }

}
