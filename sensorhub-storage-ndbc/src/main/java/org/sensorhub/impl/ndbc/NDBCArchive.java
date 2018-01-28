package org.sensorhub.impl.ndbc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.sensorhub.api.common.SensorHubException;

public class NDBCArchive
{
	public void start() throws SensorHubException, IOException
    {
    	Document doc = Jsoup.connect("http://sdf.ndbc.noaa.gov/stations.shtml").get();
    	Elements stn = doc.getElementsByClass("stndata"); // Grab the <tr> elements of class "stndata"
    	List<BuoyStation> stations = new ArrayList<BuoyStation>();
    	
    	for (int i = 0; i < stn.size(); i++) {
        	BuoyStation bs = new BuoyStation();
    		bs.setId(stn.get(i).select("td").get(0).text());
    		bs.setName(stn.get(i).select("td").get(1).text());
    		bs.setOwner(stn.get(i).select("td").get(2).text());
    		
        	LocationLatLon bsLoc = new LocationLatLon();
    		bsLoc.setLat(Double.parseDouble(stn.get(i).select("td").get(3).text()));
    		bsLoc.setLon(Double.parseDouble(stn.get(i).select("td").get(4).text()));
    		
    		bs.setLoc(bsLoc);
        	String[] sensorStr = new String[stn.get(i).select("td").get(5).text().split(" ").length];
        	for (int k = 0; k < stn.get(i).select("td").get(5).text().split(" ").length; k++)
        		sensorStr[k] = stn.get(i).select("td").get(5).text().split(" ")[k].trim();
        	bs.setSensor(sensorStr);
        	stations.add(bs); // Got the stations that are currently reporting
    	}
    	for (BuoyStation buoy : stations) {
    		System.out.println(buoy.getId());
    	}
    }


    public void stop() throws SensorHubException
    {        
    }
}
