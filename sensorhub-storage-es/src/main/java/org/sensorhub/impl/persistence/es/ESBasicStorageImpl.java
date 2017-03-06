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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.codec.binary.Base64;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsRequest;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.support.AbstractClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
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
import org.sensorhub.api.persistence.IObsStorage;
import org.sensorhub.api.persistence.IRecordStorageModule;
import org.sensorhub.api.persistence.IRecordStoreInfo;
import org.sensorhub.api.persistence.IStorageModule;
import org.sensorhub.api.persistence.StorageException;
import org.sensorhub.impl.module.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * This class is Thread-safe.
 * </p>
 *
 * @author Mathieu Dhainaut <mathieu.dhainaut@gmail.com>
 * @since 2017
 */
public class ESBasicStorageImpl extends AbstractModule<ESBasicStorageConfig> implements IRecordStorageModule<ESBasicStorageConfig> {
	private static final int TIME_RANGE_CLUSTER_SCROLL_FETCH_SIZE = 5000;

	protected static final double MAX_TIME_CLUSTER_DELTA = 60.0;

	protected static final String RECORD_TYPE_FIELD_NAME = "recordType";

	protected static final String PRODUCER_ID_FIELD_NAME = "producerID";

	protected static final String RS_KEY_SEPARATOR = "##";

	protected static final String BLOB_FIELD_NAME = "blob";

	protected static final String TIMESTAMP_FIELD_NAME = "timestamp";

	protected static final double[] ALL_TIMES = new double[] {Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY};
	/**
	 * Class logger
	 */
	private static final Logger log = LoggerFactory.getLogger(ESBasicStorageImpl.class);  
	
	/**
	 * The TransportClient connects remotely to an Elasticsearch cluster using the transport module. 
	 * It does not join the cluster, but simply gets one or more initial transport addresses and communicates 
	 * with them in round robin fashion on each action (though most actions will probably be "two hop" operations).
	 */
	protected AbstractClient client;
	
	/**
	 * The data index. The data are indexed by their timestamps
	 * UUID_SOURCE/RECORD_STORE_ID/{ timestamp: <timestamp>, data: <anyData> }
	 */
	protected final static String DESC_HISTORY_IDX_NAME = "desc";
	protected final static String RS_INFO_IDX_NAME = "info";
	protected final static String RS_DATA_IDX_NAME = "data";
	
	/**
	 * Use the bulkProcessor to increase perfs when writting and deleting a set of data.
	 */
	protected BulkProcessor bulkProcessor;
	
	public ESBasicStorageImpl() {
		
	}
	
	public ESBasicStorageImpl(AbstractClient client) {
		this.client = client;
	}
	
	@Override
	public void backup(OutputStream os) throws IOException {
		//throw new UnsupportedOperationException("Does not support ES data storage backup");
	}

	@Override
	public void restore(InputStream is) throws IOException {
		//throw new UnsupportedOperationException("Does not support ES data storage restore");
	}

	@Override
	public void commit() {
		bulkProcessor.flush();
		refreshIndex();
	}

	@Override
	public void rollback() {
		// ES does not support native transaction
		//throw new UnsupportedOperationException("Does not support ES data storage rollback");
	}

	@Override
	public void sync(IStorageModule<?> storage) throws StorageException {
		// TODO Auto-generated method stub
		//throw new UnsupportedOperationException("Does not support ES data storage synchronization");

	}

	@Override
	public synchronized void start() throws SensorHubException {
		if(client == null) {
			// init transport client
			Settings settings = Settings.builder()
			        .put("cluster.name", config.storagePath)
			        .put("client.transport.ignore_cluster_name",config.ignoreClusterName)
			        //.put("client.transport.ping_timeout",config.pingTimeout)
			        //.put("client.transport.nodes_sampler_interval",config.nodeSamplerInterval)
			        .put("client.transport.sniff",config.transportSniff)
			        .build();
			
			// add transport address(es)
			TransportAddress [] transportAddresses  = new TransportAddress[config.nodeUrls.size()];
			int i=0;
			for(String nodeUrl : config.nodeUrls){
				try {
					URL url = null;
					// <host>:<port>
					if(nodeUrl.startsWith("http://")){
						url = new URL(nodeUrl);
					} else {
						url = new URL("http://"+nodeUrl);
					}
					
					transportAddresses[i++]=new InetSocketTransportAddress(
							InetAddress.getByName(url.getHost()), // host
							url.getPort()); //port
					
				} catch (MalformedURLException e) {
					log.error("Cannot initialize transport address:"+e.getMessage());
					throw new SensorHubException("Cannot initialize transport address",e);
				} catch (UnknownHostException e) {
					log.error("Cannot initialize transport address:"+e.getMessage());
					throw new SensorHubException("Cannot initialize transport address",e);
				}
			}
				// build the client
				client = new PreBuiltTransportClient(settings)
				        .addTransportAddresses(transportAddresses);
		}	
		
		try{
			// build the bulk processor
			bulkProcessor = BulkProcessor.builder(
			        client,  
			        new BulkProcessor.Listener() {
			            @Override
			            public void beforeBulk(long executionId,
			                                   BulkRequest request) {  } 
	
			            @Override
			            public void afterBulk(long executionId,
			                                  BulkRequest request,
			                                  BulkResponse response) {  } 
	
			            @Override
			            public void afterBulk(long executionId,
			                                  BulkRequest request,
			                                  Throwable failure) {  } 
			        })
			        .setBulkActions(config.bulkActions) 
			        .setBulkSize(new ByteSizeValue(config.bulkSize, ByteSizeUnit.MB)) 
			        .setFlushInterval(TimeValue.timeValueSeconds(config.bulkFlushInterval)) 
			        .setConcurrentRequests(config.bulkConcurrentRequests) 
			        .setBackoffPolicy(
			            BackoffPolicy.exponentialBackoff(TimeValue.timeValueMillis(100), 3)) 
			        .build();
			// create indices if they dont exist
			boolean exists = client.admin().indices()
				    .prepareExists(getLocalID())
				    .execute().actionGet().isExists();
			if(!exists) {
				createIndices();
			}
		}catch(Throwable ex) {
			log.error("Cannot initialize the client",ex);
		}
	}

	protected void createIndices (){
		// create the index 
		CreateIndexRequest indexRequest = new CreateIndexRequest(getLocalID());
		client.admin().indices().create(indexRequest).actionGet();
		
		// create the corresponding data mapping
		try {
			client.admin().indices() 
			    .preparePutMapping(getLocalID()) 
			    .setType(RS_DATA_IDX_NAME)
			    .setSource(getRsDataMapping())
			    .execute().actionGet();
		} catch (IOException e) {
			log.error("Cannot create indices",e);
		}
	}
	
	@Override
	public synchronized void stop() throws SensorHubException {
		if(client != null) {
			client.close();
		}
		// flush and close the bulk processor
		if(bulkProcessor != null) {
			try {
				bulkProcessor.flush();
				bulkProcessor.awaitClose(10, TimeUnit.MINUTES);
			} catch (InterruptedException e) {
				log.error("Cannot close/flush the bulk processor",e);
				throw new SensorHubException(e.getMessage());
			}
		}
	}

	@Override
	public AbstractProcess getLatestDataSourceDescription() {
		if(!isTypeExist(getLocalID(),DESC_HISTORY_IDX_NAME)) {
			return null;
		}
		// build search response given a timestamp desc sort
		SearchResponse response = client.prepareSearch(getLocalID()).setTypes(DESC_HISTORY_IDX_NAME)
							.addSort(TIMESTAMP_FIELD_NAME, SortOrder.DESC)
							.get();
		
		AbstractProcess result = null;
		// the response should contain the whole list of source
		// sorted desc by their timestamp
		if(response.getHits().getTotalHits() > 0){
			// get the first one of the list means the most recent
			Object blob = response.getHits().getAt(0).getSource().get(BLOB_FIELD_NAME);
			result = this.<AbstractProcess>getObject(blob);
		}
		return result;
	}

	@Override
	public List<AbstractProcess> getDataSourceDescriptionHistory(double startTime, double endTime) {
		List<AbstractProcess> results = new ArrayList<AbstractProcess>();
		
		// query ES to get the corresponding timestamp
		// the response is applied a post filter allowing to specify a range request on the timestamp
		// the hits should be directly filtered
		SearchRequestBuilder request = client.prepareSearch(getLocalID()).setTypes(DESC_HISTORY_IDX_NAME)
				.setScroll(new TimeValue(config.scrollMaxDuration))
				.addSort(TIMESTAMP_FIELD_NAME,SortOrder.ASC)
				.setPostFilter(QueryBuilders.rangeQuery(TIMESTAMP_FIELD_NAME)
						.from(startTime).to(endTime))     // Query
		        ;
		
		Iterator<SearchHit> iterator = new ESIterator(client, request,config.scrollFetchSize);
		// the corresponding filtering hits
		while(iterator.hasNext()) {
			SearchHit hit = iterator.next();
			// deSerialize the AbstractProcess stored object 
			Object blob = hit.getSource().get(BLOB_FIELD_NAME);
			results.add(this.<AbstractProcess>getObject(blob));
		}
		return results;
	}

	@Override
	public AbstractProcess getDataSourceDescriptionAtTime(double time) {
		// if the type does not exist, return
		if(!isTypeExist(getLocalID(),DESC_HISTORY_IDX_NAME)) {
			return null;
		}
		
		AbstractProcess result = null;
		
		// build the request
		GetRequest getRequest = new GetRequest( getLocalID(),DESC_HISTORY_IDX_NAME,time+"");
		
		// build  and execute the response
		GetResponse response = client.get(getRequest).actionGet();
		
		// if any response
		if (response.isExists()) {
			// get the blob from the source response field
			Object blob = response.getSource().get(BLOB_FIELD_NAME);
			
			// deserialize the object
			result = this.getObject(blob);
		}
		return result;
	}

	protected boolean storeDataSourceDescription(AbstractProcess process, double time, boolean update) {
		// prepare source map
		Map<String, Object> json = new HashMap<String, Object>();
		json.put(TIMESTAMP_FIELD_NAME,time);
		json.put(BLOB_FIELD_NAME,this.getBlob(process));
		
		if (update) {
			// prepare update
			UpdateRequest updateRequest = new UpdateRequest(getLocalID(), DESC_HISTORY_IDX_NAME, time+"");
			updateRequest.doc(json);
			
			String id=null;
			try {
				id = client.update(updateRequest).get().getId();
			} catch (InterruptedException | ExecutionException e) {
				log.error("[ES] Cannot update: ");
			}
            return (id != null);
        } else {
        	// send request and check if the id is not null
        	 String id = client.prepareIndex(getLocalID(),DESC_HISTORY_IDX_NAME).setId(time+"")
    					.setSource(json).get().getId();
            return (id != null);
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
	public void storeDataSourceDescription(AbstractProcess process) {
		storeDataSourceDescription(process, false);
	}

	@Override
	public void updateDataSourceDescription(AbstractProcess process) {
		storeDataSourceDescription(process, true);
	}

	@Override
	public void removeDataSourceDescription(double time) {
		DeleteRequest deleteRequest = new DeleteRequest(getLocalID(), DESC_HISTORY_IDX_NAME, time+"");
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
		SearchResponse response = client.prepareSearch(getLocalID()).setTypes(DESC_HISTORY_IDX_NAME)
				.setPostFilter(QueryBuilders.rangeQuery(TIMESTAMP_FIELD_NAME)
					.from(startTime).to(endTime)) // Query
				.setFetchSource(new String[]{}, new String[]{"*"}) // does not fetch source
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
	public  Map<String, ? extends IRecordStoreInfo> getRecordStores() {
		Map<String, IRecordStoreInfo> result = new HashMap<>();
		SearchResponse response = client.prepareSearch(getLocalID()).setTypes(RS_INFO_IDX_NAME).get();
		
		String name = null;
		DataStreamInfo rsInfo = null;
		for(SearchHit hit : response.getHits()) {
			name = hit.getId(); // name
			rsInfo = this.<DataStreamInfo>getObject(hit.getSource().get(BLOB_FIELD_NAME)); // DataStreamInfo
			result.put(name,rsInfo);
		}
		return result;
	}

	@Override
	public void addRecordStore(String name, DataComponent recordStructure, DataEncoding recommendedEncoding) {
		DataStreamInfo rsInfo = new DataStreamInfo(name, recordStructure, recommendedEncoding);
        
		// add new record storage
		Object blob = this.getBlob(rsInfo);
		
		Map<String, Object> json = new HashMap<String, Object>();
		json.put(BLOB_FIELD_NAME,blob);
		
		// set id and blob before executing the request
		//String id = client.prepareIndex(getLocalID(),RS_INFO_IDX_NAME).setId(name).setSource(json).get().getId();
		bulkProcessor.add(client.prepareIndex(getLocalID(),RS_INFO_IDX_NAME).setId(name).setSource(json).request());
		//TODO: make the link to the recordStore storage
		// either we can use an intermediate mapping table or use directly the recordStoreInfo index
		// to fetch the corresponding description
		
		// To test: try to use the recordStoreInfo index directly
		// do nothing 
	}

	@Override
	public int getNumRecords(String recordType) {
		SearchResponse response = client.prepareSearch(getLocalID()).setTypes(RS_DATA_IDX_NAME)
				.setPostFilter(QueryBuilders.matchQuery(RECORD_TYPE_FIELD_NAME, recordType))
				.setFetchSource(new String[]{}, new String[]{"*"}) // does not fetch source
		        .get();
		return (int) response.getHits().getTotalHits();
	}

	@Override  
	public synchronized double[] getRecordsTimeRange(String recordType) {
		double[] result = new double[2];
		
		// build request to get the least recent record
		SearchResponse response = client.prepareSearch(getLocalID()).setTypes(RS_DATA_IDX_NAME)
				.setQuery(QueryBuilders.matchQuery(RECORD_TYPE_FIELD_NAME, recordType))
				.addSort(TIMESTAMP_FIELD_NAME, SortOrder.ASC) // sort results by DESC timestamp
				.setFetchSource(new String[]{TIMESTAMP_FIELD_NAME}, new String[]{}) // get only the timestamp
				.setSize(1) // fetch only 1 result
		        .get();
		
		if(response.getHits().getTotalHits()> 0) {
			result[0] = (double) response.getHits().getAt(0).getSource().get(TIMESTAMP_FIELD_NAME);
		}
		
		// build request to get the most recent record
		 response = client.prepareSearch(getLocalID()).setTypes(RS_DATA_IDX_NAME)
				.setQuery(QueryBuilders.matchQuery(RECORD_TYPE_FIELD_NAME, recordType))
				.addSort(TIMESTAMP_FIELD_NAME, SortOrder.DESC) // sort results by DESC timestamp
				.setFetchSource(new String[]{TIMESTAMP_FIELD_NAME}, new String[]{}) // get only the timestamp
				//.setSize(1) // fetch only 1 result
		        .get();
		
		if(response.getHits().getTotalHits()> 0) {
			result[1] = (double) response.getHits().getAt(0).getSource().get(TIMESTAMP_FIELD_NAME);
		}
		
		return result;
	}

	@Override
	public Iterator<double[]> getRecordsTimeClusters(String recordType) {
		// build response
		final SearchRequestBuilder scrollReq = client.prepareSearch(getLocalID()).setTypes(RS_DATA_IDX_NAME)
				//TOCHECK
				//.addSort(SortOrder.ASC)
				.addSort(TIMESTAMP_FIELD_NAME, SortOrder.ASC)
		        .setScroll(new TimeValue(config.pingTimeout))
		        .setRequestCache(true)
		        .setQuery(QueryBuilders.matchQuery(RECORD_TYPE_FIELD_NAME, recordType))
		        .setFetchSource(new String[]{TIMESTAMP_FIELD_NAME}, new String[]{}); // get only the timestamp
		    	
        // wrap the request into custom ES Scroll iterator
		final Iterator<SearchHit> searchHitsIterator = new ESIterator(client, scrollReq,
				TIME_RANGE_CLUSTER_SCROLL_FETCH_SIZE); //max of scrollFetchSize hits will be returned for each scroll
				
		return new Iterator<double[]>() {
			Double lastTime = Double.NaN;
			
			@Override
			public boolean hasNext() {
				return searchHitsIterator.hasNext();
			}

			@Override
			public double[] next() {
				double[] clusterTimeRange = new double[2];
                clusterTimeRange[0] = lastTime;
                
                SearchHit nextSearchHit = null;
                long storedTimestamp = 0;
                double recTime = 0.0;
                double dt = 0.0;
                
				while (searchHitsIterator.hasNext()) {
					nextSearchHit = searchHitsIterator.next();
					recTime = (double) nextSearchHit.getSource().get(TIMESTAMP_FIELD_NAME);

					synchronized (lastTime) {
						if (Double.isNaN(lastTime)) {
							clusterTimeRange[0] = recTime;
							lastTime = recTime;
						} else {
							 dt = recTime - lastTime;
							lastTime = recTime;
							if (dt > MAX_TIME_CLUSTER_DELTA)
								break;
						}
					}
					clusterTimeRange[1] = recTime;
				}
				return clusterTimeRange;
			}
		};
	}

	@Override
	public DataBlock getDataBlock(DataKey key) {
		DataBlock result = null;
		// build the key as recordTYpe_timestamp_producerID
		String esKey = getRsKey(key);
		
		// build the request
		GetRequest getRequest = new GetRequest( getLocalID(),RS_DATA_IDX_NAME,esKey);
		
		// build  and execute the response
		GetResponse response = client.get(getRequest).actionGet();
		
		// deserialize the blob field from the response if any
		if(response.isExists()) {
			result = this.<DataBlock>getObject(response.getSource().get(BLOB_FIELD_NAME)); // DataBlock
		}
		return result;
	}

	@Override
	public Iterator<DataBlock> getDataBlockIterator(IDataFilter filter) {
		double[] timeRange = getTimeRange(filter);
		
		// prepare filter
		QueryBuilder timeStampRangeQuery = QueryBuilders.rangeQuery(TIMESTAMP_FIELD_NAME).from(timeRange[0]).to(timeRange[1]);
		QueryBuilder recordTypeQuery = QueryBuilders.matchQuery(RECORD_TYPE_FIELD_NAME, filter.getRecordType());
		
		// aggregate queries
		BoolQueryBuilder filterQueryBuilder = QueryBuilders.boolQuery()
				.must(timeStampRangeQuery);
		
		// check if any producerIDs
		if(filter.getProducerIDs() != null && !filter.getProducerIDs().isEmpty()) {
			filterQueryBuilder.must(QueryBuilders.matchQuery(PRODUCER_ID_FIELD_NAME, filter.getProducerIDs()));
		}
				
		// build response
		final SearchRequestBuilder scrollReq = client.prepareSearch(getLocalID()).setTypes(RS_DATA_IDX_NAME)
				//TOCHECK
				.addSort(TIMESTAMP_FIELD_NAME, SortOrder.ASC)
				.setFetchSource(new String[]{BLOB_FIELD_NAME}, new String[]{}) // get only the BLOB
		        .setScroll(new TimeValue(config.pingTimeout))
		        .setQuery(recordTypeQuery)
		        .setRequestCache(true)
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
				return ESBasicStorageImpl.this.<DataBlock>getObject(blob); // DataBlock
			}
		};
	}
	
	@Override
	public Iterator<? extends IDataRecord> getRecordIterator(IDataFilter filter) {
		double[] timeRange = getTimeRange(filter);
		
		// prepare filter
		QueryBuilder timeStampRangeQuery = QueryBuilders.rangeQuery(TIMESTAMP_FIELD_NAME).from(timeRange[0]).to(timeRange[1]);
		QueryBuilder recordTypeQuery = QueryBuilders.matchQuery(RECORD_TYPE_FIELD_NAME, filter.getRecordType());
		
		// aggregate queries
		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
				.must(timeStampRangeQuery)
				.must(recordTypeQuery);
		
		// check if any producerIDs
		if(filter.getProducerIDs() != null && !filter.getProducerIDs().isEmpty()) {
			queryBuilder.must(QueryBuilders.matchQuery(PRODUCER_ID_FIELD_NAME, filter.getProducerIDs()));
		}
		
		// build response
		final SearchRequestBuilder scrollReq = client.prepareSearch(getLocalID()).setTypes(RS_DATA_IDX_NAME)
				.addSort(TIMESTAMP_FIELD_NAME, SortOrder.ASC)
		        .setScroll(new TimeValue(config.pingTimeout))
		        .setQuery(recordTypeQuery)
		        //.setRequestCache(true)
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
				key.timeStamp = (double) nextSearchHit.getSource().get(TIMESTAMP_FIELD_NAME);
				// get DataBlock from blob
				final DataBlock datablock=ESBasicStorageImpl.this.<DataBlock>getObject(nextSearchHit.getSource().get(BLOB_FIELD_NAME)); // DataBlock
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
		double[] timeRange = getTimeRange(filter);

		// aggregate queries
		BoolQueryBuilder filterQueryBuilder = QueryBuilders.boolQuery();
				
		// prepare filter
		QueryBuilder timeStampRangeQuery = QueryBuilders.rangeQuery(TIMESTAMP_FIELD_NAME).from(timeRange[0]).to(timeRange[1]);
		QueryBuilder recordTypeQuery = QueryBuilders.matchQuery(RECORD_TYPE_FIELD_NAME, filter.getRecordType());

		filterQueryBuilder.must(timeStampRangeQuery);
		
		// check if any producerIDs
		if(filter.getProducerIDs() != null && !filter.getProducerIDs().isEmpty()) {
			filterQueryBuilder.must(QueryBuilders.matchQuery(PRODUCER_ID_FIELD_NAME, filter.getProducerIDs()));
		}
		
		// build response
		final SearchResponse scrollResp = client.prepareSearch(getLocalID()).setTypes(RS_DATA_IDX_NAME)
		        .setScroll(new TimeValue(config.pingTimeout))
		        .setQuery(recordTypeQuery)
		        .setRequestCache(true)
		        .setPostFilter(filterQueryBuilder)
		        .setFetchSource(new String[]{}, new String[]{"*"}) // does not fetch source
		        .setSize(config.scrollFetchSize).get(); //max of scrollFetchSize hits will be returned for each scroll
		return (int) scrollResp.getHits().getTotalHits();
	}
	@Override
	public void storeRecord(DataKey key, DataBlock data) {
		// build the key as recordTYpe_timestamp_producerID
		String esKey = getRsKey(key);
		
		// get blob from dataBlock object using serializer
		Object blob = this.getBlob(data);
		
		Map<String, Object> json = new HashMap<String, Object>();
		json.put(TIMESTAMP_FIELD_NAME,key.timeStamp); // store timestamp
		json.put(PRODUCER_ID_FIELD_NAME,key.producerID); // store producerID
		json.put(RECORD_TYPE_FIELD_NAME,key.recordType); // store recordType
		json.put(BLOB_FIELD_NAME,blob); // store DataBlock
		
		// set id and blob before executing the request
		/*String id = client.prepareIndex(getLocalID(),RS_DATA_IDX_NAME)
				.setId(esKey)
				.setSource(json)
				.get()
				.getId();*/
		bulkProcessor.add(client.prepareIndex(getLocalID(),RS_DATA_IDX_NAME)
				.setId(esKey)
				.setSource(json).request());
	}

	@Override
	public void updateRecord(DataKey key, DataBlock data) {
		// build the key as recordTYpe_timestamp_producerID
		String esKey = getRsKey(key);
		
		// get blob from dataBlock object using serializer
		Object blob = this.getBlob(data);
		
		Map<String, Object> json = new HashMap<String, Object>();
		json.put(TIMESTAMP_FIELD_NAME,key.timeStamp); // store timestamp
		json.put(PRODUCER_ID_FIELD_NAME,key.producerID); // store producerID
		json.put(RECORD_TYPE_FIELD_NAME,key.recordType); // store recordType
		json.put(BLOB_FIELD_NAME,blob); // store DataBlock
		
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
		double[] timeRange = getTimeRange(filter);
		
		// MultiSearch API does not support scroll?!
		// prepare filter
		QueryBuilder timeStampRangeQuery = QueryBuilders.rangeQuery(TIMESTAMP_FIELD_NAME).from(timeRange[0]).to(timeRange[1]);
		QueryBuilder recordTypeQuery = QueryBuilders.matchQuery(RECORD_TYPE_FIELD_NAME, filter.getRecordType());
		
		// aggregate queries
		BoolQueryBuilder filterQueryBuilder = QueryBuilders.boolQuery()
				.must(timeStampRangeQuery);
		
		// check if any producerIDs
		if(filter.getProducerIDs() != null && !filter.getProducerIDs().isEmpty()) {
			filterQueryBuilder.must(QueryBuilders.matchQuery(PRODUCER_ID_FIELD_NAME, filter.getProducerIDs()));
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
	
	/**
	 * Get a serialized object from an object. 
	 * The object is serialized using Kryo.
	 * @param object The raw object
	 * @return the serialized object
	 */
	protected <T> byte[] getBlob(T object){
		return KryoSerializer.serialize(object);
	}
	
	/**
	 * Get an object from a base64 encoding String.
	 * The object is deserialized using Kryo.
	 * @param blob The base64 encoding String
	 * @return The deserialized object
	 */
	protected <T> T getObject(Object blob) {
		// Base 64 decoding
		byte [] base64decodedData = Base64.decodeBase64(blob.toString().getBytes());
		// Kryo deserialize
		return KryoSerializer.<T>deserialize(base64decodedData);
	}
	
	/**
	 * Transform a DataKey into an ES key as: <recordtype><SEPARATOR><timestamp><SEPARATOR><producerID>.
	 * @param key the ES key.
	 * @return the ES key. 
	 */
	protected String getRsKey(DataKey key) {
		return key.recordType+RS_KEY_SEPARATOR+key.timeStamp+RS_KEY_SEPARATOR+key.producerID;
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
    		dataKey = new DataKey(split[0], split[2], Double.parseDouble(split[1]));
    	}
		return dataKey;
	}
	
	/**
	 * Check if a type exist into the ES index.
	 * @param indexName 
	 * @param typeName
	 * @return true if the type exists, false otherwise
	 */
	protected boolean isTypeExist(String indexName, String typeName) {
		TypesExistsRequest typeExistRequest = new TypesExistsRequest(new String[]{indexName},typeName);
		return client.admin().indices().typesExists(typeExistRequest).actionGet().isExists();
	}
	
	protected double[] getTimeRange(IDataFilter filter) {
		double[] timeRange = filter.getTimeStampRange();
		if (timeRange != null)
			return timeRange;
		else
			return ALL_TIMES;
	}
	
	/**
	 * Refreshes the index.
	 */
	protected void refreshIndex() {
		client.admin().indices().prepareRefresh(getLocalID()).get();
	}
	
	/**
	 * Build and return the data mapping.
	 * @return The object used to map the type
	 * @throws IOException
	 */
	protected synchronized XContentBuilder getRsDataMapping() throws IOException {
		XContentBuilder builder = XContentFactory.jsonBuilder()
				.startObject()
					.startObject(RS_DATA_IDX_NAME)
						.startObject("properties")
							// map the timestamp as double and analyze
							.startObject(TIMESTAMP_FIELD_NAME)
								.field("type", "double")
								.field("index", "analyzed")
							.endObject()
							// map the type as string (to exact match) and does not analyze
							.startObject(RECORD_TYPE_FIELD_NAME)
								.field("type", "string")
								.field("index", "not_analyzed")
							.endObject()
							// map the producer id as string (to exact match) and does not analyze
							.startObject(PRODUCER_ID_FIELD_NAME)
								.field("type", "string")
								.field("index", "not_analyzed")
							.endObject()
							// map the blob as binary data
							.startObject(BLOB_FIELD_NAME)
								.field("type", "binary")
							.endObject()
						.endObject()
					.endObject()
				.endObject();
		return builder;
	}
}