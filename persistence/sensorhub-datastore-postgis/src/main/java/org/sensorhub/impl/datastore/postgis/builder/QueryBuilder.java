/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.builder;

import org.sensorhub.api.datastore.command.ICommandStatusStore;
import org.sensorhub.api.datastore.command.ICommandStore;
import org.sensorhub.api.datastore.command.ICommandStreamStore;
import org.sensorhub.api.datastore.deployment.IDeploymentStore;
import org.sensorhub.api.datastore.feature.IFeatureStore;
import org.sensorhub.api.datastore.feature.IFoiStore;
import org.sensorhub.api.datastore.obs.IDataStreamStore;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.api.datastore.procedure.IProcedureStore;
import org.sensorhub.api.datastore.system.ISystemDescStore;

public abstract class QueryBuilder {
    private final String tableName;

    protected IDataStreamStore dataStreamStore;
    protected IProcedureStore procedureStore;
    protected IDeploymentStore deploymentStore;
    protected IObsStore obsStore;
    protected IFeatureStore featureStore;
    protected ICommandStreamStore commandStreamStore;
    protected ICommandStore commandStore;
    protected ISystemDescStore systemStore;
    protected IFoiStore foiStore;
    protected ICommandStatusStore commandStatusStore;

    protected QueryBuilder(String tableName) {
        this.tableName = tableName;
    }

    public String getStoreTableName() {
        return this.tableName;
    }

    public String clearQuery() {
        return "DELETE FROM " + this.getStoreTableName();
    }

    public String selectByIdQuery() {
        return "SELECT * FROM " + this.getStoreTableName() + " WHERE id = ?";
    }

    public String selectLastIdQuery() {
        return "SELECT id FROM " + this.getStoreTableName() + " order by id ASC";
    }

    public String removeByIdQuery() {
        return "DELETE FROM " + this.getStoreTableName() + " WHERE id = ?";
    }

    public String removeAllQuery() {
        return "DELETE FROM "+this.getStoreTableName();
    }
    public String dropQuery() {
        return "DROP TABLE IF EXISTS " + this.getStoreTableName();
    }

    public String cloningTableQuery() {
        return "CREATE TABLE " + this.getStoreTableName() + "_backup AS TABLE " + this.getStoreTableName();
    }

    public String restoringTableQuery() {
        return "CREATE TABLE " + this.getStoreTableName() + " AS TABLE " + this.getStoreTableName() + "_backup";
    }

    public abstract String createTableQuery();

    public String countQuery() {
        return "SELECT COUNT(*) AS recordsCount FROM " + this.getStoreTableName();
    }

    public void linkTo(ISystemDescStore systemStore) { this.systemStore = systemStore; }
    public void linkTo(IObsStore obsStore) { this.obsStore = obsStore; }
    public void linkTo(IFeatureStore featureStore) { this.featureStore = featureStore; }
    public void linkTo(IDataStreamStore dataStreamStore) {
        this.dataStreamStore = dataStreamStore;
    }
    public void linkTo(IProcedureStore procedureStore) {
        this.procedureStore = procedureStore;
    }
    public void linkTo(IDeploymentStore deploymentStore) {
        this.deploymentStore = deploymentStore;
    }
    public void linkTo(ICommandStreamStore commandStreamStore) {
        this.commandStreamStore = commandStreamStore;
    }
    public void linkTo(ICommandStore commandStore) {
        this.commandStore = commandStore;
    }
    public void linkTo(IFoiStore foiStore) {
        this.foiStore = foiStore;
    }
    public void linkTo(ICommandStatusStore commandStatusStore) {
        this.commandStatusStore = commandStatusStore;
    }

}
