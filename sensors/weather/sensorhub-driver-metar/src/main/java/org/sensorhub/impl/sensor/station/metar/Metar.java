package org.sensorhub.impl.sensor.station.metar;

import java.util.ArrayList;
import java.util.Date;

import org.sensorhub.impl.sensor.station.metar.SkyCondition.Coverage;

public class Metar {
	public String reportString = null;
	public String dateString = "";
	@Deprecated //  use TimeUtil and long to internally represent date
	public Date date = null;
	public long timeUtc;
	public String stationId = null;
	public Integer windDirection = null;
	public Integer windDirectionMin = null;
	public Integer windDirectionMax = null;
	public boolean windDirectionIsVariable = false;
	private Double windSpeed = null; // (in knots x 1.1508 = MPH) - private to force units (think about this)
	public Double windGust = null; // (in knots x 1.1508 = MPH)
	public Integer windDirectionGust = null; // (in knots x 1.1508 = MPH)
	public boolean isCavok = false;
	private Double visibilityMiles = null; 
	private Double visibilityKilometers = null;
	public boolean visibilityLessThan = false;
	public Double pressure = null;
	public Double altimeter = null;
	public Integer temperatureC = null;
	public Double temperature = null;
	public Double temperaturePrecise = null; // C still?	
	public Double dewPoint = null;
	public Integer dewPointC = null;
	public Double dewPointPrecise = null;
	public Double hourlyPrecipInches = null;
	public ArrayList<PresentWeather> presentWeathers = new ArrayList<>();
	public ArrayList<SkyCondition> skyConditions = new ArrayList<>();
	public ArrayList<RunwayVisualRange> runwayVisualRanges = new ArrayList<>();
	public ArrayList<String> obscurations = new ArrayList<>();
	public boolean isSpeci = false;
	public boolean isCorrection = false;

	public Double getVisibilityMiles() {
		return visibilityMiles;
	}
	
	public void setVisibilityMiles(Double visibilityMiles) {
		this.visibilityMiles = visibilityMiles;
		this.visibilityKilometers = visibilityMiles * MetarConstants.STATUTEMILES_TO_KILOMETERS;
	}
	
	public Double getVisibilityKilometers() {
		return visibilityKilometers;
	}
	
	public void setVisibilityKilometers(Double visibilityKilometers) {
		this.visibilityKilometers = visibilityKilometers;
		this.visibilityMiles = visibilityKilometers * MetarConstants.KILOMETERS_TO_STATUTEMILES;
	}

	public Double getWindSpeed() {
		return windSpeed;
	}

	public Double getWindGust() {
		return windGust;
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
	
//	Doouble temp = metar.getTempFahrenheit();  // impl in Metar- check precise first and send if it's there
//	Double dewPt = metar.getDewPointFahrenheit(); // ditto
//	Double pressure = metar.getPressureInches();  // ?
//	Dooble windSpeed = metar.getWindSpeed();
//	Float windGust = metar.windGust;
//	Integer windDir = metar.windDirection;
//	Float visMiles = metar.getVisibility();
	
	public Integer getWindDirection() {
		return windDirection;
	}
	
	//  pressure contains either QPPP or SLP in millibars - use it if it exists
	//  altimeter setting is already inches
	public Double getPressureInches() {
		if(pressure != null)
			return MetarUtil.millibarsToInches(pressure);
		if(altimeter != null)
			return altimeter;
		return null;
	}
	
	public Double getTempFahrenheit() {
		if(temperaturePrecise != null)
			return MetarUtil.c_to_f(temperaturePrecise);
		if(temperatureC != null)
			return MetarUtil.c_to_f(temperatureC);
		return null;
	}
	
	public Double getDewpointFahrenheit() {
		if(dewPointPrecise != null)
			return MetarUtil.c_to_f(dewPointPrecise);
		if(dewPointC != null)
			return MetarUtil.c_to_f(dewPointC);
		return null;
	}
	
	public Double getRelativeHumidity() {
		Double temp = temperaturePrecise;
		if (temp == null && temperatureC != null)  temp = temperatureC.doubleValue();
		Double dp = dewPointPrecise;
		if (dp == null && dewPointC != null)  dp = dewPointC.doubleValue(); 
		if(temp == null || dp == null)
			return null;
		Double rh =  MetarUtil.computeRH(temp, dp);
		return rh;
//		Long rhRound = Math.round(rh);  // rounding?
//		return rhRound.intValue();
	}

	//  For backwards comp with SDMetarEmwin table and SkyConditions table
//	1	Not Reported
//	2	Clear
//	3	Few Clouds
//	4	Partly Cloudy
//	5	Mostly Cloudy
//	6	Cloudy
	//  if multiple SkyConditions, let's report the most cloudy 
	public int getSkyConditionsId () {
		if(skyConditions.size() == 0)
			return 1;
		int id = 2;
		for(SkyCondition sc: skyConditions) {
			Coverage c = sc.coverage;
			switch(c) {
			case CLR:
				id = 2;
				break;
			case BKN:
				id = 5;
				break;
			case FEW:
				id = 3; 
				break;
			case NCD:
				id = 1;
				break;
			case OVC:
				id = 6;
				break;
			case SCT:
				id = 4;
				break;
			case SKC:
				id = 1;
				break;
			case VV:  // TODO  report this in correct col
				break;
			default:
				break;
			}
		}
		return id;
	}
	
	//  For backwards comp with SDMetarEmwin table
	public String getSkyConditionsAsString() {
		if(skyConditions.size() == 0)
			return null;
		StringBuilder b = new StringBuilder();
		for(SkyCondition sc: skyConditions) {
			b.append(sc + "; ");
		}
		String str = b.toString();
		str = str.substring(0, str.length() - 2);  // remove last '; '
		if(str.length() > 100)
			str = str.substring(0, 100);
		return str;
	}
	
	public String getPresentWeathers() {
		StringBuilder b = new StringBuilder();
		for(PresentWeather pw: presentWeathers) {
			b.append(pw.toString() + ";");
		}
		return b.toString();
	}

	public String getRunwayVisualRanges() {
		StringBuilder b = new StringBuilder();
		for(RunwayVisualRange rvr: runwayVisualRanges) {
			b.append(rvr.toString() + ";");
		}
		return b.toString();
	}

}

