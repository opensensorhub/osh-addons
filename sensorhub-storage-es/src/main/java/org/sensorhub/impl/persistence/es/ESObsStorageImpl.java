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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.IFoiFilter;
import org.sensorhub.api.persistence.IObsStorage;
import org.sensorhub.api.persistence.IObsStorageModule;
import org.sensorhub.api.persistence.IRecordStoreInfo;
import org.sensorhub.api.persistence.IStorageModule;
import org.sensorhub.api.persistence.StorageException;
import org.sensorhub.impl.module.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.util.Bbox;

import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.AbstractTimeGeometricPrimitive;
import net.opengis.gml.v32.TimeInstant;
import net.opengis.gml.v32.TimePeriod;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;

/**
 * <p>
 * ES implementation of {@link IObsStorage} for storing observations.
 * </p>
 *
 * @author Mathieu Dhainaut <mathieu.dhainaut@gmail.com>
 * @since 2017
 */
public class ESObsStorageImpl extends AbstractModule<ESStorageConfig> implements IObsStorageModule<ESStorageConfig> {
	private static final String RECORD_TYPE_FIELD_NAME = "recordType";

	private static final String PRODUCER_ID_FIELD_NAME = "producerID";

	private static final int DOUBLE_TO_LONG_MULTIPLIER = 1000;

	private static final String RS_KEY_SEPARATOR = "$__$";

	private static final String BLOB_FIELD_NAME = "blob";

	private static final String TIMESTAMP_FIELD_NAME = "timestamp";

	/**
	 * Class logger
	 */
	private static final Logger log = LoggerFactory.getLogger(ESObsStorageImpl.class);  
	
	/**
	 * The TransportClient connects remotely to an Elasticsearch cluster using the transport module. 
	 * It does not join the cluster, but simply gets one or more initial transport addresses and communicates 
	 * with them in round robin fashion on each action (though most actions will probably be "two hop" operations).
	 */
	protected TransportClient client;
	
	// indices
	/**
	 * The process description index.
	 * UUID_SOURCE/PROCESS_DESC
	 */
	protected IndexRequestBuilder processDescIdx;
	
	/**
	 * The record store info index.
	 * UUID_SOURCE/RECORD_STORE_INFO
	 */
	protected IndexRequestBuilder recordStoreInfoIdx;
	
	/**
	 * The data index. The data are indexed by their timestamps
	 * UUID_SOURCE/RECORD_STORE_ID/{ timestamp: <timestamp>, data: <anyData> }
	 */
	protected IndexRequestBuilder recordStoreIdx;
	
	private final static String DESC_HISTORY_IDX_NAME = "desc";
	private final static String RS_INFO_IDX_NAME = "info";
	private final static String RS_DATA_IDX_NAME = "data";
	
	// search
	protected SearchRequestBuilder processDescSearch;
	protected SearchRequestBuilder recordStoreInfoSearch;
	protected SearchRequestBuilder recordStoreSearch;
	
	@Override
	public void backup(OutputStream os) throws IOException {
		throw new UnsupportedOperationException("Does not support ES data storage backup");
	}

	@Override
	public void restore(InputStream is) throws IOException {
		throw new UnsupportedOperationException("Does not support ES data storage restore");
	}

	@Override
	public void commit() {
		// ES does not support native transaction
		throw new UnsupportedOperationException("Does not support ES data storage commit");
	}

	@Override
	public void rollback() {
		// ES does not support native transaction
		throw new UnsupportedOperationException("Does not support ES data storage rollback");
	}

	@Override
	public void sync(IStorageModule<?> storage) throws StorageException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Does not support ES data storage synchronization");

	}

	@Override
	public void start() throws SensorHubException {
		// init transport client
		Settings settings = Settings.builder()
		        .put("cluster.name", config.storagePath)
		        .put("client.transport.ignore_cluster_name",config.ignoreClusterName)
		        .put("client.transport.ping_timeout",config.pingTimeout)
		        .put("client.transport.nodes_sampler_interval",config.nodeSamplerInterval)
		        .put("client.transport.sniff",config.transportSniff)
		        .build();
		
		client = new PreBuiltTransportClient(settings);
		
		// add transport address(es)
		for(String nodeUrl : config.nodeUrls){
			try {
				// <host>:<port>
				URL url = new URL(nodeUrl);
				client.addTransportAddress(new InetSocketTransportAddress(
						InetAddress.getByName(url.getHost()), // host
						url.getPort())); //port
			} catch (MalformedURLException e) {
				throw new SensorHubException("Cannot initialize transport address:"+e.getMessage());
			} catch (UnknownHostException e) {
				throw new SensorHubException("Cannot initialize transport address:"+e.getMessage());
			}
		}
		
		// prepare indices
		processDescIdx = client.prepareIndex(getLocalID(),DESC_HISTORY_IDX_NAME);
		recordStoreInfoIdx = client.prepareIndex(getLocalID(),RS_INFO_IDX_NAME);
		recordStoreIdx = client.prepareIndex(getLocalID(),RS_DATA_IDX_NAME);
		
		// prepare search requests
		processDescSearch = client.prepareSearch(getLocalID()).setTypes(DESC_HISTORY_IDX_NAME);
		recordStoreInfoSearch = client.prepareSearch(getLocalID()).setTypes(RS_INFO_IDX_NAME);
		recordStoreSearch = client.prepareSearch(getLocalID()).setTypes(RS_DATA_IDX_NAME);
	}

	@Override
	public void stop() throws SensorHubException {
		if(client != null) {
			client.close();
		}
	}

	@Override
	public AbstractProcess getLatestDataSourceDescription() {
		// build search response given a timestamp desc sort
		SearchResponse response = processDescSearch
							.addSort(TIMESTAMP_FIELD_NAME, SortOrder.DESC)
							.get();
		
		AbstractProcess result = null;
		// the response should contain the whole list of source
		// sorted desc by their timestamp
		if(response.getHits().getTotalHits() > 0){
			// get the first one of the list means the most recent
			Object blob = response.getHits().getAt(0).getSource().get(BLOB_FIELD_NAME);
			result = ESObsStorageImpl.<AbstractProcess>getObject(blob);
		}
		return result;
	}

	@Override
	public List<AbstractProcess> getDataSourceDescriptionHistory(double startTime, double endTime) {
		List<AbstractProcess> results = new ArrayList<AbstractProcess>();
		
		// query ES to get the corresponding timestamp
		// the response is applied a post filter allowing to specify a range request on the timestamp
		// the hits should be directly filtered
		SearchResponse response = processDescSearch
				.setQuery(QueryBuilders.rangeQuery(TIMESTAMP_FIELD_NAME)
						.from((long)(startTime*DOUBLE_TO_LONG_MULTIPLIER)).to((long)(endTime*DOUBLE_TO_LONG_MULTIPLIER)))     // Query
		        .get();
		
		// the corresponding filtering hits
		for(SearchHit hit : response.getHits()) {
			Object blob = hit.getSource().get(BLOB_FIELD_NAME);
			results.add(ESObsStorageImpl.<AbstractProcess>getObject(blob));
		}
		return results;
	}

	@Override
	public AbstractProcess getDataSourceDescriptionAtTime(double time) {
		AbstractProcess result = null;
		
		// build the request
		GetRequest getRequest = new GetRequest( getLocalID(),DESC_HISTORY_IDX_NAME,(long)(time*DOUBLE_TO_LONG_MULTIPLIER)+"");
		
		// build  and execute the response
		GetResponse response = client.get(getRequest).actionGet();
		
		// if any response
		if (response.isExists()) {
			// get the blob from the source response field
			Object blob = response.getSource().get(BLOB_FIELD_NAME);
			
			// deserialize the object
			result = ESObsStorageImpl.getObject(blob);
		}
		return result;
	}

	protected boolean storeDataSourceDescription(AbstractProcess process, double time, boolean update) {
		long timestampAsLong = (long) (time*DOUBLE_TO_LONG_MULTIPLIER);
		
		Map<String, Object> json = new HashMap<String, Object>();
		json.put(TIMESTAMP_FIELD_NAME,timestampAsLong);
		json.put(BLOB_FIELD_NAME,ESObsStorageImpl.getBlob(process));
		
		if (update) {
			// prepare update
			UpdateRequest updateRequest = new UpdateRequest(getLocalID(), DESC_HISTORY_IDX_NAME, timestampAsLong+"");
			updateRequest.doc(json);
			
			String id=null;
			try {
				id = client.update(updateRequest).get().getId();
			} catch (InterruptedException | ExecutionException e) {
				log.error("[ES] Cannot update: ");
			}
            return (id != null);
        } else {
        	processDescIdx.setId(time+"");
    		processDescIdx.setSource(json);
    		// send request and check if the id is not null
            return (processDescIdx.get().getId() == null);
        }
	}

	protected boolean storeDataSourceDescription(AbstractProcess process, boolean update) {
		boolean ok = false;

		if (process.getNumValidTimes() > 0) {
			// we add the description in index for each validity period/instant
			for (AbstractTimeGeometricPrimitive validTime : process.getValidTimeList()) {
				double time = Double.NaN;

				if (validTime instanceof TimeInstant)
					time = ((TimeInstant) validTime).getTimePosition().getDecimalValue();
				else if (validTime instanceof TimePeriod)
					time = ((TimePeriod) validTime).getBeginPosition().getDecimalValue();

				if (!Double.isNaN(time))
					ok = storeDataSourceDescription(process, time, update);
			}
		} else {
			double time = System.currentTimeMillis() / 1000.;
			ok = storeDataSourceDescription(process, time, update);
		}
		
		return ok;
	}

	@Override
	public synchronized void storeDataSourceDescription(AbstractProcess process) {
		storeDataSourceDescription(process, false);
	}

	@Override
	public void updateDataSourceDescription(AbstractProcess process) {
		storeDataSourceDescription(process, true);
	}

	@Override
	public void removeDataSourceDescription(double time) {
		DeleteRequest deleteRequest = new DeleteRequest(getLocalID(), DESC_HISTORY_IDX_NAME, (long)(time*DOUBLE_TO_LONG_MULTIPLIER)+"");
		try {
			client.delete(deleteRequest).get().getId();
		} catch (InterruptedException | ExecutionException e) {
			log.error("[ES] Cannot delete the object with the index: "+time);
		}
	}

	@Override
	public void removeDataSourceDescriptionHistory(double startTime, double endTime) {
		// query ES to get the corresponding timestamp
		// the response is applied a post filter allowing to specify a range request on the timestamp
		// the hits should be directly filtered
		SearchResponse response = processDescSearch
				.setQuery(QueryBuilders.rangeQuery(TIMESTAMP_FIELD_NAME)
					.from((long)(startTime*DOUBLE_TO_LONG_MULTIPLIER)).to((long)endTime*DOUBLE_TO_LONG_MULTIPLIER)) // Query
		        .get();
		
		// the corresponding filtering hits
		DeleteRequest deleteRequest = new DeleteRequest(getLocalID(), DESC_HISTORY_IDX_NAME,"");
		for(SearchHit hit : response.getHits()) {
			deleteRequest.id(hit.getId());
			try {
				client.delete(deleteRequest).get().getId();
			} catch (InterruptedException | ExecutionException e) {
				log.error("[ES] Cannot delete the object with the index: "+hit.getId());
			}
		}
	}

	@Override
	public Map<String, ? extends IRecordStoreInfo> getRecordStores() {
		Map<String, IRecordStoreInfo> result = new HashMap<>();
		SearchResponse response = recordStoreInfoSearch.get();
		
		String name = null;
		DataStreamInfo rsInfo = null;
		for(SearchHit hit : response.getHits()) {
			name = hit.getId(); // name
			rsInfo = ESObsStorageImpl.<DataStreamInfo>getObject(hit.getSource().get(BLOB_FIELD_NAME)); // DataStreamInfo
			result.put(name,rsInfo);
		}
		return result;
	}

	@Override
	public void addRecordStore(String name, DataComponent recordStructure, DataEncoding recommendedEncoding) {
		DataStreamInfo rsInfo = new DataStreamInfo(name, recordStructure, recommendedEncoding);
        
		// add new record storage
		Object blob = ESObsStorageImpl.getBlob(rsInfo);
		
		// set id and blob before executing the request
		String id = recordStoreInfoIdx.setId(name).setSource(blob).get().getId();
		
		//TODO: make the link to the recordStore storage
		// either we can use an intermediate mapping table or use directly the recordStoreInfo index
		// to fetch the corresponding description
		
		// To test: try to use the recordStoreInfo index directly
		// do nothing 
	}

	@Override
	public int getNumRecords(String recordType) {
		SearchResponse response = recordStoreSearch
				.setQuery(QueryBuilders.matchQuery("type", recordType))
		        .get();
		return (int) response.getHits().getTotalHits();
	}

	@Override
	public double[] getRecordsTimeRange(String recordType) {
		SearchResponse response = processDescSearch
				.setQuery(QueryBuilders.termQuery(RECORD_TYPE_FIELD_NAME, recordType))
				.addSort(TIMESTAMP_FIELD_NAME, SortOrder.DESC) // sort results by DESC timestamp
		        .get();
		
		double[] result = null;
		long totalHits = response.getHits().getTotalHits();
		
		// if at least 2 hits
		if(totalHits >= 2) {
			// get the first one and the last one
			//get the timestamp of the first hit
			long storedTimestamp = (long) response.getHits().getAt(0).getSource().get(TIMESTAMP_FIELD_NAME);
			
			// convert long to double
			double firstTimestamp = storedTimestamp / DOUBLE_TO_LONG_MULTIPLIER;
			
			// get the timestamp of the last hit
			storedTimestamp = (long) response.getHits().getAt((int) (totalHits-1)).getSource().get(TIMESTAMP_FIELD_NAME);
			// convert long to double
			double lastTimestamp = storedTimestamp / DOUBLE_TO_LONG_MULTIPLIER;
			
			// store the result
			result = new double[]{firstTimestamp,lastTimestamp};
		}
		return result;
	}

	@Override
	public Iterator<double[]> getRecordsTimeClusters(String recordType) {
		System.err.println("TODO: getRecordsTimeClusters");
		return null;
	}

	@Override
	public DataBlock getDataBlock(DataKey key) {
		// build the key as recordTYpe_timestamp_producerID
		String esKey = getRsKey(key);
		
		// build the request
		GetRequest getRequest = new GetRequest( getLocalID(),RS_DATA_IDX_NAME,esKey);
		
		// build  and execute the response
		GetResponse response = client.get(getRequest).actionGet();
		
		DataBlock result = null;
		
		// deserialize the blob field from the response if any
		if(response.isExists()) {
			result = ESObsStorageImpl.<DataBlock>getObject(response.getSource().get(BLOB_FIELD_NAME)); // DataBlock
		}
		return result;
	}

	@Override
	public Iterator<DataBlock> getDataBlockIterator(IDataFilter filter) {
		// build filter
		long startTimestamp = (long)(filter.getTimeStampRange()[0]*DOUBLE_TO_LONG_MULTIPLIER);
		long endTimestamp = (long)(filter.getTimeStampRange()[1]*DOUBLE_TO_LONG_MULTIPLIER);
		
		// prepare filter
		QueryBuilder timeStampRangeQuery = QueryBuilders.rangeQuery(TIMESTAMP_FIELD_NAME).from(startTimestamp).to(endTimestamp);
		QueryBuilder recordTypeQuery = QueryBuilders.termQuery(RECORD_TYPE_FIELD_NAME, filter.getRecordType());
		QueryBuilder producerID = QueryBuilders.termQuery(PRODUCER_ID_FIELD_NAME, filter.getProducerIDs());
		
		// aggregate queries
		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
				.must(timeStampRangeQuery)
				.must(recordTypeQuery)
				.must(producerID);
		
		// build response
		final SearchResponse scrollResp = recordStoreSearch
		        .setScroll(new TimeValue(config.pingTimeout))
		        .setQuery(queryBuilder)
		        .setSize(config.scrollFetchSize).get(); //max of scrollFetchSize hits will be returned for each scroll
		
		// get iterator from response
		final Iterator<SearchHit> searchHitsIterator = scrollResp.getHits().iterator();
		
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
				return ESObsStorageImpl.<DataBlock>getObject(nextSearchHit.getSource().get(BLOB_FIELD_NAME)); // DataBlock
			}
		};
	}

	@Override
	public Iterator<? extends IDataRecord> getRecordIterator(IDataFilter filter) {
		// build filter
		long startTimestamp = (long)(filter.getTimeStampRange()[0]*DOUBLE_TO_LONG_MULTIPLIER);
		long endTimestamp = (long)(filter.getTimeStampRange()[1]*DOUBLE_TO_LONG_MULTIPLIER);
		
		// prepare filter
		QueryBuilder timeStampRangeQuery = QueryBuilders.rangeQuery(TIMESTAMP_FIELD_NAME).from(startTimestamp).to(endTimestamp);
		QueryBuilder recordTypeQuery = QueryBuilders.termQuery(RECORD_TYPE_FIELD_NAME, filter.getRecordType());
		QueryBuilder producerID = QueryBuilders.termQuery(PRODUCER_ID_FIELD_NAME, filter.getProducerIDs());
		
		// aggregate queries
		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
				.must(timeStampRangeQuery)
				.must(recordTypeQuery)
				.must(producerID);
		
		// build response
		final SearchResponse scrollResp = recordStoreSearch
		        .setScroll(new TimeValue(config.pingTimeout))
		        .setQuery(queryBuilder)
		        .setSize(config.scrollFetchSize).get(); //max of scrollFetchSize hits will be returned for each scroll
		
		// get iterator from response
		final Iterator<SearchHit> searchHitsIterator = scrollResp.getHits().iterator();
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
				final DataBlock datablock=ESObsStorageImpl.<DataBlock>getObject(nextSearchHit.getSource().get(BLOB_FIELD_NAME)); // DataBlock
				
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
	public int getNumMatchingRecords(IDataFilter filter, long maxCount) {
		// build filter
		long startTimestamp = (long)(filter.getTimeStampRange()[0]*DOUBLE_TO_LONG_MULTIPLIER);
		long endTimestamp = (long)(filter.getTimeStampRange()[1]*DOUBLE_TO_LONG_MULTIPLIER);
		
		// prepare filter
		QueryBuilder timeStampRangeQuery = QueryBuilders.rangeQuery(TIMESTAMP_FIELD_NAME).from(startTimestamp).to(endTimestamp);
		QueryBuilder recordTypeQuery = QueryBuilders.termQuery(RECORD_TYPE_FIELD_NAME, filter.getRecordType());
		QueryBuilder producerID = QueryBuilders.termQuery(PRODUCER_ID_FIELD_NAME, filter.getProducerIDs());
		
		// aggregate queries
		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
				.must(timeStampRangeQuery)
				.must(recordTypeQuery)
				.must(producerID);
		
		// build response
		final SearchResponse scrollResp = recordStoreSearch
		        .setScroll(new TimeValue(config.pingTimeout))
		        .setQuery(queryBuilder)
		        .setSize(config.scrollFetchSize).get(); //max of scrollFetchSize hits will be returned for each scroll
		return (int) scrollResp.getHits().getTotalHits();
	}

	@Override
	public void storeRecord(DataKey key, DataBlock data) {
		// build the key as recordTYpe_timestamp_producerID
		String esKey = getRsKey(key);
		
		// get blob from dataBlock object using serializer
		Object blob = ESObsStorageImpl.getBlob(data);
		
		Map<String, Object> json = new HashMap<String, Object>();
		json.put(TIMESTAMP_FIELD_NAME,key.timeStamp); // store timestamp
		json.put(PRODUCER_ID_FIELD_NAME,key.producerID); // store producerID
		json.put(RECORD_TYPE_FIELD_NAME,key.recordType); // store recordType
		json.put(BLOB_FIELD_NAME,ESObsStorageImpl.getBlob(blob)); // store DataBlock
		
		// set id and blob before executing the request
		String id = recordStoreIdx.setId(esKey).setSource(json).get().getId();
	}

	@Override
	public void updateRecord(DataKey key, DataBlock data) {
		// build the key as recordTYpe_timestamp_producerID
		String esKey = getRsKey(key);
		
		// get blob from dataBlock object using serializer
		Object blob = ESObsStorageImpl.getBlob(data);
		
		//TOCHECK: do we need to store the whole key?
		Map<String, Object> json = new HashMap<String, Object>();
		json.put(TIMESTAMP_FIELD_NAME,key.timeStamp); // store timestamp
		json.put(PRODUCER_ID_FIELD_NAME,key.producerID); // store producerID
		json.put(RECORD_TYPE_FIELD_NAME,key.recordType); // store recordType
		json.put(BLOB_FIELD_NAME,ESObsStorageImpl.getBlob(blob)); // store DataBlock
		
		// prepare update
		UpdateRequest updateRequest = new UpdateRequest(getLocalID(), RS_DATA_IDX_NAME, esKey);
		updateRequest.doc(json);
		
		String id=null;
		try {
			id = client.update(updateRequest).get().getId();
		} catch (InterruptedException | ExecutionException e) {
			log.error("[ES] Cannot update the object with the key: "+esKey);
		}
	}

	@Override
	public void removeRecord(DataKey key) {
		// build the key as recordTYpe_timestamp_producerID
		String esKey = getRsKey(key);
		
		// prepare delete request
		DeleteRequest deleteRequest = new DeleteRequest(getLocalID(), RS_DATA_IDX_NAME, esKey);
		try {
			// execute delete
			client.delete(deleteRequest).get().getId();
		} catch (InterruptedException | ExecutionException e) {
			log.error("[ES] Cannot delete the object with the key: "+esKey);
		}
	}

	@Override
	public int removeRecords(IDataFilter filter) {
		// get filter
		long startTimestamp = (long)(filter.getTimeStampRange()[0]*DOUBLE_TO_LONG_MULTIPLIER);
		long endTimestamp = (long)(filter.getTimeStampRange()[1]*DOUBLE_TO_LONG_MULTIPLIER);
		
		// MultiSearch API does not support scroll?!
		// prepare filter
		QueryBuilder timeStampRangeQuery = QueryBuilders.rangeQuery(TIMESTAMP_FIELD_NAME).from(startTimestamp).to(endTimestamp);
		QueryBuilder recordTypeQuery = QueryBuilders.termQuery(RECORD_TYPE_FIELD_NAME, filter.getRecordType());
		QueryBuilder producerID = QueryBuilders.termQuery(PRODUCER_ID_FIELD_NAME, filter.getProducerIDs());
		
		// aggregate queries
		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
				.must(timeStampRangeQuery)
				.must(recordTypeQuery)
				.must(producerID);
		
		// build response
		SearchResponse scrollResp = recordStoreSearch
		        .setScroll(new TimeValue(config.pingTimeout))
		        .setQuery(queryBuilder)
		        .setSize(config.scrollFetchSize).get(); //max of scrollFetchSize hits will be returned for each scroll
		//Scroll until no hits are returned
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
		
		log.info("[ES] Delete "+nb+" records from "+new Date(startTimestamp)+" to "+new Date(endTimestamp));
		return 0;
	}

	@Override
	public int getNumFois(IFoiFilter filter) {
		System.err.println("TODO: getNumFois");
		return 0;
	}

	@Override
	public Bbox getFoisSpatialExtent() {
		System.err.println("TODO: getFoisSpatialExtent");
		return null;
	}

	@Override
	public Iterator<String> getFoiIDs(IFoiFilter filter) {
		System.err.println("TODO: getFoiIDs");
		return null;
	}

	@Override
	public Iterator<AbstractFeature> getFois(IFoiFilter filter) {
		System.err.println("TODO: getFois");
		return null;
	}

	@Override
	public void storeFoi(String producerID, AbstractFeature foi) {
		System.err.println("TODO: storeFoi");
	}
	
	//TODO: use Kryo
	private static  <T> Object getBlob(T object) {
		return null;
		
	}
	
	//TODO: use Kryo
	private static  <T> T getObject(Object blob) {
		return null;
	}
	
	/**
	 * Transform a DataKey into an ES key as: <recordtype><SEPARATOR><timestamp><SEPARATOR><producerID>.
	 * @param key the ES key.
	 * @return the ES key. 
	 */
	protected String getRsKey(DataKey key) {
		return key.recordType+RS_KEY_SEPARATOR+(long)(key.timeStamp*DOUBLE_TO_LONG_MULTIPLIER)+RS_KEY_SEPARATOR+key.producerID;
	}
	
	/**
	 * Transform the recordStorage data key into a DataKey by splitting <recordtype><SEPARATOR><timestamp><SEPARATOR><producerID>.
	 * @param dataKey the corresponding dataKey
	 * @return the dataKey. NULL if the length != 3 after splitting
	 */
	protected DataKey getDataKey(String rsKey) {
		DataKey dataKey = null;
		
		// split the rsKey using separator
		String [] split = rsKey.split(RS_KEY_SEPARATOR);
		
		// must find <recordtype><SEPARATOR><timestamp><SEPARATOR><producerID>
    	if(split.length == 3) {
    		dataKey = new DataKey(split[0], split[2], Long.parseLong(split[1])/(double)DOUBLE_TO_LONG_MULTIPLIER);
    	}
		return dataKey;
	}
}
