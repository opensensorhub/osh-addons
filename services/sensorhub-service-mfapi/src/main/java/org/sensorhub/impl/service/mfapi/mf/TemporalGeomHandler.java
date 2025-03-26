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
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.SpatialFilter;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.event.IEventBus;
import org.sensorhub.impl.service.consys.InvalidRequestException;
import org.sensorhub.impl.service.consys.ObsSystemDbWrapper;
import org.sensorhub.impl.service.consys.ServiceErrors;
import org.sensorhub.impl.service.consys.RestApiServlet.ResourcePermissions;
import org.sensorhub.impl.service.consys.resource.BaseResourceHandler;
import org.sensorhub.impl.service.consys.resource.RequestContext;
import org.sensorhub.impl.service.consys.resource.ResourceFormat;
import org.sensorhub.impl.system.DataStreamTransactionHandler;
import org.vast.swe.helper.GeoPosHelper;
import org.sensorhub.impl.service.consys.resource.ResourceBinding;
import org.sensorhub.impl.service.consys.resource.RequestContext.ResourceRef;


public class TemporalGeomHandler extends BaseResourceHandler<BigId, IObsData, ObsFilter, IObsStore>
{
    public static final String[] NAMES = { "tgeometries" };
    
    final IEventBus eventBus;
    final IObsSystemDatabase db;
    
    
    public static class ObsHandlerContextData
    {
        public BigId dsID;
        public IDataStreamInfo dsInfo;
        public BigId foiId;
        public DataStreamTransactionHandler dsHandler;
    }
    
    
    public TemporalGeomHandler(IEventBus eventBus, ObsSystemDbWrapper db, ResourcePermissions permissions)
    {
        super(db.getReadDb().getObservationStore(), db.getObsIdEncoder(), db.getIdEncoders(), permissions);
        
        this.eventBus = eventBus;
        this.db = db.getReadDb();
    }
    
    
    @Override
    protected ResourceBinding<BigId, IObsData> getBinding(RequestContext ctx, boolean forReading) throws IOException
    {
        // select binding
        var format = ctx.getFormat();
        if (format.isOneOf(ResourceFormat.JSON, ResourceFormat.AUTO))
            return new TemporalGeomBindingJson(ctx, idEncoders, forReading, dataStore);
        else
            throw ServiceErrors.unsupportedFormat(format);
    }


    @Override
    protected BigId getKey(RequestContext ctx, String id) throws InvalidRequestException
    {
        return decodeID(ctx, id);
    }
    
    
    @Override
    protected String encodeKey(final RequestContext ctx, BigId key)
    {
        return idEncoder.encodeID(key);
    }


    @Override
    protected ObsFilter getFilter(ResourceRef parent, Map<String, String[]> queryParams, long offset, long limit) throws InvalidRequestException
    {
        var builder = new ObsFilter.Builder()
            .withDataStreams()
                .withObservedProperties(GeoPosHelper.DEF_LONGITUDE)
                .done()
            .withFois(parent.internalID);
        
        // phenomenonTime param
        var phenomenonTime = parseTimeStampArg("datetime", queryParams);
        if (phenomenonTime != null)
            builder.withPhenomenonTime(phenomenonTime);
        
        // use opensearch bbox param to filter spatially
        var bbox = parseBboxArg("bbox", queryParams);
        if (bbox != null)
        {
            builder.withPhenomenonLocation(new SpatialFilter.Builder()
                .withBbox(bbox)
                .build());
        }
        
        // geom param
        var geom = parseGeomArg("location", queryParams);
        if (geom != null)
        {
            builder.withPhenomenonLocation(new SpatialFilter.Builder()
                .withRoi(geom)
                .build());
        }
        
        // limit
        // need to limit to offset+limit+1 since we rescan from the beginning for now
        if (limit != Long.MAX_VALUE)
            builder.withLimit(offset+limit+1);
        
        return builder.build();
    }
    
    
    @Override
    protected boolean isValidID(BigId internalID)
    {
        return false;
    }


    @Override
    protected void validate(IObsData resource)
    {
        // TODO Auto-generated method stub
    }
    
    
    @Override
    public String[] getNames()
    {
        return NAMES;
    }
}
