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
import net.opengis.sensorml.v20.AbstractProcess;
import org.sensorhub.api.datastore.feature.FeatureKey;
import org.sensorhub.api.datastore.procedure.IProcedureStore;
import org.sensorhub.api.datastore.procedure.ProcedureFilter;
import org.sensorhub.api.procedure.IProcedureWithDesc;
import org.sensorhub.impl.datastore.postgis.IdProviderType;
import org.sensorhub.impl.datastore.postgis.builder.QueryBuilderProcedureStore;
import org.sensorhub.impl.system.wrapper.SmlFeatureWrapper;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


public class PostgisProcedureStoreImpl extends
        PostgisBaseFeatureStoreImpl<IProcedureWithDesc, IProcedureStore.ProcedureField, ProcedureFilter, QueryBuilderProcedureStore> implements IProcedureStore {


    public PostgisProcedureStoreImpl(String url, String dbName, String login, String password, int idScope, IdProviderType dsIdProviderType) {
        super(url,dbName, login, password, idScope, dsIdProviderType, new QueryBuilderProcedureStore(), false);
    }

    public PostgisProcedureStoreImpl(String url, String dbName, String login, String password, String dataStoreName, int idScope, IdProviderType dsIdProviderType) {
        super(url,dbName, login, password, idScope, dsIdProviderType, new QueryBuilderProcedureStore(dataStoreName), false);
    }

    @Override
    protected IProcedureWithDesc readFeature(String data) throws IOException {
        StringReader stringReader = new StringReader(data);
        JsonReader reader = new JsonReader(stringReader);
        AbstractProcess result = (AbstractProcess) smlJsonBindings.readDescribedObject(reader);
        return new SmlFeatureWrapper(result);
    }
    @Override
    protected  String writeFeature(IProcedureWithDesc feature) throws IOException {
        StringWriter stringWriter = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(stringWriter);
        smlJsonBindings.writeDescribedObject(jsonWriter, feature.getFullDescription());
        return stringWriter.getBuffer().toString();
    }

    @Override
    public Set<Entry<FeatureKey, IProcedureWithDesc>> entrySet() {
        ProcedureFilter procedureFilter = new ProcedureFilter.Builder().build();
        return this.selectEntries(procedureFilter, new HashSet<>()).collect(Collectors.toSet());
    }

}
