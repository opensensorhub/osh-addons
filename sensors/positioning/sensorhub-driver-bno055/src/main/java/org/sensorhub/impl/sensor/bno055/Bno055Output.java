/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.bno055;

import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.sensor.SensorDataEvent;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Vector;
import org.vast.swe.DataInputStreamLI;
import org.vast.swe.DataOutputStreamLI;
import org.vast.swe.helper.GeoPosHelper;


/**
 * <p>
 * Implementation of the absolute orientation quaternion output
 * </p>
 *
 * @author Alex Robin
 * @since Apr 7, 2016
 */
public class Bno055Output extends AbstractSensorOutput<Bno055Sensor>
{
    private final static byte[] READ_QUAT_CMD =
    {
        Bno055Constants.START_BYTE,
        Bno055Constants.DATA_READ,
        Bno055Constants.QUAT_DATA_W_LSB_ADDR,
        0x08
    };
    
    private final static double QUAT_SCALE = 1<<14;
    
    
    DataComponent imuData;
    DataEncoding dataEncoding;
    Timer timer;
    DataInputStreamLI dataIn;
    DataOutputStreamLI dataOut;
    
    int decimFactor = 1;
    int sampleCounter;
    float temp;
    float[] gyro = new float[3];
    float[] accel = new float[3];
    float[] mag = new float[3];
    float[] quat = new float[4];
    
    
    public Bno055Output(Bno055Sensor parentSensor)
    {
        super(parentSensor);
    }


    @Override
    public String getName()
    {
        return "imuData";
    }


    protected void init()
    {
        GeoPosHelper fac = new GeoPosHelper();
        
        // build SWE Common record structure
        imuData = fac.newDataRecord(4);
        imuData.setName(getName());
        imuData.setDefinition("http://sensorml.com/ont/swe/property/ImuData");
        
        String localFrame = parentSensor.getUniqueIdentifier() + "#" + Bno055Sensor.CRS_ID;
                        
        // time stamp
        imuData.addComponent("time", fac.newTimeStampIsoUTC());
        
        // raw inertial measurements
        /*Vector angRate = fac.newAngularVelocityVector(
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
        imuData.addComponent("accel", accel);*/
        
        // integrated measurements
        //fac.newEulerOrientationENU(def)
        Vector quat = fac.newQuatOrientationENU(null);
        quat.setDataType(DataType.FLOAT);
        quat.setLocalFrame(localFrame);
        imuData.addComponent("attitude", quat);
     
        // also generate encoding definition as text block
        dataEncoding = fac.newTextEncoding(",", "\n");        
    }
    

    /* TODO: only using HV message; add support for HT and ML */
    private void pollAndSendMeasurement()
    {
    	long msgTime = System.currentTimeMillis();
    	
        // decode message
	    try
        {
            ByteBuffer resp = parentSensor.sendReadCommand(READ_QUAT_CMD);
            
            // read 4 quaternion components (scalar first)
            quat[3] = (float)(resp.getShort() / QUAT_SCALE);
            for (int i=0; i<3; i++)
                quat[i] = (float)(resp.getShort() / QUAT_SCALE);
        }
        catch (IOException e)
        {
            // skip measurement if there is a bus error
            return;
        }
         
        // create and populate datablock
    	DataBlock dataBlock;
    	if (latestRecord == null)
    	    dataBlock = imuData.createDataBlock();
    	else
    	    dataBlock = latestRecord.renew();
    	
    	int k = 0;
        dataBlock.setDoubleValue(k++, msgTime / 1000.);
        /*for (int i=0; i<3; i++, k++)
            dataBlock.setFloatValue(k, gyro[i]);
        for (int i=0; i<3; i++, k++)
            dataBlock.setFloatValue(k, accel[i]);*/
        for (int i=0; i<4; i++, k++)
            dataBlock.setFloatValue(k, quat[i]);
        
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = msgTime;
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, Bno055Output.this, dataBlock));        
    }
   

    protected void start(ICommProvider<?> commProvider)
    {
        sampleCounter = -1;
        dataIn = parentSensor.dataIn;
        dataOut = parentSensor.dataOut;        
        
        // start main measurement thread
        TimerTask t = new TimerTask()
        {
            public void run()
            {
                pollAndSendMeasurement();
            }
        };
        
        timer = new Timer();
        timer.schedule(t, 0, (long)(getAverageSamplingPeriod()*1000));
    }


    protected void stop()
    {
    	if (timer != null)
    	{
	        timer.cancel();
	        timer = null;
    	}
    }


    @Override
    public double getAverageSamplingPeriod()
    {
    	return 0.1;
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
