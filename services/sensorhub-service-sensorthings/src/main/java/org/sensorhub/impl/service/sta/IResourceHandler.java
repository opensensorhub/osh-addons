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

import java.util.concurrent.Flow.Subscriber;
import com.github.fge.jsonpatch.JsonPatch;
import de.fraunhofer.iosb.ilt.frostserver.model.core.Entity;
import de.fraunhofer.iosb.ilt.frostserver.model.core.EntitySet;
import de.fraunhofer.iosb.ilt.frostserver.path.ResourcePath;
import de.fraunhofer.iosb.ilt.frostserver.query.Query;
import de.fraunhofer.iosb.ilt.frostserver.util.NoSuchEntityException;


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
    static final String GEOJSON_FORMAT = "application/vnd.geo+json";
    static final String NO_DB_MESSAGE = "Create/Update/Delete disabled since persistence is not configured";
        
    
    public ResourceId create(Entity<?> entity) throws NoSuchEntityException;
    
    
    public boolean update(Entity<?> entity) throws NoSuchEntityException;
    
    
    public boolean patch(ResourceId id, JsonPatch patch) throws NoSuchEntityException;
    
    
    public boolean delete(ResourceId id) throws NoSuchEntityException;
    
    
    public T getById(ResourceId id, Query q) throws NoSuchEntityException;
    
    
    public EntitySet<?> queryCollection(ResourcePath path, Query q);
    
    
    //public default void subscribeToEntity(ResourceId id, Query q, Subscriber<T> subscriber) {}
    public default void subscribeToCollection(ResourcePath path, Query q, Subscriber<T> subscriber) {}
    
    
    public default ResourcePath getParentPath(ResourcePath path)
    {
        var parentPath = new ResourcePath();
        for (int i = 0; i < path.size()-1; i++)
            parentPath.addPathElement(path.get(i));
        parentPath.setMainElement(parentPath.getLastElement());
        parentPath.setIdentifiedElement(path.getIdentifiedElement());
        return parentPath;
    }
}
