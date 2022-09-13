/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2022 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.mfapi.home;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sensorhub.api.datastore.EmptyFilterIntersection;
import org.sensorhub.api.datastore.feature.FoiFilter;
import org.sensorhub.api.event.IEventBus;
import org.sensorhub.impl.service.mfapi.MFApiServiceConfig.CollectionConfig;
import org.sensorhub.impl.service.mfapi.mf.MFHandler;
import org.sensorhub.impl.service.sweapi.InvalidRequestException;
import org.sensorhub.impl.service.sweapi.ObsSystemDbWrapper;
import org.sensorhub.impl.service.sweapi.RestApiServlet.ResourcePermissions;
import org.sensorhub.impl.service.sweapi.ServiceErrors;
import org.sensorhub.impl.service.sweapi.resource.RequestContext.ResourceRef;


public class MFCollectionItemsHandler extends MFHandler
{
    public static final String[] NAMES = { "items" };
    
    final Map<String, FoiFilter> collectionFilters = new HashMap<>();
    
    
    public MFCollectionItemsHandler(IEventBus eventBus, ObsSystemDbWrapper db, ResourcePermissions permissions, List<CollectionConfig> collections)
    {
        super(eventBus, db, permissions);
        
        for (var col: collections)
            collectionFilters.put(col.name, col.includeFilter);
    }


    @Override
    public String[] getNames()
    {
        return NAMES;
    }
    
    
    @Override
    public FoiFilter getFilter(final ResourceRef parent, final Map<String, String[]> queryParams, long offset, long limit) throws InvalidRequestException
    {
        var filter = super.getFilter(parent, queryParams, offset, limit);
        
        try
        {
            return filter.intersect(collectionFilters.get(parent.id));
        }
        catch (EmptyFilterIntersection e)
        {
            throw ServiceErrors.notFound();
        }
    }
}
