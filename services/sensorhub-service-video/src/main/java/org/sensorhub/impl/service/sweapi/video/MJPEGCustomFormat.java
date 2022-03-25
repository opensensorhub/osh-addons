/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2022 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sweapi.video;

import java.math.BigInteger;
import java.util.List;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.impl.service.sweapi.IdEncoder;
import org.sensorhub.impl.service.sweapi.obs.CustomObsFormat;
import org.sensorhub.impl.service.sweapi.resource.RequestContext;
import org.sensorhub.impl.service.sweapi.resource.ResourceBinding;
import net.opengis.swe.v20.BinaryBlock;
import net.opengis.swe.v20.BinaryEncoding;
import net.opengis.swe.v20.BinaryMember;
import net.opengis.swe.v20.DataEncoding;


public class MJPEGCustomFormat implements CustomObsFormat
{

    @Override
    public boolean isCompatible(IDataStreamInfo dsInfo)
    {
        DataEncoding resultEncoding = dsInfo.getRecordEncoding();
        if (resultEncoding instanceof BinaryEncoding)
        {
            List<BinaryMember> mbrList = ((BinaryEncoding)resultEncoding).getMemberList();
            BinaryBlock videoFrameSpec = null;

            // try to find binary block encoding def in list
            for (BinaryMember spec: mbrList)
            {
                if (spec instanceof BinaryBlock)
                {
                    videoFrameSpec = (BinaryBlock)spec;
                    break;
                }
            }

            if (videoFrameSpec != null)
            {
                var codec = videoFrameSpec.getCompression();
                if ("JPEG".equalsIgnoreCase(codec))
                    return true;
            }
        }
        
        return false;
    }

    @Override
    public ResourceBinding<DataStreamKey, IDataStreamInfo> getSchemaBinding(RequestContext ctx, IdEncoder idEncoder, IDataStreamInfo dsInfo)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResourceBinding<BigInteger, IObsData> getObsBinding(RequestContext ctx, IdEncoder idEncoder, IDataStreamInfo dsInfo)
    {
        return new MJPEGSerializer(ctx, idEncoder, dsInfo);
    }

}
