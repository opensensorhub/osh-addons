/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sweapi.video;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.Arrays;
import java.util.Date;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;
import java.util.Set;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.mp4parser.streaming.StreamingTrack;
import org.mp4parser.streaming.input.h264.H264NalConsumingTrack;
import org.mp4parser.streaming.output.mp4.FragmentedMp4Writer;
import org.mp4parser.streaming.output.mp4.FragmentedMp4WriterUtils;
import org.sensorhub.api.data.ObsEvent;
import org.sensorhub.impl.service.sos.AbstractAsyncSerializer;
import org.sensorhub.impl.service.sos.ISOSAsyncResultSerializer;
import org.sensorhub.impl.service.sos.SOSServlet;
import org.sensorhub.impl.service.swe.RecordTemplate;
import org.vast.data.DataBlockMixed;
import org.vast.ows.sos.GetResultRequest;
import org.vast.ows.sos.SOSException;
import com.google.common.collect.Sets;


public class MP4Serializer extends AbstractAsyncSerializer<GetResultRequest, ObsEvent> implements ISOSAsyncResultSerializer
{
    public static String MP4_MIME_TYPE = "video/mp4";
    private static final Set<String> IMG_ARRAY_COMPONENT_NAMES = Sets.newHashSet("img", "videoFrame");
    
    int imgComponentIdx = -1;
    FragmentedMp4Writer mp4Muxer;
    H264DBtrack h264Source;
    
    
    class H264DBtrack extends H264NalConsumingTrack
    {
        boolean hasTime = false;
        boolean hasSps = false;
        boolean hasPps = false;
        
        
        public void sendNextFrame(DataBlock nextFrame, OutputStream os) throws IOException
        {
            // write each record in output stream
            // set creation time as first record time
            if (!hasTime)
            {
                double samplingTime = nextFrame.getDoubleValue(0);
                Date creationTime = new Date((long)(samplingTime * 1000.));
                FragmentedMp4WriterUtils.setCreationTime((FragmentedMp4Writer)sampleSink, creationTime);
                hasTime = true;
            }
            
            // get H264 frame data
            DataBlock frameBlk = ((DataBlockMixed)nextFrame).getUnderlyingObject()[imgComponentIdx];
            byte[] frameData = (byte[])frameBlk.getUnderlyingObject();
            ByteBuffer nals = ByteBuffer.wrap(frameData);
            
            // debug
            //os.write(frameData);
            //os.flush();
            
            // look for next nal unit
            while (nals.remaining() > 0)
            {
                // skip 4 sync bytes
                // this takes us to beginning of NAL unit content
                nals.getInt();
                
                // read NAL unit type
                nals.mark();
                int nalUnitType = (nals.get() & 0x1f);
                
                // compute next NAL unit boundaries                        
                boolean found = false;
                while (!found && nals.remaining() > 0)
                {
                    byte b = nals.get();
                    int pos = nals.position()-1;
                    if (b == 1 && nals.get(pos-1) == 0 && nals.get(pos-2) == 0 && nals.get(pos-3) == 0)
                    {
                        nals.position(nals.position()-4);
                        found = true;
                    }
                }
                nals.limit(nals.position());
                
                // send to muxer
                // make sure we send SPS and PPS only once
                if ((!hasSps && nalUnitType == 7) || (!hasPps && nalUnitType == 8) ||
                    (hasSps && hasPps && nalUnitType != 7 && nalUnitType != 8)) 
                {
                    // slice btw position and limit
                    // this sends only one NAL unit to muxer
                    nals.reset();
                    //nals.position(nals.position()-4);
                    //nals.mark();
                    //ch.write(nals);
                    //nals.reset();
                    //nals.position(nals.position()+4);
                    this.consumeNal(nals.slice());
                    
                    // to remember we already sent SPS and PPS
                    if (nalUnitType == 7)
                        hasSps = true;
                    else if (nalUnitType == 8)
                        hasPps = true;
                }
                
                // prepare for slicing next NAL unit
                nals.position(nals.limit());
                nals.limit(nals.capacity());
            }
            
            // flush output to make sure encoded frame is sent right away
            os.flush();
        }
    };


    @Override
    public void init(SOSServlet servlet, AsyncContext asyncCtx, GetResultRequest req, RecordTemplate resultTemplate) throws SOSException, IOException
    {
        super.init(servlet, asyncCtx, req);
        
        if (asyncCtx != null)
        {
            // set MIME type for MP4 format
            ((HttpServletResponse)asyncCtx.getResponse()).setContentType(MP4_MIME_TYPE);
        }
        
        // get index of image component
        DataComponent dataStruct = resultTemplate.getDataStructure();
        for (int i = dataStruct.getComponentCount()-1; i >= 0; i--)
        {
            if (IMG_ARRAY_COMPONENT_NAMES.contains(dataStruct.getComponent(i).getName()))
            {
                imgComponentIdx = i;
                break;
            }
        }
        
        // adapt swe common data as H264 streaming track
        h264Source = new H264DBtrack();
        h264Source.setTimescale(30); // TODO compute frame rate based on actual stream info
        h264Source.setFrametick(1);
        
        // start streaming and muxing on the fly
        //os = new FileOutputStream("/home/alex/testsos.mp4");
        mp4Muxer = new FragmentedMp4Writer(Arrays.<StreamingTrack>asList(h264Source), Channels.newChannel(os));        
    }


    @Override
    protected void beforeRecords() throws IOException
    {
        // nothing to do here
    }


    @Override
    protected void writeRecord(ObsEvent item) throws IOException
    {
        // mux and write each frame to output stream
        for (var obs: item.getObservations())
            h264Source.sendNextFrame(obs.getResult(), os);
    }


    @Override
    protected void close() throws IOException
    {
        mp4Muxer.close();
        super.close();
    }


    @Override
    protected void afterRecords() throws IOException
    {    
        // nothing to do here
    }
}
