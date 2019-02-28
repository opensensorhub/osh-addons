package datasimulation;

/**
 * Created by Ian Patterson on 6/11/2017.
 */

// TODO: add method to make the source mobile
public class PointSource
{
	private double lat;
	private double lon;
	private double alt;
	private double intensity;
	private double radiusinKM;
	private ChemAgent agent;

	public PointSource(double lat, double lon, double alt, double intensity, String agent_type, double radius) {
		this.lat = lat;
		this.lon = lon;
		this.alt = alt;
		this.intensity = intensity;
		this.agent = new ChemAgent(agent_type);
		this.radiusinKM = radius;
	}

	public double getLat() {
		return lat;
	}

	public double getLon() {
		return lon;
	}

	public double getAlt() {
		return alt;
	}

	public double getIntensity() {
		return intensity;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public void setLon(double lon) {
		this.lon = lon;
	}

	public void setAlt(double alt) {
		this.alt = alt;
	}

	public void setIntensity(double intensity) {
		this.intensity = intensity;
	}

	public ChemAgent getAgent() {
		return agent;
	}

	public void setAgent(ChemAgent agent) {
		this.agent = agent;
	}

    public double getRadiusinKM() {
        return radiusinKM;
    }

    public void setRadiusinKM(double radiusinKM) {
        this.radiusinKM = radiusinKM;
    }

    public double findObservedIntensity(double b_lat, double b_lon, double b_alt)
	{
		double radiusInKM = 1;
		// Distance between point source and observer
		double dist = Haversine.haversine(b_lat, b_lon, lat, lon);
		double obsInt = -(intensity /radiusInKM) * dist + intensity;
        //System.out.println(obsInt);
        if (obsInt < 0.01) {obsInt = 0;}
		return obsInt;
	}
}
