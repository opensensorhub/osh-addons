package org.sensorhub.impl.ndbc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.ndbc.BuoyEnums.ObsParam;
import org.vast.ogc.om.SamplingPoint;
import org.vast.swe.SWEHelper;
import org.vast.util.Bbox;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.Point;
import net.opengis.gml.v32.impl.GMLFactory;

/**
 * <p>
 * Loader for NDBC tabular data of observation stations<br/>
 * See <a href="http://sdf.ndbc.noaa.gov/">
 * web service documentation</a>
 * </p>
 *
 * @author Lee Butler <labutler10@gmail.com>
 * @since Jan 27, 2018
 */
public class ObsStationLoader
{
    static final String STN_INFO_URL = NDBCArchive.BASE_NDBC_URL + "/stations.shtml";
    static final String FOI_UID_PREFIX = NDBCArchive.UID_PREFIX + "station:wmo:";
    static final String BASE_URL = NDBCArchive.BASE_NDBC_URL + "/sos/server.php?request=GetObservation&service=SOS&version=1.0.0";
    
    AbstractModule<?> module;
    
    
    public ObsStationLoader(AbstractModule<?> module)
    {
        this.module = module;
    }

    public void loadStations(Map<String, AbstractFeature> fois, DataFilter filter) throws IOException
    {
        StringBuilder buf = new StringBuilder(BASE_URL);
        
        // site bbox
        if (filter.siteBbox != null && !filter.siteBbox.isNull())
        {
            Bbox bbox = filter.siteBbox;
            buf.append("&offering=urn:ioos:network:noaa.nws.ndbc:all&featureofinterest=BBOX:")
               .append(bbox.getMinX()).append(",")
               .append(bbox.getMinY()).append(",")
               .append(bbox.getMaxX()).append(",")
               .append(bbox.getMaxY()).append("&");
        }
        
        // parameters
        if (!filter.parameters.isEmpty())
        {
            buf.append("observedProperty=");
            for (ObsParam param: filter.parameters)
                buf.append(param.toString().toLowerCase()).append(',');
            buf.setCharAt(buf.length()-1, '&');
        }
        
        buf.append("responseformat=text/csv&eventtime=latest"); // output type
    	module.getLogger().debug("Requesting observations from: " + buf.toString());
    	URL url = new URL(buf.toString());
    	
    	BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
    	
    	String line;
        while ((line = reader.readLine()) != null)
        {                
            line = line.trim();
            
            // parse header
            if (line.startsWith("station_id,sensor_id"))
                line = reader.readLine(); // skip headers
            
            String[] token = line.split(",");
            String[] idArr = token[0].split(":");
            
            GMLFactory gmlFac = new GMLFactory(true);
            SamplingPoint station = new SamplingPoint();
            
            // Get Buoy ID
            station.setId(idArr[idArr.length-1]);
            station.setUniqueIdentifier(token[0]);
            station.setName("NDBC Buoy Station " + idArr[idArr.length-1]);
            
            // Get Buoy Location
            Point stnLoc = gmlFac.newPoint();
            stnLoc.setSrsDimension(2);
            stnLoc.setSrsName(SWEHelper.getEpsgUri(4269)); // NAD83
            stnLoc.setPos(new double[] {Double.parseDouble(token[2]), Double.parseDouble(token[3])});
            station.setShape(stnLoc);
            
            fois.put(idArr[idArr.length-1], station);
    	}
    }
}
