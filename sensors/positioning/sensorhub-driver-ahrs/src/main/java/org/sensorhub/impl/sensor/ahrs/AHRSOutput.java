package org.sensorhub.impl.sensor.ahrs;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Vector;


public class AHRSOutput extends AbstractSensorOutput<AHRSSensor> 
{
    protected final static byte PREAMBLE = (byte)0xFA;
    private final static int MSG_SIZE = 1024; 
    
    DataComponent ahrsData;
    DataEncoding dataEncoding;
    boolean started; 
    
    DataInputStream dataIn;
    DataOutputStream dataOut;
   
    byte[] msgBytes = new byte[MSG_SIZE];
    ByteBuffer msgBuf = ByteBuffer.wrap(msgBytes);
    
    int decimFactor = 1;
    int sampleCounter;
    float temp;
    
    // Mike mods -------------------------------------------------------------------------------
    float[] att = new float[3];
    int packetID;
    
    // End Mike mods -------------------------------------------------------------------------------
    
    public AHRSOutput(AHRSSensor parentSensor)
    {
        super(parentSensor);
    }

    @Override
    public String getName()
    {
    	return "ahrsData";
    }
   
    protected void init()
    {
    	GeoPosHelper fac = new GeoPosHelper();
    	
    	// build SWE Common record structure
    	ahrsData = fac.newDataRecord(2);
    	ahrsData.setName(getName());
//    	imuData.setDefinition("http://sensorml.com/ont/swe/property/ImuData");
    	ahrsData.setDefinition("http://sensorml.com/ont/swe/property/ImuData");

    	String localRefFrame = parentSensor.getUniqueIdentifier() + "#" + AHRSSensor.CRS_ID;
    	// time stamp
    	ahrsData.addComponent("time", fac.newTimeStampIsoUTC());
      
    	// raw inertial measurements
//    	Vector angRate = fac.newAngularVelocityVector(
//              SWEHelper.getPropertyUri("AngularRate"),
//              localRefFrame,
//              "deg/s");
//    	angRate.setDataType(DataType.FLOAT);
//    	imuData.addComponent("angRate", angRate);
//      
//    	Vector accel = fac.newAccelerationVector(
//              SWEHelper.getPropertyUri("Acceleration"),
//              localRefFrame,
//              "m/s2");
//    	accel.setDataType(DataType.FLOAT);
//    	imuData.addComponent("accel", accel);
    	
    	// integrated measurements
//    	Vector quat = fac.newQuatOrientationENU(
//              SWEHelper.getPropertyUri("Orientation"));
//    	quat.setDataType(DataType.FLOAT);
//    	imuData.addComponent("attitude", quat);

    	Vector att = fac.newEulerOrientationECEF(
                SWEHelper.getPropertyUri("Attitude"));
      	att.setDataType(DataType.FLOAT);
      	ahrsData.addComponent("Attitude", att);
   
    	// also generate encoding definition as text block
    	dataEncoding = fac.newTextEncoding(",", "\n");        
    }
    
    private void pollAndSendMeasurement()
    {
    	long msgTime = System.currentTimeMillis();
    	
        // decode message
    	if (!decodeNextMessage())
    	    return;
         
        // create and populate datablock
    	DataBlock dataBlock;
    	if (latestRecord == null)
    	    dataBlock = ahrsData.createDataBlock();
    	else
    	    dataBlock = latestRecord.renew();
    	
    	int k = 0;
        dataBlock.setDoubleValue(k++, msgTime / 1000.);
        for (int i=0; i<3; i++, k++)
            dataBlock.setFloatValue(k, att[i]);
        
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = msgTime;
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, AHRSOutput.this, dataBlock));        
    }

    protected boolean decodeNextMessage()
//    public boolean decodeNextMessage()
    {
    	try
    	{
    		msgBuf.clear();
      	
    		dataOut.write(206);

    		int len = 0;
        
    		while( len < 19  && len > -1 )
    		{
    			len = dataIn.available();
    		}

    		len = dataIn.read(msgBytes);

    		sampleCounter++;
    		if (sampleCounter % decimFactor != 0)
    		{	
    			return false;
    		}          
    		
    		// ID = 206
    		packetID = ((int) msgBuf.get()) & 0xFF;
          
    		/**** ROLL ***********************************************/
         
    		att[0] = msgBuf.getFloat();
    		// Convert from radians to degrees
    		att[0] = att[0] * 180.0f / (float)Math.PI;

    		/**** PITCH ***********************************************/

    		att[1] = msgBuf.getFloat();
    		// Convert from radians to degrees
    		att[1] = att[1] * 180.0f / (float)Math.PI;
    		
    		/**** HEADING ***********************************************/

    		att[2] = msgBuf.getFloat();
    		// Convert from radians to degrees
    		att[2] = att[2] * 180.0f / (float)Math.PI;
      
    		System.out.format("%6.0f   %6.0f   %6.0f\n",att[0],att[1],att[2]);
 
    	}
    	catch (IOException e)
    	{
    		// log error except when stopping voluntarily
    		if (started)
    		{	
    			AHRSSensor.log.error("Error while decoding IMU stream. Stopping", e);
    		}
    		started = false;
    		return false;
    	}
      
    	return true;
    }
 
//    protected void start(ICommProvider<?> commProvider)
    public void start(ICommProvider<?> commProvider)
    {
    	if (started)
    	{	
    		return;
    	}
      
    	started = true;
    	sampleCounter = -1;
      
    	// connect to data stream
    	try
    	{
    		dataIn = new DataInputStream(commProvider.getInputStream());
    		dataOut = new DataOutputStream(commProvider.getOutputStream());
    		AHRSSensor.log.info("Connected to AHRS data stream");
    	}
    	catch (IOException e)
    	{
    		throw new RuntimeException("Error while initializing communications ", e);
    	}
      
    	// start main measurement thread
    	Runnable r = new t();
    	new Thread(r).start();
  	}
    
  	public class t implements Runnable
  	{
  		public t()
  		{
  		}
  	
  		public void run()
  		{
  			while(started)
  			{
  				pollAndSendMeasurement();
  			}                

  			try 
  			{
  				dataIn.close();
  				dataOut.close();
  			} 
  			catch (IOException e) 
  			{
  				// TODO Auto-generated catch block
  				e.printStackTrace();
  			}
          
//  			System.out.println("Exiting thread ...\n");

  			dataIn = null;
      	}
  	}
 
  	protected void stop()
//  	public void stop()
  	{
  		started = false;
      
  		if (dataIn != null)
  		{
  			try 
  			{ 
  				dataIn.close();
  				dataOut.close();
//  				System.out.println("Closed data port objects ...\n");
  			}
  			catch (IOException e) { }
  		}
  	}

  	//  @Override
  	public double getAverageSamplingPeriod()
  	{
  		return 0.01;
  	}
  
  	//  @Override
  	public DataComponent getRecordDescription()
  	{
  		return ahrsData;
  	}

  	//  @Override
  	public DataEncoding getRecommendedEncoding()
  	{
  		return dataEncoding;
  	}


}
