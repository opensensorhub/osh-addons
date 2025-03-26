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

import java.util.List;
import java.util.Map;
import org.sensorhub.api.event.IEventBus;
import org.sensorhub.impl.service.mfapi.MFApiServiceConfig.CollectionConfig;
import org.sensorhub.impl.service.consys.ObsSystemDbWrapper;
import org.sensorhub.impl.service.consys.RestApiServlet.ResourcePermissions;
import org.sensorhub.impl.service.consys.home.CollectionHandler;
import org.sensorhub.impl.service.consys.resource.RequestContext;
import org.sensorhub.impl.service.consys.resource.ResourceFormat;
import org.sensorhub.impl.service.consys.resource.ResourceLink;
import org.vast.swe.SWEConstants;


public class MFCollectionsHandler extends CollectionHandler
{
    
    public MFCollectionsHandler(IEventBus eventBus, ObsSystemDbWrapper db, ResourcePermissions permissions, List<CollectionConfig> collections)
    {
        super();
        addCustomCollections(collections);
    }
    
    
    @Override
    protected void addDefaultCollections()
    {
        // moving features collection
        var mfCol = new CollectionInfo();
        mfCol.id = "all";
        mfCol.title = "All Moving Features";
        mfCol.description = "All moving features monitored by this server";
        mfCol.featureType = "MovingFeature";
        mfCol.crs.add(SWEConstants.REF_FRAME_CRS84h);
        mfCol.links.add(ResourceLink.self(NAMES[0], ResourceFormat.JSON.getMimeType()));
        addItemsLink("Access the features",
            "/" +MFCollectionsHandler.NAMES[0] +
            "/" +mfCol.id +
            "/" +MFCollectionItemsHandler.NAMES[0], mfCol.links);
        allCollections.put(mfCol.id, mfCol);
    }
    
    
    protected void addCustomCollections(List<CollectionConfig> collections)
    {
        for (var config: collections)
        {
            var mfCol = new CollectionInfo();
            mfCol.id = config.name;
            mfCol.title = config.title;
            mfCol.description = config.description;
            mfCol.featureType = "MovingFeature";
            mfCol.crs.add(SWEConstants.REF_FRAME_CRS84h);
            mfCol.links.add(ResourceLink.self(NAMES[0], ResourceFormat.JSON.getMimeType()));
            addItemsLink("Access the features", 
                "/" +MFCollectionsHandler.NAMES[0] +
                "/" +mfCol.id +
                "/" +MFCollectionItemsHandler.NAMES[0], mfCol.links);
            allCollections.put(mfCol.id, mfCol);
        }
    }
    
    
    protected Map<String, CollectionInfo> getCollections(RequestContext ctx)
    {
        return allCollections;
    }
    
}
