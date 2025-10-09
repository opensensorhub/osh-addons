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

import com.google.common.io.BaseEncoding;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBWriter;
import org.sensorhub.api.datastore.FullTextFilter;
import org.sensorhub.api.datastore.SpatialFilter;
import org.sensorhub.api.datastore.TemporalFilter;
import org.sensorhub.api.datastore.feature.FeatureFilter;
import org.sensorhub.api.datastore.feature.FeatureFilterBase;
import org.sensorhub.impl.datastore.postgis.utils.PostgisUtils;
import org.vast.ogc.gml.IFeature;

import java.time.Instant;
import java.util.SortedSet;
import java.util.stream.Collectors;

public class BaseFeatureFilterQuery<V extends IFeature,F extends FeatureFilterBase<? super V>> extends FilterQuery {
    protected ThreadLocal<WKBWriter> threadLocalWriter = ThreadLocal.withInitial(WKBWriter::new);

    protected BaseFeatureFilterQuery(String tableName, FilterQueryGenerator filterQueryGenerator) {
        super(tableName, filterQueryGenerator);
    }

    public FilterQueryGenerator build(F filter) {
        this.handleInternalIDs(filter.getInternalIDs());
        this.handleValidTimeFilter(filter.getValidTime());
        this.handleSpatialFilter(filter.getLocationFilter());
        this.handleFullTextFilter(filter.getFullTextFilter());
        this.handleUniqueIds(filter.getUniqueIDs());
        return this.filterQueryGenerator;
    }

    protected void handleValidTimeFilter(TemporalFilter temporalFilter) {
        if(temporalFilter != null) {
            var timeRange = PostgisUtils.getRangeFromTemporal(temporalFilter);
            String sb = this.tableName + ".validTime " +
                    PostgisUtils.getOperator(temporalFilter) + " '[" + timeRange[0] + "," + timeRange[1] + "]'";
            filterQueryGenerator.addCondition(sb);
        }
    }

    protected void handleParentFilter(FeatureFilter parentFilter) {
        if (parentFilter != null) {
            if (parentFilter.getInternalIDs() != null && !parentFilter.getInternalIDs().isEmpty()) {
                filterQueryGenerator.addCondition(tableName+".parentId in (" +
                        parentFilter.getInternalIDs().stream().map(bigId -> String.valueOf(bigId.getIdAsLong())).collect(Collectors.joining(",")) +
                        ")");
            }
            if(parentFilter.getUniqueIDs() != null) {
                filterQueryGenerator.addInnerJoin(this.tableName+ " t2 ON " + this.tableName + ".parentId" + " = t2.id");
                for(String uid: parentFilter.getUniqueIDs()) {
                    filterQueryGenerator.addCondition("t2.data->'properties'->>'uid' = '"+uid+"'");
                }
            }
        }
    }

    protected void handleFullTextFilter(FullTextFilter fullTextFilter) {
        if (fullTextFilter != null) {
            // can use directly ~* for fast lookup
            // https://www.postgresql.org/docs/current/pgtrgm.html
            if(fullTextFilter.getKeywords() != null) {
                String sb = "(" + tableName + ".data->'properties'->>'description') ~* '(" +
                        fullTextFilter.getKeywords().stream().collect(Collectors.joining("|")) +
                        ")'";
                filterQueryGenerator.addCondition(sb);
            }
        }
    }

    protected void handleSpatialFilter(SpatialFilter spatialFilter) {
        if (spatialFilter != null) {
            StringBuilder sb = new StringBuilder();
            Geometry geometry = spatialFilter.getRoi();

            byte[] geomAsBinary = threadLocalWriter.get().write(geometry);
            sb.append("ST_Intersects(").append(tableName).append(".geometry,'").append(encodeHexString(geomAsBinary)).append("')");
            filterQueryGenerator.addCondition(sb.toString());
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
            filterQueryGenerator.addCondition(sb.toString());
        }
    }

    private String encodeHexString(byte[] byteArray) {
        return BaseEncoding.base16().encode(byteArray);
    }
}
