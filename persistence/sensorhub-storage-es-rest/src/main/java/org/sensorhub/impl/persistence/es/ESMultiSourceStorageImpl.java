/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.es;

import java.io.IOException;
import java.util.*;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.support.AbstractClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.support.ValueType;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.sensorhub.api.persistence.IMultiSourceStorage;
import org.sensorhub.api.persistence.IObsStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * ES implementation of {@link IObsStorage} for storing observations.
 * </p>
 *
 * @author Mathieu Dhainaut <mathieu.dhainaut@gmail.com>
 * @author Nicolas Fortin, UMRAE Ifsttar
 * @since 2017
 */
public class ESMultiSourceStorageImpl extends ESObsStorageImpl implements IMultiSourceStorage<IObsStorage> {

    private static final Logger log = LoggerFactory.getLogger(ESMultiSourceStorageImpl.class); 
    
    
    public ESMultiSourceStorageImpl() {
        // default constructor
    }
    
    public ESMultiSourceStorageImpl(RestHighLevelClient client) {
		super(client);
	}	 
	
	@Override
	public Collection<String> getProducerIDs() {
		ArrayList<String> resultList = new ArrayList<>();
		final String aggregateName = "producers";
		// Compute unique values of producers id in the foi meta data index
		SearchRequest searchRequest = new SearchRequest(indexNameMetaData);
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.size(0); // Do not get hits
		BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

		if(config.filterByStorageId) {
			boolQueryBuilder.must(new TermQueryBuilder(STORAGE_ID_FIELD_NAME, config.id));
		}

		boolQueryBuilder.must(new TermQueryBuilder(METADATA_TYPE_FIELD_NAME, FOI_IDX_NAME));

		sourceBuilder.query(boolQueryBuilder);

		sourceBuilder.aggregation(new TermsAggregationBuilder(aggregateName, ValueType.STRING).field(ESDataStoreTemplate.PRODUCER_ID_FIELD_NAME));
		searchRequest.source(sourceBuilder);
		try {
			SearchResponse response = client.search(searchRequest);
			Aggregation responseMap = response.getAggregations().getAsMap().get(aggregateName);
			Object result = responseMap.getMetaData().get("bucket");

			if(result instanceof Collection) {
				for(Object res : (Collection)result) {
					if(res instanceof Map) {
						resultList.add((String)((Map) res).get("key"));
					}
				}
			}
		} catch (IOException ex) {
			log.error(ex.getLocalizedMessage(), ex);
			return Collections.emptyList();
		}
		return resultList;
	}

	@Override
	public IObsStorage getDataStore(String producerID) {
		// return this because ES does not encapsulate any storage
		return this;
	}

	@Override
	public IObsStorage addDataStore(String producerID) {
		// return this because ES does not encapsulate any storage
		return this;
	}

	
}
