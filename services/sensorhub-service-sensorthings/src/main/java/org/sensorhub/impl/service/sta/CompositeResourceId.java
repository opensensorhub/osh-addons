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


/**
 * <p>
 * Special resource ID class for observations backed by 3 long values 
 * </p>
 *
 * @author Alex Robin
 * @date Sep 25, 2019
 */
public class CompositeResourceId extends ResourceId
{
    static final String ID_SEPARATOR = ":";
    static final String QUOTECHAR = "'";
    
    long[] parentIDs;
    
    
    public CompositeResourceId(long dataStreamID, long foiID, long timeStampMillis)
    {
        super(timeStampMillis);
        this.parentIDs = new long[] {dataStreamID, foiID};
    }
    
    
    public CompositeResourceId(long thingID, long timeStampMillis)
    {
        super(timeStampMillis);
        this.parentIDs = new long[] {thingID};
    }
    

    public CompositeResourceId(String idString)
    {
        String[] ids = idString.substring(1, idString.length()-1).split(ID_SEPARATOR);
        this.parentIDs = new long[ids.length-1];
        for (int i = 0; i < parentIDs.length; i++)
            this.parentIDs[i] = Long.parseLong(ids[i]);
        this.internalID = Long.parseLong(ids[ids.length-1]);
    }
    
    
    @Override
    public Object getValue()
    {
        StringBuilder sb = new StringBuilder();
        for (long id: parentIDs)
            sb.append(id).append(ID_SEPARATOR);
        sb.append(internalID);
        return sb.toString();
    }


    @Override
    public String getUrl()
    {
        return QUOTECHAR + getValue() + QUOTECHAR;
    }
}
