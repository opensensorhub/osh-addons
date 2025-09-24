/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.feature;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.opengis.gml.v32.Point;
import net.opengis.sensorml.v20.AbstractProcess;
import org.sensorhub.api.datastore.deployment.IDeploymentStore;
import org.sensorhub.api.datastore.feature.FeatureKey;
import org.sensorhub.api.datastore.obs.IDataStreamStore;
import org.sensorhub.api.datastore.procedure.IProcedureStore;
import org.sensorhub.api.datastore.system.ISystemDescStore;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.api.system.ISystemWithDesc;
import org.sensorhub.impl.datastore.postgis.IdProviderType;
import org.sensorhub.impl.datastore.postgis.builder.QueryBuilderSystemDescStore;
import org.sensorhub.impl.service.consys.sensorml.SMLConverter;
import org.sensorhub.impl.system.wrapper.ProcessWrapper;
import org.sensorhub.impl.system.wrapper.SmlFeatureWrapper;
import org.vast.ogc.xlink.IXlinkReference;
import org.vast.sensorML.SMLBuilders;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


public class PostgisSystemDescStoreImpl extends
        PostgisBaseFeatureStoreImpl<ISystemWithDesc, ISystemDescStore.SystemField, SystemFilter, QueryBuilderSystemDescStore> implements ISystemDescStore {

    protected IDataStreamStore dataStreamStore;
    protected IProcedureStore procedureStore;
    protected IDeploymentStore deploymentStore;

    public PostgisSystemDescStoreImpl(String url, String dbName, String login, String password, int idScope, IdProviderType dsIdProviderType) {
        super(url,dbName, login, password, idScope, dsIdProviderType, new QueryBuilderSystemDescStore(), false);
    }

    public PostgisSystemDescStoreImpl(String url, String dbName, String login, String password, String dataStoreName, int idScope, IdProviderType dsIdProviderType){
        super(url,dbName, login, password, idScope, dsIdProviderType, new QueryBuilderSystemDescStore(dataStoreName), false);
    }

    @Override
    protected ISystemWithDesc readFeature(String data) throws IOException {
        StringReader stringReader = new StringReader(data);
        JsonReader reader = new JsonReader(stringReader);
        AbstractProcess result = (AbstractProcess) smlJsonBindings.readDescribedObject(reader);
        return new SmlFeatureWrapper(result);
    }

    @Override
    protected String writeFeature(ISystemWithDesc feature) throws IOException {
        try {
            var sml = feature.getFullDescription();
            if (sml == null && feature instanceof ISystemWithDesc) {
//                sml = new SMLConverter().genericFeatureToSystem(feature);
                SMLBuilders.AbstractProcessBuilder<?,?> builder = null;
                builder = new SMLConverter().createPhysicalSystem()
                        .uniqueID(feature.getUniqueIdentifier())
                        .name(feature.getName())
                        .description(feature.getDescription())
                        .definition(feature.getType());

                var validTime = feature.getValidTime();
                if (feature.getValidTime() != null) {
                    builder.validTimePeriod(
                            validTime.begin().atOffset(ZoneOffset.UTC),
                            validTime.end().atOffset(ZoneOffset.UTC));
                }

                if (feature.getGeometry() != null) {
                    if (feature.getGeometry() instanceof Point) {
                        ((SMLBuilders.PhysicalSystemBuilder) builder).location((Point) feature.getGeometry());
                    } else {
                        throw new IllegalStateException("Unsupported System geometry: " + feature.getGeometry());
                    }
                }

                var systemKindLink = (IXlinkReference<?>)feature.getProperties().get(new QName("systemKind"));
                if (systemKindLink != null) {
                    var href = systemKindLink.getHref().replace("f=json", "f=sml");
                    var title = systemKindLink.getTitle();
                    builder.typeOf(href, title);
                }
                sml = builder.build();
            }

            sml = ProcessWrapper.getWrapper((AbstractProcess)sml).withId(feature.getId());
            StringWriter stringWriter = new StringWriter();
            JsonWriter jsonWriter = new JsonWriter(stringWriter);
            smlJsonBindings.writeDescribedObject(jsonWriter, sml);
            jsonWriter.flush();
            return stringWriter.getBuffer().toString();
        } catch (IllegalStateException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            } else {
                throw e;
            }
        } catch (Exception e) {
            throw new IOException("Error writing SensorML JSON", e);
        }
    }

    @Override
    public Set<Entry<FeatureKey, ISystemWithDesc>> entrySet() {
        SystemFilter filter = new SystemFilter.Builder().build();
        return this.selectEntries(filter, new HashSet<>()).collect(Collectors.toSet());
    }

    @Override
    public void linkTo(IDataStreamStore dataStreamStore) {
        this.dataStreamStore = dataStreamStore;
    }

    @Override
    public void linkTo(IProcedureStore procedureStore) {
        this.procedureStore = procedureStore;
    }

    @Override
    public void linkTo(IDeploymentStore deploymentStore) {
        this.deploymentStore = deploymentStore;
    }
}
