package org.sensorhub.test.impl.sensor.simulatedcbrn;

import org.junit.Test;
import datasimulation.CBRNSimulatedData;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Created by Ian Patterson on 5/9/2017.
 */
public class CBRNSimulatedDataTest {
	@Test
	public void getInstance() throws Exception
	{
		assertNotNull(CBRNSimulatedData.getInstance());
	}

	@Test
	public void simTemp() throws Exception {
	}

	@Test
	public void getTemp() throws Exception {
	}

	@Test
	public void getMaxThreatAgent() throws Exception
	{
		assertNotNull(CBRNSimulatedData.getInstance().getDetectedAgent());
	}

	@Test
	public void setMaxThreatAgent() throws Exception {
	}

	@Test
	public void update() throws Exception
	{
		double benchmark = CBRNSimulatedData.getInstance().getDetectedAgent().getThreatLevel();
		TimeUnit.SECONDS.sleep(1);
		CBRNSimulatedData.getInstance().update();
		assertNotEquals(benchmark, CBRNSimulatedData.getInstance().getDetectedAgent().getThreatLevel());
	}

	@Test
	public void getWarnStatus() throws Exception
	{
		CBRNSimulatedData.getInstance().getDetectedAgent().setThreatLevel(0.0);
		CBRNSimulatedData.getInstance().autoSetWarnStatus();
		System.out.println(CBRNSimulatedData.getInstance().getWarnStatus());
		for(int i = 0; i< 100; i++)
		{
			CBRNSimulatedData.getInstance().getDetectedAgent().setThreatLevel(
					CBRNSimulatedData.getInstance().getDetectedAgent().getThreatLevel() + 0.1);
			CBRNSimulatedData.getInstance().autoSetWarnStatus();
			System.out.println(CBRNSimulatedData.getInstance().getWarnStatus());
		}

		for(int i = 0; i< 100; i++)
		{
			CBRNSimulatedData.getInstance().getDetectedAgent().setThreatLevel(
					CBRNSimulatedData.getInstance().getDetectedAgent().getThreatLevel() - 0.1);
			CBRNSimulatedData.getInstance().autoSetWarnStatus();
			System.out.println(CBRNSimulatedData.getInstance().getWarnStatus());
		}

	}

}