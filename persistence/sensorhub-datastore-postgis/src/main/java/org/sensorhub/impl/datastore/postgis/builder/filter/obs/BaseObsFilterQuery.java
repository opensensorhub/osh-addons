/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.builder.filter.obs;

import org.sensorhub.api.datastore.TemporalFilter;
import org.sensorhub.api.datastore.feature.FoiFilter;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.impl.datastore.postgis.builder.filter.FilterQuery;
import org.sensorhub.impl.datastore.postgis.builder.generator.FilterQueryGenerator;

public abstract class BaseObsFilterQuery<F extends FilterQueryGenerator> extends FilterQuery<F> {
    protected BaseObsFilterQuery(String tableName, F filterQueryGenerator) {
        super(tableName, filterQueryGenerator);
    }

    public F build(ObsFilter filter) {
        this.handleDataStreamFilter(filter.getDataStreamFilter());
        this.handlePhenomenonTimeFilter(filter.getPhenomenonTime());
        this.handleResultTimeFilter(filter.getResultTime());
        this.handleFoiFilter(filter.getFoiFilter(), filter);
        this.handleInternalIDs(filter.getInternalIDs());
        return this.filterQueryGenerator;
    }

    protected abstract void handleDataStreamFilter(DataStreamFilter dataStreamFilter);

    protected abstract void handlePhenomenonTimeFilter(TemporalFilter temporalFilter);

    protected abstract void handleResultTimeFilter(TemporalFilter temporalFilter);

    protected abstract void handleFoiFilter(FoiFilter foiFilter, ObsFilter obsFilter);
}
