package org.sensorhub.impl.sensor.nexrad;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * <p>Title: NexradTable.java</p>
 * <p>Description:  </p>
 * @author Tony Cook
 * 
 */

public class NexradTable extends HashMap<String, NexradSite>
{
	private static final long serialVersionUID = 1L;
	private static NexradTable instance = null;  // should make this immutable

	protected NexradTable() throws IOException {
		super();
		buildTable();
	}

	public static NexradTable getInstance() throws IOException {
		if(instance == null) {
			instance = new NexradTable();
		}
		return instance;
	}

	private void buildTable() throws IOException {
		ClassLoader classLoader = getClass().getClassLoader();
		URL url = classLoader.getResource("NexradLocations.txt");
		if(url == null)
			throw new IOException("NexradTable.buildTable().  NexradLocations.txt file not found in classpath");
		BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
		boolean feof = false;
		//  skip two header lines
		String inline = reader.readLine();
		reader.readLine();
		NexradSite site;
		String stmp;
		double lat, lon, altMeters;
		String state;
		
		while (!feof) {
			inline = reader.readLine();
			if (inline == null || inline.trim().length() == 0)
				break;

			state = inline.substring(72,74);
			if(state.trim().length() ==0)
				//  Electing to throw out 4 non-US radars
				//  Can re-add them if they buy us somtehing down the road
				continue;
			String id = inline.substring(9,13);
			site = new NexradSite(id);
			site.name = inline.substring(20, 51).trim();
			stmp = inline.substring(106,116).trim();
			lat = Double.parseDouble(stmp);
			stmp = inline.substring(116,127).trim();
			lon = Double.parseDouble(stmp);
			stmp = inline.substring(127,134).trim();
			site.elevationFeet = Integer.parseInt(stmp);
			altMeters = (double)site.elevationFeet * NexradSite.FEET_TO_METERS;
			site.lat = lat;
			site.lon = lon;
			site.elevation = altMeters;
			this.put(site.id, site);
		}
	}

	/** Should return a copy of the table so that the internal table is not modified!! **/ 
	public Collection<NexradSite> getAllSites() {
		Collection <NexradSite> values =  instance.values();
		return new ArrayList<NexradSite>(values);
	}
	
	/** Should return a copy of the keys so that the internal table is not modified!! **/ 
	public Collection<String> getAllSiteIds() {
		Set<String> set= instance.keySet();
		return new ArrayList<String>(set);
	}
	
	private NexradSite getSiteFrom3LetterId(String id3) {
		for(Map.Entry entry: instance.entrySet()) {
			String id4 = (String)entry.getKey();
			if(id4.endsWith(id3))
				return (NexradSite)entry.getValue();
		}
		
		return null;
	}
	
	public NexradSite getSite(String id) {
		if(id.length() == 4) 
			return get(id.toUpperCase());
		
		if(id.length() == 3)
			return getSiteFrom3LetterId(id.toUpperCase());
		
		return null;	
	}
	
	
	public static void main(String[] args) throws Exception  {
//		NexradTable.outputCsv();
		System.err.println(NexradTable.getInstance().getSite("LBB"));
	}
}	
