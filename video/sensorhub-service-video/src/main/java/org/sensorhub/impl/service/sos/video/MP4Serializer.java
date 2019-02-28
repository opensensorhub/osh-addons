/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sos.video;

import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.Arrays;
import net.opengis.swe.v20.DataBlock;
import org.mp4parser.streaming.StreamingTrack;
import org.mp4parser.streaming.input.h264.H264NalConsumingTrack;
import org.mp4parser.streaming.output.mp4.FragmentedMp4Writer;
import org.sensorhub.impl.service.sos.ISOSCustomSerializer;
import org.sensorhub.impl.service.sos.ISOSDataProvider;
import org.vast.data.DataBlockMixed;
import org.vast.ows.OWSRequest;


public class MP4Serializer implements ISOSCustomSerializer
{
    public static String MP4_MIME_TYPE = "video/mp4";
        
    
    class H264DBtrack extends H264NalConsumingTrack
    {
        public void start(ISOSDataProvider dataProvider, OutputStream os) throws IOException
        {
            // write each record in output stream
            DataBlock nextRecord;
            try
            {
                //OutputStream os = new FileOutputStream("/home/alex/testsos.h264");
                //WritableByteChannel ch = Channels.newChannel(os);
                boolean hasTime = false;
                boolean hasSps = false;
                boolean hasPps = false;
                while ((nextRecord = dataProvider.getNextResultRecord()) != null)
                {
                    // set creation time as first record time
                    if (!hasTime)
                    {
                        double samplingTime = nextRecord.getDoubleValue(0);
                        ((FragmentedMp4Writer)sampleSink).setCreationTime((long)(samplingTime * 1000.)); 
                        hasTime = true;
                    }
                    
                    // get H264 frame data
                    DataBlock frameBlk = ((DataBlockMixed)nextRecord).getUnderlyingObject()[1];
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
            }
            catch (EOFException e)
            {
                // this happens if output stream is closed by client
                // we stop silently in that case
            }
            catch (Exception e)
            {
                throw new IOException("Error while requesting provider data", e);
            } 
        }
    };
    
    
    @Override
    public void write(ISOSDataProvider dataProvider, OWSRequest request) throws IOException
    {
        BufferedOutputStream os = new BufferedOutputStream(request.getResponseStream());
        
        // set MIME type for MP4 format
        if (request.getHttpResponse() != null)
            request.getHttpResponse().setContentType(MP4_MIME_TYPE);
        
        // adapt swe common data as H264 streaming track
        H264DBtrack source = new H264DBtrack();
        source.setTimescale(30);
        source.setFrametick(1);
        
        // start streaming and encoding on the fly
        //os = new FileOutputStream("/home/alex/testsos.mp4");
        FragmentedMp4Writer mp4Muxer = new FragmentedMp4Writer(Arrays.<StreamingTrack>asList(source), Channels.newChannel(os));        
        //StandardMp4Writer mp4Muxer = new StandardMp4Writer(Arrays.<StreamingTrack>asList(source), Channels.newChannel(os));
        source.start(dataProvider, os);
        mp4Muxer.close();

    }

}
