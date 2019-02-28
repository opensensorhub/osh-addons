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
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.usgs.water.ObsSiteLoader;
import org.sensorhub.impl.usgs.water.RecordStore;
import org.sensorhub.impl.usgs.water.CodeEnums.ObsParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IMultiSourceDataProducer;
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
	static final String BASE_URL = "https://waterservices.usgs.gov/nwis/iv?sites=";
    static final String UID_PREFIX = "urn:usgs:water:";
    
    final int urlChunkSize = 100;
    Timer timer;
    BufferedReader reader;
    Set<CountyCode> countyCode;
    CountyCode county;
    List<String> qualCodes = new ArrayList<String>();
    
    Set<String> foiIDs;
    Map<String, AbstractFeature> siteFois = new LinkedHashMap<>();
    Map<String, PhysicalSystem> siteDesc = new LinkedHashMap<>();
    Map<String, RecordStore> dataStores = new LinkedHashMap<>();
    
    
    List<USGSDataRecord> waterTempRecList = new ArrayList<USGSDataRecord>();
    List<USGSDataRecord> dischargeRecList = new ArrayList<USGSDataRecord>();
    List<USGSDataRecord> gageHeightRecList = new ArrayList<USGSDataRecord>();
    List<USGSDataRecord> conductanceRecList = new ArrayList<USGSDataRecord>();
    List<USGSDataRecord> oxyRecList = new ArrayList<USGSDataRecord>();
    List<USGSDataRecord> pHRecList = new ArrayList<USGSDataRecord>();
    List<USGSDataRecord> allRecList = new ArrayList<USGSDataRecord>();
    
    WaterTempCelsiusOutput tempCelOut;
    DischargeOutput dischargeOut;
    GageHeightOutput gageHeightOut;
    SpecificConductanceOutput conductanceOut;
    DissolvedOxygenOutput oxyOut;
    PhOutput pHOut;
    AllWaterOutput allOut;
    
    public USGSWaterDriver()
    {
    	this.foiIDs = new LinkedHashSet<String>();
	    this.siteFois = new LinkedHashMap<String, AbstractFeature>();
	    this.siteDesc = new LinkedHashMap<String, PhysicalSystem>();
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
        
    	this.countyCode = new LinkedHashSet<CountyCode>();

//        try {populateCountyCodes();}
//        catch (IOException e) {e.printStackTrace();}
        
        loadFois();
        
//        for (Map.Entry<String, AbstractFeature> entry : siteFois.entrySet())
//        {
//            System.out.println(entry.getKey() + "/" + entry.getValue().getDescription());
//        }
        
        loadQualCodes();
        // generate identifiers
        this.uniqueID = "urn:usgs:water:live";
        this.xmlID = "USGS_WATER_DATA_NETWORK";
        
        if (config.exposeFilter.parameters.contains(ObsParam.WATER_TEMP))
        {
        	this.tempCelOut = new WaterTempCelsiusOutput(this);
        	addOutput(tempCelOut, false);
        	tempCelOut.init();
        }
        
        
        if (config.exposeFilter.parameters.contains(ObsParam.DISCHARGE))
        {
        	this.dischargeOut = new DischargeOutput(this);
        	addOutput(dischargeOut, false);
        	dischargeOut.init();
        }
        
        if (config.exposeFilter.parameters.contains(ObsParam.GAGE_HEIGHT))
        {
        	this.gageHeightOut = new GageHeightOutput(this);
        	addOutput(gageHeightOut, false);
        	gageHeightOut.init();
        }
        
        if (config.exposeFilter.parameters.contains(ObsParam.CONDUCTANCE))
        {
        	this.conductanceOut = new SpecificConductanceOutput(this);
        	addOutput(conductanceOut, false);
        	conductanceOut.init();
        }
        
        if (config.exposeFilter.parameters.contains(ObsParam.OXY))
        {
        	this.oxyOut = new DissolvedOxygenOutput(this);
        	addOutput(oxyOut, false);
        	oxyOut.init();
        }
        
        if (config.exposeFilter.parameters.contains(ObsParam.PH))
        {
        	this.pHOut = new PhOutput(this);
        	addOutput(pHOut, false);
        	pHOut.init();
        }
        // COMMENT OUT FOR NOW
//        if (config.exposeFilter.parameters.isEmpty())
//        {
//        	this.allOut = new AllWaterOutput(this);
//        	addOutput(allOut, false);
//        	allOut.init();
//        }
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
    public void start() throws SensorHubException
    {
	    
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
			foiIDs.add(uid);
			
			log.debug("New site added as FOI: {}", uid);
        }
	    
    	//sender = new ObsSender(getRecordDescription());
    	if (timer != null)
            return;
    	timer = new Timer();
        
    	TimerTask timerTask = new TimerTask()
    	{
			public void run()
    		{	
		    	waterTempRecList.clear(); dischargeRecList.clear();
		    	gageHeightRecList.clear(); conductanceRecList.clear();
		    	oxyRecList.clear(); pHRecList.clear();
		    	allRecList.clear();
				String[] requestUrl = buildIvRequest(siteFois);
				
				try
				{	
					for (int i=0; i<requestUrl.length; i++)
						sendRequests(requestUrl[i], siteFois); // Pass url array and fois map to sendRequests()

					if (!waterTempRecList.isEmpty()) // water temp record list only initialized if listed as desired parameter in expose filter
					{					
						sortList(waterTempRecList);
//						System.out.println("Total Num Temp Recs: " + waterTempRecList.size());
//						
//						for (USGSDataRecord r : waterTempRecList)
//							System.out.println("[" + r.getDataType() + "," + r.getTimeStamp() + "," + r.getSiteCode() + "," + r.getSiteLat() + "," + r.getSiteLon() + "," + r.getDataValue() + "]");
//						System.out.println();
						
						// send to output publishData()
						tempCelOut.publishData(waterTempRecList);
						waterTempRecList.clear();
					}
					
					if (!dischargeRecList.isEmpty())
					{
						sortList(dischargeRecList);
//						System.out.println("Total Num Discharge Recs: " + dischargeRecList.size());
//						for (USGSDataRecord r : dischargeRecList)
//							System.out.println("[" + r.getDataType() + "," + r.getTimeStamp() + "," + r.getSiteCode() + "," + r.getSiteLat() + "," + r.getSiteLon() + "," + r.getDataValue() + "]");
//						System.out.println();
						// send to output publishData()
						dischargeOut.publishData(dischargeRecList);
						dischargeRecList.clear();
					}
					
					if (!gageHeightRecList.isEmpty())
					{
						sortList(gageHeightRecList);
//						System.out.println("Total Num Gage Height Recs: " + gageHeightRecList.size());
//						for (USGSDataRecord r : gageHeightRecList)
//							System.out.println("[" + r.getDataType() + "," + r.getTimeStamp() + "," + r.getSiteCode() + "," + r.getSiteLat() + "," + r.getSiteLon() + "," + r.getDataValue() + "]");
//						System.out.println();
						// send to output publishData()
						gageHeightOut.publishData(gageHeightRecList);
						gageHeightRecList.clear();
					}
					
					if (!conductanceRecList.isEmpty())
					{
						sortList(conductanceRecList);
//						System.out.println("Total Num Cond Recs: " + conductanceRecList.size());
//						for (USGSDataRecord r : conductanceRecList)
//							System.out.println("[" + r.getDataType() + "," + r.getTimeStamp() + "," + r.getSiteCode() + "," + r.getSiteLat() + "," + r.getSiteLon() + "," + r.getDataValue() + "]");
//						System.out.println();
						// send to output publishData()
						conductanceOut.publishData(conductanceRecList);
						conductanceRecList.clear();
					}
					
					if (!oxyRecList.isEmpty())
					{
						sortList(oxyRecList);
//						System.out.println("Total Num Oxy Recs: " + oxyRecList.size());
//						for (USGSDataRecord r : oxyRecList)
//							System.out.println("[" + r.getDataType() + "," + r.getTimeStamp() + "," + r.getSiteCode() + "," + r.getSiteLat() + "," + r.getSiteLon() + "," + r.getDataValue() + "]");
//						System.out.println();
						// send to output publishData()
						oxyOut.publishData(oxyRecList);
						oxyRecList.clear();
					}
					
					if (!pHRecList.isEmpty())
					{
						sortList(pHRecList);
//						System.out.println("Total Num pH Recs: " + pHRecList.size());
//						for (USGSDataRecord r : pHRecList)
//							System.out.println("[" + r.getDataType() + "," + r.getTimeStamp() + "," + r.getSiteCode() + "," + r.getSiteLat() + "," + r.getSiteLon() + "," + r.getDataValue() + "]");
//						System.out.println();
						// send to output publishData()
						pHOut.publishData(pHRecList);
						pHRecList.clear();
					}
					
					if (!allRecList.isEmpty())
					{
						sortList(allRecList);
//						System.out.println("Total Num Recs: " + allRecList.size());
//						for (USGSDataRecord r : allRecList)
//							System.out.println("[" + r.getDataType() + "," + r.getTimeStamp() + "," + r.getSiteCode() + "," + r.getSiteLat() + "," + r.getSiteLon() + "," + r.getDataValue() + "]");
//						System.out.println();
						// send to output publishData()
//						allOut.publishData(allRecList); // COMMENT OUT FOR NOW
						allRecList.clear();
					}
					
				}
				catch (IOException e1)
				{
					e1.printStackTrace();
				}
			}
		};
		
		timer.scheduleAtFixedRate(timerTask, 0, (long)(getAverageSamplingPeriod()*1000));
    }
    
    // function to sort list of data records by time
    public void sortList(List<USGSDataRecord> dataList)
    {
		Collections.sort(dataList, new Comparator<USGSDataRecord>() {
			public int compare(USGSDataRecord o1, USGSDataRecord o2)
			{
				return (int) Long.compare(o1.getTimeStamp(), o2.getTimeStamp());
			}
		});
    }
    
    public String[] buildIvRequest(Map<String, AbstractFeature> fois)
    {
    	String[] idArr = new String[fois.size()];
    	int idIt = 0;
        for (Map.Entry<String, AbstractFeature> entry : fois.entrySet())
        {
        	idArr[idIt] = entry.getValue().getId();
        	idIt++;
        }
    	
    	StringBuilder buf = new StringBuilder(BASE_URL);
    	int bufStartLen = buf.length();
    	
    	int numUrl = (int)Math.ceil((double)fois.size()/urlChunkSize);
    	int maxNumSites = (int)Math.ceil((double)fois.size()/numUrl);
    	String[] urlArr = new String[numUrl];
    	
    	int idMarker = 0;
    	for (int i=0; i<numUrl; i++)
    	{
    		for (int k=0; k<maxNumSites; k++)
    		{
    			if (idMarker <= (idArr.length-1))
    			{
    				buf.append(idArr[idMarker]).append(',');
				
					if (idMarker == (idArr.length-1))
						break;
					
					if (k!=(maxNumSites-1))
						idMarker++;
    			}
    		}
			buf.setCharAt(buf.length()-1, '&');
			buf.append("format=rdb"); // output format
			idMarker++;
			
    		urlArr[i] = buf.toString();
    		buf.delete(bufStartLen, buf.length());
    	}
		return urlArr;	
    }
    
    public void sendRequests(String requestUrl, Map<String, AbstractFeature> fois) throws IOException
    {
    	AbstractFeature foiValue;
    	Set<ObsParam> params = config.exposeFilter.parameters;
    	
    	String siteId, point;
    	boolean siteSet, timeSet;
    	String[] pointArr, locArr;
    	long ts;
    	double siteLat, siteLon;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm z");
        StringBuilder buf = new StringBuilder();
    	Float f;
    	
    	URL url = new URL(requestUrl);
    	USGSWaterDriver.log.debug("Requesting observations from: " + url);
    	reader = new BufferedReader(new InputStreamReader(url.openStream()));
    	
    	String line;
        while ((line = reader.readLine()) != null)
        {                
            line = line.trim();
            
            // parse section header when data for next site begins
            if (line.startsWith("#"))
            	continue;
	        
	        // Get data field names
	        String[] fields = line.split("\t");
	        
	//        for (int s=0; s<fields.length; s++)
	//        	System.out.println("fields[s]: " + fields[s]);
	        
	        // Skip middle line in data section
	        reader.readLine();
	        
	        // Loop to get all lines with data values
	        // There are sometimes more than one
	        while ((line = reader.readLine()) != null)
	        {
		        // Now get line with data values
	        	line = line.trim();
		        
	        	// if line is not a data line, exit this loop
	        	// and go to main loop to start next section
		    	if (line.startsWith("#"))
		    		break;
		        
		        // And separate them, put them into array
		        String[] data = line.split("\t");
		        
//		        for (int d=0; d<data.length; d++)
//		        	System.out.println("data[d]: " + data[d]);
		        
		        point = null;
		        buf.setLength(0); // Clear string builder for datetime
		        siteId = "unreported"; // Initialize siteId in case it isn't reported
		        ts = 0; // Initialize time variable

		        // Initialize Site Lat/Lon
		        siteLat = Double.NaN;
		        siteLon = Double.NaN;
		        
		        f = Float.NaN; // Initialize float value to NaN
		        
		        // Flags for site & time to avoid redundant settings
		        siteSet = false;
		        timeSet = false;
		        
		    	for (ObsParam param: params) // Loop over list of observation types requested in config
		    	{
		    		// Loop over data array; size should be <= size of fields[]
		    		for (int i=0; i<data.length; i++)
		    		{
		    			if ("site_no".equalsIgnoreCase(fields[i]) && !siteSet)
		    			{
		    				siteId = data[i];
		    				// Get location of this site
		    				foiValue = fois.get(siteId);
		    				pointArr = foiValue.getLocation().toString().split("\\(");
		    				point = pointArr[1].substring(0, pointArr[1].length()-1);
		    				locArr = point.split("\\s+");
		    				
		    				siteLat = Double.parseDouble(locArr[0]);
		    				siteLon = Double.parseDouble(locArr[1]);
		    				siteSet = true;
		    			}
		    			
		    			if ("datetime".equalsIgnoreCase(fields[i]) && !timeSet)
		    			{
		    				buf.append(data[i].trim());
		    				buf.append(' ');
		    				buf.append(data[i+1].trim());
		    				
		    				try
		    				{
								ts = dateFormat.parse(buf.toString()).getTime();
								timeSet = true;
							}
		    				catch (ParseException e)
		    				{
		    					throw new IOException("Invalid time stamp " + buf.toString());
							}
		    			}
		    				
		    			if (fields[i].endsWith(param.getCode()) && !(data[i].isEmpty()) && !qualCodes.contains(data[i]))
		    			{
		    				f = Float.parseFloat(data[i]);
		    				
		    				if (!f.isNaN())
		    				{
			    				switch (param)
			    				{
			    				case WATER_TEMP:
			    					// add to water temp record list
			    					waterTempRecList.add(new USGSDataRecord(ObsParam.WATER_TEMP, ts, siteId, siteLat, siteLon, f));
			    					// add to all record list
			    					allRecList.add(new USGSDataRecord(ObsParam.WATER_TEMP, ts, siteId, siteLat, siteLon, f));
			    					break;
			    				
			    				case DISCHARGE:
			    					// add to discharge record list
			    					dischargeRecList.add(new USGSDataRecord(ObsParam.DISCHARGE, ts, siteId, siteLat, siteLon, f));
			    					// add to all record list
			    					allRecList.add(new USGSDataRecord(ObsParam.DISCHARGE, ts, siteId, siteLat, siteLon, f));
			    					break;
			    					
			    				case GAGE_HEIGHT:
			    					// add to gage height record list
			    					gageHeightRecList.add(new USGSDataRecord(ObsParam.GAGE_HEIGHT, ts, siteId, siteLat, siteLon, f));
			    					// add to all record list
			    					allRecList.add(new USGSDataRecord(ObsParam.GAGE_HEIGHT, ts, siteId, siteLat, siteLon, f));
			    					break;
			    					
			    				case CONDUCTANCE:
			    					// add to specific conductance record list
			    					conductanceRecList.add(new USGSDataRecord(ObsParam.CONDUCTANCE, ts, siteId, siteLat, siteLon, f));
			    					// add to all record list
			    					allRecList.add(new USGSDataRecord(ObsParam.CONDUCTANCE, ts, siteId, siteLat, siteLon, f));
			    					break;
			    					
			    				case OXY:
			    					// add to dissolved oxygen record list
			    					oxyRecList.add(new USGSDataRecord(ObsParam.OXY, ts, siteId, siteLat, siteLon, f));
			    					// add to all record list
			    					allRecList.add(new USGSDataRecord(ObsParam.OXY, ts, siteId, siteLat, siteLon, f));
			    					break;
			    					
			    				case PH:
			    					// add to water pH record list
			    					pHRecList.add(new USGSDataRecord(ObsParam.PH, ts, siteId, siteLat, siteLon, f));
			    					// add to all record list
			    					allRecList.add(new USGSDataRecord(ObsParam.PH, ts, siteId, siteLat, siteLon, f));
			    					break;
			    				}
		    				}
		    			}
		    		}
		    	}
	        }
        }
    }
    
    private int getAverageSamplingPeriod() {
		return 5*60;
//		return 30;
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
	
    private void loadQualCodes()
    {
    	// Populate list of qualification codes
    	// Codes found at https://waterdata.usgs.gov/nwis?codes_help
    	
    	qualCodes.add("Ssn"); // Parameter monitored seasonally
    	qualCodes.add("Bkw"); // Flow affected by backwater
    	qualCodes.add("Ice"); // Ice affected
    	qualCodes.add("Pr");  // Partial-record site
    	qualCodes.add("Rat"); // Rating being developed or revised
    	qualCodes.add("Eqp"); // Eqp = equipment malfunction
    	qualCodes.add("Fld"); // Flood damage
    	qualCodes.add("Dry"); // Dry
    	qualCodes.add("Dis"); // Data-collection discontinued
    	qualCodes.add("--");  // Parameter not determined
    	qualCodes.add("Mnt"); // Maintenance in progress
    	qualCodes.add("ZFl"); // Zero flow
    	qualCodes.add("***"); // *** = temporarily unavailable
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
    	return true;
    }


    @Override
    public void stop() {
	    if (timer != null)
        {
            timer.cancel();
            timer = null;
        }
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
		return Collections.unmodifiableCollection(foiIDs);
	}
}