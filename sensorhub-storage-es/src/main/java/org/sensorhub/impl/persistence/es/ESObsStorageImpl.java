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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.geo.builders.EnvelopeBuilder;
import org.elasticsearch.common.geo.builders.ShapeBuilders;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.persistence.IFoiFilter;
import org.sensorhub.api.persistence.IObsStorage;
import org.sensorhub.api.persistence.IObsStorageModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.util.Bbox;

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.AbstractGeometry;
import net.opengis.gml.v32.Envelope;

/**
 * <p>
 * ES implementation of {@link IObsStorage} for storing observations.
 * </p>
 *
 * @author Mathieu Dhainaut <mathieu.dhainaut@gmail.com>
 * @since 2017
 */
public class ESObsStorageImpl extends ESBasicStorageImpl implements IObsStorageModule<ESBasicStorageConfig> {

	protected final static String RS_FOI_IDX_NAME = "foi";
	protected final static String FOI_UNIQUE_ID_FIELD = "foiUniqueId";
	protected final static String SHAPE_FIELD_NAME = "shape";
	
	private String currentStorageIndexId;
	
	private Set<String> typeList;
	
	/**
	 * Class logger
	 */
	private static final Logger log = LoggerFactory.getLogger(ESObsStorageImpl.class);  
	
	public ESObsStorageImpl() {
	}
	
	@Override
	public synchronized void start() throws SensorHubException {
		super.start();
		currentStorageIndexId = getLocalID()+"_"+RS_FOI_IDX_NAME;
		
		// create indices if they dont exist
		boolean exists = client.admin().indices()
			    .prepareExists(currentStorageIndexId)
			    .execute().actionGet().isExists();
		if(!exists) {
			CreateIndexRequest indexRequest = new CreateIndexRequest(currentStorageIndexId);
			client.admin().indices().create(indexRequest).actionGet();
			refreshIndex();
		}
		
		typeList = new HashSet<String>(getProducerIds());
	}
	
	protected synchronized Collection<String> getProducerIds() {
		Set<String> typeList = new HashSet<String>();
        try {
             GetMappingsResponse res = client.admin().indices().getMappings(new GetMappingsRequest().indices(currentStorageIndexId)).get();
             ImmutableOpenMap<String, MappingMetaData> mapping = res.mappings().get(currentStorageIndexId);
             for (ObjectObjectCursor<String, MappingMetaData> c : mapping) {
                 typeList.add(c.key);
             }
        } catch (InterruptedException e) {
            log.error(currentStorageIndexId+" --> Cannot Get the producer ids: "+e.getMessage());
        } catch (ExecutionException e) {
            log.error(currentStorageIndexId+" --> Cannot Get the producer ids: "+e.getMessage());
        }
        return typeList;
	}
	
	@Override
	public synchronized int getNumFois(IFoiFilter filter) {
		String[] typeListArgs = filter.getProducerIDs().toArray(new String[filter.getProducerIDs().size()]);
		
		// build query
		QueryBuilder foiIdquery = QueryBuilders
				.matchQuery(FOI_UNIQUE_ID_FIELD, filter.getFeatureIDs());
		
		// aggregate queries
		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
				.must(foiIdquery);

		try {
			queryBuilder.must(getPolygonGeoQuery(filter.getRoi()));
		} catch (IOException e) {
			log.error("[PolygonGeoQueryBuilder] Cannot build the polygon Geo query");
		}
		
		final SearchRequestBuilder scrollReq = client.prepareSearch(currentStorageIndexId)
				.setTypes(typeListArgs)
				.setQuery(queryBuilder)
				.setFetchSource(new String[]{FOI_UNIQUE_ID_FIELD}, new String[]{}) // get only the ids
		        .setScroll(new TimeValue(config.pingTimeout));
		
		return (int) scrollReq.get().getHits().getTotalHits();
	}

	@Override
	public Bbox getFoisSpatialExtent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public synchronized Iterator<String> getFoiIDs(IFoiFilter filter) {
		String[] typeListArgs = filter.getProducerIDs().toArray(new String[filter.getProducerIDs().size()]);
		
		// build query
		QueryBuilder foiIdquery = QueryBuilders
				.matchQuery(FOI_UNIQUE_ID_FIELD, filter.getFeatureIDs());
		
		// aggregate queries
		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
				.must(foiIdquery);

		try {
			queryBuilder.must(getPolygonGeoQuery(filter.getRoi()));
		} catch (IOException e) {
			log.error("[PolygonGeoQueryBuilder] Cannot build the polygon Geo query");
		}
		
		final SearchRequestBuilder scrollReq = client.prepareSearch(currentStorageIndexId)
				.setTypes(typeListArgs)
				.setQuery(queryBuilder)
				.setFetchSource(new String[]{FOI_UNIQUE_ID_FIELD}, new String[]{}) // get only the ids
		        .setScroll(new TimeValue(config.pingTimeout));

		// wrap the request into custom ES Scroll iterator
		final Iterator<SearchHit> searchHitsIterator = new ESIterator(client, scrollReq,
				config.scrollFetchSize); //max of scrollFetchSize hits will be returned for each scroll
				
		return new Iterator<String>(){

			@Override
			public boolean hasNext() {
				return searchHitsIterator.hasNext();
			}

			@Override
			public String next() {
				SearchHit nextSearchHit = searchHitsIterator.next();
				// get IDs from source
				return nextSearchHit.getSource().get(FOI_UNIQUE_ID_FIELD).toString(); 
			}
			
		};
	}

	@Override
	public synchronized Iterator<AbstractFeature> getFois(IFoiFilter filter) {
		String[] typeListArgs = filter.getProducerIDs().toArray(new String[filter.getProducerIDs().size()]);
		
		// build query
		QueryBuilder foiIdquery = QueryBuilders
				.matchQuery(FOI_UNIQUE_ID_FIELD, filter.getFeatureIDs());
		
		// aggregate queries
		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
				.must(foiIdquery);

		try {
			queryBuilder.must(getPolygonGeoQuery(filter.getRoi()));
		} catch (IOException e) {
			log.error("[PolygonGeoQueryBuilder] Cannot build the polygon Geo query");
		}
		
		final SearchRequestBuilder scrollReq = client.prepareSearch(currentStorageIndexId)
				.setTypes(typeListArgs)
				.setQuery(queryBuilder)
				.setFetchSource(new String[]{BLOB_FIELD_NAME}, new String[]{}) // get only the blob
		        .setScroll(new TimeValue(config.pingTimeout));

		// wrap the request into custom ES Scroll iterator
		final Iterator<SearchHit> searchHitsIterator = new ESIterator(client, scrollReq,
				config.scrollFetchSize); //max of scrollFetchSize hits will be returned for each scroll
				
		return new Iterator<AbstractFeature>(){

			@Override
			public boolean hasNext() {
				return searchHitsIterator.hasNext();
			}

			@Override
			public AbstractFeature next() {
				SearchHit nextSearchHit = searchHitsIterator.next();
				
				// get Feature from blob
				return
					ESObsStorageImpl.this.<AbstractFeature>getObject(nextSearchHit.getSource().get(BLOB_FIELD_NAME)); // Feature
			}
			
		};
	}

	@Override
	public synchronized void storeFoi(String producerID, AbstractFeature foi) {
		// check type
		checkType(producerID);
		
		// build the foi index request
		IndexRequestBuilder foiIdxReq = client.prepareIndex(currentStorageIndexId,producerID);
		
		AbstractGeometry geometry = foi.getLocation();
		EnvelopeBuilder envelopeBuilder = getEnvelopeBuilder(geometry.getGeomEnvelope());
		
		// build source
		Map<String, Object> json = new HashMap<String, Object>();
		json.put(FOI_UNIQUE_ID_FIELD,foi.getUniqueIdentifier()); // store for future indexed searches
		json.put(BLOB_FIELD_NAME,this.getBlob(foi)); // store AbstractFeature
		json.put(SHAPE_FIELD_NAME,envelopeBuilder);
		
		// set source before executing the request
		String id = foiIdxReq.setSource(json).get().getId();
	}
	
	protected void checkType(String type) {
		if(!typeList.contains(type)) {
			// create mapping
			try {
				client.admin().indices() 
				.preparePutMapping(currentStorageIndexId) 
				.setType(type)
				.setSource(getMapping(type)) 
				.execute().actionGet();
				
				refreshIndex();
				typeList.add(type);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	protected synchronized XContentBuilder getMapping(String type) throws IOException {
		XContentBuilder builder = XContentFactory.jsonBuilder()
		    .startObject()
		    	.startObject(type)
			    	.startObject("properties") 
			    		.startObject(SHAPE_FIELD_NAME).field("type", "geo_shape").endObject()
			    		.startObject(FOI_UNIQUE_ID_FIELD).field("type", "string").endObject()
			    		.startObject(BLOB_FIELD_NAME).field("type", "binary").endObject()
			    	.endObject()
			    .endObject()
		    .endObject();
		return builder;
	}
	
	protected synchronized EnvelopeBuilder getEnvelopeBuilder(Envelope envelope) {
		double[] upperCorner = envelope.getUpperCorner();
		double[] lowerCorner = envelope.getLowerCorner();
		
		return ShapeBuilders.newEnvelope(
				new Coordinate(upperCorner[0],upperCorner[1]),
				new Coordinate(lowerCorner[0],lowerCorner[1]));
	}
	
	protected synchronized QueryBuilder getPolygonGeoQuery(Polygon polygon) throws IOException {
		List<GeoPoint> points = new ArrayList<GeoPoint>();
		
		for(Coordinate c : polygon.getCoordinates()) {
			points.add(new GeoPoint(c.x,c.y));
		}

		return QueryBuilders.geoShapeQuery(
		        SHAPE_FIELD_NAME,                         
		        ShapeBuilders.newMultiPoint(Arrays.asList(polygon.getCoordinates())));  
	}
}
