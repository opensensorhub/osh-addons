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
import org.sensorhub.impl.service.consys.obs.ObsHandler.ObsHandlerContextData;
import org.sensorhub.impl.service.consys.resource.RequestContext;
import org.sensorhub.impl.service.consys.resource.ResourceLink;
import org.vast.json.JsonInliningWriter;
import org.vast.swe.SWEConstants;
import org.vast.swe.helper.GeoPosHelper;
import org.sensorhub.impl.service.consys.resource.ResourceBindingJson;
import com.google.common.collect.ImmutableSet;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.opengis.swe.v20.Boolean;
import net.opengis.swe.v20.Category;
import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.ScalarComponent;
import net.opengis.swe.v20.Text;


public class TemporalPropBindingJson extends ResourceBindingJson<BigId, IObsData>
{
    ObsHandlerContextData contextData;
    IObsStore obsStore;
    Map<BigId, TemporalPropCollector> dataStreamCollectors;
    
    static final Set<String> SKIPPED_DEFS = ImmutableSet.of(
        SWEConstants.DEF_PHENOMENON_TIME,
        SWEConstants.DEF_SAMPLING_TIME,
        SWEConstants.DEF_FORECAST_TIME,
        SWEConstants.DEF_RUN_TIME,
        GeoPosHelper.DEF_LATITUDE_GEODETIC,
        GeoPosHelper.DEF_LONGITUDE,
        GeoPosHelper.DEF_ALTITUDE_ELLIPSOID,
        GeoPosHelper.DEF_ALTITUDE_MSL
    );

    
    TemporalPropBindingJson(RequestContext ctx, IdEncoders idEncoders, boolean forReading, IObsStore obsStore) throws IOException
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
            
            // try to fetch datastream since it's needed to configure binding
            var dsID = obs.getDataStreamID();
            var dsInfo = obsStore.getDataStreams().get(new DataStreamKey(dsID));
            
            var col = new TemporalPropCollector(dsID);
            col.setDataComponents(dsInfo.getRecordStructure());
            return col;
        });
        
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
            
            writer.name("datetimes").beginArray();
            for (var t: ds.dateTimes)
                writer.value(t.toString());
            writer.endArray();
            
            for (var prop: ds.temporalProperties)
            {
                var comp = prop.comp;
                
                if (SKIPPED_DEFS.contains(comp.getDefinition()))
                    continue;
                
                writer.name(comp.getName()).beginObject();
                
                writer.name("type").value(getType(comp));
                
                if (comp.getDefinition() != null)
                    writer.name("form").value(comp.getDefinition());
                
                if (comp.getDescription() != null)
                    writer.name("description").value(comp.getDescription());
                
                writer.name("interpolation").value("Linear");
                
                writer.name("values").beginArray();
                ((JsonInliningWriter)writer).writeInline(true);
                for (var v: prop.values)
                {
                    if (v instanceof String)
                        writer.value((String)v);
                    else
                        writer.jsonValue(v.toString());
                }
                writer.endArray();
                ((JsonInliningWriter)writer).writeInline(false);
                
                writer.endObject();
            }
            
            writer.endObject();
        }
        
        endJsonCollection(writer, links);
    }
    
    
    protected String getType(ScalarComponent comp)
    {
        if (comp instanceof Quantity)
        {
            if (comp.getDataType() == DataType.FLOAT)
                return "TFloat";
            else
                return "TDouble";
        }
        
        else if (comp instanceof Count)
            return "TInt";
        
        else if (comp instanceof Category || comp instanceof Text)
            return "TText";
        
        else if (comp instanceof Boolean)
            return "TBool";
        
        return "Unknown";
    }
    
    
    protected String getItemsPropertyName()
    {
        return "temporalProperties";
    }
}
