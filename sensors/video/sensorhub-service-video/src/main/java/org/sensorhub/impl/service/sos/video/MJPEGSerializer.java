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
import java.util.Set;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.impl.service.sos.ISOSCustomSerializer;
import org.sensorhub.impl.service.sos.ISOSDataProvider;
import org.vast.data.DataBlockMixed;
import org.vast.ows.OWSRequest;
import com.google.common.collect.Sets;


public class MJPEGSerializer implements ISOSCustomSerializer
{
    private static final String MIME_TYPE_MULTIPART = "multipart/x-mixed-replace; boundary=--myboundary"; 
    private static final byte[] MIME_BOUNDARY_JPEG = new String("--myboundary\r\nContent-Type: image/jpeg\r\nContent-Length: ").getBytes();
    private static final byte[] END_MIME = new byte[] {0xD, 0xA, 0xD, 0xA};
    private static final Set<String> IMG_ARRAY_COMPONENT_NAMES = Sets.newHashSet("img", "videoFrame");
    
    
    @Override
    public void write(ISOSDataProvider dataProvider, OWSRequest request) throws IOException
    {
        request.getHttpResponse().addHeader("Cache-Control", "no-cache");
        request.getHttpResponse().addHeader("Pragma", "no-cache");                    
        // set multi-part MIME so that browser can properly decode it in an img tag
        request.getHttpResponse().setContentType(MIME_TYPE_MULTIPART);
    
        OutputStream os = new BufferedOutputStream(request.getResponseStream());
        
        // write each record in output stream
        // skip time stamp to provide raw MJPEG
        // TODO set timestamp in JPEG metadata
        DataBlock nextRecord;
        try
        {
            // get index of image component
            DataComponent dataStruct = dataProvider.getResultStructure();
            int imgCompIdx = 0;
            for (int i = dataStruct.getComponentCount()-1; i >= 0; i--)
            {
                if (IMG_ARRAY_COMPONENT_NAMES.contains(dataStruct.getComponent(i).getName()))
                {
                    imgCompIdx = i;
                    break;
                }
            }
            
            while ((nextRecord = dataProvider.getNextResultRecord()) != null)
            {
                DataBlock frameBlk = ((DataBlockMixed)nextRecord).getUnderlyingObject()[imgCompIdx];
                byte[] frameData = (byte[])frameBlk.getUnderlyingObject();
                
                // write MIME boundary
                os.write(MIME_BOUNDARY_JPEG);
                os.write(Integer.toString(frameData.length).getBytes());
                os.write(END_MIME);
                
                os.write(frameData);
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

}
