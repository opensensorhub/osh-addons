/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.persistence;

import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;


public interface IRecordStoreInfo
{
    
    /**
     * @return Name of record type
     */
    public String getName();
    
    
    /**
     * @return Description of data blocks persisted in this data store
     */
    public DataComponent getRecordDescription();
    
    
    /**
     * @return Recommended encoding for records contained in this data store
     */
    public DataEncoding getRecommendedEncoding();
}
