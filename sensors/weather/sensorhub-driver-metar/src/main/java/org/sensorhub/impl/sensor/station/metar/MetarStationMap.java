package org.sensorhub.impl.sensor.station.metar;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

import org.sensorhub.impl.sensor.station.Station;

import com.opencsv.CSVReader;

/**
 * <p>Title: MetarStationMap.java</p>
 * <p>Description: </p>
 *
 * @author tcook
 * @date Sep 26, 2016
 */
public class MetarStationMap {
	private static final String MAP_FILE_PATH = "metarStations.csv";
//	private static final String MAP_FILE_PATH = "stationsAll.txt";
	private HashMap<String, Station> map;
	private static MetarStationMap instance = null;
	
	private MetarStationMap(String mapPath) throws IOException {
		loadCsvMap(mapPath);
	}
	
	public static MetarStationMap getInstance(String mapPath) throws IOException {
		if(instance == null)
			instance = new MetarStationMap(mapPath);
		
		return instance;
	}
	
	private void loadGilbertMap(String mapPath) throws IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader(mapPath))) {
			
		}
	}
	
	private void loadCsvMap(String mapPath) throws IOException {
		System.err.println("MMap file: " + mapPath);
		map = new HashMap<>();
		
		CSVReader reader = new CSVReader(new FileReader(mapPath));
		try {
			String [] line;
			reader.readNext(); // skip hdr line
			while ((line = reader.readNext()) != null ) {
				String id = line[1];
//				System.err.println(id);
				if(id.trim().length() < 4) {
					System.err.println("Skipping stn: " + id);
					continue;
				}
				String name = line[2];
				double lat = 0.0, lon = 0.0, el = 0.0;
				if(!line[3].equals(""))
					lat = Double.parseDouble(line[3]);
				if(!line[4].equals(""))
					lon = Double.parseDouble(line[4]);
				if(!line[5].equals(""))
					el = Double.parseDouble(line[5]);

				Station s = new Station();
				s.setId(id.toUpperCase());
				s.setName(name);
				s.setLat(lat);
				s.setLon(lon);
				s.setElevation(el);
				map.put(id, s);

			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			reader.close();
		}
	}

	public Station getStation(String id) {
		return map.get(id.toUpperCase());
	}
	
	public Collection<Station> getStations() {
		return map.values();
	}
	
	public static void main(String[] args) throws IOException {
		MetarStationMap metarMap = MetarStationMap.getInstance("C:/Users/tcook/root/workOsh/osh-sensors-weather/sensorhub-driver-metar/src/main/resources/metarStations.csv");

		System.err.println(metarMap.getStation("KEVW"));
	}
}
