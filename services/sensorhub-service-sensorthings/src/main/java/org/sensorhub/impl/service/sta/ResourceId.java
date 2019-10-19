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

import org.vast.util.Asserts;
import de.fraunhofer.iosb.ilt.frostserver.model.core.Id;
import de.fraunhofer.iosb.ilt.frostserver.persistence.BasicPersistenceType;


/**
 * <p>
 * ResourceId class backed by a single long value
 * </p>
 *
 * @author Alex Robin
 * @date Sep 7, 2019
 */
public class ResourceId implements Id
{
    static final String BAD_ID_MSG = "IDs must be positive integers";
        
    long internalID;
    
    
    protected ResourceId()
    {        
    }
    
    
    public ResourceId(long internalID)
    {
        Asserts.checkArgument(internalID > 0, BAD_ID_MSG);
        this.internalID = internalID;
    }
    
    
    public ResourceId(String idString)
    {
        this.internalID = Long.parseLong(idString);
        Asserts.checkArgument(internalID > 0, BAD_ID_MSG);
    }
    
    
    @Override
    public Object getValue()
    {
        return internalID;
    }


    @Override
    public String getUrl()
    {
        return Long.toString(internalID);
    }
    
    
    @Override
    public String toString()
    {
        return getValue().toString();
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
