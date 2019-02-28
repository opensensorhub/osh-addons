package org.sensorhub.impl.sensor.plume;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tony Cook
 * @date  9/25/15
 *
 */


public class Plume {
	double sourceLat, sourceLon, sourceHeight;
	
	List<PlumeStep> steps = new ArrayList<>();

	public void addStep(PlumeStep step) {
		steps.add(step);
	}
	
	public double getSourceLat() {
		return sourceLat;
	}

	public void setSourceLat(double sourceLat) {
		this.sourceLat = sourceLat;
	}

	public double getSourceLon() {
		return sourceLon;
	}

	public void setSourceLon(double sourceLon) {
		this.sourceLon = sourceLon;
	}

	public double getSourceStackHeight() {
		return sourceHeight;
	}

	public void setSourceStackHeight(double sourceStackHeight) {
		this.sourceHeight = sourceStackHeight;
	}

	public List<PlumeStep> getSteps() {
		return steps;
	}
}
