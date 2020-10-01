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

import org.sensorhub.api.obs.IDataStreamStore;

/**
 * <p>
 * TODO ISTADataStreamStore type description
 * </p>
 *
 * @author Alex Robin
 * @date Oct 20, 2019
 */
public interface ISTADataStreamStore extends IDataStreamStore
{
    
    
    public Long getAssociatedThing(long dataStreamID);
}
