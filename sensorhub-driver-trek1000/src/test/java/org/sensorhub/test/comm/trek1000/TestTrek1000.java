package org.sensorhub.test.comm.trek1000;

import static org.junit.Assert.*;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.comm.trek1000.Trek1000Config;
import org.sensorhub.impl.comm.trek1000.Trek1000Sensor;
import org.vast.sensorML.SMLUtils;
import org.vast.swe.SWEUtils;

import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataComponent;

public class TestTrek1000 implements IEventListener
{
	Trek1000Sensor driver;
	Trek1000Config config;
	int sampleCount = 0;
	
	@Before
	public void init() throws Exception {
		config = new Trek1000Config();
		config.id = UUID.randomUUID().toString();
		config.serialPort = "/dev/tty.usbmodem1421";
		
		driver = new Trek1000Sensor();
		driver.setConfiguration(config);
		driver.init();
	}

	@Test
	public void testGetOutputDesc() throws Exception
	{
		for (ISensorDataInterface di: driver.getObservationOutputs().values())
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
	public void testTriangulation() {
		
	}

	@Test
	public void test() {
		try {
			driver.start();
			try {
				TimeUnit.SECONDS.sleep(5);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (SensorHubException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void handleEvent(Event<?> e) {
		assertTrue(e instanceof SensorDataEvent);
		SensorDataEvent newDataEvent = (SensorDataEvent)e;
		
		System.out.println("\nNo. " + Integer.toString(sampleCount) + 
				": New data received from sensor " + newDataEvent.getSensorID());
		sampleCount++;
		
		if (sampleCount >= 2) {
			try {
				driver.stop();
			} catch (SensorHubException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		synchronized (this) { this.notify(); }
	}
}
