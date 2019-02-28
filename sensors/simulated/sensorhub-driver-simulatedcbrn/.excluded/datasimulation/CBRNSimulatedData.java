package datasimulation;

import org.sensorhub.impl.sensor.simulatedcbrn.SimCBRNConfig;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import java.util.Timer;

/**
 * Created by Ian Patterson on 5/4/2017.
 */

public class CBRNSimulatedData {

	Timer timer;
	long lastUpdateTime;
	Random rand = new Random();
	SimCBRNConfig config;

	// Reference Variables
	double tempRef = 20.0;

	// "Actual" Sim Variables to be fed as sensor data
	double[] sensorLoc;
	PointSource[] sources;
	double temp = tempRef;
	ChemAgent detectedAgent = null;
	double preWarnStatus = 0;
	String warnStatus = "NONE";
	double threatLevel;
	double min_threat = 0;
	double max_threat =600;
	double lat;
	double lon;
	double alt;



	public CBRNSimulatedData()
	{
		sensorLoc = new double[]{0,0,0};
		threatLevel = 0;
		lastUpdateTime = Calendar.getInstance().getTimeInMillis();
		sources = new PointSource[1];
		sources[0] = config.source1;
		detectedAgent = null;
		//update();
		autoSetWarnStatus();
	}

	public double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public double getLon() {
		return lon;
	}

	public void setLon(double lon) {
		this.lon = lon;
	}

	public double getAlt() {
		return alt;
	}

	public void setAlt(double alt) {
		this.alt = alt;
	}

	//need to create several forms of data randomization to supply the sensor with sim data
	public double simTemp()
	{
		// Temperature sim (copied from FakeWeatherOutput)
		temp += variation(temp, tempRef, 0.001, 0.1);
		return temp;
	}


	public double getTemp() {
		return temp;
	}


	public ChemAgent getDetectedAgent()
	{
		return detectedAgent;
	}


	/**
	 * <p>To run through the variables and make all adjustments
	 * recommend calling this every time the sensor makes a call
	 * to its outputs</p>
	 */
	public void update(SimCBRNConfig config)
	{
		if(Calendar.getInstance().getTimeInMillis() - lastUpdateTime >= 1000)
		{
			simTemp();
			simulate(config);
			autoSetWarnStatus();
			lastUpdateTime = Calendar.getInstance().getTimeInMillis();
		}
	}

	private void simulate(SimCBRNConfig config)
	{
		// Update the sensor's location
		GPSsim.generateRandomTrajectory();
		sensorLoc = GPSsim.sendMeasurement();
		config.location.lat = sensorLoc[0];
		config.location.lon = sensorLoc[1];
		config.location.alt = sensorLoc[2];

		// Get the intensity of the detected source
		threatLevel = getObservedIntensity();
		detectedAgent = sources[0].getAgent();
		//detectedAgent = config.source1.getAgent();
	}


	public void autoSetWarnStatus()
	{
		if (findThreatLevel() - preWarnStatus > 0.0 && findThreatLevel() > 0.0
				&& findThreatLevel() < 300)
		{
			warnStatus = "WARN";
		}

		else if (findThreatLevel() - preWarnStatus > 0.0 && findThreatLevel() >= 300)
		{
			warnStatus = "ALERT";
		}
		else if (findThreatLevel() - preWarnStatus < 0.0 && findThreatLevel() < 300
				&& warnStatus.equals("ALERT"))
		{
			warnStatus = "DEALERT";
		}
		else if (findThreatLevel() - preWarnStatus < 0.0 && findThreatLevel() == 0.0
				&& warnStatus.equals("WARN"))
		{
			warnStatus = "DEWARN";
		}
		else if(warnStatus.equals("DEALERT") && findThreatLevel() > 0.0
				&& findThreatLevel() < 300)
		{
			warnStatus = "WARN";
		}
		else if(warnStatus.equals("DEWARN") && findThreatLevel() == 0.0)
		{
			warnStatus = "NONE";
		}
		else
		{
			warnStatus = warnStatus;
		}

		preWarnStatus = findThreatLevel();
	}


	public void setWarnStatus(String newWarnStatus)
	{
		warnStatus = newWarnStatus;
	}


	public String getWarnStatus()
	{
		return warnStatus;
	}


	private double variation(double val, double ref, double dampingCoef, double noiseSigma)
	{
		return -dampingCoef*(val - ref) + noiseSigma*rand.nextGaussian();
	}

	private double getObservedIntensity()
	{
		double avgIntensity = 0;
		for (PointSource source1 : sources)
		{
			avgIntensity += source1.findObservedIntensity(sensorLoc[0], sensorLoc[1], sensorLoc[2]);
		}
		return avgIntensity/sources.length;
	}

	/*public void addPointSource(double lat, double lon, double alt, double intensity, String agent_type)
	{
		sources.add(new PointSource(lat, lon, alt, intensity, agent_type));
	}*/

	public void addSources(PointSource[] sources)
	{
		this.sources = sources;
	}


	public int findThreatLevel()
	{
		if(threatLevel == 0) return 0;
		else if (threatLevel > 0.00 && threatLevel <= 100) return 1;
		else if (threatLevel > 100 && threatLevel <= 200) return 2;
		else if (threatLevel > 200 && threatLevel <= 300) return 3;
		else if (threatLevel > 300 && threatLevel <= 400) return 4;
		else if (threatLevel > 400 && threatLevel <= 500) return 5;
		else if (threatLevel > 500) return 6;
		else return -1;
	}

	// TODO: add in low threat level here and as an allowable token
	public String findThreatString()
	{
		if(threatLevel == min_threat)
		{
			return "NONE";
		}
		else if(threatLevel > min_threat && threatLevel <= max_threat/2)
		{
			return "MEDIUM";
		}
		else if(threatLevel > max_threat/2)
		{
			return "HIGH";
		}
		else
		{
			return null;
		}
	}
}
