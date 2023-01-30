/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2020 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.mfapi.mf;

import java.io.IOException;
import java.util.Map;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.feature.FeatureKey;
import org.sensorhub.api.datastore.feature.FoiFilter;
import org.sensorhub.api.datastore.feature.IFoiStore;
import org.sensorhub.api.event.IEventBus;
import org.sensorhub.impl.service.sweapi.InvalidRequestException;
import org.sensorhub.impl.service.sweapi.ObsSystemDbWrapper;
import org.sensorhub.impl.service.sweapi.ResourceParseException;
import org.sensorhub.impl.service.sweapi.RestApiServlet.ResourcePermissions;
import org.sensorhub.impl.service.sweapi.ServiceErrors;
import org.sensorhub.impl.service.sweapi.feature.AbstractFeatureHandler;
import org.sensorhub.impl.service.sweapi.feature.DynamicFoiBindingGeoJson;
import org.sensorhub.impl.service.sweapi.resource.BaseResourceHandler;
import org.sensorhub.impl.service.sweapi.resource.RequestContext;
import org.sensorhub.impl.service.sweapi.resource.ResourceFormat;
import org.sensorhub.impl.service.sweapi.resource.ResourceBinding;
import org.sensorhub.impl.service.sweapi.resource.RequestContext.ResourceRef;
import org.vast.ogc.gml.IFeature;


public class MFHandler extends AbstractFeatureHandler<IFeature, FoiFilter, FoiFilter.Builder, IFoiStore>
{
    public static final String[] NAMES = { "features" };
    
    final IObsSystemDatabase db;
    
    
    public MFHandler(IEventBus eventBus, ObsSystemDbWrapper db, ResourcePermissions permissions)
    {
        super(db.getFoiStore(), db.getFoiIdEncoder(), db.getIdEncoders(), permissions);
        this.db = db.getReadDb();
    }


    @Override
    protected ResourceBinding<FeatureKey, IFeature> getBinding(RequestContext ctx, boolean forReading) throws IOException
    {
        var format = ctx.getFormat();
        
        if (format.equals(ResourceFormat.HTML) ||
           (format.equals(ResourceFormat.AUTO) && ctx.isBrowserHtmlRequest()))
        {
            return new MFBindingHtml(ctx, idEncoders, true, db);
        }
        else if (format.isOneOf(ResourceFormat.AUTO, ResourceFormat.JSON, ResourceFormat.GEOJSON))
        {
            if (ctx.getParameterMap().containsKey("snapshot"))
                return new DynamicFoiBindingGeoJson(ctx, idEncoders, forReading, db);
            else
                return new MFBindingGeoJson(ctx, idEncoders, forReading);
        }
        else
            throw ServiceErrors.unsupportedFormat(format);
    }
    
    
    @Override
    protected boolean isValidID(BigId internalID)
    {
        return dataStore.contains(internalID);
    }


    @Override
    protected void buildFilter(ResourceRef parent, Map<String, String[]> queryParams, FoiFilter.Builder builder) throws InvalidRequestException
    {
        super.buildFilter(parent, queryParams, builder);
        
        if (parent.internalID != null)
        {
            builder.withParents()
                .withInternalIDs(parent.internalID)
                .includeMembers(true)
                .done();
        }
        else
        {
            // parent ID
            var ids = parseResourceIds("parentId", queryParams, idEncoders.getFoiIdEncoder());
            if (ids != null && !ids.isEmpty())
                builder.withParents().withInternalIDs(ids).done();
            
            // parent UID
            var uids = parseMultiValuesArg("parentUid", queryParams);
            if (uids != null && !uids.isEmpty())
                builder.withParents().withUniqueIDs(uids).done();
        }
    }
    
    
    @Override
    public void doPost(RequestContext ctx) throws InvalidRequestException, IOException, SecurityException
    {
        throw ServiceErrors.unsupportedOperation(BaseResourceHandler.READ_ONLY_ERROR);
    }


    @Override
    public void doPut(RequestContext ctx) throws InvalidRequestException, IOException, SecurityException
    {
        throw ServiceErrors.unsupportedOperation(BaseResourceHandler.READ_ONLY_ERROR);
    }


    @Override
    public void doDelete(RequestContext ctx) throws InvalidRequestException, IOException, SecurityException
    {
        throw ServiceErrors.unsupportedOperation(BaseResourceHandler.READ_ONLY_ERROR);
    }


    @Override
    protected void validate(IFeature resource) throws ResourceParseException
    {
        super.validate(resource);
    }


    @Override
    public String[] getNames()
    {
        return NAMES;
    }
}
