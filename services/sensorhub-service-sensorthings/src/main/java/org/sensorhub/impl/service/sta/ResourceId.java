/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2020 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sta;


import org.sensorhub.api.common.BigId;
import de.fraunhofer.iosb.ilt.frostserver.model.core.Id;

/**
 * <p>
 * Base interface for resource IDs used by this implementation
 * </p>
 *
 * @author Alex Robin
 * @date Mar 17, 2020
 */
public interface ResourceId extends Id, BigId
{
}
