
package org.sensorhub.test.sensor.AHRS;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.ahrs.AHRSConfig;
import org.sensorhub.impl.sensor.ahrs.AHRSSensor;


public class AHRSSensorTest 
{
	public static void main(String[] args)
	{
		// Create serial object 
		AHRSConfig mcfg = new AHRSConfig();
		
		// Create instance of AHRSSensor class
		AHRSSensor ahrs = new AHRSSensor();
		// Check for parent/super classes - for debugging purposes
		//ahrs.superclassCheck();

		try 
		{
			// Set serial parameters
			ahrs.init(mcfg);
		} 
		catch (SensorHubException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Poll sensor
		try 
		{
			ahrs.start();
		} 
		catch (SensorHubException e) 
		{
			e.printStackTrace();
		}
		
        try 
        {
            Thread.sleep(15000); // Let it run for 15 seconds
        } 
        catch(InterruptedException e)
        {
            System.out.println("Exception: "+e+", in Sleep thread in class AttCom");
        }    
        
        // Tell it to stop ...
        try 
        {
			ahrs.stop();
		} 
        catch (SensorHubException e) 
        {
			e.printStackTrace();
		}
        
		System.out.println("End of main ...\n");
	}

}
