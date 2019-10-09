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

import com.github.fge.jsonpatch.JsonPatch;
import de.fraunhofer.iosb.ilt.frostserver.model.core.Entity;
import de.fraunhofer.iosb.ilt.frostserver.model.core.EntitySet;
import de.fraunhofer.iosb.ilt.frostserver.path.ResourcePath;
import de.fraunhofer.iosb.ilt.frostserver.query.Query;


/**
 * <p>
 * Base interface for resource handlers
 * </p>
 *
 * @author Alex Robin
 * @param <T> Type of Entity
 * @date Sep 7, 2019
 */
@SuppressWarnings("rawtypes")
public interface IResourceHandler<T extends Entity>
{
    static final String ID_PROPERTY = "id";
    static final String NAME_PROPERTY = "name";
    static final String DESCRIPTION_PROPERTY = "description";
        
    
    public ResourceId create(Entity<?> entity);
    
    
    public boolean update(Entity<?> entity);
    
    
    public boolean patch(ResourceId id, JsonPatch patch);
    
    
    public boolean delete(ResourceId id);
    
    
    public T getById(ResourceId id, Query q);
    
    
    public EntitySet<?> queryCollection(ResourcePath path, Query q);
}
