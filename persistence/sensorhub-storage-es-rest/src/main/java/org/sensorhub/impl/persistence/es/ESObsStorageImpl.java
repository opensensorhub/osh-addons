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

import com.vividsolutions.jts.geom.*;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.AbstractGeometry;
import net.opengis.gml.v32.impl.EnvelopeJTS;
import net.opengis.gml.v32.impl.PointJTS;
import net.opengis.gml.v32.impl.PolygonJTS;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.geo.builders.*;
import org.elasticsearch.common.geo.parsers.ShapeParser;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.*;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.geobounds.ParsedGeoBounds;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.shape.Shape;
import org.locationtech.spatial4j.shape.jts.JtsPoint;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.util.Bbox;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * <p>
 * ES implementation of {@link IObsStorage} for storing observations.
 * </p>
 *
 * @author Mathieu Dhainaut <mathieu.dhainaut@gmail.com>
 * @author Nicolas Fortin, UMRAE Ifsttar
 * @since 2017
 */
public class ESObsStorageImpl extends ESBasicStorageImpl implements IObsStorageModule<ESBasicStorageConfig> {

	private static final String POLYGON_QUERY_ERROR_MSG = "Cannot build polygon geo query";
	private static final String RESULT_TIME_FIELD_NAME = "resultTime";
	protected static final String FOI_IDX_NAME = "foi";
	protected static final String GEOBOUNDS_IDX_NAME = "geobounds";
	protected static final String FOI_UNIQUE_ID_FIELD = "foiID";
	protected static final String SHAPE_FIELD_NAME = "geometry";
	protected Bbox foiExtent = new Bbox();
	protected static final String METADATA_TYPE_FOI = "foi";
	private GeometryFactory geometryFactory = new GeometryFactory();
	
	/**
	 * Class logger
	 */
	private static final Logger log = LoggerFactory.getLogger(ESObsStorageImpl.class);

	public ESObsStorageImpl() {
        // default constructor
    }
    
    public ESObsStorageImpl(RestHighLevelClient client) {
		super(client);
	}
	
	@Override
	public void start() throws SensorHubException {
	    super.start();
	    
	    // preload bbox
	    if (client != null) {
	    	try {
				GetResponse response = client.get(new GetRequest(indexNameMetaData, INDEX_METADATA_TYPE, GEOBOUNDS_IDX_NAME));
				if(response != null && response.getSource() != null && response.getSource().containsKey(BLOB_FIELD_NAME)) {
					foiExtent = getObject(response.getSource().get(BLOB_FIELD_NAME));
				}
			} catch (IOException ex) {
	    		log.error(ex.getLocalizedMessage(), ex);
			}
	    }
	}

	/**
	 * Transform the recordStorage data key into a DataKey by splitting <recordtype><SEPARATOR><timestamp><SEPARATOR><producerID>.
	 * @param rsKey the corresponding dataKey
	 * @return the dataKey. NULL if the length != 3 after splitting
	 */
	protected DataKey getDataKey(String rsKey, Map<String, Object> content) {

		DataKey dataKey = super.getDataKey(rsKey, content);

		return new ObsKey(dataKey.recordType, dataKey.producerID, (String) content.get(FOI_UNIQUE_ID_FIELD), dataKey.timeStamp);
	}

	/**
	 * Overload query builder in order to manage IObsFilter
	 * @param filter Filter results
	 * @return
	 */
	@Override
    BoolQueryBuilder queryByFilter(IDataFilter filter) {
	    BoolQueryBuilder queryBuilder = super.queryByFilter(filter);
	    if(filter instanceof ObsFilter) {
            IObsFilter obsFilter = (IObsFilter) filter;

            // filter on Foi
            if(obsFilter.getFoiIDs() != null && !obsFilter.getFoiIDs().isEmpty()) {
                queryBuilder.must(QueryBuilders.termsQuery(FOI_UNIQUE_ID_FIELD, obsFilter.getFoiIDs()));
            }

            // filter on roi?
            if (obsFilter.getRoi() != null) {
                try {
                    // build geo query
                    queryBuilder.must(getPolygonGeoQuery(obsFilter.getRoi()));
                } catch (IOException e) {
                    log.error(POLYGON_QUERY_ERROR_MSG, e);
                }
            }
        }
        return queryBuilder;
    }

    BoolQueryBuilder queryByFilter(IFoiFilter filter) {
        BoolQueryBuilder query = QueryBuilders.boolQuery();

        if(config.filterByStorageId) {
            query.must(QueryBuilders.termQuery(STORAGE_ID_FIELD_NAME, config.id));
        }

        // check if any producerIDs
        if(filter.getProducerIDs() != null && !filter.getProducerIDs().isEmpty()) {
            query.must(QueryBuilders.termsQuery(ESDataStoreTemplate.PRODUCER_ID_FIELD_NAME, filter.getProducerIDs()));
        }


        // filter on Foi
        if(filter.getFeatureIDs() != null && !filter.getFeatureIDs().isEmpty()) {
            query.must(QueryBuilders.termsQuery("_id", filter.getFeatureIDs()));
        }

        // filter on roi?
        if (filter.getRoi() != null) {
            try {
                // build geo query
                query.must(getPolygonGeoQuery(filter.getRoi()));
            } catch (IOException e) {
                log.error(POLYGON_QUERY_ERROR_MSG, e);
            }
        }
        return query;
    }

	@Override
	public synchronized int getNumFois(IFoiFilter filter) {

	    int result = 0;

		BoolQueryBuilder queryBuilder = queryByFilter(filter);

        SearchRequest searchRequest = new SearchRequest(indexNameMetaData);
        searchRequest.source(new SearchSourceBuilder().size(0)
                .query(queryBuilder));
        try {
            SearchResponse response = client.search(searchRequest);
            try {
                result = Math.toIntExact(response.getHits().getTotalHits());
            } catch (ArithmeticException ex) {
                getLogger().error("Too many records");
                result = Integer.MAX_VALUE;
            }
        } catch (IOException | ElasticsearchStatusException ex) {
            log.error("getRecordStores failed", ex);
        }
		return result;
	}

	@Override
	public Bbox getFoisSpatialExtent() {
	    SearchRequest searchRequest = new SearchRequest(indexNameMetaData);
	    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
	    searchSourceBuilder.query(QueryBuilders.boolQuery());
        searchSourceBuilder.size(0);
        searchSourceBuilder.aggregation(AggregationBuilders.geoBounds("agg").field(SHAPE_FIELD_NAME));
	    searchRequest.source(searchSourceBuilder);
	    try {
            SearchResponse response = client.search(searchRequest);
            Object obj = response.getAggregations().asMap().get("agg");
            if(obj instanceof ParsedGeoBounds) {
                ParsedGeoBounds geoBounds = (ParsedGeoBounds) obj;
                if(geoBounds.bottomRight() != null && geoBounds.topLeft() != null) {
					foiExtent = new Bbox(Math.nextDown((float)geoBounds.topLeft().getLon()),
							Math.nextDown((float)geoBounds.bottomRight().getLat()), 0,
							Math.nextUp((float)geoBounds.bottomRight().getLon()),
							Math.nextUp((float)geoBounds.topLeft().getLat()), 0);
				}
            }
        } catch (IOException ex) {
	        log.error(ex.getLocalizedMessage(), ex);
        }
	    return foiExtent.copy();
	}

    @Override
    void createDataMappingFields(XContentBuilder builder) throws IOException {
        builder.startObject(SHAPE_FIELD_NAME);
        {
            builder.field("type", "geo_point");
        }
        builder.endObject();
        builder.startObject(FOI_UNIQUE_ID_FIELD);
        {
            builder.field("type", "keyword");
        }
        builder.endObject();
        super.createDataMappingFields(builder);
    }

    @Override
	void createMetaMappingProperties(XContentBuilder builder) throws IOException {
		builder.startObject(ESDataStoreTemplate.PRODUCER_ID_FIELD_NAME);
		{
			builder.field("type", "keyword");
		}
		builder.endObject();
		builder.startObject(SHAPE_FIELD_NAME);
		{
			builder.field("type", "geo_point");
		}
		builder.endObject();
		super.createMetaMappingProperties(builder);
	}

	/**
     * Parse shape returned by query result
     * @param source queryResult.getSourceAsString()
     * @return Shape instance
     * @throws IOException Issue with input data
     */
    static Shape parseResultSourceGeometry(String source) throws IOException {
        XContentParser parser = XContentHelper.createParser(NamedXContentRegistry.EMPTY,
                LoggingDeprecationHandler.INSTANCE, new BytesArray(source), XContentType.JSON);
        parser.nextToken();
        // Continue while we not found the geometry field
        while (parser.currentToken() != XContentParser.Token.FIELD_NAME ||
                !parser.currentName().equals(SHAPE_FIELD_NAME)) {
            parser.nextToken();
        }
        parser.nextToken(); // Go into field content
        ShapeBuilder shapeBuilder = ShapeParser.parse(parser);
        return shapeBuilder.buildS4J();
    }

    @Override
	public synchronized Iterator<String> getFoiIDs(IFoiFilter filter) {

		commit();

		// build query
		// aggregate queries
		BoolQueryBuilder filterQueryBuilder = queryByFilter(filter);

		SearchRequest searchRequest = new SearchRequest(indexNameMetaData);
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(filterQueryBuilder);
		if(filter.getRoi() != null) {
		    // ElasticSearch return false positive (only approximate bound box test)
            // We have to check before returning values
            searchSourceBuilder.fetchSource(SHAPE_FIELD_NAME, null);
        } else {
            searchSourceBuilder.fetchSource(false);
        }
		searchSourceBuilder.size(config.scrollFetchSize);
		searchSourceBuilder.sort(new FieldSortBuilder(ESDataStoreTemplate.TIMESTAMP_FIELD_NAME).order(SortOrder.ASC));
		searchRequest.source(searchSourceBuilder);
		searchRequest.scroll(TimeValue.timeValueMillis(config.scrollMaxDuration));

		final ESIterator searchHitsIterator = new ESIterator(client, searchRequest, TimeValue.timeValueMillis(config.scrollMaxDuration)); //max of scrollFetchSize hits will be returned for each scroll

		// build a IDataRecord iterator based on the searchHits iterator

		final Shape geomTest = filter.getRoi() == null ? null : getPolygonBuilder(filter.getRoi()).buildS4J();

		return new Iterator<String>() {
            String nextFeature = null;
            org.locationtech.jts.geom.GeometryFactory factory = new org.locationtech.jts.geom.GeometryFactory();

            @Override
            public boolean hasNext() {
                if(nextFeature == null && searchHitsIterator.hasNext()) {
                    fetchNext();
                }
                return nextFeature != null;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            void fetchNext() {
                nextFeature = null;
                if(searchHitsIterator.hasNext()) {
                    SearchHit nextSearchHit = searchHitsIterator.next();
                    if (geomTest != null) {
                        try {
                            do {
                                // Extract shape from the geometry field using ES shape parser
                                //Shape geom = ESObsStorageImpl.parseResultSourceGeometry(nextSearchHit.getSourceAsString());
                                Map pt = (Map)nextSearchHit.getSourceAsMap().get(SHAPE_FIELD_NAME);
                                Shape geom = new JtsPoint(factory.createPoint(new org.locationtech.jts.geom.Coordinate(((Number)pt.get("lon")).doubleValue(),((Number)pt.get("lat")).doubleValue())), JtsSpatialContext.GEO);
                                if (geom.relate(geomTest).intersects()) {
                                    break;
                                }
                                if (!searchHitsIterator.hasNext()) {
                                    return;
                                }
                                nextSearchHit = searchHitsIterator.next();
                            } while (true);
                        } catch (Exception ex) {
                            log.error(ex.getLocalizedMessage(), ex);
                        }
                    }
                    nextFeature = nextSearchHit.getId();
                }
            }

            @Override
            public String next() {
                String ret = nextFeature;
                fetchNext();
                return ret;
            }
		};
//
//		// build query
//		// aggregate queries
//		BoolQueryBuilder filterQueryBuilder = QueryBuilders.boolQuery();
//		// filter on feature ids?
//		if(filter.getFeatureIDs() != null && !filter.getFeatureIDs().isEmpty()) {
//			filterQueryBuilder.must(QueryBuilders.termsQuery(FOI_UNIQUE_ID_FIELD, filter.getFeatureIDs()));
//		}
//
//		// filter on producer ids?
//		if(filter.getProducerIDs() != null && !filter.getProducerIDs().isEmpty()) {
//			filterQueryBuilder.must(QueryBuilders.termsQuery(PRODUCER_ID_FIELD_NAME, filter.getProducerIDs()));
//		}
//
//		// filter on ROI?
//		if (filter.getRoi() != null) {
//			try {
//				// build geo query
//				filterQueryBuilder.must(getPolygonGeoQuery(filter.getRoi()));
//			} catch (IOException e) {
//				log.error(POLYGON_QUERY_ERROR_MSG, e);
//			}
//		}
//
//		final SearchRequestBuilder scrollReq = client.prepareSearch(indexNamePrepend)
//				.setTypes("_doc")
//				.setQuery(filterQueryBuilder)
//				 // get only the id
//				.setFetchSource(new String[] { FOI_UNIQUE_ID_FIELD }, new String[] {})
//				.setScroll(new TimeValue(config.scrollMaxDuration));
//		// wrap the request into custom ES Scroll iterator
//		final Iterator<SearchHit> searchHitsIterator = new ESIterator(client, scrollReq, config.scrollFetchSize);
//
//		return new Iterator<String>() {
//
//			@Override
//			public boolean hasNext() {
//				return searchHitsIterator.hasNext();
//			}
//
//			@Override
//			public void remove() {
//
//			}
//
//			@Override
//			public String next() {
//				SearchHit nextSearchHit = searchHitsIterator.next();
//				// get Feature id
//				return nextSearchHit.getSourceAsMap().get(FOI_UNIQUE_ID_FIELD).toString();
//			}
//		};
	}

	@Override
	public synchronized Iterator<AbstractFeature> getFois(IFoiFilter filter) {

		commit();

		// build query
		// aggregate queries
		BoolQueryBuilder filterQueryBuilder = QueryBuilders.boolQuery();


        if(config.filterByStorageId) {
            filterQueryBuilder.must(QueryBuilders.termQuery(STORAGE_ID_FIELD_NAME, config.id));
        }

		// filter on producer ids?
		if(filter.getProducerIDs() != null && !filter.getProducerIDs().isEmpty()) {
			filterQueryBuilder.must(QueryBuilders.termsQuery(ESDataStoreTemplate.PRODUCER_ID_FIELD_NAME, filter.getProducerIDs()));
		}

		if(filter.getFeatureIDs() != null && !filter.getFeatureIDs().isEmpty()) {
			filterQueryBuilder.must(QueryBuilders.termsQuery("_id" ,filter.getFeatureIDs().toArray(new String[filter.getFeatureIDs().size()])));
		}

		// filter on roi?
		if (filter.getRoi() != null) {
			try {
				// build geo query
				filterQueryBuilder.must(getPolygonGeoQuery(filter.getRoi()));
			} catch (IOException e) {
				log.error(POLYGON_QUERY_ERROR_MSG, e);
			}
		}

        final Shape geomTest = filter.getRoi() == null ? null : getPolygonBuilder(filter.getRoi()).buildS4J();

		SearchRequest searchRequest = new SearchRequest(indexNameMetaData);
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(filterQueryBuilder);
		searchSourceBuilder.size(config.scrollFetchSize);
		searchSourceBuilder.sort(new FieldSortBuilder(ESDataStoreTemplate.TIMESTAMP_FIELD_NAME).order(SortOrder.ASC));
		searchRequest.source(searchSourceBuilder);
		searchRequest.scroll(TimeValue.timeValueMillis(config.scrollMaxDuration));

		final ESIterator searchHitsIterator = new ESIterator(client, searchRequest, TimeValue.timeValueMillis(config.scrollMaxDuration)); //max of scrollFetchSize hits will be returned for each scroll

		// build a IDataRecord iterator based on the searchHits iterator

		return new AbstractFeatureIterator(searchHitsIterator, geomTest);
	}

    void storeRecordIndexRequestFields(XContentBuilder builder, DataKey key) throws IOException {
        if((key instanceof ObsKey)) {
            ObsKey obsKey = (ObsKey) key;


			// obs part
			if(!Double.isNaN(obsKey.resultTime)) {
			    builder.timeField(RESULT_TIME_FIELD_NAME, ESDataStoreTemplate.toEpochMillisecond(obsKey.resultTime));
			}

			if(obsKey.foiID != null) {
			    builder.field(FOI_UNIQUE_ID_FIELD, obsKey.foiID);
			}

			if(obsKey.samplingGeometry != null) {
                Point centroid = obsKey.samplingGeometry.getCentroid();
                builder.startObject(SHAPE_FIELD_NAME);
                {
                    builder.field("lat", centroid.getY());
                    builder.field("lon", centroid.getX());
                }
                builder.endObject();
			}
        }
        super.storeRecordIndexRequestFields(builder, key);
    }

	
	@Override
	public synchronized void storeFoi(String producerID, AbstractFeature foi) {
		log.info("ESObsStorageImpl:storeFoi");

		// add new record storage
		byte[] bytes = this.getBlob(foi);

		try {
			XContentBuilder builder = XContentFactory.jsonBuilder();
			builder.startObject();
			{
				// Convert to elastic search epoch millisecond
				builder.field(STORAGE_ID_FIELD_NAME, config.id);
				builder.field(METADATA_TYPE_FIELD_NAME, METADATA_TYPE_FOI);
				Point centroid = getCentroid(foi.getLocation());
				builder.startObject(SHAPE_FIELD_NAME);
                {
                    builder.field("lat", centroid.getY());
                    builder.field("lon", centroid.getX());
                }
                builder.endObject();
				builder.field(ESDataStoreTemplate.PRODUCER_ID_FIELD_NAME, producerID);
				builder.field(ESDataStoreTemplate.TIMESTAMP_FIELD_NAME, System.currentTimeMillis());
				builder.field(BLOB_FIELD_NAME, bytes);
			}
			builder.endObject();
			IndexRequest request = new IndexRequest(indexNameMetaData, INDEX_METADATA_TYPE, foi.getUniqueIdentifier());

			request.source(builder);

			client.index(request);

			storeChanged = System.currentTimeMillis();

		} catch (IOException | SensorHubException ex) {
			getLogger().error(String.format("storeFoi exception %s:%s in elastic search driver",producerID, foi.getName()), ex);
		}
	}

	/**
     * Gets the envelope builder from a bbox object.
     * @param bbox the bbox
     * @returnthe Envelope builder
     */
    protected synchronized EnvelopeBuilder getEnvelopeBuilder(Bbox bbox) {
		org.locationtech.jts.geom.Coordinate topLeft = new org.locationtech.jts.geom.Coordinate(bbox.getMinX(), bbox.getMaxY());
		org.locationtech.jts.geom.Coordinate btmRight = new org.locationtech.jts.geom.Coordinate(bbox.getMaxX(), bbox.getMinY());
        return new EnvelopeBuilder(topLeft, btmRight);
    }

	/**
	 * Gets the envelope builder from envelope geometry.
	 * @param env the envelope geometry
	 * @returnthe Envelope builder
	 */
	protected synchronized EnvelopeBuilder getEnvelopeBuilder(Envelope env) {
		org.locationtech.jts.geom.Coordinate topLeft = new org.locationtech.jts.geom.Coordinate(env.getMinX(), env.getMaxY());
		org.locationtech.jts.geom.Coordinate btmRight = new org.locationtech.jts.geom.Coordinate(env.getMaxX(), env.getMinY());
        return new EnvelopeBuilder(topLeft, btmRight);
	}

	/**
	 * Build a polygon builder from a polygon geometry.
	 * @param polygon the Polygon geometry
	 * @return the builder
	 */
	protected synchronized PolygonBuilder getPolygonBuilder(Polygon polygon) {
		// get coordinates list from polygon
		CoordinatesBuilder coordinates = new CoordinatesBuilder();
		for(Coordinate coordinate : polygon.getExteriorRing().getCoordinates()) {
            double x = coordinate.x;
            double y = coordinate.y;
            // Handle out of bounds points
            if(x < -180 || x > 180) {
                x = -180 + x % 180;
                log.warn("Point %f,%f out of bounds",x,y);
            }
            if(y < -90 || y > 90) {
                y = -90 + y % 90;
                log.warn("Point %f,%f out of bounds",x,y);
            }
			coordinates.coordinate(x, y);
		}
		// build shape builder from coordinates
		return new PolygonBuilder(coordinates);
		//.relation(ShapeRelation.WITHIN); strategy, Default is INTERSECT
	}

	/**
	 * Build a point builder from a point geometry.
	 * @param point the Point geometry
	 * @return the builder
	 */
	protected synchronized PointBuilder getPointBuilder(Point point) {
		// build shape builder from coordinates
        double x = point.getX();
        double y = point.getY();
        // Handle out of bounds points
        if(x < -180 || x > 180) {
            x = -180 + x % 180;
            log.warn("Point %f,%f out of bounds",x,y);
        }
        if(y < -90 || y > 90) {
            y = -90 + y % 90;
            log.warn("Point %f,%f out of bounds",x,y);
        }
		return new PointBuilder(x, y);
	}

	Point getCentroid(AbstractGeometry geometry) throws SensorHubException {
        if(geometry instanceof PolygonJTS) {
            return ((PolygonJTS)geometry).getCentroid();
        } else if(geometry instanceof PointJTS) {
            return  (PointJTS)geometry;
        } else if(geometry instanceof EnvelopeJTS) {
            return geometryFactory.createPoint(((Envelope)geometry).centre());
        } else {
            throw new SensorHubException("Unsupported Geometry exception: "+geometry.getClass());
        }
    }

	/**
	 * Build the corresponding builder given a generic geometry.
	 * @param geometry The abstract geometry
	 * @return the corresponding builder. The current supported builder are: PolygonJTS, Point, EnvelopeJTS
	 * @throws SensorHubException if the geometry is not supported
	 */
	protected synchronized ShapeBuilder getShapeBuilder(AbstractGeometry geometry) throws SensorHubException {
		if(geometry instanceof PolygonJTS) {
			return getPolygonBuilder((PolygonJTS)geometry);
		} else if(geometry instanceof PointJTS) {
			return getPointBuilder((PointJTS)geometry);
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
	    Envelope envelope = polygon.getEnvelopeInternal();
		return QueryBuilders.geoBoundingBoxQuery(SHAPE_FIELD_NAME).setCorners(envelope.getMaxY(), envelope.getMinX(), envelope.getMinY(), envelope.getMaxX());
	}

	static final class AbstractFeatureIterator implements Iterator<AbstractFeature> {
        ESIterator searchHitsIterator;
        Shape geomTest;
        AbstractFeature nextFeature = null;
        org.locationtech.jts.geom.GeometryFactory factory = new org.locationtech.jts.geom.GeometryFactory();

        public AbstractFeatureIterator(ESIterator searchHitsIterator, Shape geomTest) {
            this.searchHitsIterator = searchHitsIterator;
            this.geomTest = geomTest;
        }

        @Override
        public boolean hasNext() {
            if(nextFeature == null && searchHitsIterator.hasNext()) {
                fetchNext();
            }
            return nextFeature != null;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        void fetchNext() {
            nextFeature = null;
            if(searchHitsIterator.hasNext()) {
                SearchHit nextSearchHit = searchHitsIterator.next();
                if (geomTest != null) {
                    try {
                        do {
                            // Extract shape from the geometry field using ES shape parser
                            //Shape geom = ESObsStorageImpl.parseResultSourceGeometry(nextSearchHit.getSourceAsString());

                            Map pt = (Map)nextSearchHit.getSourceAsMap().get(SHAPE_FIELD_NAME);
                            Shape geom = new JtsPoint(factory.createPoint(new org.locationtech.jts.geom.Coordinate(((Number)pt.get("lon")).doubleValue(),((Number)pt.get("lat")).doubleValue())), JtsSpatialContext.GEO);

                            if (geom.relate(geomTest).intersects()) {
                                break;
                            }
                            if (!searchHitsIterator.hasNext()) {
                                return;
                            }
                            nextSearchHit = searchHitsIterator.next();
                        } while (true);
                    } catch (Exception ex) {
                        log.error(ex.getLocalizedMessage(), ex);
                    }
                }
                nextFeature = ESObsStorageImpl.getObject(nextSearchHit.getSourceAsMap().get(BLOB_FIELD_NAME));
            }
        }

        @Override
        public AbstractFeature next() {
            AbstractFeature ret = nextFeature;
            fetchNext();
            return ret;
        }

    }
}
