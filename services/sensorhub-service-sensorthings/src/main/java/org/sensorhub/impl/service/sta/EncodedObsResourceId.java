/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2019 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sta;

import java.nio.ByteBuffer;
import org.h2.mvstore.DataUtils;


/**
 * <p>
 * Special resource ID class for observations backed by 3 long values 
 * </p>
 *
 * @author Alex Robin
 * @date Oct 11, 2019
 */
public class EncodedObsResourceId extends ResourceId
{
    static final String ID_SEPARATOR = ":";
    static final String ID_FORMAT = "%d:%d:%d";
    static final String URL_FORMAT = "'%d:%d:%d'";
    
    long dataStreamID;
    long foiID;    
    long packedID;
    
    
    public EncodedObsResourceId(long dataStreamID, long foiID, long timeStampMillis)
    {
        super(timeStampMillis);
        this.dataStreamID = dataStreamID;
        this.foiID = foiID;
        encode();        
    }
    

    public EncodedObsResourceId(String idString)
    {
        this.packedID = Long.parseLong(idString);
        decode();
    }
    
    
    private void encode()
    {
        ByteBuffer buf = ByteBuffer.allocate(8);
        DataUtils.writeVarLong(buf, dataStreamID);
        DataUtils.writeVarLong(buf, foiID);
        DataUtils.writeVarLong(buf, internalID);
        buf.flip();
        this.packedID = buf.getLong();
    }
    
    
    private void decode()
    {
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putLong(this.packedID);
        buf.flip();
        this.dataStreamID = DataUtils.readVarLong(buf);
        this.foiID = DataUtils.readVarLong(buf);
        this.internalID = DataUtils.readVarLong(buf);
    }
    
    
    @Override
    public Object getValue()
    {
        return packedID;
    }


    @Override
    public String getUrl()
    {
        return String.format("'%d'", packedID);
    }
}
