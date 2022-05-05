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
import java.util.Collection;
import java.util.Set;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.impl.service.sweapi.IdEncoder;
import org.sensorhub.impl.service.sweapi.resource.RequestContext;
import org.sensorhub.impl.service.sweapi.resource.ResourceBinding;
import org.sensorhub.impl.service.sweapi.resource.ResourceLink;
import org.vast.data.DataBlockMixed;
import com.google.common.collect.Sets;


public class MJPEGSerializer extends ResourceBinding<BigId, IObsData>
{
    private static final String MIME_TYPE_MULTIPART = "multipart/x-mixed-replace; boundary=--myboundary"; 
    private static final byte[] MIME_BOUNDARY_JPEG = new String("--myboundary\r\nContent-Type: image/jpeg\r\nContent-Length: ").getBytes();
    private static final byte[] END_MIME = new byte[] {0xD, 0xA, 0xD, 0xA};
    private static final Set<String> IMG_ARRAY_COMPONENT_NAMES = Sets.newHashSet("img", "videoFrame");
    
    int imgComponentIdx;


    public MJPEGSerializer(RequestContext ctx, IdEncoder idEncoder, IDataStreamInfo dsInfo)
    {
        super(ctx, idEncoder);
        
        // disable browser cache to make sure image is refreshed
        ctx.setResponseHeader("Cache-Control", "no-cache");
        ctx.setResponseHeader("Pragma", "no-cache");
            
        // set multi-part MIME so that browser can properly decode it in an img tag
        ctx.setResponseContentType(MIME_TYPE_MULTIPART);
        
        // get index of image component
        DataComponent dataStruct = dsInfo.getRecordStructure();
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
    public IObsData deserialize() throws IOException
    {
        throw new UnsupportedOperationException();
    }
    
    
    @Override
    public void serialize(BigId key, IObsData obs, boolean showLinks) throws IOException
    {
        // skip time stamp and any other field around image data to provide raw MJPEG
        // TODO set timestamp in JPEG metadata
        var rec = obs.getResult();
        DataBlock frameBlk = ((DataBlockMixed)rec).getUnderlyingObject()[imgComponentIdx];
        byte[] frameData = (byte[])frameBlk.getUnderlyingObject();
        
        // write MIME boundary
        ctx.getOutputStream().write(MIME_BOUNDARY_JPEG);
        ctx.getOutputStream().write(Integer.toString(frameData.length).getBytes());
        ctx.getOutputStream().write(END_MIME);
        
        ctx.getOutputStream().write(frameData);
        ctx.getOutputStream().flush();
    }
    
    
    public void startCollection() throws IOException
    {
        // nothing to do here
    }
    
    
    public void endCollection(Collection<ResourceLink> links) throws IOException
    {
        // nothing to do here
    }

}
