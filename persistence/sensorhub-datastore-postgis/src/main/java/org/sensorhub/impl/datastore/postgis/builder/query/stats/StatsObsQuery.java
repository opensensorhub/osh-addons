/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.builder.query.stats;

import org.sensorhub.api.datastore.feature.IFoiStore;
import org.sensorhub.api.datastore.obs.IDataStreamStore;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.datastore.obs.ObsStatsQuery;
import org.sensorhub.api.datastore.system.ISystemDescStore;
import org.sensorhub.impl.datastore.postgis.builder.filter.stats.SelectObsStatsFilterQuery;

import static org.sensorhub.api.datastore.obs.IObsStore.ObsField.PHENOMENON_TIME;

public class StatsObsQuery extends StatsQuery {
    private IFoiStore foiStore;
    private ISystemDescStore systemDescStore;
    private IDataStreamStore dataStreamStore;

    private long foiId;

    private long dsId;

    protected static abstract class Init<T extends Init<T>> extends StatsQuery.Init<T> {
        private IFoiStore foiStore;
        private ISystemDescStore systemDescStore;
        private  IDataStreamStore dataStreamStore;

        private long foiId;
        private long dsId;

        public T linkTo(IFoiStore foiStore) {
            this.foiStore = foiStore;
            return self();
        }
        public T linkTo(ISystemDescStore systemDescStore) {
            this.systemDescStore = systemDescStore;
            return self();
        }

        public T linkTo(IDataStreamStore dataStreamStore) {
            this.dataStreamStore = dataStreamStore;
            return self();
        }

        public T withObsStatsFilter(ObsStatsQuery filter) {
            if(filter != null) {
                long durationInSeconds = filter.getHistogramBinSize().getSeconds();
                filterQueryGenerator.addSelectField("floor((extract('epoch' from "+ PHENOMENON_TIME +") / "+durationInSeconds+" ))" +
                        " * "+durationInSeconds+" as interval_alias");
                filterQueryGenerator.addSelectField("COUNT(*)");
                filterQueryGenerator.addGroupBy("interval_alias");
                filterQueryGenerator.addOrderBy("interval_alias");
            }
            ObsFilter obsFilter = filter.getObsFilter();

            if(obsFilter != null) {
                SelectObsStatsFilterQuery obsFilterQuery = new SelectObsStatsFilterQuery(this.tableName, filterQueryGenerator);
                obsFilterQuery.setFoiId(foiId);
                obsFilterQuery.setFoiId(dsId);

                if(systemDescStore != null) {
                    obsFilterQuery.setSysDescTableName(systemDescStore.getDatastoreName());
                }
                if(dataStreamStore != null) {
                    obsFilterQuery.setDataStreamTableName(dataStreamStore.getDatastoreName());
                }
                filterQueryGenerator = obsFilterQuery.build(obsFilter);
            }


            return self();
        }

        public T withFoi(long foiId) {
            this.foiId = foiId;
            return self();
        }

        public T withDataStream(long dsId) {
            this.dsId = dsId;
            return self();
        }

        public StatsObsQuery build() {
            return new StatsObsQuery(this);
        }
    }

    public static class Builder extends Init<Builder> {
        @Override
        protected Builder self() {
            return this;
        }
    }

    protected StatsObsQuery(Init<?> init) {
        super(init);
        this.foiStore = init.foiStore;
        this.systemDescStore = init.systemDescStore;
        this.dataStreamStore = init.dataStreamStore;
        this.foiId = init.foiId;
        this.dsId = init.dsId;
    }
}
