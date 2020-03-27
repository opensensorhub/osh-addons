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
public class ResourceIdBigInt implements ResourceId
{
    static final String BAD_ID_MSG = "IDs must be positive integers";
    static final BigInteger MAX_VALUE_JS = new BigInteger("9007199254740991");
    static final String QUOTECHAR = "'";
    
    BigInteger internalID;
    
    
    protected ResourceIdBigInt()
    {        
    }
    
    
    public ResourceIdBigInt(BigInteger internalID)
    {
        this.internalID = checkValue(internalID);
    }
    
    
    public ResourceIdBigInt(long internalID)
    {
        this.internalID = checkValue(BigInteger.valueOf(internalID));
    }
    
    
    public ResourceIdBigInt(String idString)
    {
        // parse big int from base36 encoded string
        this.internalID = checkValue(new BigInteger(idString, 36));
    }
    
    
    private BigInteger checkValue(BigInteger val)
    {
        Asserts.checkArgument(val.compareTo(BigInteger.ZERO) > 0, BAD_ID_MSG);
        return val;
    }
    
    
    @Override
    public Object getValue()
    {
        if (internalID.compareTo(MAX_VALUE_JS) >= 0)
            return internalID.toString(36);
        else
            return internalID;
    }


    @Override
    public String getUrl()
    {
        if (internalID.compareTo(MAX_VALUE_JS) >= 0)
            return QUOTECHAR + internalID.toString(36) + QUOTECHAR; 
        else
            return internalID.toString();
    }
    
    
    @Override
    public String toString()
    {
        if (internalID.compareTo(MAX_VALUE_JS) >= 0)
            return "\"" + internalID.toString() + "\""; 
        else
            return internalID.toString();
    }
    
    
    public long asLong()
    {
        return internalID.longValueExact();
    }
    
    
    public BigInteger asBigInt()
    {
        return internalID;
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

}
