package org.sensorhub.impl.ndbc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.sensorhub.impl.ndbc.BuoyEnums.ObsParam;
import org.slf4j.Logger;
import org.vast.util.Bbox;

public class BuoyParser {
	static final String BASE_URL = NDBCArchive.BASE_NDBC_URL + "/sos/server.php?request=GetObservation&service=SOS&version=1.0.0";
	DataFilter filter;
	ObsParam recordType;
	String requestURL;
	Logger logger;

	public BuoyParser(DataFilter filter, ObsParam recordType)
	{
		this.filter = filter;
		this.recordType = recordType;
	}

	public static void main(String[] args) throws IOException {
		DataFilter filter = new DataFilter();
//		filter.startTime=
		BuoyParser parser = new BuoyParser(filter, ObsParam.AIR_TEMPERATURE);
		parser.getRecords();
	}
	
	//    protected String buildInstantValuesRequest(DataFilter filter, Map<String, String[]> sensorOfferings)
	protected String buildRequest()
	{
		StringBuilder buf = new StringBuilder(BASE_URL);

		// site ids
		if (!filter.stationIds.isEmpty())
		{
			buf.append("&offering=");
			for (String id: filter.stationIds)
				buf.append("urn:ioos:station:wmo:").append(id).append(',');
			buf.setCharAt(buf.length()-1, '&');
		} else {
			buf.append("&offering=urn:ioos:network:noaa.nws.ndbc:all");
		}

		// site bbox
		if (filter.siteBbox != null && !filter.siteBbox.isNull())
		{
			Bbox bbox = filter.siteBbox;
			buf.append("&featureofinterest=BBOX:")
			.append(bbox.getMinX()).append(",")
			.append(bbox.getMinY()).append(",")
			.append(bbox.getMaxX()).append(",")
			.append(bbox.getMaxY()).append("&");
		}

		// parameters
		//  NOTE: only one ovservedProperty per request is honored (and required)  
		buf.append("&observedProperty=" + recordType.toString().toLowerCase());

		buf.append("&responseformat=text/csv"); // output type

		// time range
		if (filter.startTime != null && filter.endTime != null) {
			buf.append("&eventtime=")
			.append(Instant.ofEpochMilli(filter.startTime).toString().substring(0,19) + "Z");
			buf.append("/")
			.append(Instant.ofEpochMilli(filter.endTime).toString().substring(0,19) + "Z");
		} else {
			buf.append("&eventtime=latest");
		}
		
		return buf.toString();
	}

	public List<BuoyRecord> getRecords() throws IOException {
		requestURL = buildRequest();
//		logger.info("Requesting observations from: " + requestURL);
		System.err.println("Requesting observations from: " + requestURL);
		URL url = new URL(requestURL);

		List<BuoyRecord> recs = parseResponse(url.openStream());
		return recs;
	}

	protected List<BuoyRecord> parseResponse(InputStream is) throws IOException {
		List<BuoyRecord> recs = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
			// header line
			String inline = br.readLine();
			while(true) {
				try {
					inline = br.readLine();
					if(inline == null)   break;
					// read common params
					String [] values = inline.split(",");
					BuoyRecord rec = new BuoyRecord(recordType);
					rec.stationId = values[0];
					rec.sensorId = values[1];
					rec.lat = Double.parseDouble(values[2]);
					rec.lon = Double.parseDouble(values[3]);
					rec.timeStr = values[4];
					//rec.timeMs = ;
					switch(recordType) {
					case AIR_PRESSURE_AT_SEA_LEVEL:
						rec.depth = parseDouble(values[5]);
						rec.airPressure = parseDouble(values[6]);
						break;
					case AIR_TEMPERATURE:
						rec.depth = parseDouble(values[5]);
						rec.airTemperature = parseDouble(values[6]);
						break;
					case SEA_WATER_ELECTRICAL_CONDUCTIVITY:
						rec.depth = parseDouble(values[5]);
						rec.conductivity = parseDouble(values[6]);
						break;
					case SEA_WATER_SALINITY:
						rec.depth = parseDouble(values[5]);
						rec.salinity = parseDouble(values[6]);
						break;
					case SEA_WATER_TEMPERATURE:
						rec.depth = parseDouble(values[5]);
						rec.waterTemperature = parseDouble(values[6]);
						break;
					case SEA_FLOOR_DEPTH_BELOW_SEA_SURFACE:
						rec.seaFloorDepth = parseDouble(values[5]);
						rec.depthAveragingInterval = parseInt(values[6]);
						break;
					case CURRENTS:
						rec.currents.bin = parseInt(values[5]);
						rec.depth = parseDouble(values[6]);
						rec.currents.direction_of_sea_water_velocity= parseInt(values[7]);
						rec.currents.sea_water_speed = parseDouble(values[8]);
						rec.currents.upward_sea_water_velocity = parseDouble(values[9]);
						rec.currents.error_velocity = parseDouble(values[10]);
						rec.currents.platform_orientation = parseDouble(values[11]);
						rec.currents.platform_pitch_angle = parseDouble(values[12]);
						rec.currents.platform_roll_angle = parseDouble(values[13]);
						rec.currents.sea_water_temperature = parseDouble(values[14]);
						rec.currents.pct_good_3_beam = parseInt(values[15]);
						rec.currents.pct_good_4_beam = parseInt(values[16]);
						rec.currents.pct_rejected = parseInt(values[17]);
						rec.currents.pct_bad = parseInt(values[18]);
						rec.currents.echo_intensity_beam1 = parseInt(values[19]);
						rec.currents.echo_intensity_beam2 = parseInt(values[20]);
						rec.currents.echo_intensity_beam3 = parseInt(values[21]);
						rec.currents.echo_intensity_beam4 = parseInt(values[22]);
						rec.currents.correlation_magnitude_beam1 = parseInt(values[23]);
						rec.currents.correlation_magnitude_beam2 = parseInt(values[24]);
						rec.currents.correlation_magnitude_beam3 = parseInt(values[25]);
						rec.currents.correlation_magnitude_beam4 = parseInt(values[26]);
						rec.currents.quality_flags = parseInt(values[27]);
						break;
					case WAVES:
						rec.waves.sea_surface_wave_significant_height = parseDouble(values[5]); //(m)	
						rec.waves.sea_surface_wave_peak_period = parseDouble(values[6]); //(s)	
						rec.waves.sea_surface_wave_mean_period = parseDouble(values[7]); //(s)	
						rec.waves.sea_surface_swell_wave_significant_height = parseDouble(values[8]); // (m)	
						rec.waves.sea_surface_swell_wave_period = parseDouble(values[9]); //(s)	
						rec.waves.sea_surface_wind_wave_significant_height = parseDouble(values[10]); //(m)	
						rec.waves.sea_surface_wind_wave_period = parseDouble(values[11]);  // (s)	
						rec.waves.sea_water_temperature = parseDouble(values[12]); //(c)	
						rec.waves.sea_surface_wave_to_direction = parseDouble(values[13]); //(degree)	
						rec.waves.sea_surface_swell_wave_to_direction = parseDouble(values[14]); //(degree)	
						rec.waves.sea_surface_wind_wave_to_direction = parseDouble(values[15]); //(degree)	
						rec.waves.number_of_frequencies = parseInt(values[16]); //(count)	
						//  The arrays are semicolon delimited 
//						Double [] center_frequencies = parseMultiDouble(values[]); //(Hz)	
//						Double [] bandwidths; //(Hz)	
//						Double [] spectral_energy; //(m**2/Hz)	
//						Double [] mean_wave_direction; //(degree)	
//						Double [] principal_wave_direction; //(degree)	
//						Double [] polar_coordinate_r1; //(1)	
//						Double [] polar_coordinate_r2; //(1)	
						rec.waves.calculation_method = values[24];	
						rec.waves.sampling_rate = parseInt(values[25]); //(Hz)
						break;
					case WINDS:
						rec.seaFloorDepth = parseDouble(values[5]);
						rec.winds.wind_from_direction = parseInt(values[6]); //(degree)
						rec.winds.wind_speed = parseInt(values[7]); //(m/s)
						rec.winds.wind_speed_of_gust = parseInt(values[8]); //(m/s)
						rec.winds.upward_air_velocity = parseDouble(values[9]); //(m/s)
						break;
					default:
					} 
					recs.add(rec);
				} catch (Exception e) {
//					logger.error(e.getMessage());
					e.printStackTrace(System.err);
					continue;
				}
			}
		}
		return recs;
	}

	private Double parseDouble(String s) {
		try {
			return Double.parseDouble(s);
		} catch (Exception e) {
			return null;
		}
	}

	private Integer parseInt(String s) {
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
			return null;
		}
	}
}
