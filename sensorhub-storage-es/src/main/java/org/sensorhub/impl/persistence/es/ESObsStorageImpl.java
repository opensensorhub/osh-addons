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

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
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
		
		// prepare requests
		processDescSearch = client.prepareSearch(getLocalID()).setTypes(DESC_HISTORY_IDX_NAME);
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
							.setTypes("desc")
							.addSort("timestamp", SortOrder.DESC)
							.get();
		
		AbstractProcess result = null;
		// the response should contain the whole list of source
		// sorted desc by their timestamp
		if(response.getHits().getTotalHits() > 0){
			// get the first one of the list, that means the most recent
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
				.setPostFilter(QueryBuilders.rangeQuery("timestamp").from(startTime).to(endTime))                // Query
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
		// query ES to get the corresponding timestamp
		GetRequest getRequest = new GetRequest( "uuid1","desc",time+"");
		
		GetResponse response = client.get( getRequest ).actionGet();
		if (response.isExists()) {
			Object blob = response.getSource().get(BLOB_FIELD_NAME);
			return ESObsStorageImpl.getObject(blob);
		}
		return null;
	}

	protected boolean storeDataSourceDescription(AbstractProcess process, double time, boolean update) {
		Map<String, Object> json = new HashMap<String, Object>();
		json.put(TIMESTAMP_FIELD_NAME,time);
		json.put(BLOB_FIELD_NAME,ESObsStorageImpl.getBlob(process));
		
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
		System.err.println("TODO: removeDataSourceDescription");
	}

	@Override
	public void removeDataSourceDescriptionHistory(double startTime, double endTime) {
		System.err.println("TODO: removeDataSourceDescriptionHistory");
	}

	@Override
	public Map<String, ? extends IRecordStoreInfo> getRecordStores() {
		System.err.println("TODO: getRecordStores");
		return null;
	}

	@Override
	public void addRecordStore(String name, DataComponent recordStructure, DataEncoding recommendedEncoding) {
		System.err.println("TODO: addRecordStore");
	}

	@Override
	public int getNumRecords(String recordType) {
		System.err.println("TODO: getNumRecords");
		return 0;
	}

	@Override
	public double[] getRecordsTimeRange(String recordType) {
		System.err.println("TODO: getRecordsTimeRange");
		return null;
	}

	@Override
	public Iterator<double[]> getRecordsTimeClusters(String recordType) {
		System.err.println("TODO: getRecordsTimeClusters");
		return null;
	}

	@Override
	public DataBlock getDataBlock(DataKey key) {
		System.err.println("TODO: getDataBlock");
		return null;
	}

	@Override
	public Iterator<DataBlock> getDataBlockIterator(IDataFilter filter) {
		System.err.println("TODO: getDataBlockIterator");
		return null;
	}

	@Override
	public Iterator<? extends IDataRecord> getRecordIterator(IDataFilter filter) {
		System.err.println("TODO: getRecordIterator");
		return null;
	}

	@Override
	public int getNumMatchingRecords(IDataFilter filter, long maxCount) {
		System.err.println("TODO: getNumMatchingRecords");
		return 0;
	}

	@Override
	public void storeRecord(DataKey key, DataBlock data) {
		System.err.println("TODO: storeRecord");
	}

	@Override
	public void updateRecord(DataKey key, DataBlock data) {
		System.err.println("TODO: updateRecord");
	}

	@Override
	public void removeRecord(DataKey key) {
		System.err.println("TODO: removeRecord");
	}

	@Override
	public int removeRecords(IDataFilter filter) {
		System.err.println("TODO: removeRecords");
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
}
