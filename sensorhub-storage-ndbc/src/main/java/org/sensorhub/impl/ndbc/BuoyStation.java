package org.sensorhub.impl.ndbc;

public class BuoyStation
{
	public String id;
	public String name;
	public String owner;
	public LocationLatLon loc;
	public String[] sensor;
	
	public String getId() {return id;}
	public void setId(String id) {this.id = id;}
	
	public String getName() {return name;}
	public void setName(String name) {this.name = name;}
	
	public String getOwner() {return owner;}
	public void setOwner(String owner) {this.owner = owner;}

	public LocationLatLon getLoc() {return loc;}
	public void setLoc(LocationLatLon loc) {this.loc = loc;}
	
	public String[] getSensor() {return sensor;}
	public void setSensor(String[] sensor) {this.sensor = sensor;}
}
