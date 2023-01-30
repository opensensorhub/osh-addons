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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.IdEncoders;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.impl.service.sweapi.obs.ObsHandler.ObsHandlerContextData;
import org.sensorhub.impl.service.sweapi.resource.RequestContext;
import org.sensorhub.impl.service.sweapi.resource.ResourceLink;
import org.vast.data.DataIterator;
import org.vast.json.JsonInliningWriter;
import org.vast.swe.helper.GeoPosHelper;
import org.sensorhub.impl.service.sweapi.resource.ResourceBindingJson;
import com.google.common.collect.ImmutableSet;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.Vector;


public class TemporalGeomBindingJson extends ResourceBindingJson<BigId, IObsData>
{
    ObsHandlerContextData contextData;
    IObsStore obsStore;
    Map<BigId, TemporalPropCollector> dataStreamCollectors;
    
    static final Set<String> GEOM_DEFS = ImmutableSet.of(
        GeoPosHelper.DEF_LATITUDE_GEODETIC,
        GeoPosHelper.DEF_LONGITUDE,
        GeoPosHelper.DEF_ALTITUDE_ELLIPSOID,
        GeoPosHelper.DEF_ALTITUDE_MSL
    );
    
    
    TemporalGeomBindingJson(RequestContext ctx, IdEncoders idEncoders, boolean forReading, IObsStore obsStore) throws IOException
    {
        super(ctx, idEncoders, forReading);
        this.contextData = (ObsHandlerContextData)ctx.getData();
        this.obsStore = obsStore;
        this.dataStreamCollectors = new HashMap<>();
    }
    
    
    @Override
    public IObsData deserialize(JsonReader reader) throws IOException
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public void serialize(BigId key, IObsData obs, boolean showLinks, JsonWriter writer) throws IOException
    {
        var collector = dataStreamCollectors.computeIfAbsent(obs.getDataStreamID(), k -> {
            
            // try to fetch datastream since it's needed to configure collector
            var dsID = obs.getDataStreamID();
            var dsInfo = obsStore.getDataStreams().get(new DataStreamKey(dsID));
            if (!hasGeom(dsInfo.getRecordStructure()))
                return null;
            
            var col = new TemporalPropCollector(dsID);
            col.setDataComponents(dsInfo.getRecordStructure());
            return col;
        });
        
        if (collector != null)
            collector.collect(obs);
    }
    
    
    @Override
    public void startCollection() throws IOException
    {
        startJsonCollection(writer);
    }
    
    
    @Override
    public void endCollection(Collection<ResourceLink> links) throws IOException
    {
        for (var ds: dataStreamCollectors.values())
        {
            writer.beginObject();
            
            writer.name("id").value("tg-" + idEncoders.getDataStreamIdEncoder().encodeID(ds.dsId));
            writer.name("type").value("MovingPoint");
            
            writer.name("datetimes").beginArray();
            for (var t: ds.dateTimes)
                writer.value(t.toString());
            writer.endArray();
            
            writer.name("coordinates").beginArray();
            for (int i = 0; i < ds.dateTimes.size(); i++)
            {
                writer.beginArray();
                ((JsonInliningWriter)writer).writeInline(true);
                for (var prop: ds.temporalProperties)
                {
                    if (prop.comp.getParent() instanceof Vector)
                        writer.jsonValue(prop.values.get(i).toString());
                }
                writer.endArray();
                ((JsonInliningWriter)writer).writeInline(false);
            }
            writer.endArray();
            
            writer.name("interpolation").value("Linear");
            
            writer.endObject();
        }
        
        endJsonCollection(writer, links);
    }
    
    
    boolean hasGeom(DataComponent struct)
    {
        for (var it = new DataIterator(struct); it.hasNext(); )
        {
            var c = it.next();
            if (GEOM_DEFS.contains(c.getDefinition()))
                return true;
        }
        
        return false;
    }
    
    
    protected String getItemsPropertyName()
    {
        return "temporalGeometries";
    }
}
