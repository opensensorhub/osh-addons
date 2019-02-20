package test.datasimulation;

import datasimulation.GPSsim;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by Ian Patterson on 6/12/2017.
 */
public class GPSsimTest
{
	@Test
	public void randomTrajTest() throws Exception
	{
		GPSsim test = new GPSsim();
		double[] locTest_1;
		GPSsim.generateRandomTrajectory();
		locTest_1 = GPSsim.sendMeasurement();
		System.out.println(locTest_1[0]);
	}
}