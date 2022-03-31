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

import java.io.IOException;
import java.util.Set;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.data.ObsEvent;
import org.sensorhub.impl.service.sos.AbstractAsyncSerializer;
import org.sensorhub.impl.service.sos.ISOSAsyncResultSerializer;
import org.sensorhub.impl.service.sos.SOSServlet;
import org.sensorhub.impl.service.swe.RecordTemplate;
import org.vast.data.DataBlockMixed;
import org.vast.ows.sos.GetResultRequest;
import org.vast.ows.sos.SOSException;
import com.google.common.collect.Sets;


public class MJPEGSerializer extends AbstractAsyncSerializer<GetResultRequest, ObsEvent> implements ISOSAsyncResultSerializer
{
    private static final String MIME_TYPE_MULTIPART = "multipart/x-mixed-replace; boundary=--myboundary"; 
    private static final byte[] MIME_BOUNDARY_JPEG = new String("--myboundary\r\nContent-Type: image/jpeg\r\nContent-Length: ").getBytes();
    private static final byte[] END_MIME = new byte[] {0xD, 0xA, 0xD, 0xA};
    private static final Set<String> IMG_ARRAY_COMPONENT_NAMES = Sets.newHashSet("img", "videoFrame");
    
    int imgComponentIdx;


    @Override
    public void init(SOSServlet servlet, AsyncContext asyncCtx, GetResultRequest req, RecordTemplate resultTemplate) throws SOSException, IOException
    {
        super.init(servlet, asyncCtx, req);
        
        if (asyncCtx != null)
        {
            ((HttpServletResponse)asyncCtx.getResponse()).addHeader("Cache-Control", "no-cache");
            ((HttpServletResponse)asyncCtx.getResponse()).addHeader("Pragma", "no-cache");
            
            // set multi-part MIME so that browser can properly decode it in an img tag
            ((HttpServletResponse)asyncCtx.getResponse()).setContentType(MIME_TYPE_MULTIPART);
        }
        
        // get index of image component
        DataComponent dataStruct = resultTemplate.getDataStructure();
        imgComponentIdx = 0;
        for (int i = dataStruct.getComponentCount()-1; i >= 0; i--)
        {
            if (IMG_ARRAY_COMPONENT_NAMES.contains(dataStruct.getComponent(i).getName()))
            {
                imgComponentIdx = i;
                break;
            }
        }
    }


    @Override
    protected void beforeRecords() throws IOException
    {
        // nothing to do here
    }


    @Override
    protected void writeRecord(ObsEvent item) throws IOException
    {
        // write each record in output stream
        // skip time stamp and any other field around image data to provide raw MJPEG
        // TODO set timestamp in JPEG metadata
        for (var obs: item.getObservations())
        {
            var rec = obs.getResult();
            DataBlock frameBlk = ((DataBlockMixed)rec).getUnderlyingObject()[imgComponentIdx];
            byte[] frameData = (byte[])frameBlk.getUnderlyingObject();
            
            // write MIME boundary
            os.write(MIME_BOUNDARY_JPEG);
            os.write(Integer.toString(frameData.length).getBytes());
            os.write(END_MIME);
            
            os.write(frameData);
            os.flush();
        }        
    }


    @Override
    protected void afterRecords() throws IOException
    {
        // nothing to do here
    }

}
