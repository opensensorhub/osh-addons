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
import de.fraunhofer.iosb.ilt.frostserver.persistence.IdManager;


/**
 * <p>
 * SensorThings service ID Manager implementation for OSH
 * </p>
 *
 * @author Alex Robin
 * @date Sep 7, 2019
 */
public class ResourceIdManager implements IdManager<String>
{

    @Override
    public Class<ResourceId> getIdClass()
    {
        return ResourceId.class;
    }

    
    @Override
    public Id parseId(String input)
    {
        Asserts.checkNotNull(input, "id");
        
        if (input.contains(CompositeResourceId.ID_SEPARATOR))
            return new CompositeResourceId(input);
        else
            return new ResourceId(input);
    }

    
    @Override
    public Id fromObject(String input)
    {
        return parseId(input);
    }

}
