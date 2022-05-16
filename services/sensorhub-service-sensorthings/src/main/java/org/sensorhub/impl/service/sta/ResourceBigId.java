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

import java.math.BigInteger;
import java.nio.ByteBuffer;
import org.sensorhub.api.common.BigId;
import org.sensorhub.utils.VarInt;
import org.vast.util.Asserts;
import de.fraunhofer.iosb.ilt.frostserver.persistence.BasicPersistenceType;


/**
 * <p>
 * ResourceId class backed by a BigInteger value
 * </p>
 *
 * @author Alex Robin
 * @date March 17, 2020
 */
public class ResourceBigId implements ResourceId
{
    static final String BAD_ID_MSG = "IDs must be positive integers";
    static final BigInteger MAX_VALUE_JS = new BigInteger("9007199254740991");
    static final String QUOTECHAR = "'";
    
    final BigId internalID;
    
    
    public ResourceBigId(BigId internalID)
    {
        this.internalID = checkValue(internalID);
    }
    
    
    /*
     * Construct from a long value containing both scope and id parts
     */
    public ResourceBigId(long idLong)
    {
        var buf = ByteBuffer.allocate(8);
        buf.putLong(Long.reverse(idLong));
        buf.rewind();
        var scope = VarInt.getVarInt(buf);
        var id = VarInt.getVarLong(buf);
        this.internalID = checkValue(BigId.fromLong(scope, id));
    }
    
    
    /*
     * Construct from a base32 string containing both scope and id parts
     */
    public ResourceBigId(String idString)
    {
        this.internalID = checkValue(BigId.fromString32(idString));
    }
    
    
    private BigId checkValue(BigId val)
    {
        Asserts.checkArgument(val.size() > 5 || val.getIdAsLong() > 0, BAD_ID_MSG);
        return val;
    }
    
    
    @Override
    public Object getValue()
    {
        if (internalID.size() > 5)
            return BigId.toString32(internalID);
        else
            return getFullIdAsLong();
    }


    @Override
    public String getUrl()
    {
        if (internalID.size() > 5)
            return QUOTECHAR + BigId.toString32(internalID) + QUOTECHAR; 
        else
            return Long.toString(getFullIdAsLong());
    }
    
    
    long getFullIdAsLong()
    {
        var buf = ByteBuffer.allocate(8);
        VarInt.putVarInt(getScope(), buf);
        VarInt.putVarLong(getIdAsLong(), buf);
        buf.rewind();
        return Long.reverse(buf.getLong());
    }
    
    
    @Override
    public boolean equals(Object obj)
    {
        return internalID.equals(obj);
    }
    
    
    @Override
    public int compareTo(BigId other)
    {
        return internalID.compareTo(other);
    }
    
    
    @Override
    public int hashCode()
    {
        return internalID.hashCode();
    }


    @Override
    public String toString()
    {
        return toStaString(this);
    }


    @Override
    public BasicPersistenceType getBasicPersistenceType()
    {
        return null;
    }


    @Override
    public Object asBasicPersistenceType()
    {
        return null;
    }


    @Override
    public void fromBasicPersitenceType(Object data)
    {
    }


    @Override
    public int getScope()
    {
        return internalID.getScope();
    }


    @Override
    public byte[] getIdAsBytes()
    {
        return internalID.getIdAsBytes();
    }


    @Override
    public long getIdAsLong()
    {
        return internalID.getIdAsLong();
    }
    
    
    public static String toStaString(BigId internalID)
    {
        if (internalID.size() > 6)
            return BigId.toString32(internalID);
        else
            return Long.toString(internalID.getIdAsLong());
    }

}
