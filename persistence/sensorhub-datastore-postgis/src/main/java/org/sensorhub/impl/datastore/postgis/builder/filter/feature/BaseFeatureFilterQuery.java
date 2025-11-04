/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.builder.filter.feature;

import com.google.common.io.BaseEncoding;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBWriter;
import org.sensorhub.api.datastore.FullTextFilter;
import org.sensorhub.api.datastore.SpatialFilter;
import org.sensorhub.api.datastore.TemporalFilter;
import org.sensorhub.api.datastore.feature.FeatureFilter;
import org.sensorhub.api.datastore.feature.FeatureFilterBase;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.impl.datastore.postgis.builder.filter.FilterQuery;
import org.sensorhub.impl.datastore.postgis.builder.generator.FilterQueryGenerator;
import org.sensorhub.impl.datastore.postgis.utils.PostgisUtils;
import org.vast.ogc.gml.IFeature;

import java.util.SortedSet;
import java.util.stream.Collectors;

public abstract class BaseFeatureFilterQuery<V extends IFeature,F extends FeatureFilterBase<? super V>, FQ extends FilterQueryGenerator> extends FilterQuery<FQ> {
    protected ThreadLocal<WKBWriter> threadLocalWriter = ThreadLocal.withInitial(WKBWriter::new);

    protected BaseFeatureFilterQuery(String tableName, FQ filterQueryGenerator) {
        super(tableName, filterQueryGenerator);
    }

    public FQ build(F filter) {
        this.handleInternalIDs(filter.getInternalIDs());
        this.handleValidTimeFilter(filter.getValidTime());
        this.handleSpatialFilter(filter.getLocationFilter());
        this.handleFullTextFilter(filter.getFullTextFilter());
        this.handleUniqueIds(filter.getUniqueIDs());
        return this.filterQueryGenerator;
    }

    protected void handleValidTimeFilter(TemporalFilter temporalFilter) {
        if(temporalFilter != null) {
            this.handleValidTimeFilter(temporalFilter, PostgisUtils.getOperator(temporalFilter));
        }
    }

    public void handleValidTimeFilter(TemporalFilter temporalFilter, String rangeOpStr) {
        if(temporalFilter != null) {
            var timeRange = PostgisUtils.getRangeFromTemporal(temporalFilter);
            String sb =" '[" + timeRange[0] + "," + timeRange[1] + "]' "+rangeOpStr+" "+this.tableName + ".validTime ";
            addCondition(sb);
        }
    }

    protected abstract void handleParentFilter(FeatureFilter parentFilter);

    protected void handleFullTextFilter(FullTextFilter fullTextFilter) {
        if (fullTextFilter != null) {
            // can use directly ~* for fast lookup
            // https://www.postgresql.org/docs/current/pgtrgm.html
            if(fullTextFilter.getKeywords() != null) {
                String sb = "(" + tableName + ".data->'properties'->>'description') ~* '(" +
                        fullTextFilter.getKeywords().stream().collect(Collectors.joining("|")) +
                        ")'";
                addCondition(sb);
            }
        }
    }

    protected void handleSpatialFilter(SpatialFilter spatialFilter) {
        if (spatialFilter != null) {
            StringBuilder sb = new StringBuilder();
            Geometry geometry = spatialFilter.getRoi();

            byte[] geomAsBinary = threadLocalWriter.get().write(geometry);
            sb.append("ST_Intersects(").append(tableName).append(".geometry,'").append(encodeHexString(geomAsBinary)).append("')");
            addCondition(sb.toString());
        }
    }

    protected void handleUniqueIds(SortedSet<String> uniqueIds) {
        if (uniqueIds != null) {
            StringBuilder sb = new StringBuilder();
            // Id can be regex
            // we have to use ILIKE behind trigram INDEX
            String currentId;
            int i = 0;
            sb.append("(");
            for(String uid: uniqueIds) {
                // ILIKE use % OPERATOR
                currentId = uid.replaceAll("\\*","%");
                sb.append("(").append(tableName).append(".data->'properties'->>'uid') ILIKE '").append(currentId).append("'");
                if(++i < uniqueIds.size()) {
                    sb.append(" OR ");
                }
            }
            sb.append(")");
            addCondition(sb.toString());
        }
    }
    protected abstract void handleParentFilter(SystemFilter parentFilter);

    private String encodeHexString(byte[] byteArray) {
        return BaseEncoding.base16().encode(byteArray);
    }
}
