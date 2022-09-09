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
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import org.sensorhub.api.common.IdEncoders;
import org.sensorhub.api.datastore.feature.FeatureKey;
import org.sensorhub.api.feature.FeatureWrapper;
import org.sensorhub.impl.service.sweapi.feature.AbstractFeatureBindingGeoJson;
import org.sensorhub.impl.service.sweapi.resource.RequestContext;
import org.sensorhub.impl.service.sweapi.resource.ResourceLink;
import org.vast.ogc.gml.GeoJsonBindings;
import org.vast.ogc.gml.IFeature;
import org.vast.util.Asserts;
import com.google.gson.stream.JsonWriter;


/**
 * <p>
 * GeoJSON formatter for moving feature resources
 * </p>
 *
 * @author Alex Robin
 * @since Aug 15, 2022
 */
public class MFBindingGeoJson extends AbstractFeatureBindingGeoJson<IFeature>
{
    
    public MFBindingGeoJson(RequestContext ctx, IdEncoders idEncoders, boolean forReading) throws IOException
    {
        super(ctx, idEncoders, forReading);
    }
    
    
    @Override
    protected GeoJsonBindings getJsonBindings()
    {
        return new GeoJsonBindings() {
            @Override
            protected void writeDateTimeValue(JsonWriter writer, OffsetDateTime dateTime) throws IOException
            {
                super.writeDateTimeValue(writer, dateTime.truncatedTo(ChronoUnit.SECONDS));
            }
            
            @Override
            protected void writeCustomJsonProperties(JsonWriter writer, IFeature bean) throws IOException
            {
                if (showLinks.get())
                {
                    var links = new ArrayList<ResourceLink>();
                    
                    links.add(new ResourceLink.Builder()
                        .rel("temporalGeometries")
                        .title("Temporal Geometries")
                        .href("/" + MFHandler.NAMES[0] + "/" +
                            bean.getId() + "/" + TemporalGeomHandler.NAMES[0])
                        .build());
                    
                    links.add(new ResourceLink.Builder()
                        .rel("temporalPropertiesCollection")
                        .title("Temporal Properties")
                        .href("/" + MFHandler.NAMES[0] + "/" +
                            bean.getId() + "/" + TemporalPropHandler.NAMES[0])
                        .build());
                    
                    writeLinksAsJson(writer, links);
                }
            }
        };
    }
    
    
    @Override
    protected IFeature getFeatureWithId(FeatureKey key, IFeature f)
    {
        Asserts.checkNotNull(key, FeatureKey.class);
        Asserts.checkNotNull(f, IFeature.class);
        
        return new FeatureWrapper(f) {
            @Override
            public String getId()
            {
                return idEncoders.getFoiIdEncoder().encodeID(key.getInternalID());
            }
        };
    }
}
