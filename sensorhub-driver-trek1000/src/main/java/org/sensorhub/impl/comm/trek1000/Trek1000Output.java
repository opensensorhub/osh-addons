package org.sensorhub.impl.comm.trek1000;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.comm.UARTConfig;
import org.sensorhub.impl.comm.rxtx.RxtxSerialCommProvider;
import org.sensorhub.impl.comm.rxtx.RxtxSerialCommProviderConfig;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.data.DataRecordImpl;
import org.vast.swe.SWEConstants;
import org.vast.swe.helper.GeoPosHelper;

import ch.qos.logback.core.net.server.Client;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.Vector;

public class Trek1000Output extends AbstractSensorOutput<Trek1000Sensor>
{
	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
	
	DataRecord trekData;
	DataEncoding trekEncoding;
	
	Boolean started = false;
	RxtxSerialCommProvider serialPort;
	ArrayList<LLALocation> anchorLocations = null;

	public Trek1000Output(Trek1000Sensor parentSensor)
	{
		super(parentSensor);
	}
	
	protected void init()
	{
		trekData = new DataRecordImpl();
		trekData.setName(getName());
		trekData.setDefinition("http://sensorml.com/ont/swe/property/Event");

		GeoPosHelper fac = new GeoPosHelper();
		Vector locVector = fac.newLocationVectorLatLon(SWEConstants.DEF_SENSOR_LOC);
		locVector.setLabel("Location");
        locVector.setDescription("Lat/Long vector of tag");

		// Add timestamp, location of tag
	    trekData.addComponent("time", fac.newTimeStampIsoUTC());
        trekData.addComponent("location", locVector);
        
        trekEncoding = fac.newTextEncoding(",", "\n");
	}
	
	public void start(Trek1000Config config) throws InterruptedException
	{
		anchorLocations = config.anchorLocations;

		UARTConfig protocol = new UARTConfig();
		protocol.portName = config.serialPort;
		protocol.baudRate = config.baudRate;
		RxtxSerialCommProviderConfig serialConfig = new RxtxSerialCommProviderConfig();
		serialConfig.protocol = protocol;

		serialPort = new RxtxSerialCommProvider();
		serialPort.setConfiguration(serialConfig);
		try {
			serialPort.start();
		} catch (SensorHubException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Create thread to read and publish sensor data
		Thread t = new Thread(new Runnable()
		{
			public void run()
			{
				while (started)
				{
					String output = "";
					String ch = "";
					boolean foundStart = false;

					byte[] buffer = new byte[1024];
					int len = -1;

					try {
						InputStream is = serialPort.getInputStream();
						while ( (len = is.read(buffer)) > -1 )
						{
							ch = new String(buffer, 0, len);
							if (ch.charAt(0) == 'm')
								foundStart = true;

							if (foundStart)
								output += ch;
							
							if (output.length() == 63)
								break;
						}
					} catch ( IOException e ) {
						e.printStackTrace();
					}

					publishData(output.trim());
				}
			}
		});

	    started = true;
	    t.start();
	}

	protected void stop()
	{
		try {
			started = false;
			serialPort.stop();
		} catch (SensorHubException e) {
			e.printStackTrace();
		}
	}
	
	private void publishData(String data)
	{
		String[] parts = data.trim().split(" ");

		if (!parts[0].trim().equals("mc"))
			return;

		Double dis0 = Integer.parseInt(parts[2].trim(), 16) * 0.001;
		Double dis1 = Integer.parseInt(parts[3].trim(), 16) * 0.001;
		Double dis2 = Integer.parseInt(parts[4].trim(), 16) * 0.001;
		Double dis3 = Integer.parseInt(parts[5].trim(), 16) * 0.001;

		System.out.printf("%s Dis0: %.2fm Dis1: %.2fm Dis2: %.2fm Dis3: %.2fm\n",
				parts[0], dis0, dis1, dis2, dis3);

		double time = System.currentTimeMillis() / 1000.0;
		double lat = 0.0;
		double lon = 0.0;
		
		DataBlock dataBlock = trekData.createDataBlock();
		dataBlock.setDoubleValue(0, time);
		dataBlock.setDoubleValue(1, lat);
		dataBlock.setDoubleValue(2, lon);
		/*
		*/
		
		latestRecord = dataBlock;
		latestRecordTime = System.currentTimeMillis();
		eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, Trek1000Output.this, dataBlock));
	}
	
	public boolean isConnect()
	{
		return false;
	}
	
	@Override
	public String getName()
	{
		return "Trek_1000_Output";
	}

	@Override
	public DataComponent getRecordDescription() {
		// TODO Auto-generated method stub
		return trekData;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		// TODO Auto-generated method stub
		return trekEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() {
		// TODO Auto-generated method stub
		return 1.0;
	}

    /** */
    public static class SerialReader implements Runnable 
    {
        InputStream in;
        
        public SerialReader ( InputStream in )
        {
            this.in = in;
        }
        
        public void run ()
        {
        }
    }
}
