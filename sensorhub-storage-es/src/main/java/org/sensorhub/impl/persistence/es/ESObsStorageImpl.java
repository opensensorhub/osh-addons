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

import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
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

	@Override
	protected void createIndices (){
		super.createIndices();
		// create FOI mapping
		try {
			client.admin().indices() 
			    .preparePutMapping(getLocalID()) 
			    .setType(RS_FOI_IDX_NAME)
			    .setSource(getMapping(RS_FOI_IDX_NAME)) 
			    .execute().actionGet();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//TODO: update mapping for RS_DATA and add support for samplingTime => geo_shape
	}
	
	@Override
	public synchronized Iterator<DataBlock> getDataBlockIterator(IDataFilter filter) {
		if(!(filter instanceof ObsFilter)) {
			return super.getDataBlockIterator(filter);
		}
		
		IObsFilter obsFilter = (IObsFilter) filter;
		
		double[] timeRange = getTimeRange(obsFilter);
		
		// prepare filter
		QueryBuilder timeStampRangeQuery = QueryBuilders.rangeQuery(TIMESTAMP_FIELD_NAME).from(timeRange[0]).to(timeRange[1]);
		QueryBuilder recordTypeQuery = QueryBuilders.matchQuery(RECORD_TYPE_FIELD_NAME, filter.getRecordType());
		
		// aggregate queries
		BoolQueryBuilder filterQueryBuilder = QueryBuilders.boolQuery()
				.must(timeStampRangeQuery);
		
		// filter on producerIDs
		if(obsFilter.getProducerIDs() != null && !obsFilter.getProducerIDs().isEmpty()) {
			filterQueryBuilder.must(QueryBuilders.matchQuery(PRODUCER_ID_FIELD_NAME, new ArrayList<String>(obsFilter.getProducerIDs())));
		}
		
		// filter on fois
		if(obsFilter.getFoiIDs() != null && !obsFilter.getFoiIDs().isEmpty()) {
			filterQueryBuilder.must(QueryBuilders.matchQuery(FOI_UNIQUE_ID_FIELD, new ArrayList<String>(obsFilter.getFoiIDs())));
		}
		
		// filter on ROI
		if (obsFilter.getRoi() != null) {
			try {
				filterQueryBuilder.must(getPolygonGeoQuery(obsFilter.getRoi()));
			} catch (IOException e) {
				log.error("[PolygonGeoQueryBuilder] Cannot build the polygon Geo query");
			}
		}
		
		// build response
		final SearchRequestBuilder scrollReq = client.prepareSearch(getLocalID()).setTypes(RS_DATA_IDX_NAME)
				//TOCHECK
				.addSort(TIMESTAMP_FIELD_NAME, SortOrder.ASC)
				.setFetchSource(new String[]{BLOB_FIELD_NAME}, new String[]{}) // get only the BLOB
		        .setScroll(new TimeValue(config.scrollMaxDuration))
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
				return ESObsStorageImpl.this.<DataBlock>getObject(blob); // DataBlock
			}
		};
	}
	
	@Override
	public synchronized int getNumFois(IFoiFilter filter) {
		// build query
		// aggregate queries
		
		BoolQueryBuilder filterQueryBuilder = QueryBuilders.boolQuery();
		if(filter.getFeatureIDs() != null && !filter.getFeatureIDs().isEmpty()) {
			filterQueryBuilder.must(QueryBuilders.termsQuery(FOI_UNIQUE_ID_FIELD, new ArrayList<String>(filter.getFeatureIDs())));
		}

		if(filter.getProducerIDs() != null && !filter.getProducerIDs().isEmpty()) {
			filterQueryBuilder.must(QueryBuilders.termsQuery(PRODUCER_ID_FIELD_NAME, new ArrayList<String>(filter.getProducerIDs())));
		}

		if (filter.getRoi() != null) {
			try {
				filterQueryBuilder.must(getPolygonGeoQuery(filter.getRoi()));
			} catch (IOException e) {
				log.error("[PolygonGeoQueryBuilder] Cannot build the polygon Geo query");
			}
		}

		final SearchRequestBuilder scrollReq = client.prepareSearch(getLocalID())
				.setTypes(RS_FOI_IDX_NAME)
				.setQuery(filterQueryBuilder)
				.setFetchSource(new String[] {}, new String[] {"*"}) 
				.setScroll(new TimeValue(config.scrollMaxDuration));

		return (int) scrollReq.get().getHits().getTotalHits();
	}

	@Override
	public Bbox getFoisSpatialExtent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public synchronized Iterator<String> getFoiIDs(IFoiFilter filter) {
		// build query
		// aggregate queries
		BoolQueryBuilder filterQueryBuilder = QueryBuilders.boolQuery();
		if(filter.getFeatureIDs() != null && !filter.getFeatureIDs().isEmpty()) {
			filterQueryBuilder.must(QueryBuilders.termsQuery(FOI_UNIQUE_ID_FIELD, new ArrayList<String>(filter.getFeatureIDs())));
		}

		if(filter.getProducerIDs() != null && !filter.getProducerIDs().isEmpty()) {
			filterQueryBuilder.must(QueryBuilders.termsQuery(PRODUCER_ID_FIELD_NAME, new ArrayList<String>(filter.getProducerIDs())));
		}
		
		if (filter.getRoi() != null) {
			try {
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
		if(filter.getFeatureIDs() != null && !filter.getFeatureIDs().isEmpty()) {
			filterQueryBuilder.must(QueryBuilders.termsQuery(FOI_UNIQUE_ID_FIELD, new ArrayList<String>(filter.getFeatureIDs())));
		}

		if(filter.getProducerIDs() != null && !filter.getProducerIDs().isEmpty()) {
			filterQueryBuilder.must(QueryBuilders.termsQuery(PRODUCER_ID_FIELD_NAME, new ArrayList<String>(filter.getProducerIDs())));
		}
		
		if (filter.getRoi() != null) {
			try {
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
	public synchronized void storeRecord(DataKey key, DataBlock data) {
		if(!(key instanceof ObsKey)) {
			super.storeRecord(key, data);
		} else {
			ObsKey obsKey = (ObsKey) key;
			// build the key as recordTYpe_timestamp_producerID
			String esKey = getRsKey(key);
			
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
			
			// set id and blob before executing the request
			String id = client.prepareIndex(getLocalID(),RS_DATA_IDX_NAME).setId(esKey).setSource(json).get().getId();
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
		
		try {
			ShapeBuilder shapeBuilder = getShapeBuilder(geometry);
			json.put(SHAPE_FIELD_NAME, shapeBuilder);
		} catch (SensorHubException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// set source before executing the request
		String id = foiIdxReq.setSource(json).get().getId();
	}

	protected synchronized XContentBuilder getMapping(String type) throws IOException {
		XContentBuilder builder = XContentFactory.jsonBuilder()
				.startObject()
					//.startObject(type)
						.startObject("properties")
							.startObject(SHAPE_FIELD_NAME).field("type", "geo_shape").endObject()
							.startObject(FOI_UNIQUE_ID_FIELD).field("type", "text").endObject()
							.startObject(PRODUCER_ID_FIELD_NAME).field("type", "text").endObject()
							.startObject(BLOB_FIELD_NAME).field("type", "binary").endObject()
						.endObject()
					//.endObject()
				.endObject();
		return builder;
	}

	protected synchronized EnvelopeBuilder getEnvelopeBuilder(Envelope envelope) {
		double[] upperCorner = envelope.getUpperCorner();
		double[] lowerCorner = envelope.getLowerCorner();
		return ShapeBuilders.newEnvelope(new Coordinate(upperCorner[0], upperCorner[1]),
				new Coordinate(lowerCorner[0], lowerCorner[1]));
	}

	protected synchronized PolygonBuilder getPolygonBuilder(Polygon polygon) {
		// get coordinates list from polygon
		List<Coordinate> coords = Arrays.asList(polygon.getCoordinates());
		// build shape builder from coordinates
		return ShapeBuilders.newPolygon(coords);
		//.relation(ShapeRelation.WITHIN); strategy, Default is INTERSECT
	}
	
	protected synchronized PointBuilder getPointBuilder(Point point) {
		// build shape builder from coordinates
		return ShapeBuilders.newPoint(new Coordinate(point.getPos()[1],point.getPos()[0]));
	}
	
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
	
	protected synchronized QueryBuilder getPolygonGeoQuery(Polygon polygon) throws IOException {
		List<GeoPoint> points = new ArrayList<GeoPoint>();

		for (Coordinate c : polygon.getCoordinates()) {
			points.add(new GeoPoint(c.x, c.y));
		}

		return QueryBuilders.geoShapeQuery(SHAPE_FIELD_NAME,
				ShapeBuilders.newMultiPoint(Arrays.asList(polygon.getCoordinates())));
	}
}
