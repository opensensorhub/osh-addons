/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.store.feature;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.opengis.sensorml.v20.Deployment;
import org.sensorhub.api.datastore.deployment.DeploymentFilter;
import org.sensorhub.api.datastore.deployment.IDeploymentStore;
import org.sensorhub.api.datastore.feature.FeatureKey;
import org.sensorhub.api.datastore.system.ISystemDescStore;
import org.sensorhub.api.system.IDeploymentWithDesc;
import org.sensorhub.impl.datastore.postgis.IdProviderType;
import org.sensorhub.impl.datastore.postgis.builder.QueryBuilderDeploymentStore;
import org.sensorhub.impl.service.consys.sensorml.DeploymentAdapter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


public class PostgisDeploymentStoreImpl extends
        PostgisBaseFeatureStoreImpl<IDeploymentWithDesc, IDeploymentStore.DeploymentField, DeploymentFilter, QueryBuilderDeploymentStore> implements IDeploymentStore {

    public PostgisDeploymentStoreImpl(String url, String dbName, String login, String password,
                                      int idScope, IdProviderType dsIdProviderType, boolean useBatch) {
        super(url,dbName, login, password, idScope, dsIdProviderType, new QueryBuilderDeploymentStore(), useBatch);
    }

    public PostgisDeploymentStoreImpl(String url, String dbName, String login, String password, String dataStoreName,
                                      int idScope, IdProviderType dsIdProviderType, boolean useBatch) {
        super(url,dbName, login, password, idScope, dsIdProviderType, new QueryBuilderDeploymentStore(dataStoreName), useBatch);
    }

    @Override
    protected IDeploymentWithDesc readFeature(String data) throws IOException {
        StringReader stringReader = new StringReader(data);
        JsonReader reader = new JsonReader(stringReader);
        Deployment result = (Deployment) smlJsonBindings.readDescribedObject(reader);
        return new DeploymentAdapter(result);
    }
    @Override
    protected  String writeFeature(IDeploymentWithDesc feature) throws IOException {
        StringWriter stringWriter = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(stringWriter);
        smlJsonBindings.writeDescribedObject(jsonWriter, feature.getFullDescription());
        return stringWriter.getBuffer().toString();
    }

    @Override
    public Set<Entry<FeatureKey, IDeploymentWithDesc>> entrySet() {
        DeploymentFilter procedureFilter = new DeploymentFilter.Builder().build();
        return this.selectEntries(procedureFilter, new HashSet<>()).collect(Collectors.toSet());
    }

    @Override
    public void linkTo(ISystemDescStore systemStore) {
        super.linkTo(systemStore);
    }
}
