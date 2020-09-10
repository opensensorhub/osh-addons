package org.sensorhub.impl.ndbc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.vast.util.Bbox;

public class CapsTask extends TimerTask  
{
	CapabilitiesReader capsReader; // = new GetCaps();
	
	public CapsTask(String serverUrl, long updateTimeMs) {
		capsReader = new CapabilitiesReader(serverUrl, updateTimeMs);
	}

    //  NDBC Uses EPSG:4326 coord ordering (lat,lon), so reverse our config for filtering 
	public void setBbox(double minLat, double minLon, double maxLat, double maxLon) {
		capsReader.bbox = new Bbox(minLat, minLon, maxLat, maxLon);
	}
	
	@Override
	public void run() {
		System.out.println("Caps updating at : " + 
				LocalDateTime.ofInstant(Instant.ofEpochMilli(scheduledExecutionTime()), 
				ZoneId.of("UTC")));
		try {
			capsReader.getTimes();
		} catch (Exception e) {
			e.printStackTrace();
			//  retry and adjust schedule accordingly
		}
	}
	
	public static void main(String[] args) throws InterruptedException {
		Timer timer = new Timer();
		CapsTask capsTask = new CapsTask("https://sdf.ndbc.noaa.gov/sos/server.php", TimeUnit.MINUTES.toMillis(60L));
		capsTask.setBbox(31.0, -120.0, 35.0, -115.0);

		timer.scheduleAtFixedRate(capsTask, 0, 60_000L);
		
	}

}
