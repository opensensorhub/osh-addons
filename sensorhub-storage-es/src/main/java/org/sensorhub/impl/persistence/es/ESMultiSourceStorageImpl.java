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

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.IMultiSourceStorage;
import org.sensorhub.api.persistence.IObsStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opengis.swe.v20.DataBlock;

/**
 * <p>
 * ES implementation of {@link IObsStorage} for storing observations.
 * </p>
 *
 * @author Mathieu Dhainaut <mathieu.dhainaut@gmail.com>
 * @since 2017
 */
public class ESMultiSourceStorageImpl extends ESObsStorageImpl implements IMultiSourceStorage<IObsStorage> {

	/**
	 * Class logger
	 */
	private static final Logger log = LoggerFactory.getLogger(ESMultiSourceStorageImpl.class);  
	
	@Override
	public synchronized Iterator<? extends IDataRecord> getRecordIterator(IDataFilter filter) {
		double[] timeRange = getTimeRange(filter);
		
		// prepare filter
		QueryBuilder timeStampRangeQuery = QueryBuilders.rangeQuery(TIMESTAMP_FIELD_NAME).from(timeRange[0]).to(timeRange[1]);
		QueryBuilder recordTypeQuery = QueryBuilders.matchQuery(RECORD_TYPE_FIELD_NAME, filter.getRecordType());
		
		// check if any producerIDs
		QueryBuilder producerID = null;
		if(filter.getProducerIDs() != null && !filter.getProducerIDs().isEmpty()) {
			producerID = QueryBuilders.matchQuery(PRODUCER_ID_FIELD_NAME, filter.getProducerIDs());
		}
		
		// aggregate queries
		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
				.must(timeStampRangeQuery)
				.must(recordTypeQuery);
		
		// if any producerIDs
		if(producerID != null) {
			queryBuilder.must(producerID);
		}
		
		// build response
		final SearchRequestBuilder scrollReq = client.prepareSearch(getLocalID()).setTypes(RS_DATA_IDX_NAME)
				.addSort(TIMESTAMP_FIELD_NAME, SortOrder.ASC)
		        .setScroll(new TimeValue(config.pingTimeout))
		        .setQuery(recordTypeQuery)
		        .setPostFilter(queryBuilder);
		
        // wrap the request into custom ES Scroll iterator
		final Iterator<SearchHit> searchHitsIterator = new ESIterator(client, scrollReq,
				config.scrollFetchSize); //max of scrollFetchSize hits will be returned for each scroll
			
		// build a datablock iterator based on the searchHits iterator
		
		return new Iterator<IDataRecord>(){

			@Override
			public boolean hasNext() {
				return searchHitsIterator.hasNext();
			}

			@Override
			public IDataRecord next() {
				SearchHit nextSearchHit = searchHitsIterator.next();
				
				// build key
				final DataKey key = getDataKey(nextSearchHit.getId());
				// get DataBlock from blob
				final DataBlock datablock=ESMultiSourceStorageImpl.this.<DataBlock>getObject(nextSearchHit.getSource().get(BLOB_FIELD_NAME)); // DataBlock
				
				return new IDataRecord(){

					@Override
					public DataKey getKey() {
						return key;
					}

					@Override
					public DataBlock getData() {
						return datablock;
					}
					
				};
			}
		};
	}
	
	@Override
	public synchronized int getNumMatchingRecords(IDataFilter filter, long maxCount) {
		double[] timeRange = getTimeRange(filter);
		
		// prepare filter
		QueryBuilder timeStampRangeQuery = QueryBuilders.rangeQuery(TIMESTAMP_FIELD_NAME).from(timeRange[0]).to(timeRange[1]);
		QueryBuilder recordTypeQuery = QueryBuilders.matchQuery(RECORD_TYPE_FIELD_NAME, filter.getRecordType());
		
		// check if any producerIDs
		QueryBuilder producerID = null;
		if(filter.getProducerIDs() != null && !filter.getProducerIDs().isEmpty()) {
			producerID = QueryBuilders.matchQuery(PRODUCER_ID_FIELD_NAME, filter.getProducerIDs());
		}
		
		// aggregate queries
		BoolQueryBuilder filterQueryBuilder = QueryBuilders.boolQuery()
				.must(timeStampRangeQuery);
		
		// if any producerIDs
		if(producerID != null) {
			filterQueryBuilder.must(producerID);
		}
		
		// build response
		final SearchResponse scrollResp = client.prepareSearch(getLocalID()).setTypes(RS_DATA_IDX_NAME)
		        .setScroll(new TimeValue(config.pingTimeout))
		        .setQuery(recordTypeQuery)
		        .setPostFilter(filterQueryBuilder)
		        .setFetchSource(new String[]{}, new String[]{"*"}) // does not fetch source
		        .setSize(config.scrollFetchSize).get(); //max of scrollFetchSize hits will be returned for each scroll
		return (int) scrollResp.getHits().getTotalHits();
	}
	
	@Override
	public synchronized int removeRecords(IDataFilter filter) {
		double[] timeRange = getTimeRange(filter);
		
		// MultiSearch API does not support scroll?!
		// prepare filter
		QueryBuilder timeStampRangeQuery = QueryBuilders.rangeQuery(TIMESTAMP_FIELD_NAME).from(timeRange[0]).to(timeRange[1]);
		QueryBuilder recordTypeQuery = QueryBuilders.matchQuery(RECORD_TYPE_FIELD_NAME, filter.getRecordType());
		
		// check if any producerIDs
		QueryBuilder producerID = null;
		if(filter.getProducerIDs() != null && !filter.getProducerIDs().isEmpty()) {
			producerID = QueryBuilders.matchQuery(PRODUCER_ID_FIELD_NAME, filter.getProducerIDs());
		}
		
		// aggregate queries
		BoolQueryBuilder filterQueryBuilder = QueryBuilders.boolQuery()
				.must(timeStampRangeQuery);
		
		// if any producerIDs
		if(producerID != null) {
			filterQueryBuilder.must(producerID);
		}
		
		// build response
		SearchResponse scrollResp = client.prepareSearch(getLocalID()).setTypes(RS_DATA_IDX_NAME)
		        .setScroll(new TimeValue(config.pingTimeout))
		        .setQuery(recordTypeQuery)
		        .setFetchSource(new String[]{}, new String[]{"*"}) // does not fetch source
		        .setPostFilter(filterQueryBuilder)
		        .setSize(config.scrollFetchSize).get(); //max of scrollFetchSize hits will be returned for each scroll
				
		//Scroll until no hit are returned
		int nb = 0;
		do {
		    for (SearchHit hit : scrollResp.getHits().getHits()) {
		    	// prepare delete request
				DeleteRequest deleteRequest = new DeleteRequest(getLocalID(),RS_DATA_IDX_NAME, hit.getId());
				// execute delete
				try {
					client.delete(deleteRequest).get().getId();
				} catch (InterruptedException | ExecutionException e) {
					log.error("[ES] Cannot delete the object with the key: "+hit.getId());
				}
		    	nb++;
		    }

		    scrollResp = client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(config.scrollMaxDuration)).execute().actionGet();
		} while(scrollResp.getHits().getHits().length != 0); // Zero hits mark the end of the scroll and the while loop.
		
		log.info("[ES] Delete "+nb+" records from "+new Date((long)timeRange[0]*1000)+" to "+new Date((long)timeRange[1]*1000));
		return 0;
	}
	
	@Override
	public synchronized Iterator<DataBlock> getDataBlockIterator(IDataFilter filter) {
		double[] timeRange = getTimeRange(filter);
		
		// prepare filter
		QueryBuilder timeStampRangeQuery = QueryBuilders.rangeQuery(TIMESTAMP_FIELD_NAME).from(timeRange[0]).to(timeRange[0]);
		QueryBuilder recordTypeQuery = QueryBuilders.matchQuery(RECORD_TYPE_FIELD_NAME, filter.getRecordType());
		
		// check if any producerIDs
		QueryBuilder producerID = null;
		if(filter.getProducerIDs() != null && !filter.getProducerIDs().isEmpty()) {
			producerID = QueryBuilders.matchQuery(PRODUCER_ID_FIELD_NAME, filter.getProducerIDs());
		}
		
		// aggregate queries
		BoolQueryBuilder filterQueryBuilder = QueryBuilders.boolQuery()
				.must(timeStampRangeQuery);
		
		// if any producerIDs
		if(producerID != null) {
			filterQueryBuilder.must(producerID);
		}
		
		// build response
		final SearchRequestBuilder scrollReq = client.prepareSearch(getLocalID()).setTypes(RS_DATA_IDX_NAME)
				//TOCHECK
				.addSort(TIMESTAMP_FIELD_NAME, SortOrder.ASC)
				.setFetchSource(new String[]{BLOB_FIELD_NAME}, new String[]{}) // get only the BLOB
		        .setScroll(new TimeValue(config.pingTimeout))
		        .setQuery(recordTypeQuery)
		        .setPostFilter(filterQueryBuilder);
		
		// wrap the request into custom ES Scroll iterator
		final Iterator<SearchHit> searchHitsIterator = new ESIterator(client, scrollReq,
				config.scrollFetchSize); //max of scrollFetchSize hits will be returned for each scroll
		
		// build a datablock iterator based on the searchHits iterator
		return new Iterator<DataBlock>(){

			@Override
			public boolean hasNext() {
				return searchHitsIterator.hasNext();
			}

			@Override
			public DataBlock next() {
				SearchHit nextSearchHit = searchHitsIterator.next();
				// get DataBlock from blob
				Object blob = nextSearchHit.getSource().get(BLOB_FIELD_NAME);
				return ESMultiSourceStorageImpl.this.<DataBlock>getObject(blob); // DataBlock
			}
		};
	}
	
	@Override
	public Collection<String> getProducerIDs() {
		return null;
	}

	@Override
	public IObsStorage getDataStore(String producerID) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IObsStorage addDataStore(String producerID) {
		// TODO Auto-generated method stub
		return null;
	}

	
}
