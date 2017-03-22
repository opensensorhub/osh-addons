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
import net.opengis.gml.v32.Point;
import net.opengis.gml.v32.impl.GMLFactory;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.sensorml.v20.IdentifierList;
import net.opengis.sensorml.v20.PhysicalSystem;
import net.opengis.sensorml.v20.Term;
import org.sensorhub.impl.module.RobustConnection;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IMultiSourceDataProducer;
import org.vast.sensorML.SMLFactory;
import org.vast.sensorML.SMLHelper;
import org.vast.swe.SWEHelper;


/**
 * <p>
 * Implementation of sensor interface for USGS Water Data using IP
 * protocol. This particular class stores configuration parameters.
 * </p>
 * 
 * @author Lee Butler <labutler10@gmail.com>
 * @since October 30, 2014
 */

public class USGSWaterDriver extends AbstractSensorModule <USGSWaterConfig> implements IMultiSourceDataProducer
{
	static final Logger log = LoggerFactory.getLogger(USGSWaterDriver.class);
    static final String SENSOR_UID_PREFIX = "urn:osh:usgs:water:";
    static final String SITE_UID_PREFIX = SENSOR_UID_PREFIX + "site:";
    
    Set<String> foiIDs;
    Map<String, PhysicalSystem> siteFois;
    Map<String, PhysicalSystem> siteDesc;
    
    RobustConnection connection;
    USGSWaterOutput waterOut;
    
    List<String> jsonURLs = new ArrayList<String>();
    
    String jsonURL;
    String serialNumber;
    String modelNumber;
    String longName;
    String shortName;
    
    public USGSWaterDriver()
    {
	    this.foiIDs = new LinkedHashSet<String>();
	    this.siteFois = new LinkedHashMap<String, PhysicalSystem>();
	    this.siteDesc = new LinkedHashMap<String, PhysicalSystem>();
    }
    
    @Override
    public void setConfiguration(final USGSWaterConfig config) {
        super.setConfiguration(config);
    };


    @Override
    public void init() throws SensorHubException {
        // reset internal state in case init() was already called
        super.init();
        
        // generate identifiers
        this.uniqueID = "urn:usgs:water:network";
        this.xmlID = "USGS_WATER_DATA_NETWORK";
        
        // init main data interface
        this.waterOut = new USGSWaterOutput(this);
        addOutput(waterOut, false);
        
        //jsonURL = config.getUrlBase() + "&sites=" + config.getSiteCode() + "&parameterCd=00060,00065";
        
        waterOut.init();
    }

    @Override
    public void start() throws SensorHubException {
	    SMLHelper smlFac = new SMLHelper();
	    GMLFactory gmlFac = new GMLFactory(true);
	    
	    //MetarStationMap map = null;
	    //try {
		//	map = MetarStationMap.getInstance();
		//} catch (IOException e) {
		//	throw new SensorHubException("IO Exception trying to load metarStationMap");
		//}
	    // generate station FOIs and full descriptions
	    for (String siteCode: config.siteCodes)
        {
	        String uid = SITE_UID_PREFIX + siteCode;
	        String description = "USGS Water Data Site: " + siteCode;
	        
	        // generate small SensorML for FOI (in this case the system is the FOI)
	        //Station station = map.getStation(stationID);
	        PhysicalSystem foi = smlFac.newPhysicalSystem();
	        foi.setId(siteCode);
	        foi.setUniqueIdentifier(uid);
	        //foi.setName(station.getName());
	        foi.setDescription(description);

	        Point stationLoc = gmlFac.newPoint();
	        //stationLoc.setPos(new double[] {station.getLat(), station.getLon(), station.getElevation()});
	        foi.setLocation(stationLoc);
	        siteFois.put(uid, foi);
	        foiIDs.add(uid);
	        
	        // TODO generate full SensorML for sensor description
	        PhysicalSystem sensorDesc = smlFac.newPhysicalSystem();
	        sensorDesc.setId("STATION_" + siteCode);
	        sensorDesc.setUniqueIdentifier(uid);
            //sensorDesc.setName(station.getName());
            sensorDesc.setDescription(description);
            siteDesc.put(uid, sensorDesc);
        }
    	waterOut.start();
    }


    @Override
    protected void updateSensorDescription()
    {
		synchronized (sensorDescLock)
		{
			super.updateSensorDescription();
			sensorDescription.setDescription("METAR weather station network");
			
			// append href to all stations composing the network
			for (String stationID: config.siteCodes)
			{
			    String name = "station" + stationID;
			    String href = SITE_UID_PREFIX + stationID;
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
    public Collection<String> getFeaturesOfInterestIDs()
    {
        return Collections.unmodifiableCollection(foiIDs);
    }


    @Override
    public Collection<String> getEntitiesWithFoi(String foiID)
    {
        return Arrays.asList(foiID);
    }
}