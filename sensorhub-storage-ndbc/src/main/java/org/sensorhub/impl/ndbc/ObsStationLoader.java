package org.sensorhub.impl.ndbc;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.sensorhub.impl.module.AbstractModule;
import org.vast.ogc.om.SamplingPoint;
import org.vast.swe.SWEHelper;
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
    static final String STN_INFO_URL = NDBCArchive.BASE_USGS_URL + "/stations.shtml";
    static final String FOI_UID_PREFIX = NDBCArchive.UID_PREFIX + "station:wmo:";
    static final String AREA_UID_PREFIX = NDBCArchive.UID_PREFIX + "region:";
    
    AbstractModule<?> module;
    
    
    public ObsStationLoader(AbstractModule<?> module)
    {
        this.module = module;
    }
    
    public void loadStations(Map<String, AbstractFeature> fois) throws IOException
    {
    	Document doc = Jsoup.connect(STN_INFO_URL).get();
    	Elements stn = doc.getElementsByClass("stndata"); // Grab the <tr> elements of class "stndata"
    	
    	for (int i = 0; i < stn.size(); i++) {
        	GMLFactory gmlFac = new GMLFactory(true);
            SamplingPoint station = new SamplingPoint();
            
            station.setId(stn.get(i).select("td").get(0).text());
            station.setUniqueIdentifier(FOI_UID_PREFIX + stn.get(i).select("td").get(0).text());
            station.setName("NDBC Buoy Station " + stn.get(i).select("td").get(0).text());
            station.setDescription(stn.get(i).select("td").get(1).text());
    		
        	Point stnLoc = gmlFac.newPoint();
            stnLoc.setSrsDimension(2);
            stnLoc.setSrsName(SWEHelper.getEpsgUri(4269)); // NAD83
            stnLoc.setPos(new double[] {Double.parseDouble(stn.get(i).select("td").get(3).text()), Double.parseDouble(stn.get(i).select("td").get(4).text())});
            station.setShape(stnLoc);
            
            fois.put(stn.get(i).select("td").get(0).text(), station);
    	}
    }
}
