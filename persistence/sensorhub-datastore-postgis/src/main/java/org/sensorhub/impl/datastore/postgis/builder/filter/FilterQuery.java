/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.builder.filter;

import org.sensorhub.api.common.BigId;
import org.sensorhub.impl.datastore.postgis.utils.PostgisUtils;

import java.time.Instant;
import java.util.SortedSet;
import java.util.stream.Collectors;

public abstract class FilterQuery {
    protected String commandStreamTableName;
    protected String commandStatusTableName;
    protected String commandTableName;
    protected String sysDescTableName;
    protected String dataStreamTableName;
    protected String foiTableName;

    protected String tableName;

    protected FilterQueryGenerator filterQueryGenerator;
    protected FilterQuery(String tableName, FilterQueryGenerator filterQueryGenerator) {
        this.tableName = tableName;
        this.filterQueryGenerator = filterQueryGenerator;
    }

    protected void handleInternalIDs(SortedSet<BigId> ids) {
        if (ids != null && !ids.isEmpty()) {
            filterQueryGenerator.addCondition(this.tableName+".id in (" +
                    ids.stream().map(bigId -> String.valueOf(bigId.getIdAsLong())).collect(Collectors.joining(",")) +
                    ")");
        }
    }

    public void setCommandStreamTableName(String commandStreamTableName) {
        this.commandStreamTableName = commandStreamTableName;
    }

    public void setCommandStatusTableName(String commandStatusTableName) {
        this.commandStatusTableName = commandStatusTableName;
    }

    public void setCommandTableName(String commandTableName) {
        this.commandTableName = commandTableName;
    }

    public void setSysDescTableName(String sysDescTableName) {
        this.sysDescTableName = sysDescTableName;
    }

    public void setDataStreamTableName(String dataStreamTableName) {
        this.dataStreamTableName = dataStreamTableName;
    }

    public void setFoiTableName(String foiTableName) {
        this.foiTableName = foiTableName;
    }

}
