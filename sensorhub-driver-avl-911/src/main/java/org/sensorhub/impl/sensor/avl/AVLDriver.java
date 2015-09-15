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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.sensorml.v20.PhysicalSystem;
import org.sensorhub.api.comm.CommConfig;
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

	static final String SENSOR_UID_PREFIX = "urn:osh:sensors:avl:911:";
	static final String VEHICLE_UID_PREFIX = SENSOR_UID_PREFIX + "vehicle:";
    
    Set<String> foiIDs;
    Set<String> vehicleIDs;
    Map<String, AbstractFeature> vehicleFois;
    	
	ICommProvider<? super CommConfig> commProvider;
    AVLOutput dataInterface;
	
    
    public AVLDriver()
    {        
    }
    
    
    @Override
    public void init(AVLConfig config) throws SensorHubException
    {
        super.init(config);
        
        // create foi maps
        this.foiIDs = new LinkedHashSet<String>();
        this.vehicleIDs = new LinkedHashSet<String>();
        this.vehicleFois = new LinkedHashMap<String, AbstractFeature>();
        
        // init main data interface
        dataInterface = new AVLOutput(this);
        addOutput(dataInterface, false);
        dataInterface.init();
    }


    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescription)
        {
            super.updateSensorDescription();
            sensorDescription.setId("AVL-911");
            sensorDescription.setUniqueIdentifier(SENSOR_UID_PREFIX + "fleet");
            sensorDescription.setDescription("AVL data for emergency vehicle location and status");
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
        if (dataInterface != null)
            dataInterface.stop();
                    
        if (commProvider != null)
        {
            commProvider.stop();
            commProvider = null;
        }
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
        if (!vehicleIDs.contains(vehicleID))
        {
            String name = vehicleID;
            String uid = VEHICLE_UID_PREFIX + vehicleID;
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
            vehicleIDs.add(vehicleID);
            vehicleFois.put(uid, foi);
            
            // send event
            long now = System.currentTimeMillis();
            eventHandler.publishEvent(new FoiEvent(now, this, foi, recordTime));
            
            log.debug("New vehicle added as FOI: {}", uid);
        }
    }


    @Override
    public Collection<String> getEntityIDs()
    {
        return Collections.unmodifiableCollection(vehicleFois.keySet());
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
        return Collections.unmodifiableCollection(foiIDs);
    }

}
