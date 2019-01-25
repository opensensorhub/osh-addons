package datasimulation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

/**
 * Created by Ian Patterson on 6/12/2017.
 */

// Repurposed FakeGPS code to fit into this sensor sim
public class GPSsim 
{
	DataComponent posDataStruct;
	DataEncoding posDataEncoding;
	static List<double[]> trajPoints;
	static boolean sendData;
	static Timer timer;
	static double currentTrackPos;

	private static String googleApiUrl = "http://maps.googleapis.com/maps/api/directions/json";

	// use these to add specific start and stop locations
	private static Double startLatitude = null;  // in degrees
	private static Double startLongitude = null;  // in degrees
	private static Double stopLatitude = null;  // in degrees
	private static Double stopLongitude = null;  // in degrees

	// otherwise use these to generate random start and stop locations
	private static double centerLatitude = 34.7300; // in deg
	private static double centerLongitude = -86.5850; // in deg
	private static double areaSize = 0.1; // in deg

	private static double vehicleSpeed = 40; // km/h
	private static boolean walkingMode = false;


	public GPSsim()
	{
		trajPoints = new ArrayList<double[]>();
	}

	public static boolean generateRandomTrajectory()
	{
		// used fixed start/end coordinates or generate random ones 
		double startLat;
		double startLong;
		double endLat;
		double endLong;

		if (trajPoints.isEmpty())
		{
			startLat = centerLatitude + (Math.random()-0.5) * areaSize;
			startLong = centerLongitude + (Math.random()-0.5) * areaSize;

			// if fixed start and end locations not given, pick random values within area 
			if (startLatitude == null || startLongitude == null ||
					stopLatitude == null || stopLongitude == null)
			{
				startLat = centerLatitude + (Math.random()-0.5) * areaSize;
				startLong = centerLongitude + (Math.random()-0.5) * areaSize;
				endLat = centerLatitude + (Math.random()-0.5) * areaSize;
				endLong = centerLongitude + (Math.random()-0.5) * areaSize;
			}

			// else use start/end locations provided in configuration
			else
			{
				startLat = startLatitude;
				startLong = startLongitude;
				endLat = stopLatitude;
				endLong = stopLongitude;
			}
		}
		else
		{
			// restart from end of previous track
			double[] lastPoint = trajPoints.get(trajPoints.size()-1);
			startLat = lastPoint[0];
			startLong = lastPoint[1];
			endLat = centerLatitude + (Math.random()-0.5) * areaSize;
			endLong = centerLongitude + (Math.random()-0.5) * areaSize;
		}


		try
		{
			// request directions using Google API
			URL dirRequest = new URL(googleApiUrl + "?origin=" + startLat + "," + startLong +
					"&destination=" + endLat + "," + endLong + ((walkingMode) ? "&mode=walking" : ""));
			//log.debug("Google API request: " + dirRequest);
			InputStream is = new BufferedInputStream(dirRequest.openStream());

			// parse JSON track
			JsonParser reader = new JsonParser();
			JsonElement root = reader.parse(new InputStreamReader(is));
			//System.out.println(root);
			JsonArray routes = root.getAsJsonObject().get("routes").getAsJsonArray();
			if (routes.size() == 0)
				throw new Exception("No route available");

			JsonElement polyline = routes.get(0).getAsJsonObject().get("overview_polyline");
			String encodedData = polyline.getAsJsonObject().get("points").getAsString();

			// decode polyline data
			decodePoly(encodedData);
			currentTrackPos = 0.0;
			//parentSensor.clearError();
			return true;
		}
		catch (Exception e)
		{
			//parentSensor.reportError("Error while retrieving Google directions", e);
			trajPoints.clear();
			try { Thread.sleep(60000L); }
			catch (InterruptedException e1) {}
			return false;
		}
	}


	private static void decodePoly(String encoded)
	{
		int index = 0, len = encoded.length();
		int lat = 0, lng = 0;
		trajPoints.clear();

		while (index < len)
		{
			int b, shift = 0, result = 0;
			do
			{
				b = encoded.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			}
			while (b >= 0x20);
			int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lat += dlat;

			shift = 0;
			result = 0;
			do
			{
				b = encoded.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			}
			while (b >= 0x20);
			int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lng += dlng;

			double[] p = new double[] {(double) lat / 1E5, (double) lng / 1E5};
			trajPoints.add(p);
		}
	}


	public static double[] sendMeasurement()
	{
		if (trajPoints.isEmpty() || currentTrackPos >= trajPoints.size()-2)
		{
			if (!generateRandomTrajectory())
				return null;

			// skip if generated traj is too small
			if (trajPoints.size() < 2)
			{
				trajPoints.clear();
				return null;
			}
			//for (double[] p: trajPoints)
			//     System.out.println(Arrays.toString(p));
		}

		// convert speed from km/h to lat/lon deg/s
		double speed = vehicleSpeed / 20000 * 180 / 3600;
		int trackIndex = (int)currentTrackPos;
		double ratio = currentTrackPos - trackIndex;
		double[] p0 = trajPoints.get(trackIndex);
		double[] p1 = trajPoints.get(trackIndex+1);
		double dLat = p1[0] - p0[0];
		double dLon = p1[1] - p0[1];
		double dist = Math.sqrt(dLat*dLat + dLon*dLon);

		// compute new position
		double time = System.currentTimeMillis() / 1000.;
		double lat = p0[0] + dLat*ratio;
		double lon = p0[1] + dLon*ratio;
		double alt = 193;

		/*// build and publish datablock
		DataBlock dataBlock = posDataStruct.createDataBlock();
		dataBlock.setDoubleValue(0, time);
		dataBlock.setDoubleValue(1, lat);
		dataBlock.setDoubleValue(2, lon);
		dataBlock.setDoubleValue(3, alt);

		// update latest record and send event
		latestRecord = dataBlock;
		latestRecordTime = System.currentTimeMillis();
		eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, FakeGpsOutput.this, dataBlock));*/

		currentTrackPos += speed / dist;
		return new double[] {lat, lon, alt};
	}
}

