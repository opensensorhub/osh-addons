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


/**
 * <p>
 * Special resource ID class for observations backed by 3 long values 
 * </p>
 *
 * @author Alex Robin
 * @date Sep 25, 2019
 */
public class ObsResourceId extends ResourceId
{
    static final String ID_SEPARATOR = ":";
    static final String ID_FORMAT = "%d:%d:%d";
    static final String URL_FORMAT = "'%d:%d:%d'";
    
    long dataStreamID;
    long foiID;
    
    
    public ObsResourceId(long dataStreamID, long foiID, long timeStampMillis)
    {
        super(timeStampMillis);
        this.dataStreamID = dataStreamID;
        this.foiID = foiID;
    }
    

    public ObsResourceId(String idString)
    {
        String[] ids = idString.substring(1, idString.length()-1).split(ID_SEPARATOR);
        Asserts.checkArgument(ids.length == 3);
        this.dataStreamID = Long.parseLong(ids[0]);
        this.foiID = Long.parseLong(ids[1]);
        this.internalID = Long.parseLong(ids[2]);
        Asserts.checkArgument(dataStreamID > 0, BAD_ID_MSG);
        Asserts.checkArgument(foiID >= 0, BAD_ID_MSG);
    }
    
    
    @Override
    public Object getValue()
    {
        return String.format(ID_FORMAT, dataStreamID, foiID, internalID);
    }


    @Override
    public String getUrl()
    {
        return String.format(URL_FORMAT, dataStreamID, foiID, internalID);
    }
}
