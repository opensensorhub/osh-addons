package org.sensorhub.impl.sensor.usgswater;

public class WaterDataRecord {
	private Long measurementTime;
	private String siteCode;
	private String siteName;
	private Double siteLat;
	private Double siteLon;
	private Double discharge;
	private Double gageHeight;
	private Double waterTemp;
	private Double conductance;
	private Double dissOxygen;
	private Double waterPH;
	
	public Long getMeasurementTime() {return measurementTime;}
	public void setMeasurementTime(Long recordTime) {this.measurementTime = recordTime;}
	
	public String getSiteCode() {return siteCode;}
	public void setSiteCode(String siteCode) {this.siteCode = siteCode;}
	
	public String getSiteName() {return siteName;}
	public void setSiteName(String siteName) {this.siteName = siteName;}
	
	public Double getSiteLat() {return siteLat;}
	public void setSiteLat(Double siteLat) {this.siteLat = siteLat;}
	
	public Double getSiteLon() {return siteLon;}
	public void setSiteLon(Double siteLon) {this.siteLon = siteLon;}
	
	public Double getDischarge() {return discharge;}
	public void setDischarge(Double discharge){this.discharge = discharge;}
	
	public Double getGageHeight() {return gageHeight;}
	public void setGageHeight(Double gageHeight) {this.gageHeight = gageHeight;}
	
	public Double getWaterTemp() {return waterTemp;}
	public void setWaterTemp(Double waterTemp) {this.waterTemp = waterTemp;}
	
	public Double getConductance() {return conductance;}
	public void setConductance(Double conductance) {this.conductance = conductance;}
	
	public Double getDissOxygen() {return dissOxygen;}
	public void setDissOxygen(Double dissOxygen) {this.dissOxygen = dissOxygen;}
	
	public Double getWaterPH() {return waterPH;}
	public void setWaterPH(Double waterPH) {this.waterPH = waterPH;}
}
