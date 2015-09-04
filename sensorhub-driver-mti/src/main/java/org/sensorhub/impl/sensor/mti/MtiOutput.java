/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.mti;

import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.sensor.SensorDataEvent;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Vector;
import org.vast.swe.SWEHelper;


public class MtiOutput extends AbstractSensorOutput<MtiSensor>
{
    protected final static byte PREAMBLE = (byte)0xFA;
    private final static byte BUS_ID = (byte)0xFF;
    private final static byte MSG_ID = (byte)0x32;
    private final static int MSG_SIZE = 62; 
    
    DataComponent imuData;
    DataEncoding dataEncoding;
    boolean started;
    
    DataInputStream dataIn;
    byte[] msgBytes = new byte[MSG_SIZE];
    ByteBuffer msgBuf = ByteBuffer.wrap(msgBytes);
    
    int decimFactor = 1;
    int sampleCounter;
    float temp;
    float[] gyro = new float[3];
    float[] accel = new float[3];
    float[] mag = new float[3];
    float[] quat = new float[4];
    
    
    public MtiOutput(MtiSensor parentSensor)
    {
        super(parentSensor);
    }


    @Override
    public String getName()
    {
        return "imuData";
    }


    @Override
    protected void init()
    {
        SWEHelper fac = new SWEHelper();
        
        // build SWE Common record structure
        imuData = fac.newDataRecord(4);
        imuData.setName(getName());
        imuData.setDefinition("http://sensorml.com/ont/swe/property/ImuData");
        
        String localRefFrame = parentSensor.getCurrentDescription().getUniqueIdentifier() + "#" + MtiSensor.CRS_ID;
                        
        // time stamp
        imuData.addComponent("time", fac.newTimeStampIsoUTC());
        
        // raw inertial measurements
        Vector angRate = fac.newAngularVelocityVector(
                SWEHelper.getPropertyUri("AngularRate"),
                localRefFrame,
                "deg/s");
        angRate.setDataType(DataType.FLOAT);
        imuData.addComponent("angRate", angRate);
        
        Vector accel = fac.newAccelerationVector(
                SWEHelper.getPropertyUri("Acceleration"),
                localRefFrame,
                "m/s2");
        accel.setDataType(DataType.FLOAT);
        imuData.addComponent("accel", accel);
        
        // integrated measurements
        Vector quat = fac.newQuatOrientationENU(
                SWEHelper.getPropertyUri("Orientation"));
        quat.setDataType(DataType.FLOAT);
        imuData.addComponent("attitude", quat);
     
        // also generate encoding definition as text block
        dataEncoding = fac.newTextEncoding(",", "\n");        
    }
    

    /* TODO: only using HV message; add support for HT and ML */
    private void pollAndSendMeasurement()
    {
    	long msgTime = System.currentTimeMillis();
    	
        // decode message
    	if (!decodeNextMessage())
    	    return;
         
        // create and populate datablock
    	DataBlock dataBlock;
    	if (latestRecord == null)
    	    dataBlock = imuData.createDataBlock();
    	else
    	    dataBlock = latestRecord.renew();
    	
    	int k = 0;
        dataBlock.setDoubleValue(k++, msgTime / 1000.);
        for (int i=0; i<3; i++, k++)
            dataBlock.setFloatValue(k, gyro[i]);
        for (int i=0; i<3; i++, k++)
            dataBlock.setFloatValue(k, accel[i]);
        for (int i=0; i<4; i++, k++)
            dataBlock.setFloatValue(k, quat[i]);
        
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = msgTime;
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, MtiOutput.this, dataBlock));        
    }
    
    
    protected boolean decodeNextMessage()
    {
        try
        {
            // wait for sync
            byte b = 0;
            while (b != PREAMBLE)
                b = dataIn.readByte();
            
            // read message fully
            dataIn.readFully(msgBytes);
            
            sampleCounter++;
            if (sampleCounter % decimFactor != 0)
                return false;
                        
            // validate checksum
            int checksum = 0;
            for (int i=0; i<MSG_SIZE; i++)
            {
                b = msgBytes[i];
                checksum += (b & 0xFF);
            }
            if ((checksum & 0xFF) != 0)
            {
                MtiSensor.log.error("Wrong message checksum");
                return false;
            }
            
            // check header bytes
            msgBuf.clear();
            b = msgBuf.get();
            if (b != BUS_ID)
                return false;
            b = msgBuf.get();
            if (b != MSG_ID)
                return false;
            b = msgBuf.get();
            if (b != MSG_SIZE-4)
                return false;
            
            // get measurement values
            temp = msgBuf.getFloat();
            accel[0] = msgBuf.getFloat();
            accel[1] = msgBuf.getFloat();
            accel[2] = msgBuf.getFloat();
            gyro[0] = msgBuf.getFloat();
            gyro[1] = msgBuf.getFloat();
            gyro[2] = msgBuf.getFloat();
            mag[0] = msgBuf.getFloat();
            mag[1] = msgBuf.getFloat();
            mag[2] = msgBuf.getFloat();
            quat[0] = msgBuf.getFloat();
            quat[1] = msgBuf.getFloat();
            quat[2] = msgBuf.getFloat();
            quat[3] = msgBuf.getFloat();
        }
        catch (EOFException e)
        {
            started = false;
        }
        catch (IOException e)
        {
            MtiSensor.log.error("Error while decoding IMU stream. Stopping", e);
            started = false;
        }
        
        return true;
    }


    protected void start(ICommProvider<?> commProvider)
    {
        if (started)
            return;
        
        started = true;
        sampleCounter = -1;
        
        // connect to data stream
        try
        {
            dataIn = new DataInputStream(commProvider.getInputStream());
            MtiSensor.log.info("Connected to IMU data stream");
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error while initializing communications ", e);
        }
        
        // start main measurement thread
        Thread t = new Thread(new Runnable()
        {
            public void run()
            {
                while (started)
                {
                    pollAndSendMeasurement();
                }                

                dataIn = null;
            }
        });
        t.start();
    }


    protected void stop()
    {
        started = false;
        
        if (dataIn != null)
        {
            try { dataIn.close(); }
            catch (IOException e) { }
        }
    }


    @Override
    public double getAverageSamplingPeriod()
    {
    	return 0.01;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return imuData;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return dataEncoding;
    }
}
