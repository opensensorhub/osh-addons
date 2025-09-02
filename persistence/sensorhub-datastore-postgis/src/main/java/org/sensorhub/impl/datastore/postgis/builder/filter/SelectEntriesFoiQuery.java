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

import org.sensorhub.api.datastore.feature.FoiFilter;

public class SelectEntriesFoiQuery extends SelectEntriesQuery {
    protected static abstract class Init<T extends SelectEntriesFoiQuery.Init<T>> extends SelectEntriesQuery.Init<T> {

        public T withFoiFilter(FoiFilter filter) {
            if(filter != null) {
                FoiFilterQuery filterQuery = new FoiFilterQuery(this.tableName, filterQueryGenerator);
                filterQueryGenerator = filterQuery.build(filter);
            }
            return self();
        }

        public SelectEntriesFoiQuery build() {
            return new SelectEntriesFoiQuery(this);
        }
    }

    public static class Builder extends SelectEntriesFoiQuery.Init<SelectEntriesFoiQuery.Builder> {
        @Override
        protected SelectEntriesFoiQuery.Builder self() {
            return this;
        }
    }

    protected SelectEntriesFoiQuery(SelectEntriesFoiQuery.Init<?> init) {
        super(init);
    }
}
