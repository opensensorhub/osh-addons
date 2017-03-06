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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.support.AbstractClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.geo.builders.EnvelopeBuilder;
import org.elasticsearch.common.geo.builders.PointBuilder;
import org.elasticsearch.common.geo.builders.PolygonBuilder;
import org.elasticsearch.common.geo.builders.ShapeBuilder;
import org.elasticsearch.common.geo.builders.ShapeBuilders;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.IFoiFilter;
import org.sensorhub.api.persistence.IObsFilter;
import org.sensorhub.api.persistence.IObsStorage;
import org.sensorhub.api.persistence.IObsStorageModule;
import org.sensorhub.api.persistence.ObsFilter;
import org.sensorhub.api.persistence.ObsKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.util.Bbox;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.AbstractGeometry;
import net.opengis.gml.v32.Envelope;
import net.opengis.gml.v32.Point;
import net.opengis.gml.v32.impl.EnvelopeJTS;
import net.opengis.gml.v32.impl.PolygonJTS;
import net.opengis.swe.v20.DataBlock;

/**
 * <p>
 * ES implementation of {@link IObsStorage} for storing observations.
 * </p>
 *
 * @author Mathieu Dhainaut <mathieu.dhainaut@gmail.com>
 * @since 2017
 */
public class ESObsStorageImpl extends ESBasicStorageImpl implements IObsStorageModule<ESBasicStorageConfig> {

	private static final String NO_PARENT_VALUE = "-1";
	private static final String RESULT_TIME_FIELD_NAME = "resultTime";
	private static final String SAMPLING_GEOMETRY_FIELD_NAME = "samplingGeometry";
	protected final static String RS_FOI_IDX_NAME = "foi";
	protected final static String FOI_UNIQUE_ID_FIELD = "foiUniqueId";
	protected final static String SHAPE_FIELD_NAME = "shapeObject";
	protected final static String PRODUCER_ID_FIELD_NAME = "producerID";
	protected final static String TIMESTAMP_INSERT_FIELD_NAME ="insertTime";
	/**
	 * Class logger
	 */
	private static final Logger log = LoggerFactory.getLogger(ESObsStorageImpl.class);

	public ESObsStorageImpl() {
	}

	public ESObsStorageImpl(AbstractClient client) {
		super(client);
	}
	@Override
	protected void createIndices (){
		super.createIndices();
		
		// create FOI mapping
		try {
			client.admin().indices() 
			    .preparePutMapping(getLocalID()) 
			    .setType(RS_FOI_IDX_NAME)
			    .setSource(getMapping()) 
			    .execute().actionGet();
		} catch (IOException e) {
			log.error("Cannot create the foi mapping",e);
		}
		
		
		//TODO: update mapping for RS_DATA and add support for samplingTime => geo_shape
	}
	
	@Override
	public synchronized Iterator<? extends IDataRecord> getRecordIterator(IDataFilter filter) {
		// if not Obs, use the super one
		if(!(filter instanceof ObsFilter)) {
			return super.getRecordIterator(filter);
		}
		
		// otherwise case as obsFilter
		IObsFilter obsFilter = (IObsFilter) filter;
		
		BoolQueryBuilder foiFilterQueryBuilder = QueryBuilders.boolQuery(); //children query
		BoolQueryBuilder dataFilterQueryBuilder = QueryBuilders.boolQuery(); //parent query

		// FOI STORE has: 
		// Foi: uniqueId
		// ProducerId: producer id
		// Shape : geo_shape = roi
		
		boolean useFoiFilter = false;
		
		// filter on Producer Ids
		if(obsFilter.getProducerIDs() != null && !obsFilter.getProducerIDs().isEmpty()) {
			foiFilterQueryBuilder.must(QueryBuilders.matchQuery(PRODUCER_ID_FIELD_NAME, new ArrayList<String>(obsFilter.getProducerIDs())));
		}
		
		// filter on Foi
		if(obsFilter.getFoiIDs() != null && !obsFilter.getFoiIDs().isEmpty()) {
			foiFilterQueryBuilder.must(QueryBuilders.matchQuery(FOI_UNIQUE_ID_FIELD, new ArrayList<String>(obsFilter.getFoiIDs())));
		}
		
		// filter on ROI
		if (obsFilter.getRoi() != null) {
			try {
				useFoiFilter = true;
				// get the geo query
				foiFilterQueryBuilder.must(getPolygonGeoQuery(obsFilter.getRoi()));
			} catch (IOException e) {
				log.error("[PolygonGeoQueryBuilder] Cannot build the polygon Geo query");
			}
		}
		
		// Build parent query = data
		// Data Store has:
		// Timestamp: double
		// Producer Id: producer Id
		// Record type: record type
		// Foi id: foi unique id
		// Blob: datablock
		
		// build the RS data request using the filtering list if not null
		double[] timeRange = getTimeRange(obsFilter);
		if(timeRange[0] != Double.NEGATIVE_INFINITY && timeRange[1] != Double.NEGATIVE_INFINITY){
			dataFilterQueryBuilder.must(QueryBuilders.rangeQuery(TIMESTAMP_FIELD_NAME).from(timeRange[0]).to(timeRange[1]));
		}
		
		// prepare filters
		// filter on record type?
		if(filter.getRecordType() != null) {
			dataFilterQueryBuilder.must(QueryBuilders.matchQuery(RECORD_TYPE_FIELD_NAME, filter.getRecordType()));
		}
		
		// filter on producer id?
		if(filter.getProducerIDs() != null && !filter.getProducerIDs().isEmpty()) {
			dataFilterQueryBuilder.must(QueryBuilders.matchQuery(PRODUCER_ID_FIELD_NAME, new ArrayList<String>(obsFilter.getProducerIDs())));
		}
				
		// filter on Foi?
		if(obsFilter.getFoiIDs() != null && !obsFilter.getFoiIDs().isEmpty()) {
			dataFilterQueryBuilder.must(QueryBuilders.matchQuery(FOI_UNIQUE_ID_FIELD, new ArrayList<String>(obsFilter.getFoiIDs())));
		}		
		
		// queries concatener
		BoolQueryBuilder filterQueryBuilder = QueryBuilders.boolQuery();
		
		if(useFoiFilter) {
			// combine queries
			filterQueryBuilder.must(QueryBuilders.hasParentQuery(RS_FOI_IDX_NAME, foiFilterQueryBuilder, false))
					.must(dataFilterQueryBuilder);
		}	else {
			filterQueryBuilder = dataFilterQueryBuilder;
		}
		
		// build response
		SearchRequestBuilder scrollReq = client.prepareSearch(getLocalID())
				.setTypes(RS_DATA_IDX_NAME)
				.addSort(TIMESTAMP_FIELD_NAME, SortOrder.ASC)
		        .setScroll(new TimeValue(config.scrollMaxDuration))
		        .setQuery(filterQueryBuilder);
		
		// wrap the request into custom ES Scroll iterator
		final Iterator<SearchHit> searchHitsIterator = new ESIterator(client, scrollReq,
				config.scrollFetchSize); //max of scrollFetchSize hits will be returned for each scroll
		
		return new Iterator<IDataRecord>(){

			@Override
			public boolean hasNext() {
				return searchHitsIterator.hasNext();
			}

			@Override
			public IDataRecord next() {
				SearchHit nextSearchHit = searchHitsIterator.next();
				
				// build key
				String recordType = nextSearchHit.getSource().get(RECORD_TYPE_FIELD_NAME).toString();
				String foiID = nextSearchHit.getSource().get(FOI_UNIQUE_ID_FIELD).toString();
				double timeStamp = (double) nextSearchHit.getSource().get(TIMESTAMP_FIELD_NAME);
				
				final ObsKey key = new ObsKey(recordType, foiID, timeStamp);
				
				// get DataBlock from blob
				final DataBlock datablock=ESObsStorageImpl.this.<DataBlock>getObject(nextSearchHit.getSource().get(BLOB_FIELD_NAME)); // DataBlock
				
				return new IDataRecord(){

					@Override
					public ObsKey getKey() {
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
	public synchronized Iterator<DataBlock> getDataBlockIterator(IDataFilter filter) {
		// if not Obs, use the super one
		if(!(filter instanceof ObsFilter)) {
			return super.getDataBlockIterator(filter);
		}
		
		// otherwise case as obsFilter
		IObsFilter obsFilter = (IObsFilter) filter;
		
		BoolQueryBuilder foiFilterQueryBuilder = QueryBuilders.boolQuery(); //children query
		BoolQueryBuilder dataFilterQueryBuilder = QueryBuilders.boolQuery(); //parent query

		// FOI STORE has: 
		// Foi: uniqueId
		// ProducerId: producer id
		// Shape : geo_shape = roi
		
		boolean useFoiFilter = false;
		
		// filter on Producer Ids
		if(obsFilter.getProducerIDs() != null && !obsFilter.getProducerIDs().isEmpty()) {
			foiFilterQueryBuilder.must(QueryBuilders.matchQuery(PRODUCER_ID_FIELD_NAME, new ArrayList<String>(obsFilter.getProducerIDs())));
		}
		
		// filter on Foi
		if(obsFilter.getFoiIDs() != null && !obsFilter.getFoiIDs().isEmpty()) {
			foiFilterQueryBuilder.must(QueryBuilders.matchQuery(FOI_UNIQUE_ID_FIELD, new ArrayList<String>(obsFilter.getFoiIDs())));
		}
		
		// filter on ROI
		if (obsFilter.getRoi() != null) {
			try {
				useFoiFilter = true;
				foiFilterQueryBuilder.must(getPolygonGeoQuery(obsFilter.getRoi()));
			} catch (IOException e) {
				log.error("[PolygonGeoQueryBuilder] Cannot build the polygon Geo query");
			}
		}
		
		// Build parent query = data
		// Data Store has:
		// Timestamp: double
		// Producer Id: producer Id
		// Record type: record type
		// Foi id: foi unique id
		// Blob: datablock
		
		// build the RS data request using the filtering list if not null
		double[] timeRange = getTimeRange(obsFilter);
		if(timeRange[0] != Double.NEGATIVE_INFINITY && timeRange[1] != Double.NEGATIVE_INFINITY){
			dataFilterQueryBuilder.must(QueryBuilders.rangeQuery(TIMESTAMP_FIELD_NAME).from(timeRange[0]).to(timeRange[1]));
		}
		
		// prepare filter
		if(filter.getRecordType() != null) {
			dataFilterQueryBuilder.must(QueryBuilders.matchQuery(RECORD_TYPE_FIELD_NAME, filter.getRecordType()));
		}
		
		if(filter.getProducerIDs() != null && !filter.getProducerIDs().isEmpty()) {
			dataFilterQueryBuilder.must(QueryBuilders.matchQuery(PRODUCER_ID_FIELD_NAME, new ArrayList<String>(obsFilter.getProducerIDs())));
		}
				
		// filter on Foi
		if(obsFilter.getFoiIDs() != null && !obsFilter.getFoiIDs().isEmpty()) {
			dataFilterQueryBuilder.must(QueryBuilders.matchQuery(FOI_UNIQUE_ID_FIELD, new ArrayList<String>(obsFilter.getFoiIDs())));
		}		
		
		BoolQueryBuilder filterQueryBuilder = QueryBuilders.boolQuery();
		
		if(useFoiFilter) {
			// combine queries
			filterQueryBuilder.must(QueryBuilders.hasParentQuery(RS_FOI_IDX_NAME, foiFilterQueryBuilder, false))
					.must(dataFilterQueryBuilder);
		}	else {
			filterQueryBuilder = dataFilterQueryBuilder;
		}
		
		// build response
		SearchRequestBuilder scrollReq = client.prepareSearch(getLocalID())
				.setTypes(RS_DATA_IDX_NAME)
				.addSort(TIMESTAMP_FIELD_NAME, SortOrder.ASC)
				.setFetchSource(new String[]{BLOB_FIELD_NAME}, new String[]{}) // get only the BLOB
		        .setScroll(new TimeValue(config.scrollMaxDuration))
		        .setQuery(filterQueryBuilder);
		
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
				return ESObsStorageImpl.this.<DataBlock>getObject(blob); // DataBlock
			}
		};
	}
	
	@Override
	public synchronized int getNumFois(IFoiFilter filter) {
		// build query
		// aggregate queries
		
		BoolQueryBuilder filterQueryBuilder = QueryBuilders.boolQuery();
		// filter on feature ids?
		if(filter.getFeatureIDs() != null && !filter.getFeatureIDs().isEmpty()) {
			filterQueryBuilder.must(QueryBuilders.termsQuery(FOI_UNIQUE_ID_FIELD, new ArrayList<String>(filter.getFeatureIDs())));
		}

		// filter on producer ids
		if(filter.getProducerIDs() != null && !filter.getProducerIDs().isEmpty()) {
			filterQueryBuilder.must(QueryBuilders.termsQuery(PRODUCER_ID_FIELD_NAME, new ArrayList<String>(filter.getProducerIDs())));
		}

		// filter on ROI
		if (filter.getRoi() != null) {
			try {
				// build geo query from filter ROI
				filterQueryBuilder.must(getPolygonGeoQuery(filter.getRoi()));
			} catch (IOException e) {
				log.error("[PolygonGeoQueryBuilder] Cannot build the polygon Geo query");
			}
		}

		// build the request
		final SearchRequestBuilder scrollReq = client.prepareSearch(getLocalID())
				.setTypes(RS_FOI_IDX_NAME)
				.setQuery(filterQueryBuilder)
				.setFetchSource(new String[] {}, new String[] {"*"}); 

		// return the number of total hits
		return (int) scrollReq.get().getHits().getTotalHits();
	}

	@Override
	public Bbox getFoisSpatialExtent() {
		// compute the maxExtent of the shapes contained in the DB
		final EnvelopeJTS maxExtent = new EnvelopeJTS();
		
		final SearchRequestBuilder scrollReq = client.prepareSearch(getLocalID())
				.setTypes(RS_FOI_IDX_NAME)
				.setFetchSource(new String[] {SHAPE_FIELD_NAME}, new String[] {}) 
				.setScroll(new TimeValue(config.scrollMaxDuration));
		
		final Iterator<SearchHit> searchHitsIterator = new ESIterator(client, scrollReq, config.scrollFetchSize); 
		SearchHit currentHit = null;
		
		// iterate over every hit to get the corresponding geo_shape object
		while(searchHitsIterator.hasNext()) {
			currentHit = searchHitsIterator.next();
		}
		return null;
	}

	@Override
	public synchronized Iterator<String> getFoiIDs(IFoiFilter filter) {
		// build query
		// aggregate queries
		BoolQueryBuilder filterQueryBuilder = QueryBuilders.boolQuery();
		// filter on feature ids?
		if(filter.getFeatureIDs() != null && !filter.getFeatureIDs().isEmpty()) {
			filterQueryBuilder.must(QueryBuilders.termsQuery(FOI_UNIQUE_ID_FIELD, new ArrayList<String>(filter.getFeatureIDs())));
		}

		// filter on producer ids?
		if(filter.getProducerIDs() != null && !filter.getProducerIDs().isEmpty()) {
			filterQueryBuilder.must(QueryBuilders.termsQuery(PRODUCER_ID_FIELD_NAME, new ArrayList<String>(filter.getProducerIDs())));
		}
		
		// filter on ROI?
		if (filter.getRoi() != null) {
			try {
				// build geo query
				filterQueryBuilder.must(getPolygonGeoQuery(filter.getRoi()));
			} catch (IOException e) {
				log.error("[PolygonGeoQueryBuilder] Cannot build the polygon Geo query");
			}
		}
		
		final SearchRequestBuilder scrollReq = client.prepareSearch(getLocalID())
				.setTypes(RS_FOI_IDX_NAME)
				.setQuery(filterQueryBuilder)
				.addSort(TIMESTAMP_INSERT_FIELD_NAME, SortOrder.ASC)
				 // get only the blob
				.setFetchSource(new String[] { FOI_UNIQUE_ID_FIELD }, new String[] {}) 
				.setScroll(new TimeValue(config.scrollMaxDuration));
		// wrap the request into custom ES Scroll iterator
		final Iterator<SearchHit> searchHitsIterator = new ESIterator(client, scrollReq, config.scrollFetchSize); 

		return new Iterator<String>() {

			@Override
			public boolean hasNext() {
				return searchHitsIterator.hasNext();
			}

			@Override
			public String next() {
				SearchHit nextSearchHit = searchHitsIterator.next();
				// get Feature id
				return nextSearchHit.getSource().get(FOI_UNIQUE_ID_FIELD).toString();
			}
		};
	}

	@Override
	public synchronized Iterator<AbstractFeature> getFois(IFoiFilter filter) {
		// build query
		// aggregate queries
		BoolQueryBuilder filterQueryBuilder = QueryBuilders.boolQuery();
		
		// filter on feature ids?
		if(filter.getFeatureIDs() != null && !filter.getFeatureIDs().isEmpty()) {
			filterQueryBuilder.must(QueryBuilders.termsQuery(FOI_UNIQUE_ID_FIELD, new ArrayList<String>(filter.getFeatureIDs())));
		}

		// filter on producer ids?
		if(filter.getProducerIDs() != null && !filter.getProducerIDs().isEmpty()) {
			filterQueryBuilder.must(QueryBuilders.termsQuery(PRODUCER_ID_FIELD_NAME, new ArrayList<String>(filter.getProducerIDs())));
		}
		
		// filter on roi?
		if (filter.getRoi() != null) {
			try {
				// build geo query
				filterQueryBuilder.must(getPolygonGeoQuery(filter.getRoi()));
			} catch (IOException e) {
				log.error("[PolygonGeoQueryBuilder] Cannot build the polygon Geo query");
			}
		}
		
		// create scroll request
		final SearchRequestBuilder scrollReq = client.prepareSearch(getLocalID())
				.setTypes(RS_FOI_IDX_NAME)
				.setQuery(filterQueryBuilder)
				.addSort(TIMESTAMP_INSERT_FIELD_NAME, SortOrder.ASC)
				 // get only the blob
				.setFetchSource(new String[] { BLOB_FIELD_NAME }, new String[] {}) 
				.setScroll(new TimeValue(config.scrollMaxDuration));

		// wrap the request into custom ES Scroll iterator
		final Iterator<SearchHit> searchHitsIterator = new ESIterator(client, scrollReq, config.scrollFetchSize);

		return new Iterator<AbstractFeature>() {

			@Override
			public boolean hasNext() {
				return searchHitsIterator.hasNext();
			}

			@Override
			public AbstractFeature next() {
				SearchHit nextSearchHit = searchHitsIterator.next();

				// get Feature from blob
				AbstractFeature f = ESObsStorageImpl.this.<AbstractFeature>getObject(nextSearchHit.getSource().get(BLOB_FIELD_NAME)); // Feature
				return f;
			}

		};
	}

	@Override
	public synchronized DataBlock getDataBlock(DataKey key) {
		// because we need to specify parent in any case
		String parent = "";
		
		// test if obsKey
		// if not, set parent to -1. If no parent is specified, ES will throw an exception.
		// we must specify a parent
		if(!(key instanceof ObsKey)) {
			parent = NO_PARENT_VALUE;
		} else {
			parent = ((ObsKey)key).foiID;
		}
		
		DataBlock result = null;
		// build the key as recordTYpe_timestamp_producerID
		String esKey = getRsKey(key);
		
		// build the request
		GetResponse  response = client.prepareGet(getLocalID(),RS_DATA_IDX_NAME,esKey)
				.setParent(parent)
				.get();
		
		// deserialize the blob field from the response if any
		if(response.isExists()) {
			result = this.<DataBlock>getObject(response.getSource().get(BLOB_FIELD_NAME)); // DataBlock
		}
		return result;
	}
	
	@Override
	public synchronized void removeRecord(DataKey key) {
		// because we need to specify parent in any case
		String parent = "";
		
		// test if obsKey
		// if not, set parent to -1. If no parent is specified, ES will throw an exception.
		// we must specify a parent
		if(!(key instanceof ObsKey)) {
			parent = NO_PARENT_VALUE;
		} else {
			parent = ((ObsKey)key).foiID;
		}
		
		// build the key as recordTYpe_timestamp_producerID
		String esKey = getRsKey(key);
		
		// prepare delete request
		client.prepareDelete(getLocalID(), RS_DATA_IDX_NAME, esKey)
			.setParent(parent).get();
	}
	
	@Override
	public synchronized void updateRecord(DataKey key, DataBlock data) {
		String esKey = getRsKey(key);
		
		if(!(key instanceof ObsKey)) {
			// build the key as recordTYpe_timestamp_producerID
			
			// get blob from dataBlock object using serializer
			Object blob = this.getBlob(data);
			
			//TOCHECK: do we need to store the whole key?
			Map<String, Object> json = new HashMap<String, Object>();
			json.put(TIMESTAMP_FIELD_NAME,key.timeStamp); // store timestamp
			json.put(PRODUCER_ID_FIELD_NAME,key.producerID); // store producerID
			json.put(RECORD_TYPE_FIELD_NAME,key.recordType); // store recordType
			json.put(BLOB_FIELD_NAME,blob); // store DataBlock
			
			// create update request
			client.prepareUpdate(getLocalID(), RS_DATA_IDX_NAME, esKey)
				.setDoc(json)
				.setParent(NO_PARENT_VALUE).get();
		} else {
			ObsKey obsKey = (ObsKey) key;

			// get blob from dataBlock object using serializer
			Object blob = this.getBlob(data);
			
			Map<String, Object> json = new HashMap<String, Object>();
			json.put(TIMESTAMP_FIELD_NAME,key.timeStamp); // store timestamp
			
			if(key.producerID != null) {
				json.put(PRODUCER_ID_FIELD_NAME,key.producerID); // store producerID
			}
			
			if(key.recordType != null) {
				json.put(RECORD_TYPE_FIELD_NAME,key.recordType); // store recordType
			}
			
			json.put(BLOB_FIELD_NAME,blob); // store DataBlock
			
			// obs part
			json.put(RESULT_TIME_FIELD_NAME,obsKey.resultTime);
			
			if(obsKey.foiID != null) {
				json.put(FOI_UNIQUE_ID_FIELD,obsKey.foiID);
			}
			
			if(obsKey.samplingGeometry != null) {
				json.put(SAMPLING_GEOMETRY_FIELD_NAME,getPolygonBuilder(obsKey.samplingGeometry));
			}
			
			// create update request
			client.prepareUpdate(getLocalID(), RS_DATA_IDX_NAME, esKey)
				.setDoc(json)
				.setParent(obsKey.foiID).get();
		}
		
		
	}
	
	@Override
	public synchronized void storeRecord(DataKey key, DataBlock data) {
		if(!(key instanceof ObsKey)) {
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
			String id = client.prepareIndex(getLocalID(),RS_DATA_IDX_NAME)
					.setId(esKey)
					.setSource(json)
					.setParent(NO_PARENT_VALUE)
					.get().getId();
		} else {
			ObsKey obsKey = (ObsKey) key;

			// get blob from dataBlock object using serializer
			Object blob = this.getBlob(data);
			
			Map<String, Object> json = new HashMap<String, Object>();
			json.put(TIMESTAMP_FIELD_NAME,key.timeStamp); // store timestamp
			
			if(key.producerID != null) {
				json.put(PRODUCER_ID_FIELD_NAME,key.producerID); // store producerID
			}
			
			if(key.recordType != null) {
				json.put(RECORD_TYPE_FIELD_NAME,key.recordType); // store recordType
			}
			
			json.put(BLOB_FIELD_NAME,blob); // store DataBlock
			
			// obs part
			if(!Double.isNaN(obsKey.resultTime)) {
				json.put(RESULT_TIME_FIELD_NAME,obsKey.resultTime);
			}
			
			String uniqueParentID = NO_PARENT_VALUE;
			
			if(obsKey.foiID != null) {
				json.put(FOI_UNIQUE_ID_FIELD,obsKey.foiID);
				uniqueParentID = obsKey.foiID;
			}
			
			if(obsKey.samplingGeometry != null) {
				json.put(SAMPLING_GEOMETRY_FIELD_NAME,getPolygonBuilder(obsKey.samplingGeometry));
			}
			
			
			// set id and blob before executing the request
			String id = client.prepareIndex(getLocalID(),RS_DATA_IDX_NAME)
					.setParent(uniqueParentID)
					.setSource(json).get().getId();
		}
	}
	
	@Override
	public synchronized void storeFoi(String producerID, AbstractFeature foi) {
		// build the foi index requQueryBuilders.termsQuery("producerId", producerIds)QueryBuilders.termsQuery("producerId", producerIds)est
		IndexRequestBuilder foiIdxReq = client.prepareIndex(getLocalID(), RS_FOI_IDX_NAME);

		AbstractGeometry geometry = foi.getLocation();

		// build source
		Map<String, Object> json = new HashMap<String, Object>();
		json.put(FOI_UNIQUE_ID_FIELD, foi.getUniqueIdentifier()); 
		json.put(BLOB_FIELD_NAME, this.getBlob(foi)); // store AbstractFeature
		json.put(PRODUCER_ID_FIELD_NAME, producerID);
		json.put(TIMESTAMP_INSERT_FIELD_NAME,new Date().getTime());

		if(geometry != null) {
			try {
				// get shape builder from Geom
				ShapeBuilder shapeBuilder = getShapeBuilder(geometry);
				// store Shape builder
				json.put(SHAPE_FIELD_NAME, shapeBuilder);
			} catch (SensorHubException e) {
				log.error("Cannot create shape builder",e);
			}
		}
		
		// set source before executing the request
		String id = foiIdxReq.setSource(json).setId(foi.getUniqueIdentifier()).get().getId();
	}

	/**
	 * Build and return the FOI mapping.
	 * @return The object used to map the type
	 * @throws IOException
	 */
	protected synchronized XContentBuilder getMapping() throws IOException {
		XContentBuilder builder = XContentFactory.jsonBuilder()
				.startObject()
					.startObject(RS_FOI_IDX_NAME)
						.startObject("properties")
							.startObject(SHAPE_FIELD_NAME).field("type", "geo_shape").endObject()
							.startObject(FOI_UNIQUE_ID_FIELD).field("type", "text").endObject()
							.startObject(PRODUCER_ID_FIELD_NAME).field("type", "text").endObject()
							.startObject(BLOB_FIELD_NAME).field("type", "binary").endObject()
						.endObject()
					.endObject()
				.endObject();
		return builder;
	}

	/**
	 * Override and return the data mapping. This mapping adds some field like parent, sampling geometry, foi ids etc..
	 * @return The object used to map the type
	 * @throws IOException
	 */
	@Override
	protected synchronized XContentBuilder getRsDataMapping() throws IOException {
		XContentBuilder builder = XContentFactory.jsonBuilder()
				.startObject()
					.startObject(RS_DATA_IDX_NAME)
						.startObject("_parent")
							.field("type", RS_FOI_IDX_NAME)
						.endObject()
						.startObject("properties")
							.startObject(FOI_UNIQUE_ID_FIELD).field("type", "text").endObject()
							.startObject(TIMESTAMP_FIELD_NAME).field("type", "double").endObject()
							.startObject(RECORD_TYPE_FIELD_NAME).field("type", "text").endObject()
							.startObject(PRODUCER_ID_FIELD_NAME).field("type", "text").endObject()
							.startObject(SAMPLING_GEOMETRY_FIELD_NAME).field("type", "geo_shape").endObject()
							.startObject(BLOB_FIELD_NAME).field("type", "binary").endObject()
						.endObject()
					.endObject()
				.endObject();
		return builder;
	}
	
	/**
	 * Gets the envelope builder from envelope geometry.
	 * @param envelope the envelope geometry 
	 * @returnthe Envelope builder
	 */
	protected synchronized EnvelopeBuilder getEnvelopeBuilder(Envelope envelope) {
		double[] upperCorner = envelope.getUpperCorner();
		double[] lowerCorner = envelope.getLowerCorner();
		return ShapeBuilders.newEnvelope(new Coordinate(upperCorner[0], upperCorner[1]),
				new Coordinate(lowerCorner[0], lowerCorner[1]));
	}

	/**
	 * Build a polygon builder from a polygon geometry.
	 * @param polygon the Polygon geometry
	 * @return the builder
	 */
	protected synchronized PolygonBuilder getPolygonBuilder(Polygon polygon) {
		// get coordinates list from polygon
		List<Coordinate> coords = Arrays.asList(polygon.getCoordinates());
		// build shape builder from coordinates
		return ShapeBuilders.newPolygon(coords);
		//.relation(ShapeRelation.WITHIN); strategy, Default is INTERSECT
	}
	
	/**
	 * Build a point builder from a point geometry.
	 * @param point the Point geometry
	 * @return the builder
	 */
	protected synchronized PointBuilder getPointBuilder(Point point) {
		// build shape builder from coordinates
		return ShapeBuilders.newPoint(new Coordinate(point.getPos()[0],point.getPos()[1]));
	}
	
	/**
	 * Build the corresponding builder given a generic geometry.
	 * @param geometry The abstract geometry
	 * @return the corresponding builder. The current supported builder are: PolygonJTS, Point, EnvelopeJTS
	 * @throws SensorHubException if the geometry is not supported
	 */
	protected synchronized ShapeBuilder getShapeBuilder(AbstractGeometry geometry) throws SensorHubException {
		if(geometry instanceof PolygonJTS) {
			return getPolygonBuilder(((PolygonJTS)geometry));
		} else if(geometry instanceof Point) {
			return getPointBuilder((Point)geometry);
		} else if(geometry instanceof EnvelopeJTS) {
			return getEnvelopeBuilder((Envelope)geometry);
		} else {
			throw new SensorHubException("Unsupported Geometry exception: "+geometry.getClass());
		}
	}
	
	/**
	 * Build the geo shape query from a Polygon. The query will use a geo intersection query
	 * @param polygon The geometry to build the query
	 * @return The corresponding builder
	 * @throws IOException
	 */
	protected synchronized QueryBuilder getPolygonGeoQuery(Polygon polygon) throws IOException {
		return QueryBuilders.geoIntersectionQuery(SHAPE_FIELD_NAME, 
				ShapeBuilders.newPolygon(Arrays.asList(polygon.getCoordinates())));
	}
}
