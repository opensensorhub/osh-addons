/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.rtpcam;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.opengis.swe.v20.BinaryBlock;
import net.opengis.swe.v20.BinaryComponent;
import net.opengis.swe.v20.BinaryEncoding;
import net.opengis.swe.v20.ByteEncoding;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Time;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.cdm.common.CDMException;
import org.vast.data.AbstractDataBlock;
import org.vast.data.DataBlockMixed;
import org.vast.swe.SWEHelper;


/**
 * <p>
 * Implementation of data interface for RTP camera stream
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Dec 12, 2015
 */
public class RTPVideoOutput extends AbstractSensorOutput<RTPCameraDriver> implements RTPH264Callback
{
    DataComponent dataStruct;
    BinaryEncoding dataEncoding;
    RTSPClient rtspClient;
    RTPH264Receiver rtpThread;
    RTCPSender rtcpThread;
    
    FileOutputStream fos;
    FileChannel fch;
    ExecutorService executor;
    boolean firstFrameReceived;
    
    
    protected RTPVideoOutput(RTPCameraDriver driver)
    {
        super(driver);
    }
    
    
    @Override
    public String getName()
    {
        return "camOutput";
    }
    
    
    @Override
    public void init() throws SensorException
    {
        RTPCameraConfig config = parentSensor.getConfiguration();
        
        // create SWE Common data structure
        SWEHelper fac = new SWEHelper();
        dataStruct = fac.newDataRecord(2);
        dataStruct.setName(getName());
        
        // time stamp
        Time time = fac.newTimeStampIsoUTC();
        dataStruct.addComponent("time", time);
        
        // video frame
        DataArray img = fac.newRgbImage(config.frameWidth, config.frameHeight, DataType.BYTE);
        img.setDefinition("http://sensorml.com/ont/swe/property/VideoFrame");
        dataStruct.addComponent("videoFrame", img);
        
        // SWE Common encoding
        dataEncoding = fac.newBinaryEncoding();
        dataEncoding.setByteEncoding(ByteEncoding.RAW);
        dataEncoding.setByteOrder(ByteOrder.BIG_ENDIAN);
        BinaryComponent timeEnc = fac.newBinaryComponent();
        timeEnc.setRef("/" + time.getName());
        timeEnc.setCdmDataType(DataType.DOUBLE);
        dataEncoding.addMemberAsComponent(timeEnc);
        BinaryBlock compressedBlock = fac.newBinaryBlock();
        compressedBlock.setRef("/" + img.getName());
        compressedBlock.setCompression("H264");
        dataEncoding.addMemberAsBlock(compressedBlock);
        
        // resolve encoding so compressed blocks can be properly generated
        try
        {
            SWEHelper.assignBinaryEncoding(dataStruct, dataEncoding);
        }
        catch (CDMException e)
        {
            throw new SensorException("Invalid output definition", e);
        }
    }
    
    
    public void start()
    {
        RTPCameraConfig config = parentSensor.getConfiguration();
        
        // open backup file
        try
        {
            if (config.backupFile != null)
            {
                fos = new FileOutputStream(config.backupFile);
                fch = fos.getChannel();
            }
        }
        catch (IOException e)
        {
            RTPCameraDriver.log.error("Error while opening backup file", e);
        }
        
        // start payload process executor
        executor = Executors.newSingleThreadExecutor();
        firstFrameReceived = false;
        
        // setup stream with RTSP server
        boolean useRtsp = true;
        try
        {
            rtspClient = new RTSPClient(
                    config.remoteHost,
                    config.remoteRtspPort,
                    config.videoPath,
                    config.rtspLogin,
                    config.rtspPasswd,
                    config.localUdpPort);
            try
            {
                rtspClient.sendDescribe();
                rtspClient.sendSetup();
            }
            catch (SocketTimeoutException e)
            {
                RTPCameraDriver.log.warn("RTSP server not responding but video stream may still be playing OK");
                useRtsp = false;
            }          
        
            // start RTP receiving thread
            rtpThread = new RTPH264Receiver(config.remoteHost, config.localUdpPort, this);
            rtpThread.start();
            
            // play stream with RTSP if server responded to SETUP
            if (useRtsp)
            {
                // start RTCP sending thread
                rtcpThread = new RTCPSender(config.remoteHost, config.localUdpPort+1, rtspClient.getRemoteRtcpPort(), 200);
                rtcpThread.start();
                
                // send PLAY request
                rtspClient.sendPlay();          
            }
        }
        catch (IOException e)
        {
            RTPCameraDriver.log.error("Error while starting playback from RTP server", e);
            config.enabled = false;
        } 
    }


    @Override
    public double getAverageSamplingPeriod()
    {
        return 1.0 / parentSensor.getConfiguration().frameRate;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return dataStruct;
    }
    
    
    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return dataEncoding;
    }
    
    
    @Override
    public void stop()
    {
        if (rtpThread != null)
            rtpThread.interrupt();
        
        if (rtcpThread != null)
            rtcpThread.stop();
        
        try
        {
            if (rtspClient != null)
                rtspClient.teardown();
        }
        catch (IOException e)
        {
        }
    }


    @Override
    public void onFrame(long timeStamp, int seqNum, ByteBuffer frameBuf, boolean packetLost)
    {
        rtcpThread.setStats(seqNum);
        
        if (!packetLost)
        {
            final byte[] frameBytes = new byte[frameBuf.limit()];
            frameBuf.get(frameBytes);
            
            executor.execute(new Runnable() {
                public void run()
                {            
                    if (!firstFrameReceived)
                    {
                        RTPCameraDriver.log.info("Connected to H264 RTP stream");
                        firstFrameReceived = true;
                    }
                    
                    if (fch != null)
                    {
                        try
                        {
                            fch.write(ByteBuffer.wrap(frameBytes));
                            fos.flush();
                        }
                        catch (IOException e)
                        {
                            RTPCameraDriver.log.error("Error while writing to backup file", e);
                        }
                    }
                    
                    // generate new data record
                    DataBlock newRecord;
                    if (latestRecord == null)
                        newRecord = dataStruct.createDataBlock();
                    else
                        newRecord = latestRecord.renew();
                    
                    // set time stamp
                    double samplingTime = System.currentTimeMillis() / 1000.0;
                    newRecord.setDoubleValue(0, samplingTime);
                    
                    // set encoded data
                    AbstractDataBlock frameData = ((DataBlockMixed)newRecord).getUnderlyingObject()[1];
                    frameData.setUnderlyingObject(frameBytes);
                    
                    // send event
                    latestRecord = newRecord;
                    latestRecordTime = System.currentTimeMillis();
                    eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, RTPVideoOutput.this, latestRecord));
                }
            });                
        }
    }


    @Override
    public void onError(Throwable e)
    {                
    }

}
