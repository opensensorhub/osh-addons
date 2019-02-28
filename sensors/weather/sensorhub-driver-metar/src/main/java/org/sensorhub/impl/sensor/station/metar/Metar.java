package org.sensorhub.impl.sensor.station.metar;

import java.util.ArrayList;
import java.util.Date;

public class Metar 
{
	public static final double KILOMETERS_TO_STATUTEMILES = 0.6213712;
	public static final double STATUTEMILES_TO_KILOMETERS = 1.609344;

	public String reportString = null;
	public String dateString = "";
	@Deprecated //  use TimeUtil and long to internally represent date
	public Date date = null;
	public long timeUtc;
	public String stationID = null;
	public Integer windDirection = 0;  // what should this default to for missing data?
	public Integer windDirectionMin = null;
	public Integer windDirectionMax = null;
	public boolean windDirectionIsVariable = false;
	private Double windSpeed = Double.NaN;; // (in knots x 1.1508 = MPH) - private to force units (think about this)
	public Double windGust = Double.NaN;; // (in knots x 1.1508 = MPH)
	public Integer windDirectionGust = null; // (in knots x 1.1508 = MPH)
	public boolean isCavok = false;
	private Double visibilityMiles = Double.NaN; 
	private Double visibilityKilometers = null;
	public boolean visibilityLessThan = false;
	public Double pressure = Double.NaN;;
	public Double altimeter = null;
	private Double temperatureC = Double.NaN;
	private Double temperature = Double.NaN;

	private Double temperaturePrecise = Double.NaN;
	private Double dewPoint = Double.NaN;
	private Double dewPointC = Double.NaN;

	private Double dewPointPrecise = Double.NaN;
	public Double hourlyPrecipInches = Double.NaN;
	public ArrayList<PresentWeather> presentWeathers = new ArrayList<>();
	public ArrayList<SkyCondition> skyConditions = new ArrayList<>();
	public ArrayList<RunwayVisualRange> runwayVisualRanges = new ArrayList<>();
	public ArrayList<String> obscurations = new ArrayList<>();
	public boolean isSpeci = false;
	public boolean isCorrection = false;
//	private boolean isNoSignificantChange = false;
//	private String becoming = null;
	


	public Double getVisibilityMiles() {
		return visibilityMiles;
	}
	
	public void setVisibilityMiles(Double visibilityMiles) {
		this.visibilityMiles = visibilityMiles;
		this.visibilityKilometers = visibilityMiles * STATUTEMILES_TO_KILOMETERS;
	}
	
	public Double getVisibilityKilometers() {
		return visibilityKilometers;
	}
	
	public void setVisibilityKilometers(Double visibilityKilometers) {
		this.visibilityKilometers = visibilityKilometers;
		this.visibilityMiles = visibilityKilometers * KILOMETERS_TO_STATUTEMILES;
	}

	public Double getWindSpeed() {
		return windSpeed;
	}

	public void setWindSpeed(Double windSpeed) {
		this.windSpeed = windSpeed;
	}
	
	public void addRunwayVisualRange(RunwayVisualRange rvr) {
		this.runwayVisualRanges.add(rvr);
	}
	
	public void addPresentWeather(PresentWeather pw) {
		this.presentWeathers.add(pw);
	}

	public void addSkyCondition(SkyCondition sc) {
		this.skyConditions.add(sc);
	}
	
	public String getPresentWeathers() {
		StringBuilder b = new StringBuilder();
		for(PresentWeather pw: presentWeathers) {
			b.append(pw.toString() + ";");
		}
		return b.toString();
	}

	public String getSkyConditions() {
		StringBuilder b = new StringBuilder();
		for(SkyCondition sc: skyConditions) {
			b.append(sc.toString() + ";");
		}
		return b.toString();
	}

	public Double getTemperature() {
		return temperature;
	}

	public Double getDewPoint() {
		return dewPoint;
	}

	public void setTemperatureC(Double temperatureC) {
		this.temperatureC = temperatureC;
		this.temperature = MetarUtil.c_to_f(temperatureC); 
	}

	public void setDewPointC(Double dewPointC) {
		this.dewPointC = dewPointC;
		this.dewPoint = MetarUtil.c_to_f(dewPointC); 
	}

	public void setTemperaturePrecise(Double temperaturePrecise) {
		this.temperaturePrecise = temperaturePrecise;
		setTemperatureC(temperaturePrecise);
	}

	public void setDewPointPrecise(Double dewPointPrecise) {
		this.dewPointPrecise = dewPointPrecise;
		setDewPointC(dewPointPrecise);
	}

	public String getRunwayVisualRanges() {
		StringBuilder b = new StringBuilder();
		for(RunwayVisualRange rvr: runwayVisualRanges) {
			b.append(rvr.toString() + ";");
		}
		return b.toString();
	}
	
	public Double getRelativeHumidity() {
//		if(dewPointC == Double.NaN || temperatureC == Double.NaN)
//			return Double.NaN;
		if(Double.compare(temperatureC, Double.NaN) == 0)
			return Double.NaN;
		if(Double.compare(dewPointC, Double.NaN) == 0)
			return Double.NaN;
		
		return MetarUtil.computeRH(temperatureC, dewPointC);
	}
}


