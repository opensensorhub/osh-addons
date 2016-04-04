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
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.comm.TCPConfig;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.rtpcam.RTSPClient.StreamInfo;
import org.sensorhub.impl.sensor.videocam.BasicVideoConfig;
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
 * @param <SensorType> Type of parent sensor
 * @since Dec 12, 2015
 */
public class RTPVideoOutput<SensorType extends ISensorModule<?>> extends AbstractSensorOutput<SensorType> implements RTPH264Callback
{
    BasicVideoConfig videoConfig;
    TCPConfig netConfig;
    RTSPConfig rtspConfig;
    
    DataComponent dataStruct;
    BinaryEncoding dataEncoding;
    RTSPClient rtspClient;
    RTPH264Receiver rtpThread;
    RTCPSender rtcpThread;
    
    FileOutputStream fos;
    FileChannel fch;
    ExecutorService executor;
    boolean firstFrameReceived;
    
    
    public RTPVideoOutput(SensorType driver, BasicVideoConfig videoConfig, TCPConfig netConfig, RTSPConfig rtspConfig)
    {
        this(driver, "videoOutput", videoConfig, netConfig, rtspConfig);
    }
    
    
    public RTPVideoOutput(SensorType driver, String name, BasicVideoConfig videoConfig, TCPConfig netConfig, RTSPConfig rtspConfig)
    {
        super(driver);
        this.name = name;
        this.videoConfig = videoConfig;
        this.netConfig = netConfig;
        this.rtspConfig = rtspConfig;
    }
    
    
    @Override
    public String getName()
    {
        return name;
    }
    
    
    @Override
    public void init() throws SensorException
    {
        // create SWE Common data structure
        SWEHelper fac = new SWEHelper();
        dataStruct = fac.newDataRecord(2);
        dataStruct.setName(getName());
        
        // time stamp
        Time time = fac.newTimeStampIsoUTC();
        dataStruct.addComponent("time", time);
        
        // video frame
        int width = videoConfig.getResolution().getWidth();
        int height = videoConfig.getResolution().getHeight();
        DataArray img = fac.newRgbImage(width, height, DataType.BYTE);
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
    
    
    public void start() throws SensorException
    {
        // open backup file
        try
        {
            if (videoConfig.backupFile != null)
            {
                fos = new FileOutputStream(videoConfig.backupFile);
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
        try
        {
            rtspClient = new RTSPClient(
                    netConfig.remoteHost,
                    rtspConfig.rtspPort,
                    rtspConfig.videoPath,
                    netConfig.user,
                    netConfig.password,
                    rtspConfig.localUdpPort);
            try
            {
                rtspClient.sendOptions();
                rtspClient.sendDescribe();
                rtspClient.sendSetup();   
            }
            catch (SocketTimeoutException e)
            {
                // for solo that doesn't have any RTSP server
                RTPCameraDriver.log.warn("RTSP server not responding but video stream may still be playing OK");
                rtspClient = null;
            }
            
            // look for H264 stream
            StreamInfo h264Stream = null;
            int streamIndex = 0;
            int i = 0;
            if (rtspClient != null)
            {
                for (StreamInfo stream: rtspClient.getMediaStreams())
                {
                    if (stream.codecString.contains("H264"))
                    {
                        h264Stream = stream;
                        streamIndex = i;                        
                    }
                    
                    i++;
                }
                
                if (h264Stream == null)
                    throw new IOException("No stream with supported codec found");
            }
        
            // start RTP receiving thread
            rtpThread = new RTPH264Receiver(netConfig.remoteHost, rtspConfig.localUdpPort, this);            
            // transfer parameter sets if we received them via RTSP
            if (rtspClient != null && h264Stream.paramSets != null)
                rtpThread.setParameterSets(h264Stream.paramSets);
            rtpThread.start();
            
            // play stream with RTSP if server responded to SETUP
            if (rtspClient != null)
            {
                // send PLAY request
                rtspClient.sendPlay(streamIndex);
                
                // start RTCP sending thread
                rtcpThread = new RTCPSender(netConfig.remoteHost, rtspConfig.localUdpPort+1, rtspClient.getRemoteRtcpPort(), 1000, rtspClient);
                rtcpThread.start();
            }
        }
        catch (IOException e)
        {
            throw new SensorException("Error while starting playback from RTP server", e);            
        } 
    }


    @Override
    public double getAverageSamplingPeriod()
    {
        return 1.0 / videoConfig.frameRate;
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
        if (rtcpThread != null)
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
