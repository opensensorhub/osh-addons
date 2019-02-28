/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.fakecam;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import net.opengis.swe.v20.BinaryBlock;
import net.opengis.swe.v20.BinaryEncoding;
import net.opengis.swe.v20.ByteEncoding;
import net.opengis.swe.v20.ByteOrder;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Time;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.CountImpl;
import org.vast.data.DataBlockCompressed;
import org.vast.data.SWEFactory;
import org.vast.swe.SWEConstants;
import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.MovieBox;
import com.coremedia.iso.boxes.fragment.MovieFragmentBox;
import com.coremedia.iso.boxes.fragment.MovieFragmentHeaderBox;


public class FakeCamOutput extends AbstractSensorOutput<FakeCamSensor>
{
    private static final Logger log = LoggerFactory.getLogger(FakeCamOutput.class);
    DataComponent videoDataStruct;
    BinaryEncoding encoding;
    boolean sendData;
    Timer timer;
    IsoFile mp4File;
    Iterator<Box> mp4Boxes;
    ByteArrayOutputStream videoFrameBuffer;
    long sampleCounter;
    

    public FakeCamOutput(FakeCamSensor parentSensor)
    {
        super(parentSensor);
    }


    @Override
    public String getName()
    {
        return "videoOut";
    }


    protected void init()
    {
        int[] imgSize = getVideoSize();
        SWEFactory fac = new SWEFactory();
        
        // video output structure
        videoDataStruct = fac.newDataRecord(2);
        videoDataStruct.setName(getName());
        
        Time time = fac.newTime();
        time.getUom().setHref(Time.ISO_TIME_UNIT);
        time.setDefinition(SWEConstants.DEF_SAMPLING_TIME);
        videoDataStruct.addComponent("time", time);
                
        DataArray img = fac.newDataArray(imgSize[1]);
        img.setDefinition("http://sensorml.com/ont/swe/property/VideoFrame");
        videoDataStruct.addComponent("videoFrame", img);
        
        DataArray imgRow = fac.newDataArray(imgSize[0]);
        img.addComponent("row", imgRow);
        
        DataRecord imgPixel = fac.newDataRecord(3);
        imgPixel.addComponent("red", new CountImpl(DataType.BYTE));
        imgPixel.addComponent("green", new CountImpl(DataType.BYTE));
        imgPixel.addComponent("blue", new CountImpl(DataType.BYTE));
        imgRow.addComponent("pixel", imgPixel);
        
        // video encoding
        encoding = fac.newBinaryEncoding();
        encoding.setByteEncoding(ByteEncoding.RAW);
        encoding.setByteOrder(ByteOrder.BIG_ENDIAN);
        BinaryBlock blockEnc = fac.newBinaryBlock();
        blockEnc.setRef("/");
        blockEnc.setCompression("H264");
        encoding.addMemberAsBlock(blockEnc);
    }
    
    
    private int[] getVideoSize()
    {
        return new int[] {640, 480};
    }


    private void sendMeasurement()
    {
        // read next video frame
        try
        {
            videoFrameBuffer.reset();
            WritableByteChannel byteChannel = Channels.newChannel(videoFrameBuffer);
            
            // init or loop
            if (mp4Boxes == null || !mp4Boxes.hasNext())
                mp4Boxes = mp4File.getBoxes().iterator();
            
            // copy all box until actual frame data is found
            while (mp4Boxes.hasNext())
            {
                Box currentBox = mp4Boxes.next();
                                
                if (currentBox instanceof MovieBox)
                {
                    log.trace(((MovieBox) currentBox).getMovieHeaderBox().toString());
                }
                
                if (currentBox instanceof MovieFragmentBox)
                {
                    // always increment seq number
                    sampleCounter++;
                    ((MovieFragmentBox) currentBox).getBoxes(MovieFragmentHeaderBox.class).get(0).setSequenceNumber(sampleCounter);;
                    
                    // need to rewrite base offset so it's not relative to begining of file
                    //((MovieFragmentBox) currentBox).getTrackFragmentHeaderBoxes().get(0).setDefaultBaseIsMoof(true);
                    ((MovieFragmentBox) currentBox).getTrackFragmentHeaderBoxes().get(0).setBaseDataOffset(-1L);
                    ((MovieFragmentBox) currentBox).getTrackFragmentHeaderBoxes().get(0).setDefaultSampleDuration(4000);
                    ((MovieFragmentBox) currentBox).getTrackRunBoxes().get(0).setDataOffset(224-8);
                    
                    log.trace(((MovieFragmentBox) currentBox).getBoxes(MovieFragmentHeaderBox.class).get(0).toString());
                    log.trace(((MovieFragmentBox) currentBox).getTrackFragmentHeaderBoxes().get(0).toString());
                    log.trace(((MovieFragmentBox) currentBox).getTrackRunBoxes().get(0).toString());
                    //for (Entry sample: ((MovieFragmentBox) currentBox).getTrackRunBoxes().get(0).getEntries())
                    //    System.out.println(sample);                    
                }
                
                if (currentBox.getType().equals("moof"))
                {
                    // TODO add time tag box (tfdt?)
                    log.trace("Copying box " + currentBox.getType());
                    currentBox.getBox(byteChannel);
                }
                
                else if (currentBox.getType().equals("mdat"))
                {
                    log.trace("Copying box " + currentBox.getType());
                    currentBox.getBox(byteChannel);
                    break;
                }
                
                else if (currentBox.getType().equals("mfra"))
                {
                    mp4Boxes = mp4File.getBoxes().iterator();
                }
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
                
        // build and publish datablock
        DataBlockCompressed dataBlock = new DataBlockCompressed();
        dataBlock.setUnderlyingObject(videoFrameBuffer.toByteArray());
        
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, FakeCamOutput.this, dataBlock));
    }


    protected void start() throws SensorHubException
    {
        if (timer != null)
            return;
        timer = new Timer();
        
        // open MP4 video file
        String videoFile = parentSensor.getConfiguration().videoFilePath;
        try
        {
           mp4File = new IsoFile(videoFile);
           videoFrameBuffer = new ByteArrayOutputStream(10*1024);
        }
        catch (IOException e)
        {
            throw new SensorHubException("Error opening video file " + videoFile, e);
        }
        
        // start main measurement generation thread
        TimerTask task = new TimerTask() {
            public void run()
            {
                sendMeasurement();
            }            
        };
        
        sampleCounter = 0;
        timer.scheduleAtFixedRate(task, 0, (long)(getAverageSamplingPeriod()*1000));
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
        return 0.8;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return videoDataStruct;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return encoding;
    }
}
