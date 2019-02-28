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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataStream;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.rtpcam.RTSPClient.StreamInfo;
import org.sensorhub.impl.sensor.videocam.BasicVideoConfig;
import org.sensorhub.impl.sensor.videocam.VideoCamHelper;
import org.vast.data.AbstractDataBlock;
import org.vast.data.DataBlockMixed;


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
    RTSPConfig rtspConfig;
    
    DataComponent dataStruct;
    DataEncoding dataEncoding;
    RTSPClient rtspClient;
    RTPH264Receiver rtpThread;
    RTCPSender rtcpThread;
    
    FileOutputStream fos;
    FileChannel fch;
    ExecutorService executor;
    boolean firstFrameReceived;
    
    
    public RTPVideoOutput(SensorType driver)
    {
        this(driver, "video");
    }
    
    
    public RTPVideoOutput(SensorType driver, String name)
    {
        super(driver);
        this.name = name;
    }
    
    
    @Override
    public String getName()
    {
        return name;
    }
    
    
    public void init(int imgWidth, int imgHeight) throws SensorException
    {
        // create SWE Common data structure
        VideoCamHelper fac = new VideoCamHelper();        
        DataStream videoStream = fac.newVideoOutputH264(getName(), imgWidth, imgHeight);
        this.dataStruct = videoStream.getElementType();
        this.dataEncoding = videoStream.getEncoding();
    }
    
    
    public void start(BasicVideoConfig videoConfig, RTSPConfig rtspConfig, int timeout) throws SensorException
    {
        this.videoConfig = videoConfig;
        this.rtspConfig = rtspConfig;
        
        // open backup file
        try
        {
            if (videoConfig.backupFile != null)
            {
                File h264File = new File(videoConfig.backupFile);
                fos = new FileOutputStream(h264File);
                fch = fos.getChannel();
                log.info("Writing raw H264 data to " + h264File.getAbsolutePath());
            }
        }
        catch (IOException e)
        {
            log.error("Error while opening backup file", e);
        }
        
        // start payload process executor
        executor = Executors.newSingleThreadExecutor();
        firstFrameReceived = false;
        
        try
        {
            // setup stream with RTSP server
            rtspClient = new RTSPClient(
                    rtspConfig.remoteHost,
                    rtspConfig.remotePort,
                    rtspConfig.videoPath,
                    rtspConfig.user,
                    rtspConfig.password,
                    rtspConfig.localUdpPort,
                    timeout);
            
            // some cameras don't have a real RTSP server (i.e. 3DR Solo UAV)
            // in this case we just need to maintain a TCP connection so keep the RTSP client alive
            if (!rtspConfig.onlyConnectRtsp)
            {
                rtspClient.sendOptions();
                rtspClient.sendDescribe();
                rtspClient.sendSetup();
                log.info("Connected to RTSP server");
            }
            
            // start RTP/H264 receiving thread
            rtpThread = new RTPH264Receiver(rtspConfig.remoteHost, rtspConfig.localUdpPort, this);
            StreamInfo h264Stream = null;
            int streamIndex = 0;
            int i = 0;
            if (rtspClient.isConnected())
            {
                // look for H264 stream
                for (StreamInfo stream: rtspClient.getMediaStreams())
                {
                    if (stream.codecString != null && stream.codecString.contains("H264"))
                    {
                        h264Stream = stream;
                        streamIndex = i;                        
                    }
                    
                    i++;
                }
                
                if (h264Stream == null)
                    throw new IOException("No stream with H264 codec found");
                
                // set initial parameter sets if we received them via RTSP
                if (h264Stream.paramSets != null)
                    rtpThread.setParameterSets(h264Stream.paramSets);
            }  
            rtpThread.start();
            
            // play stream with RTSP if server responded to SETUP
            if (rtspClient.isConnected())
            {
                // send PLAY request
                rtspClient.sendPlay(streamIndex);
                
                // start RTCP sending thread
                // some cameras need that to maintain the stream
                rtcpThread = new RTCPSender(rtspConfig.remoteHost, rtspConfig.localUdpPort+1, rtspClient.getRemoteRtcpPort(), 1000, rtspClient);
                rtcpThread.start();
            }
        }
        catch (IOException e)
        {
            throw new SensorException("Cannot connect to RTP stream", e);            
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
        // stop RTP receiver thread
        if (rtpThread != null)
        {
            rtpThread.interrupt();
            rtpThread = null;
            log.info("Disconnected from H264 RTP stream");
        }
        
        // stop RTCP keep alive thread
        if (rtcpThread != null)
        {
            rtcpThread.stop();
            rtcpThread = null;
        }
        
        // disconnect from RTSP server
        try
        {
            if (rtspClient != null)
            {
                if (rtspClient.isConnected())
                    rtspClient.teardown();
                else
                    rtspClient.close(); // just close the socket
                rtspClient = null;
            }
        }
        catch (IOException e)
        {
            log.error("Error while disconnecting from RTSP server", e);
        }
        log.info("Disconnected from RTSP server");
        
        // stop frame processor (async executor)
        if (executor != null)
        {
            try
            {
                executor.shutdownNow();
                executor.awaitTermination(10000L, TimeUnit.SECONDS);
                if (fch != null)
                    fch.close();
            }
            catch (Exception e)
            {
                log.error("Error when shutting down frame listener thread", e);
            }            
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
                        log.info("Connected to H264 RTP stream");
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
                            log.error("Error while writing to backup file", e);
                            fch = null;
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
