/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.builder.filter.command;

import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.api.datastore.TemporalFilter;
import org.sensorhub.api.datastore.command.CommandFilter;
import org.sensorhub.api.datastore.command.CommandStatusFilter;
import org.sensorhub.impl.datastore.postgis.builder.filter.FilterQuery;
import org.sensorhub.impl.datastore.postgis.builder.generator.FilterQueryGenerator;

import java.util.Set;
import java.util.stream.Collectors;

public abstract class BaseCommandStatusFilterQuery<F extends FilterQueryGenerator> extends FilterQuery<F> {

    protected BaseCommandStatusFilterQuery(String tableName, F filterQueryGenerator) {
        super(tableName, filterQueryGenerator);
    }

    public F build(CommandStatusFilter filter) {
        this.handleReportTimeFilter(filter.getReportTime());
        this.handleExecutionTimeFilter(filter.getExecutionTime());
        this.handleStatusCodes(filter.getStatusCodes());
        this.handleCommandFilter(filter.getCommandFilter());
        return this.filterQueryGenerator;
    }

    protected abstract void handleReportTimeFilter(TemporalFilter temporalFilter);

    protected abstract void handleExecutionTimeFilter(TemporalFilter temporalFilter);

    protected void handleStatusCodes(Set<ICommandStatus.CommandStatusCode> statusCodes) {
        if(statusCodes != null && !statusCodes.isEmpty()) {
            addCondition("("+this.tableName+".data->>'statusCode') in (" +
                    statusCodes.stream().map(name -> "'"+name.name()+"'").collect(Collectors.joining(",")) +
                    ")");
        }
    }

    protected abstract void handleCommandFilter(CommandFilter commandFilter);
}
