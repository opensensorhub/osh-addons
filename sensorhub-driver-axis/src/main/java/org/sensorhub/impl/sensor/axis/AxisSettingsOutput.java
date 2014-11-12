package org.sensorhub.impl.sensor.axis;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.common.BasicEventHandler;
import org.vast.cdm.common.AsciiEncoding;
import org.vast.cdm.common.DataBlock;
import org.vast.cdm.common.DataComponent;
import org.vast.cdm.common.DataEncoding;
import org.vast.cdm.common.DataType;
import org.vast.data.ConstraintList;
import org.vast.data.DataGroup;
import org.vast.data.DataValue;
import org.vast.sweCommon.IntervalConstraint;
import org.vast.sweCommon.SweConstants;


public class AxisSettingsOutput implements ISensorDataInterface
{
	AxisCameraDriver driver;
    IEventHandler eventHandler;
    DataComponent settingsDataStruct;
    DataBlock latestRecord;
    boolean polling;

    // Setup ISO Time Components
    // Set TimeZone to "UTC" ????
	TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
    
    String ipAddress;

    
    public AxisSettingsOutput(AxisCameraDriver driver)
    {
    	this.driver = driver;
        this.eventHandler = new BasicEventHandler();    	
    }
    
    protected void init()
    {
  
        df.setTimeZone(tz);
        ipAddress = driver.getConfiguration().ipAddress;
        
    	// TODO: Need to generalize this by first checking if PTZ supported
        
        
        try
        {
        	// request PTZ Limits
        	URL optionsURL = new URL("http://" + ipAddress + "/axis-cgi/view/param.cgi?action=list&group=PTZ.Limit");
			InputStream is = optionsURL.openStream();
			BufferedReader limitReader = new BufferedReader(new InputStreamReader(is));
			
			// set default values
			double minPan = -180.0;
			double maxPan = 180.0;
			double minTilt = -180.0;
			double maxTilt = 0.0;
			double maxZoom = 13333;
			
			// get limit values from IP stream
			String line;
			while ((line = limitReader.readLine()) != null)
			{				
				// parse response
				String[] tokens = line.split("=");
			
				if (tokens[0].trim().equalsIgnoreCase("root.PTZ.Limit.L1.MinPan"))
					minPan = Double.parseDouble(tokens[1]);	
				else if (tokens[0].trim().equalsIgnoreCase("root.PTZ.Limit.L1.MaxPan"))
					maxPan = Double.parseDouble(tokens[1]);	
				else if (tokens[0].trim().equalsIgnoreCase("root.PTZ.Limit.L1.MinTilt"))
					minTilt = Double.parseDouble(tokens[1]);	
				else if (tokens[0].trim().equalsIgnoreCase("root.PTZ.Limit.L1.MaxTilt"))
					maxTilt = Double.parseDouble(tokens[1]);	
				else if (tokens[0].trim().equalsIgnoreCase("root.PTZ.Limit.L1.MaxZoom"))
					maxZoom = Double.parseDouble(tokens[1]);	
			}


            
    	// Build SWE Common Data structure
         settingsDataStruct = new DataGroup(3, "cameraSettings");
         
         DataValue c;
         ConstraintList constraints;
         
         c = new DataValue("time", DataType.DOUBLE);
         c.setProperty(SweConstants.UOM_URI, SweConstants.ISO_TIME_DEF);
         c.setProperty(SweConstants.DEF_URI, SweConstants.DEF_SAMPLING_TIME);
         settingsDataStruct.addComponent(c);
         
         c = new DataValue("pan", DataType.FLOAT);
         c.setProperty(SweConstants.UOM_CODE, "deg");
         c.setProperty(SweConstants.DEF_URI, "http://sensorml.com/ont/swe/property/Pan" );
         constraints = new ConstraintList();
         constraints.add(new IntervalConstraint(minPan, maxPan));
         c.setConstraints(constraints);
         settingsDataStruct.addComponent(c);

         c = new DataValue("tilt", DataType.FLOAT);
         c.setProperty(SweConstants.UOM_CODE, "deg");
         c.setProperty(SweConstants.DEF_URI, "http://sensorml.com/ont/swe/property/Tilt" );
         constraints = new ConstraintList();
         constraints.add(new IntervalConstraint(minTilt,maxTilt));
         c.setConstraints(constraints);
         settingsDataStruct.addComponent(c);

         c = new DataValue("zoomFactor", DataType.INT);
         c.setProperty(SweConstants.DEF_URI, "http://sensorml.com/ont/swe/property/AxisZoomFactor" );
         constraints = new ConstraintList();
         constraints.add(new IntervalConstraint(0, maxZoom));
         c.setConstraints(constraints);
         settingsDataStruct.addComponent(c);

         c = new DataValue("brightnessFactor", DataType.INT);
         c.setProperty(SweConstants.DEF_URI, "http://sensorml.com/ont/swe/property/AxisBrightnessFactor" );
         settingsDataStruct.addComponent(c);

         c = new DataValue("autofocus", DataType.BOOLEAN);
         c.setProperty(SweConstants.DEF_URI, "http://sensorml.com/ont/swe/property/AutoFocusEnabled" );
         settingsDataStruct.addComponent(c);
        
         
         
         // start the thread (probably best not to start in init but in driver start() method.)
         startPolling();
         
         
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
      

    }
    
    
    protected void startPolling()
    {
    	try
		{
    		//String ipAddress = driver.getConfiguration().ipAddress;
    		final URL getSettingsUrl = new URL("http://" + ipAddress + "/axis-cgi/com/ptz.cgi?query=position");
			polling = true;
			
			Thread t = new Thread(new Runnable()
	    	{
				@Override
				public void run()
				{
					DataComponent dataStruct = settingsDataStruct.copy();
					dataStruct.assignNewDataBlock();
					
					while (polling)
					{
						// send http query
						try
						{
							InputStream is = getSettingsUrl.openStream();
							BufferedReader reader = new BufferedReader(new InputStreamReader(is));
							dataStruct.renewDataBlock();
							
							// get Time and assign to datablock (put it in UTC?)
							// should be a SWE Common function; may be a faster way
							//TimeZone tz = TimeZone.getTimeZone("UTC");
						    //DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
							//df.setTimeZone(tz);
							
							// ALEX: should this be a String?
							dataStruct.getComponent("time").getData().setStringValue(df.format(new Date()));
							

							String line;
							while ((line = reader.readLine()) != null)
							{
								
								
								// parse response
								String[] tokens = line.split("=");
							
								if (tokens[0].trim().equalsIgnoreCase("pan"))
								{
									float val = Float.parseFloat(tokens[1]);
									dataStruct.getComponent("pan").getData().setFloatValue(val);
								}
								else if (tokens[0].trim().equalsIgnoreCase("tilt"))
								{
									float val = Float.parseFloat(tokens[1]);
									dataStruct.getComponent("tilt").getData().setFloatValue(val);
									
								}
								else if (tokens[0].trim().equalsIgnoreCase("zoomFactor"))
								{
									float val = Float.parseFloat(tokens[1]);
									dataStruct.getComponent("zoom").getData().setFloatValue(val);
									
								}
								else if (tokens[0].trim().equalsIgnoreCase("brightness"))
								{
									float val = Float.parseFloat(tokens[1]);
									dataStruct.getComponent("brightnessFactor").getData().setFloatValue(val);
									
								}
								else if (tokens[0].trim().equalsIgnoreCase("autofocus"))
								{
									if (tokens[1].trim().equalsIgnoreCase("on"))
										dataStruct.getComponent("autofocus").getData().setBooleanValue(true);
									else 
										dataStruct.getComponent("autofocus").getData().setBooleanValue(false);
									
								}
							}
							
							latestRecord = dataStruct.getData();
							long time = System.currentTimeMillis();
							eventHandler.publishEvent(new SensorDataEvent(AxisSettingsOutput.this, time, settingsDataStruct, latestRecord));
							
							// TODO use a timer; set for every 1 second
							Thread.sleep(1000);
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					};
	 			}    		
	    	});
			
			t.start();
		}
    	catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    }
    

	@Override
	public ISensorModule<?> getSensorInterface()
	{
		return driver;
	}

	@Override
	public boolean isEnabled()
	{
		return true;
	}

	@Override
	public boolean isStorageSupported()
	{
		return false;
	}

	@Override
	public boolean isPushSupported()
	{
		return true;
	}

	@Override
	public double getAverageSamplingPeriod()
	{
		// assuming 30 frames per second
		return 1/30.0;
	}

	@Override
	public DataComponent getRecordDescription() throws SensorException
	{
		return settingsDataStruct;
	}

	@Override
	public DataEncoding getRecommendedEncoding() throws SensorException
	{
		// Token = "," Block = "\n"
		return new AsciiEncoding("\n",",");
	}

	@Override
	public DataBlock getLatestRecord() throws SensorException
	{
		return latestRecord;
	}

	@Override
	public int getStorageCapacity() throws SensorException
	{
		return 0;
	}

	@Override
	public int getNumberOfAvailableRecords() throws SensorException
	{
		return 1;
	}

	@Override
	public List<DataBlock> getLatestRecords(int maxRecords, boolean clear) throws SensorException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<DataBlock> getAllRecords(boolean clear) throws SensorException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int clearAllRecords() throws SensorException
	{
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public void registerListener(IEventListener listener)
	{
		eventHandler.registerListener(listener);
	}

	@Override
	public void unregisterListener(IEventListener listener)
	{
		eventHandler.unregisterListener(listener);
	}



}
