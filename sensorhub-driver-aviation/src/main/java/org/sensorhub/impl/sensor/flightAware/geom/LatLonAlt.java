package org.sensorhub.impl.sensor.flightAware.geom;

/**
 * <p>Title: LatLonAlt.java</p>
 * <p>Description:  </p>
 * @author Tony Cook
 * @since May 26, 2011
 */

public class LatLonAlt extends LatLon
{
	public double alt;
	
	public LatLonAlt() {
		
	}
	
	public LatLonAlt(double lat, double lon, double alt) {
		super(lat, lon);
		this.alt = alt; 
	}	
	
	public String toString() {
		return lat + ", " + lon + ", " + alt;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		long temp;
		temp = Double.doubleToLongBits(alt);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		LatLonAlt other = (LatLonAlt) obj;
		if (Double.doubleToLongBits(alt) != Double.doubleToLongBits(other.alt))
			return false;
		return true;
	}
}
