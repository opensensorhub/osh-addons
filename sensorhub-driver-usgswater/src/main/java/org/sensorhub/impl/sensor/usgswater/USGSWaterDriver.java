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

package org.sensorhub.impl.sensor.usgswater;

import net.opengis.gml.v32.AbstractFeature;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.sensorml.v20.PhysicalSystem;
import org.sensorhub.impl.module.RobustConnection;
import org.sensorhub.impl.persistence.FilterUtils;
import org.sensorhub.impl.persistence.FilteredIterator;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.usgs.water.ObsSiteLoader;
import org.sensorhub.impl.usgs.water.RecordStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IMultiSourceDataProducer;
import org.sensorhub.api.persistence.IFoiFilter;
import org.vast.sensorML.SMLHelper;


/**
 * <p>
 * Implementation of sensor interface for USGS Water Data using IP
 * protocol. This particular class stores configuration parameters.
 * </p>
 * 
 * @author Lee Butler <labutler10@gmail.com>
 * @since March 22, 2017
 */

public class USGSWaterDriver extends AbstractSensorModule <USGSWaterConfig> implements IMultiSourceDataProducer
{
	static final Logger log = LoggerFactory.getLogger(USGSWaterDriver.class);
    static final String BASE_USGS_URL = "https://waterservices.usgs.gov/nwis/";
    static final String UID_PREFIX = "urn:usgs:water:";
    
    Set<CountyCode> countyCode;
    CountyCode county;
    Map<String, AbstractFeature> siteFois = new LinkedHashMap<>();
    Map<String, PhysicalSystem> siteDesc;
    
    Map<String, RecordStore> dataStores = new LinkedHashMap<>();
    PhysicalSystem systemDesc;
    
    RobustConnection connection;
    USGSWaterOutput waterOut;
    
    public USGSWaterDriver()
    {
//    	this.countyCode = new LinkedHashSet<CountyCode>();
//	    this.siteFois = new LinkedHashMap<String, AbstractFeature>();
//	    this.siteDesc = new LinkedHashMap<String, PhysicalSystem>();
    }
    
    @Override
    public void setConfiguration(final USGSWaterConfig config) {
        super.setConfiguration(config);
    };
    
    @Override
    public void init() throws SensorHubException
    {
        // reset internal state in case init() was already called
        super.init();
        
        try {populateCountyCodes();}
        catch (IOException e) {e.printStackTrace();}
        
        loadFois();
        
//        for (Map.Entry<String, AbstractFeature> entry : siteFois.entrySet())
//        {
//            System.out.println(entry.getKey() + "/" + entry.getValue().getDescription());
//        }
        
        // generate identifiers
        this.uniqueID = "urn:usgs:water:network";
        this.xmlID = "USGS_WATER_DATA_NETWORK";
        
        this.waterOut = new USGSWaterOutput(this);
        addOutput(waterOut, false);
        waterOut.init();
    }
    
    public void populateCountyCodes() throws IOException
    {
    	countyCode = new LinkedHashSet<CountyCode>();
    	
        InputStream is = getClass().getResourceAsStream("CountyCodes.csv");
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String[] lineArr;
        String line;
        
        while ((line = br.readLine()) != null)
        {
        	county = new CountyCode();
        	
        	lineArr = line.split(",");
        	county.setTerritory(lineArr[0]);
        	county.setCountyCode((lineArr[1] + lineArr[2]));
        	county.setName(lineArr[3]);
        	countyCode.add(county);
        }
        br.close();
        isr.close();
        is.close();
    }

    @Override
    public void start() throws SensorHubException {
	    
    	SMLHelper sml = new SMLHelper();
	    
	    // generate station FOIs and full descriptions
	    for (Map.Entry<String, AbstractFeature> entry : siteFois.entrySet())
	    {
	    	String uid = UID_PREFIX + entry.getValue().getId();
	    	
			// generate full SensorML for sensor description
			PhysicalSystem sensorDesc = sml.newPhysicalSystem();
			sensorDesc.setId("STATION_" + entry.getValue().getId());
			sensorDesc.setUniqueIdentifier(uid);
			sensorDesc.setName(entry.getValue().getDescription());
			sensorDesc.setDescription(entry.getValue().getName());
			siteDesc.put(uid, sensorDesc);
        }
        
    	waterOut.start();
    }
    
    protected void loadFois() throws SensorHubException
    {
        // request and parse site info
        try
        {
            ObsSiteLoader parser = new ObsSiteLoader(this);
            parser.preloadSites(config.exposeFilter, siteFois);
        }
        catch (Exception e)
        {
            throw new SensorHubException("Error loading site information", e);
        }
    }

    @Override
    protected void updateSensorDescription()
    {
		synchronized (sensorDescLock)
		{
			super.updateSensorDescription();
			sensorDescription.setDescription("USGS water site network");
			
			// append href to all stations composing the network
			for (Map.Entry<String, AbstractFeature> entry : siteFois.entrySet())
			{
			    String name = "usgsSite-" + entry.getValue().getId();
			    String href = UID_PREFIX + "site:" + entry.getValue().getId();
			    ((PhysicalSystem)sensorDescription).getComponentList().add(name, href, null);
			}
		}
    }


    @Override
    public boolean isConnected() {
        if (connection == null)
            return false;

        return connection.isConnected();
    }


    @Override
    public void stop() {
        if (connection != null)
            connection.cancel();

        if (waterOut != null)
        	waterOut.stop();
    }


    @Override
    public void cleanup() {}
    
    @Override
    public Collection<String> getEntityIDs()
    {
        return Collections.unmodifiableCollection(siteFois.keySet());
    }
    
    
    @Override
    public AbstractFeature getCurrentFeatureOfInterest()
    {
        return null;
    }


    @Override
    public AbstractProcess getCurrentDescription(String entityID)
    {
        return siteDesc.get(entityID);
    }


    @Override
    public double getLastDescriptionUpdate(String entityID)
    {
        return 0;
    }


    @Override
    public AbstractFeature getCurrentFeatureOfInterest(String entityID)
    {
        return siteFois.get(entityID);
    }


    @Override
    public Collection<? extends AbstractFeature> getFeaturesOfInterest()
    {
        return Collections.unmodifiableCollection(siteFois.values());
    }


    @Override
    public Collection<String> getEntitiesWithFoi(String foiID)
    {
        return Arrays.asList(foiID);
    }

	@Override
	public Collection<String> getFeaturesOfInterestIDs() {
		// TODO Auto-generated method stub
		return null;
	}
}