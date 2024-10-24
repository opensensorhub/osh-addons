package org.sensorhub.test.impl.sensor.twitter;

import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.event.Event;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.twitter.TwitterConfig;
import org.sensorhub.impl.sensor.twitter.TwitterSensor;
import org.vast.sensorML.SMLUtils;
import org.vast.swe.SWEUtils;
import static org.junit.Assert.*;

import java.util.UUID;

import org.junit.After;

import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataComponent;

public class TestTwitterDriver implements IEventListener
{
	TwitterSensor driver;
	TwitterConfig config;
	int sampleCount = 0;
	
	@Before
	public void init() throws Exception
	{
		config = new TwitterConfig();
		config.id = UUID.randomUUID().toString();
		config.apiKey = "";
		config.apiSecret = "";
		config.accessToken = "";
		config.accessTokenSecret = "";
		config.keywords.add("#");
		
		driver = new TwitterSensor();
		driver.init(config);
	}
	
	@Test
	public void testGetOutputDesc() throws Exception
	{
		for (IStreamingDataInterface di: driver.getObservationOutputs().values())
		{
			System.out.println();
			DataComponent dataMsg = di.getRecordDescription();
            new SWEUtils(SWEUtils.V2_0).writeComponent(System.out, dataMsg, false, true);
		}
	}
	
	@Test
	public void testGetSensorDesc() throws Exception
	{
		System.out.println();
		AbstractProcess smlDesc = driver.getCurrentDescription();
		new SMLUtils(SWEUtils.V2_0).writeProcess(System.out, smlDesc, true);
	}
	
	@Test
	public void testSendMeasurements() throws Exception
	{
		System.out.println();

		IStreamingDataInterface twitterOutput = driver.getObservationOutputs().get("Twitter_Output");
		twitterOutput.registerListener(this);

		/*
        driver.start();

		synchronized (this)
		{
			while (sampleCount < 2)
			{
				wait();
			}
		}

        driver.stop();
		*/

		System.out.println();
	}
	
	@Override
	public void handleEvent(Event e)
	{
		assertTrue(e instanceof DataEvent);
		DataEvent dataEvent = (DataEvent)e;
		
		System.out.println("\nNo. " + Integer.toString(sampleCount) + ": New data received from sensor " + dataEvent.getSystemUID());
		sampleCount++;
		
		if (sampleCount >= 2) {
			try {
			    driver.stop();
			}
            catch (SensorHubException e1) {
                e1.printStackTrace();
            }
		}

		synchronized (this) { this.notify(); }
	}

    @After
    public void cleanup() throws Exception
    {
		driver.stop();
    }
}
