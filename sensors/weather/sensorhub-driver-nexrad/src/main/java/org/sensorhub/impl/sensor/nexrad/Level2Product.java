package org.sensorhub.impl.sensor.nexrad;

public enum Level2Product {
	REFLECTIVITY("Reflectivity"),
	VELOCITY("RadialVelocity"),
	SPECTRUM_WIDTH("SpectrumWidth");
	
	private final String productStr;
	
	private Level2Product(String s) {
		this.productStr = s;
	}
	
	@Override
	public String toString() {
		return productStr;
	}
}
