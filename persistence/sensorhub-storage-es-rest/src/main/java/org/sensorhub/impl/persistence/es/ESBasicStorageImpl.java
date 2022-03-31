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

import net.opengis.gml.v32.AbstractTimeGeometricPrimitive;
import net.opengis.gml.v32.TimeInstant;
import net.opengis.gml.v32.TimePeriod;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.*;
import net.opengis.swe.v20.Vector;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.CompressedClient;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.IObsStorage;
import org.sensorhub.api.persistence.IRecordStorageModule;
import org.sensorhub.api.persistence.IStorageModule;
import org.sensorhub.api.persistence.StorageException;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.persistence.StorageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.ScalarIndexer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Boolean;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.emptySet;

/**
 * <p>
 * ES implementation of {@link IObsStorage} for storing observations.
 * This class is Thread-safe.
 * </p>
 *
 * @author Mathieu Dhainaut <mathieu.dhainaut@gmail.com>
 * @author Nicolas Fortin, UMRAE Ifsttar
 * @since 2017
 */
public class ESBasicStorageImpl extends AbstractModule<ESBasicStorageConfig> implements IRecordStorageModule<ESBasicStorageConfig> {
	private static final int TIME_RANGE_CLUSTER_SCROLL_FETCH_SIZE = 5000;

	protected static final double MAX_TIME_CLUSTER_DELTA = 60.0;

	private static final int DELAY_RETRY_START = 10000;

	// ms .Fetch again record store map if it is done at least this time
	private static final int RECORD_STORE_CACHE_LIFETIME = 5000;

	// From ElasticSearch v6, multiple index type is not supported
    //                    v7 index type dropped
    protected static final String INDEX_METADATA_TYPE = "osh_metadata";

    protected static final String STORAGE_ID_FIELD_NAME = "storageID";

    // The data index, serialization of OpenSensorHub internals metadata
    protected static final String METADATA_TYPE_FIELD_NAME = "metadataType";

	protected static final String DATA_INDEX_FIELD_NAME = "index";

	protected static final String RS_KEY_SEPARATOR = "##";

	protected static final String BLOB_FIELD_NAME = "blob";

	private static final int WAIT_TIME_AFTER_COMMIT = 1000;

	public static final String Z_FIELD = "_height";

	// Last time the store has been changed, may require waiting if data changed since less than a second
	long storeChanged = 0;

	private List<String> addedIndex = new ArrayList<>();

    private Map<String, EsRecordStoreInfo> recordStoreCache = new HashMap<>();

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
	RestHighLevelClient client;

	private BulkProcessor bulkProcessor;

	// Kinds of OpenSensorHub serialized objects
	protected static final String METADATA_TYPE_DESCRIPTION = "desc";
	protected static final String METADATA_TYPE_RECORD_STORE = "info";

	String indexNamePrepend;
	String indexNameMetaData;

    BulkListener bulkListener;

	
	public ESBasicStorageImpl() {
	    // default constructor
    }
	
	public ESBasicStorageImpl(RestHighLevelClient client) {
		this.client = client;
	}
	
	@Override
	public void backup(OutputStream os) throws IOException {
		throw new UnsupportedOperationException("Backup");
	}

    public RestHighLevelClient getClient() {
        return client;
    }

    @Override
	public void restore(InputStream is) throws IOException {
		throw new UnsupportedOperationException("Restore");
	}

	@Override
	public void commit() {
		refreshIndex();
        // https://www.elastic.co/guide/en/elasticsearch/guide/current/near-real-time.html
        // document changes are not visible to search immediately, but will become visible within 1 second.
        long now = System.currentTimeMillis();
        if(now - storeChanged < WAIT_TIME_AFTER_COMMIT) {
            try {
                Thread.sleep(WAIT_TIME_AFTER_COMMIT);
            } catch (InterruptedException ignored) {
            }
        }
	}

	@Override
	public void rollback() {
		throw new UnsupportedOperationException("Rollback");
	}

	@Override
	public void sync(IStorageModule<?> storage) throws StorageException {
		throw new UnsupportedOperationException("Storage Sync");
	}
	
	@Override
	public void init() {
	    this.indexNamePrepend = (config.indexNamePrepend != null) ? config.indexNamePrepend : "";
	    this.indexNameMetaData = (config.indexNameMetaData != null && !config.indexNameMetaData.isEmpty()) ? config.indexNameMetaData : ESBasicStorageConfig.DEFAULT_INDEX_NAME_METADATA;
	}

	@Override
	public synchronized void start() throws SensorHubException {
	    log.info("ESBasicStorageImpl:start");

        // init transport client
        HttpHost[] hosts = new HttpHost[config.nodeUrls.size()];
        boolean foundOneHost = false;
        while(!foundOneHost) {
            int i=0;
            for (String nodeUrl : config.nodeUrls) {
                long tryStart = System.currentTimeMillis();
                int retry = 0;
                while (true) {
                    try {
                        URL url = null;
                        // <host>:<port>
                        if (nodeUrl.startsWith("http")) {
                            url = new URL(nodeUrl);
                        } else {
                            url = new URL("http://" + nodeUrl);
                        }

                        hosts[i++] = new HttpHost(InetAddress.getByName(url.getHost()), url.getPort(), url.getProtocol());
                        foundOneHost = true;
                        break;
                    } catch (MalformedURLException | UnknownHostException e) {
                        retry++;
                        if (retry < config.maxBulkRetry) {
                            try {
                                Thread.sleep(Math.max(1000, config.connectTimeout - (System.currentTimeMillis() - tryStart)));
                            } catch (InterruptedException ex) {
                                throw new SensorHubException("Cannot initialize transport address", e);
                            }
                        } else {
                            getLogger().error(String.format("Cannot initialize transport address after %d retries", retry), e);
                            break;
                        }
                    }
                }
            }
            if(!foundOneHost) {
                try {
                    Thread.sleep(DELAY_RETRY_START);
                } catch (InterruptedException ex) {
                    throw new SensorHubException("Cannot initialize transport address", ex);
                }
            }
        }

        RestClientBuilder restClientBuilder = RestClient.builder(hosts);

        // Handle authentication
        restClientBuilder.setHttpClientConfigCallback(httpClientBuilder -> {
                    if(!config.user.isEmpty()) {
                        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                        credentialsProvider.setCredentials(AuthScope.ANY,
                                new UsernamePasswordCredentials(config.user, config.password));
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                    if(!config.certificatesPath.isEmpty()) {

                        try {
                            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                            Path ksPath = Paths.get(System.getProperty("java.home"),
                                    "lib", "security", "cacerts");
                            if(Files.exists(ksPath)) {
                                keyStore.load(Files.newInputStream(ksPath),
                                        "changeit".toCharArray());
                            }

                            CertificateFactory cf = CertificateFactory.getInstance("X.509");
                            for(String filePath : config.certificatesPath) {
                                File file = new File(filePath);
                                if(file.exists()) {
                                    try (InputStream caInput = new BufferedInputStream(
                                            // this files is shipped with the application
                                            new FileInputStream(file))) {
                                        Certificate crt = cf.generateCertificate(caInput);
                                        getLogger().info("Added Cert for " + ((X509Certificate) crt)
                                                .getSubjectDN());

                                        keyStore.setCertificateEntry(file.getName(), crt);
                                    }
                                } else {
                                    getLogger().warn("Could not find certificate " + filePath);
                                }
                            }
                            TrustManagerFactory tmf = TrustManagerFactory
                                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());
                            tmf.init(keyStore);
                            SSLContext sslContext = SSLContext.getInstance("TLS");
                            sslContext.init(null, tmf.getTrustManagers(), null);
                            httpClientBuilder.setSSLContext(sslContext);
                        } catch (Exception e) {
                            getLogger().error(e.getLocalizedMessage(), e);
                        }
                    }
                    return httpClientBuilder;
                }
        );

        restClientBuilder.setRequestConfigCallback(new RestClientBuilder.RequestConfigCallback() {
            @Override
            public RequestConfig.Builder customizeRequestConfig(RequestConfig.Builder requestConfigBuilder) {
                return requestConfigBuilder.setConnectTimeout(config.connectTimeout).setSocketTimeout(config.socketTimeout);
            }
        });

        restClientBuilder.setMaxRetryTimeoutMillis(config.maxRetryTimeout);

        client = new CompressedClient(restClientBuilder);

		bulkListener = new BulkListener(client, config.maxBulkRetry, config.scrollMaxDuration);

        bulkProcessor = BulkProcessor.builder(client instanceof CompressedClient ? ((CompressedClient)client)::bulkCompressedAsync : client::bulkAsync, bulkListener).setBulkActions(config.bulkActions)
                .setBulkSize(new ByteSizeValue(config.bulkSize, ByteSizeUnit.MB))
                .setFlushInterval(TimeValue.timeValueSeconds(config.bulkFlushInterval))
                .setConcurrentRequests(config.bulkConcurrentRequests)
                .build();

		// Check if metadata mapping must be defined
        GetIndexRequest getIndexRequest = new GetIndexRequest();
        getIndexRequest.indices(indexNameMetaData);
        try {
            if(!client.indices().exists(getIndexRequest)) {
                createMetaMapping();
            }
        } catch (IOException ex) {
            log.error("Cannot create metadata mapping", ex);
        }

        // Retrieve store info

        initStoreInfo();
	}

	void createMetaMappingProperties(XContentBuilder builder) throws IOException {
        builder.startObject(STORAGE_ID_FIELD_NAME);
        {
            builder.field("type", "keyword");
        }
        builder.endObject();
        builder.startObject(METADATA_TYPE_FIELD_NAME);
        {
            builder.field("type", "keyword");
        }
        builder.endObject();
        builder.startObject(DATA_INDEX_FIELD_NAME);
        {
            builder.field("type", "keyword");
        }
        builder.endObject();
        builder.startObject(ESDataStoreTemplate.TIMESTAMP_FIELD_NAME);
        {
            builder.field("type", "date");
            builder.field("format", "epoch_millis");
        }
        builder.endObject();
        builder.startObject(BLOB_FIELD_NAME);
        {
            builder.field("type", "binary");
        }
        builder.endObject();
    }

	void createMetaMapping () throws IOException {
		// create the index
	    CreateIndexRequest indexRequest = new CreateIndexRequest(indexNameMetaData);
        XContentBuilder builder = XContentFactory.jsonBuilder();

        builder.startObject();
        {
            builder.startObject(INDEX_METADATA_TYPE);
            {
                builder.startObject("properties");
                {
                    createMetaMappingProperties(builder);
                }
                builder.endObject();
            }
            builder.endObject();
        }
        builder.endObject();

	    indexRequest.mapping(INDEX_METADATA_TYPE, builder);

	    client.indices().create(indexRequest);

        addedIndex.add(indexNameMetaData);
	}

    @Override
	public synchronized void stop() throws SensorHubException {
        log.info("ESBasicStorageImpl:stop");
		if(client != null) {
		    try {
                client.close();
            } catch (IOException ex) {
		        throw new SensorHubException(ex.getLocalizedMessage(), ex);
            }
		}
	}

	@Override
	public AbstractProcess getLatestDataSourceDescription() {
        AbstractProcess result = null;
        SearchRequest searchRequest = new SearchRequest(indexNameMetaData);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder query = QueryBuilders.boolQuery()
                .must(new TermQueryBuilder(METADATA_TYPE_FIELD_NAME, METADATA_TYPE_DESCRIPTION));
        if(config.filterByStorageId) {
            query.must(QueryBuilders.termQuery(STORAGE_ID_FIELD_NAME, config.id));
        }
        searchSourceBuilder.query(query);
        searchSourceBuilder.sort(new FieldSortBuilder(ESDataStoreTemplate.TIMESTAMP_FIELD_NAME).order(SortOrder.DESC));
        searchSourceBuilder.size(1);
        searchRequest.source(searchSourceBuilder);

        try {
            SearchResponse response = client.search(searchRequest);
       		if(response.getHits().getTotalHits() > 0) {
                result = this.getObject(response.getHits().getAt(0).getSourceAsMap().get(BLOB_FIELD_NAME));
            }
        } catch (IOException | ElasticsearchStatusException ex) {
            log.error("getRecordStores failed", ex);
        }

        return result;
	}

	@Override
	public List<AbstractProcess> getDataSourceDescriptionHistory(double startTime, double endTime) {
        List<AbstractProcess> results = new ArrayList<>();

        SearchRequest searchRequest = new SearchRequest(indexNameMetaData);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder query = QueryBuilders.boolQuery()
                .must(new TermQueryBuilder(METADATA_TYPE_FIELD_NAME, METADATA_TYPE_DESCRIPTION))
                .must(new RangeQueryBuilder(ESDataStoreTemplate.TIMESTAMP_FIELD_NAME).from(ESDataStoreTemplate.toEpochMillisecond(startTime)).to(ESDataStoreTemplate.toEpochMillisecond(endTime)).format("epoch_millis"));


        if(config.filterByStorageId) {
            query.must(QueryBuilders.termQuery(STORAGE_ID_FIELD_NAME, config.id));
        }

        searchSourceBuilder.query(query);
        searchSourceBuilder.sort(new FieldSortBuilder(ESDataStoreTemplate.TIMESTAMP_FIELD_NAME).order(SortOrder.DESC));
        searchSourceBuilder.size(config.scrollFetchSize);
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(TimeValue.timeValueMillis(config.scrollMaxDuration));
        try {
            SearchResponse response = client.search(searchRequest);
            do {
                String scrollId = response.getScrollId();
                for (SearchHit hit : response.getHits()) {
                    results.add(this.getObject(hit.getSourceAsMap().get(BLOB_FIELD_NAME)));
                }
                if (response.getHits().getHits().length > 0) {
                    SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                    scrollRequest.scroll(TimeValue.timeValueMillis(config.scrollMaxDuration));
                    response = client.searchScroll(scrollRequest);
                }
            } while (response.getHits().getHits().length > 0);

        } catch (IOException | ElasticsearchStatusException ex) {
            log.error("getRecordStores failed", ex);
        }

        return results;
	}

	@Override
	public AbstractProcess getDataSourceDescriptionAtTime(double time) {


        AbstractProcess result = null;
        SearchRequest searchRequest = new SearchRequest(indexNameMetaData);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder query = QueryBuilders.boolQuery()
                .must(new TermQueryBuilder(METADATA_TYPE_FIELD_NAME, METADATA_TYPE_DESCRIPTION))
                .must(new RangeQueryBuilder(ESDataStoreTemplate.TIMESTAMP_FIELD_NAME).from(0).to(Double.valueOf(time * 1000).longValue()));


        if(config.filterByStorageId) {
            query.must(QueryBuilders.termQuery(STORAGE_ID_FIELD_NAME, config.id));
        }

        searchSourceBuilder.query(query);
        searchSourceBuilder.sort(new FieldSortBuilder(ESDataStoreTemplate.TIMESTAMP_FIELD_NAME).order(SortOrder.DESC));
        searchSourceBuilder.size(1);
        searchRequest.source(searchSourceBuilder);

        try {
            SearchResponse response = client.search(searchRequest);
            if(response.getHits().getTotalHits() > 0) {
                result = this.getObject(response.getHits().getAt(0).getSourceAsMap().get(BLOB_FIELD_NAME));
            }
        } catch (IOException | ElasticsearchStatusException ex) {
            log.error("getRecordStores failed", ex);
        }

        return result;
	}

	protected boolean storeDataSourceDescription(AbstractProcess process, double time, boolean update) {
        // add new record storage
        byte[] bytes = this.getBlob(process);

        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            // Convert to elastic search epoch millisecond
            long epoch = ESDataStoreTemplate.toEpochMillisecond(time);
            builder.startObject();
            {
                builder.field(STORAGE_ID_FIELD_NAME, config.id);
                builder.field(METADATA_TYPE_FIELD_NAME, METADATA_TYPE_DESCRIPTION);
                builder.field(DATA_INDEX_FIELD_NAME, "");
                builder.field(ESDataStoreTemplate.TIMESTAMP_FIELD_NAME, epoch);
                builder.field(BLOB_FIELD_NAME, bytes);
            }
            builder.endObject();

            IndexRequest request = new IndexRequest(indexNameMetaData, INDEX_METADATA_TYPE, config.id + "_" + epoch);

            request.source(builder);

            bulkProcessor.add(request);

        } catch (IOException ex) {
            getLogger().error(String.format("storeDataSourceDescription exception %s in elastic search driver", process.getId()), ex);
        }
        return true;
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
        long epoch = ESDataStoreTemplate.toEpochMillisecond(time);
		DeleteRequest deleteRequest = new DeleteRequest(indexNameMetaData, METADATA_TYPE_DESCRIPTION, config.id + "_" + epoch);
		bulkProcessor.add(deleteRequest);

		storeChanged = System.currentTimeMillis();
	}

	@Override
	public void removeDataSourceDescriptionHistory(double startTime, double endTime) {
        try {
            // Delete by query, currently not supported by High Level Api
            BoolQueryBuilder query = QueryBuilders.boolQuery().must(
                    QueryBuilders.termQuery(METADATA_TYPE_FIELD_NAME, METADATA_TYPE_DESCRIPTION))
                    .must(new RangeQueryBuilder(ESDataStoreTemplate.TIMESTAMP_FIELD_NAME)
                            .from(ESDataStoreTemplate.toEpochMillisecond(startTime))
                            .to(ESDataStoreTemplate.toEpochMillisecond(endTime)).format("epoch_millis"));

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            XContentBuilder builder = XContentFactory.jsonBuilder(bos);
            builder.startObject();
            builder.rawField("query", new ByteArrayInputStream(query.toString().getBytes(StandardCharsets.UTF_8)), XContentType.JSON);
            builder.endObject();
            builder.flush();
            String json = bos.toString("UTF-8");
            HttpEntity entity = new NStringEntity(json, ContentType.APPLICATION_JSON);
            client.getLowLevelClient().performRequest("POST"
                    , encodeEndPoint(indexNameMetaData, "_delete_by_query")
                    , Collections.EMPTY_MAP, entity);

            storeChanged = System.currentTimeMillis();

        } catch (IOException ex) {
            log.error("Failed to removeRecords", ex);
        }
	}

	void initStoreInfo() {

        Map<String, EsRecordStoreInfo> result = new HashMap<>();

        SearchRequest searchRequest = new SearchRequest(indexNameMetaData);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder query = QueryBuilders.boolQuery()
                .must(new TermQueryBuilder(METADATA_TYPE_FIELD_NAME, METADATA_TYPE_RECORD_STORE));


        if(config.filterByStorageId) {
            query.must(QueryBuilders.termQuery(STORAGE_ID_FIELD_NAME, config.id));
        }

        searchSourceBuilder.query(query);
        // Default to 10 results
        searchSourceBuilder.size(config.scrollFetchSize);
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(TimeValue.timeValueMillis(config.scrollMaxDuration));
        try {
            SearchResponse response = client.search(searchRequest);
            do {
                String scrollId = response.getScrollId();
                for (SearchHit hit : response.getHits()) {
                    Map<String, Object> dataMap = hit.getSourceAsMap();
                    EsRecordStoreInfo rsInfo = getObject(dataMap.get(BLOB_FIELD_NAME)); // DataStreamInfo
                    result.put(rsInfo.getName(), rsInfo);
                }
                if (response.getHits().getHits().length > 0) {
                    SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                    scrollRequest.scroll(TimeValue.timeValueMillis(config.scrollMaxDuration));
                    response = client.searchScroll(scrollRequest);
                }
            } while (response.getHits().getHits().length > 0);

        } catch (IOException | ElasticsearchStatusException ex) {
            log.error("getRecordStores failed", ex);
        }

        recordStoreCache = result;
    }

	@Override
	public  Map<String, EsRecordStoreInfo> getRecordStores() {
        return recordStoreCache;
	}

    /**
     * Index in Elastic Search are restricted
     * @param indexName Index name to remove undesired chars
     * @return valid index for es
     */
	String fixIndexName(String indexName) {
        for(Character chr : Strings.INVALID_FILENAME_CHARS) {
            indexName = indexName.replace(chr.toString(), "");
        }
        indexName = indexName.replace("#", "");
        while(indexName.startsWith("_") || indexName.startsWith("-") || indexName.startsWith("+")) {
            indexName = indexName.substring(1, indexName.length());
        }
        return indexName.toLowerCase(Locale.ROOT);
    }

	@Override
	public void addRecordStore(String name, DataComponent recordStructure, DataEncoding recommendedEncoding) {
        EsRecordStoreInfo rsInfo = new EsRecordStoreInfo(name,fixIndexName(indexNamePrepend + recordStructure.getName()),
                recordStructure, recommendedEncoding);

        recordStoreCache.put(rsInfo.name, rsInfo);

		// add new record storage
		byte[] bytes = this.getBlob(rsInfo);

        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            {
                // Convert to elastic search epoch millisecond
                builder.field(STORAGE_ID_FIELD_NAME, config.id);
                builder.field(METADATA_TYPE_FIELD_NAME, METADATA_TYPE_RECORD_STORE);
                builder.field(DATA_INDEX_FIELD_NAME, rsInfo.getIndexName()); // store recordType
                builder.field(ESDataStoreTemplate.TIMESTAMP_FIELD_NAME, System.currentTimeMillis());
                builder.field(BLOB_FIELD_NAME, bytes);
            }
            builder.endObject();
            IndexRequest request = new IndexRequest(indexNameMetaData, INDEX_METADATA_TYPE);

            request.source(builder);

            client.index(request);

            // Check if metadata mapping must be defined
            GetIndexRequest getIndexRequest = new GetIndexRequest();
            getIndexRequest.indices(rsInfo.indexName);
            try {
                if(!client.indices().exists(getIndexRequest)) {
                    createDataMapping(rsInfo);
                }
            } catch (IOException ex) {
                getLogger().error("Cannot create metadata mapping", ex);
            }

            storeChanged = System.currentTimeMillis();

            refreshIndex();


        } catch (IOException ex) {
            getLogger().error(String.format("addRecordStore exception %s:%s in elastic search driver",name, recordStructure.getName()), ex);
        }
	}

	@Override
	public int getNumRecords(String recordType) {
	    commit();

        Map<String, EsRecordStoreInfo>  recordStoreInfoMap = getRecordStores();
        EsRecordStoreInfo info = recordStoreInfoMap.get(recordType);
        if(info != null) {
            SearchRequest searchRequest = new SearchRequest(info.indexName);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().size(0);
            searchRequest.source(searchSourceBuilder);
            if(config.filterByStorageId) {
                searchSourceBuilder.query(new BoolQueryBuilder().must(QueryBuilders.termQuery(STORAGE_ID_FIELD_NAME, config.id)));
            }

            try {
                SearchResponse response = client.search(searchRequest);
                try {
                    return Math.toIntExact(response.getHits().getTotalHits());
                } catch (ArithmeticException ex) {
                    getLogger().error("Too many records");
                    return Integer.MAX_VALUE;
                }
            } catch (IOException | ElasticsearchStatusException ex) {
                log.error("getRecordStores failed", ex);
            }
        }
        return 0;
	}

	@Override  
	public synchronized double[] getRecordsTimeRange(String recordType) {
		double[] result = new double[2];

        Map<String, EsRecordStoreInfo>  recordStoreInfoMap = getRecordStores();
        EsRecordStoreInfo info = recordStoreInfoMap.get(recordType);
        if(info != null) {
            SearchRequest searchRequest = new SearchRequest(info.indexName);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(QueryBuilders.matchAllQuery());
            searchSourceBuilder.sort(new FieldSortBuilder(ESDataStoreTemplate.TIMESTAMP_FIELD_NAME).order(SortOrder.ASC));
            searchSourceBuilder.size(1);
            searchRequest.source(searchSourceBuilder);

            try {

                // build request to get the least recent record
                SearchResponse response = client.search(searchRequest);

                if (response.getHits().getTotalHits() > 0) {
                    result[0] = ESDataStoreTemplate.fromEpochMillisecond((Number) response.getHits().getAt(0).getSourceAsMap().get( ESDataStoreTemplate.TIMESTAMP_FIELD_NAME));
                }

                // build request to get the most recent record
                searchRequest = new SearchRequest(info.indexName);
                searchSourceBuilder = new SearchSourceBuilder();
                searchSourceBuilder.query(QueryBuilders.matchAllQuery());
                searchSourceBuilder.sort(new FieldSortBuilder(ESDataStoreTemplate.TIMESTAMP_FIELD_NAME).order(SortOrder.DESC));
                searchSourceBuilder.size(1);
                searchRequest.source(searchSourceBuilder);

                response = client.search(searchRequest);

                if (response.getHits().getTotalHits() > 0) {
                    result[1] = ESDataStoreTemplate.fromEpochMillisecond((Number) response.getHits().getAt(0).getSourceAsMap().get( ESDataStoreTemplate.TIMESTAMP_FIELD_NAME));
                }
            } catch (IOException ex) {
                log.error("getRecordsTimeRange failed", ex);
            }
        }
		return result;
	}

    @Override
    public int[] getEstimatedRecordCounts(String recordType, double[] timeStamps) {
        return StorageUtils.computeDefaultRecordCounts(this, recordType, timeStamps);
    }

	void dataSimpleComponent(SimpleComponent dataComponent, Map data, int i, DataBlock dataBlock) {
        switch (dataComponent.getDataType()) {
            case FLOAT:
                dataBlock.setFloatValue(i, ((Number)data.get(dataComponent.getName())).floatValue());
                break;
            case DOUBLE:
                dataBlock.setDoubleValue(i, ((Number)data.get(dataComponent.getName())).doubleValue());
                break;
            case SHORT:
            case USHORT:
            case UINT:
            case INT:
                dataBlock.setIntValue(i, ((Number)data.get(dataComponent.getName())).intValue());
                break;
            case ASCII_STRING:
            case UTF_STRING:
                dataBlock.setStringValue(i, (String)data.get(dataComponent.getName()));
                break;
            case BOOLEAN:
                dataBlock.setBooleanValue(i, (Boolean) data.get(dataComponent.getName()));
                break;
            case ULONG:
            case LONG:
                dataBlock.setLongValue(i,((Number)data.get(dataComponent.getName())).longValue());
                break;
            case UBYTE:
            case BYTE:
                dataBlock.setByteValue(i, ((Number)data.get(dataComponent.getName())).byteValue());
                break;
            default:
                getLogger().error("Unsupported type " + ((SimpleComponent) dataComponent).getDataType());
        }
    }

	void dataBlockFromES(DataComponent component, Map data, DataBlock dataBlock, AtomicInteger fieldIndex) {
        if(component instanceof SimpleComponent) {
            dataSimpleComponent((SimpleComponent) component, data, fieldIndex.getAndIncrement(), dataBlock);
        } else if(component instanceof Vector) {
            // Extract coordinate component
            Object values = data.get(component.getName());
            if(values instanceof List) {
                dataBlock.setDoubleValue(fieldIndex.getAndIncrement(), ((Number) ((List) values).get(0)).doubleValue());
                dataBlock.setDoubleValue(fieldIndex.getAndIncrement(), ((Number) ((List) values).get(1)).doubleValue());
            } else if (values instanceof Map) {
                dataBlock.setDoubleValue(fieldIndex.getAndIncrement(), ((Number) ((Map) values).get("lat")).doubleValue());
                dataBlock.setDoubleValue(fieldIndex.getAndIncrement(), ((Number) ((Map) values).get("lon")).doubleValue());
            }
            // Retrieve Z value
            dataBlock.setDoubleValue(fieldIndex.getAndIncrement(),
                    ((Number)data.get(component.getName() + Z_FIELD)).doubleValue());

        } else if(component instanceof DataRecord){
            final int arraySize = component.getComponentCount();
            for (int i = 0; i < arraySize; i++) {
                DataComponent subComponent = component.getComponent(i);
                dataBlockFromES(subComponent,  data, dataBlock, fieldIndex);
            }
        } else if(component instanceof DataArray) {
            final int compSize = component.getComponentCount();
            List dataList = (List) data.get(component.getName());
            if(((DataArray) component).getElementType() instanceof ScalarComponent) {
                // Simple array
                ScalarComponent scalarComponent = (ScalarComponent)((DataArray) component).getElementType();
                switch (scalarComponent.getDataType()) {
                    case FLOAT:
                    case DOUBLE:
                        for(int ind = 0; ind < compSize; ind++) {
                            Object value = dataList.get(ind);
                            if(value instanceof Number) {
                                dataBlock.setDoubleValue(fieldIndex.getAndIncrement(), ((Number) value).doubleValue());
                            } else {
                                dataBlock.setDoubleValue(fieldIndex.getAndIncrement(), Double.NaN);
                            }
                        }
                        break;
                    case SHORT:
                    case USHORT:
                    case UINT:
                    case INT:
                        for(int ind = 0; ind < compSize; ind++) {
                            dataBlock.setIntValue(fieldIndex.getAndIncrement(), ((Number)dataList.get(ind)).intValue());
                        }
                        break;
                    case ASCII_STRING:
                    case UTF_STRING:
                        for(int ind = 0; ind < compSize; ind++) {
                            dataBlock.setStringValue(fieldIndex.getAndIncrement(), (String)dataList.get(ind));
                        }
                        break;
                    case BOOLEAN:
                        for(int ind = 0; ind < compSize; ind++) {
                            dataBlock.setBooleanValue(fieldIndex.getAndIncrement(), (boolean)dataList.get(ind));
                        }
                        break;
                    case ULONG:
                    case LONG:
                        for(int ind = 0; ind < compSize; ind++) {
                            dataBlock.setLongValue(fieldIndex.getAndIncrement(), ((Number)dataList.get(ind)).longValue());
                        }
                        break;
                    case UBYTE:
                    case BYTE:
                        for(int ind = 0; ind < compSize; ind++) {
                            dataBlock.setByteValue(fieldIndex.getAndIncrement(), (Byte)dataList.get(ind));
                        }
                        break;
                    default:
                        getLogger().error("Unsupported type " + scalarComponent.getDataType().name());
                }
            } else {
                // Complex nested value
                for (int i = 0; i < compSize; i++) {
                    DataComponent subComponent = component.getComponent(i);
                    dataBlockFromES(subComponent, (Map) dataList.get(i), dataBlock, fieldIndex);
                }
            }
        }
    }

    DataBlock dataBlockFromES(DataComponent component, Map data) {
	    AtomicInteger fieldIndex = new AtomicInteger(0);
	    DataBlock dataBlock = component.createDataBlock();
	    dataBlockFromES(component, data, dataBlock, fieldIndex);
	    return dataBlock;
    }

	@Override
	public DataBlock getDataBlock(DataKey key) {
        DataBlock result = null;
        Map<String, EsRecordStoreInfo>  recordStoreInfoMap = getRecordStores();
        EsRecordStoreInfo info = recordStoreInfoMap.get(key.recordType);
        if(info != null) {
            // build the key as recordTYpe_timestamp_producerID
            String esKey = getRsKey(key);

            // build the request
            GetRequest getRequest = new GetRequest(info.indexName, info.name, esKey);
            try {
                // build  and execute the response
                GetResponse response = client.get(getRequest);

                // deserialize the blob field from the response if any
                if (response.isExists()) {
                    result = dataBlockFromES(info.recordDescription, response.getSourceAsMap());
                }
            } catch (IOException ex) {
                log.error(ex.getLocalizedMessage(), ex);
            }
        }
		return result;
	}

	@Override
	public Iterator<DataBlock> getDataBlockIterator(IDataFilter filter) {

        Map<String, EsRecordStoreInfo> recordStoreInfoMap = getRecordStores();
        EsRecordStoreInfo info = recordStoreInfoMap.get(filter.getRecordType());
        if (info != null) {
            SearchRequest searchRequest = new SearchRequest(info.indexName);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(queryByFilter(filter));
            searchSourceBuilder.size(config.scrollFetchSize);
            searchSourceBuilder.sort(new FieldSortBuilder(ESDataStoreTemplate.TIMESTAMP_FIELD_NAME).order(SortOrder.ASC));
            searchRequest.source(searchSourceBuilder);
            searchRequest.scroll(TimeValue.timeValueMillis(config.scrollMaxDuration));

            final Iterator<SearchHit> searchHitsIterator = new ESIterator(client, searchRequest, TimeValue.timeValueMillis(config.scrollMaxDuration)); //max of scrollFetchSize hits will be returned for each scroll

            // build a DataBlock iterator based on the searchHits iterator

            return new Iterator<DataBlock>() {

                @Override
                public boolean hasNext() {
                    return searchHitsIterator.hasNext();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public DataBlock next() {
                    SearchHit nextSearchHit = searchHitsIterator.next();

                    return dataBlockFromES(info.recordDescription, nextSearchHit.getSourceAsMap());
                }
            };
        } else {
            return Collections.emptyIterator();
        }
	}

    Iterator<? extends IDataRecord> recordIteratorFromESQueryFilter(IDataFilter filter, BoolQueryBuilder esFilter) {

        Map<String, EsRecordStoreInfo> recordStoreInfoMap = getRecordStores();
        EsRecordStoreInfo info = recordStoreInfoMap.get(filter.getRecordType());
        if (info != null) {
            SearchRequest searchRequest = new SearchRequest(info.indexName);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(esFilter);
            searchSourceBuilder.size(config.scrollFetchSize);
            searchSourceBuilder.sort(new FieldSortBuilder(ESDataStoreTemplate.TIMESTAMP_FIELD_NAME).order(SortOrder.ASC));
            searchRequest.source(searchSourceBuilder);
            searchRequest.scroll(TimeValue.timeValueMillis(config.scrollMaxDuration));

            final Iterator<SearchHit> searchHitsIterator = new ESIterator(client, searchRequest, TimeValue.timeValueMillis(config.scrollMaxDuration)); //max of scrollFetchSize hits will be returned for each scroll

            // build a IDataRecord iterator based on the searchHits iterator

            return new Iterator<IDataRecord>() {

                @Override
                public boolean hasNext() {
                    return searchHitsIterator.hasNext();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public IDataRecord next() {
                    SearchHit nextSearchHit = searchHitsIterator.next();

                    Map<String, Object> queryResult = nextSearchHit.getSourceAsMap();
                    // build key
                    final DataKey key = getDataKey(nextSearchHit.getId(), queryResult);

                    final DataBlock datablock = dataBlockFromES(info.recordDescription, queryResult);

                    return new IDataRecord() {

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
        } else {
            return Collections.emptyIterator();
        }

    }

    @Override
	public Iterator<? extends IDataRecord> getRecordIterator(IDataFilter filter) {
        return recordIteratorFromESQueryFilter(filter, queryByFilter(filter));
	}
	
	@Override
	public int getNumMatchingRecords(IDataFilter filter, long maxCount) {
        int result = 0;
        Map<String, EsRecordStoreInfo> recordStoreInfoMap = getRecordStores();
        EsRecordStoreInfo info = recordStoreInfoMap.get(filter.getRecordType());
        if (info != null) {
            SearchRequest searchRequest = new SearchRequest(info.indexName);
            searchRequest.source(new SearchSourceBuilder().size(0)
                    .query(queryByFilter(filter)));
            try {
                SearchResponse response = client.search(searchRequest);
                try {
                    return Math.toIntExact(Math.min(response.getHits().getTotalHits(), maxCount));
                } catch (ArithmeticException ex) {
                    getLogger().error("Too many records");
                    return Integer.MAX_VALUE;
                }
            } catch (IOException | ElasticsearchStatusException ex) {
                log.error("getRecordStores failed", ex);
            }
        }
        return result;
	}



	private void parseDataMapping(XContentBuilder builder, DataComponent dataComponent, DataComponent timeFieldToIgnore) throws IOException {
        if (dataComponent instanceof SimpleComponent && !dataComponent.equals(timeFieldToIgnore)) {
            switch (((SimpleComponent) dataComponent).getDataType()) {
                case FLOAT:
                    builder.startObject(dataComponent.getName());
                    // TODO When Quantity will contains a precision information
                    // builder.field("type", "scaled_float");
                    // builder.field("index", false);
                    // builder.field("scaling_factor", 100);
                    builder.field("type", "float");
                    builder.endObject();
                    break;
                case DOUBLE:
                    builder.startObject(dataComponent.getName());
                    builder.field("type", "double");
                    builder.endObject();
                    break;
                case SHORT:
                    builder.startObject(dataComponent.getName());
                    builder.field("type", "short");
                    builder.endObject();
                    break;
                case USHORT:
                case UINT:
                case INT:
                    builder.startObject(dataComponent.getName());
                    builder.field("type", "integer");
                    builder.endObject();
                    break;
                case ASCII_STRING:
                    builder.startObject(dataComponent.getName());
                    builder.field("type", "keyword");
                    builder.endObject();
                    break;
                case UTF_STRING:
                    builder.startObject(dataComponent.getName());
                    builder.field("type", "text");
                    builder.endObject();
                    break;
                case BOOLEAN:
                    builder.startObject(dataComponent.getName());
                    builder.field("type", "boolean");
                    builder.endObject();
                    break;
                case ULONG:
                case LONG:
                    builder.startObject(dataComponent.getName());
                    builder.field("type", "long");
                    builder.endObject();
                    break;
                case UBYTE:
                case BYTE:
                    builder.startObject(dataComponent.getName());
                    builder.field("type", "byte");
                    builder.endObject();
                    break;
                default:
                    getLogger().error("Unsupported type " + ((SimpleComponent) dataComponent).getDataType());
            }
        } else if(dataComponent instanceof DataRecord) {
            for(int i = 0; i < dataComponent.getComponentCount(); i++) {
                DataComponent component = dataComponent.getComponent(i);
                parseDataMapping(builder, component, timeFieldToIgnore);
            }
        } else if(dataComponent instanceof DataArray){
            if(((DataArray) dataComponent).getElementType() instanceof SimpleComponent) {
                parseDataMapping(builder, ((DataArray) dataComponent).getElementType(), timeFieldToIgnore);
            } else {
                builder.startObject(dataComponent.getName());
                {
                    builder.field("type", "nested");
                    builder.field("dynamic", false);
                    builder.startObject("properties");
                    {
                        parseDataMapping(builder, ((DataArray) dataComponent).getElementType(), timeFieldToIgnore);
                    }
                    builder.endObject();
                }
                builder.endObject();
            }
        } else if(dataComponent instanceof Vector) {
            // Point type
            builder.startObject(dataComponent.getName());
            {
                builder.field("type", "geo_point");
            }
            builder.endObject();
            builder.startObject(dataComponent.getName()+Z_FIELD);
            {
                builder.field("type", "double");
            }
            builder.endObject();
        }
    }


    /**
     * Override this method to add special fields in es data mapping
     * @param builder
     * @throws IOException
     */
    void createDataMappingFields(XContentBuilder builder) throws IOException {
        builder.startObject(ESDataStoreTemplate.PRODUCER_ID_FIELD_NAME);
        {
            builder.field("type", "keyword");
        }
        builder.endObject();
        builder.startObject(ESDataStoreTemplate.TIMESTAMP_FIELD_NAME);
        {
            // Issue with date https://discuss.elastic.co/t/weird-issue-with-date-sort/137646
            builder.field("type", "date");
            builder.field("format", "epoch_millis");
        }
        builder.endObject();
        builder.startObject(STORAGE_ID_FIELD_NAME);
        {
            builder.field("type", "keyword");
        }
        builder.endObject();
    }

    DataComponent findTimeComponent(DataComponent parent) {
        ScalarComponent timeStamp = (ScalarComponent)SWEHelper.findComponentByDefinition(parent, SWEConstants.DEF_SAMPLING_TIME);
        if (timeStamp == null)
            timeStamp = (ScalarComponent)SWEHelper.findComponentByDefinition(parent, SWEConstants.DEF_PHENOMENON_TIME);
        if (timeStamp == null)
            return null;
        return timeStamp;
    }

    /**
     * @param rsInfo record store metadata
     * @throws IOException
     */
    void createDataMapping(EsRecordStoreInfo rsInfo) throws IOException {

        // create the index
        CreateIndexRequest indexRequest = new CreateIndexRequest(rsInfo.indexName);
        XContentBuilder builder = XContentFactory.jsonBuilder();

        builder.startObject();
        {
            builder.startObject(rsInfo.name);
            {
                builder.field("dynamic", false);
                builder.startObject("properties");
                {
                    createDataMappingFields(builder);
                    DataComponent dataComponent = rsInfo.getRecordDescription();
                    DataComponent timeComponent = findTimeComponent(dataComponent);
                    parseDataMapping(builder, dataComponent, timeComponent);
                }
                builder.endObject();
            }
            builder.endObject();
        }
        builder.endObject();

        indexRequest.mapping(rsInfo.name, builder);

        client.indices().create(indexRequest);

        addedIndex.add(rsInfo.indexName);
    }

    public List<String> getAddedIndex() {
        return Collections.unmodifiableList(addedIndex);
    }

    void dataComponentSimpleToJson(SimpleComponent component, DataBlock data, int i, XContentBuilder builder) throws IOException {
        switch (data.getDataType(i)) {
            case FLOAT:
                builder.field(component.getName(), data.getFloatValue(i));
                break;
            case DOUBLE:
                builder.field(component.getName(), data.getDoubleValue(i));
                break;
            case SHORT:
            case USHORT:
            case UINT:
            case INT:
                builder.field(component.getName(), data.getIntValue(i));
                break;
            case ASCII_STRING:
            case UTF_STRING:
                builder.field(component.getName(), data.getStringValue(i));
                break;
            case BOOLEAN:
                builder.field(component.getName(), data.getBooleanValue(i));
                break;
            case ULONG:
            case LONG:
                builder.field(component.getName(), data.getLongValue(i));
                break;
            case UBYTE:
            case BYTE:
                builder.field(component.getName(), data.getByteValue(i));
                break;
            default:
                getLogger().error("Unsupported type " + data.getDataType(i).name());
        }
    }

    private static void setXContentValue(XContentBuilder builder, double value) throws IOException {
        if(Double.isNaN(value) || !Double.isFinite(value)) {
            builder.nullValue();
        } else {
            builder.value(value);
        }
    }

    private static void setXContentValue(XContentBuilder builder, float value) throws IOException {
        if(Float.isNaN(value) || !Float.isFinite(value)) {
            builder.nullValue();
        } else {
            builder.value(value);
        }
    }

    void dataComponentToJson(DataComponent dataComponent, DataBlock data, XContentBuilder builder, AtomicInteger fieldCounter) throws IOException {
        if(dataComponent instanceof SimpleComponent) {
          dataComponentSimpleToJson((SimpleComponent) dataComponent, data, fieldCounter.getAndIncrement(), builder);
        } else if(dataComponent instanceof Vector) {
            builder.startObject(dataComponent.getName());
            {
                builder.field("lat");
                setXContentValue(builder, data.getDoubleValue(fieldCounter.getAndIncrement()));
                builder.field("lon");
                setXContentValue(builder, data.getDoubleValue(fieldCounter.getAndIncrement()));
            }
            builder.endObject();
            builder.field(dataComponent.getName()+Z_FIELD, data.getDoubleValue(fieldCounter.getAndIncrement()));
        } else if(dataComponent instanceof DataArray) {
            // DataArray of scalar values, it is Array of ElasticSearch
            if(((DataArray) dataComponent).getElementType() instanceof ScalarComponent) {
                final int compSize = dataComponent.getComponentCount();
                ScalarComponent scalarComponent = (ScalarComponent) ((DataArray) dataComponent).getElementType();
                builder.startArray(dataComponent.getName());
                switch (scalarComponent.getDataType()) {
                    case FLOAT:
                        for(int ind = 0; ind < compSize; ind++) {
                            setXContentValue(builder, data.getFloatValue(fieldCounter.getAndIncrement()));
                        }
                        break;
                    case DOUBLE:
                        for(int ind = 0; ind < compSize; ind++) {
                            setXContentValue(builder, data.getDoubleValue(fieldCounter.getAndIncrement()));
                        }
                        break;
                    case SHORT:
                    case USHORT:
                    case UINT:
                    case INT:
                        for(int ind = 0; ind < compSize; ind++) {
                            builder.value(data.getIntValue(fieldCounter.getAndIncrement()));
                        }
                        break;
                    case ASCII_STRING:
                    case UTF_STRING:
                        for(int ind = 0; ind < compSize; ind++) {
                            builder.value(data.getStringValue(fieldCounter.getAndIncrement()));
                        }
                        break;
                    case BOOLEAN:
                        for(int ind = 0; ind < compSize; ind++) {
                            builder.value(data.getBooleanValue(fieldCounter.getAndIncrement()));
                        }
                        break;
                    case ULONG:
                    case LONG:
                        for(int ind = 0; ind < compSize; ind++) {
                            builder.value(data.getLongValue(fieldCounter.getAndIncrement()));
                        }
                        break;
                    case UBYTE:
                    case BYTE:
                        for(int ind = 0; ind < compSize; ind++) {
                            builder.value(data.getByteValue(fieldCounter.getAndIncrement()));
                        }
                        break;
                    default:
                        getLogger().error("Unsupported type " + scalarComponent.getDataType().name());
                }
                builder.endArray();
            } else {
                // Array of complex type, this is nested document of ElasticSearch
                int compSize = dataComponent.getComponentCount();
                builder.startArray(dataComponent.getName());
                for (int i = 0; i < compSize; i++) {
                    builder.startObject();
                    dataComponentToJson(dataComponent.getComponent(i), data, builder, fieldCounter);
                    builder.endObject();
                }
                builder.endArray();
            }
        } else if(dataComponent instanceof DataRecord) {
            int compSize = dataComponent.getComponentCount();
            for (int i = 0; i < compSize; i++) {
                dataComponentToJson(dataComponent.getComponent(i), data, builder, fieldCounter);
            }
        }
    }

    void dataComponentToJson(DataComponent dataComponent, DataBlock data, XContentBuilder builder) throws IOException
    {
        AtomicInteger fieldCounter = new AtomicInteger(0);
        dataComponentToJson(dataComponent, data, builder, fieldCounter);
    }

    void storeRecordIndexRequestFields(XContentBuilder builder, DataKey key) throws IOException {
        builder.field(ESDataStoreTemplate.PRODUCER_ID_FIELD_NAME, key.producerID);
        builder.field(ESDataStoreTemplate.TIMESTAMP_FIELD_NAME, ESDataStoreTemplate.toEpochMillisecond(key.timeStamp));
        builder.field(STORAGE_ID_FIELD_NAME, config.id);
    }

    public IndexRequest storeRecordIndexRequest(DataKey key, DataBlock data) throws IOException {
        IndexRequest request = null;

        Map<String, EsRecordStoreInfo>  recordStoreInfoMap = getRecordStores();
        EsRecordStoreInfo info = recordStoreInfoMap.get(key.recordType);
        if(info != null) {
            //ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            //XContentBuilder builder = XContentFactory.jsonBuilder(byteArrayOutputStream);
            XContentBuilder builder = XContentFactory.jsonBuilder();

            builder.startObject();
            {
                storeRecordIndexRequestFields(builder, key);
                DataComponent dataComponent = info.getRecordDescription();
                dataComponentToJson(dataComponent, data, builder);
            }
            builder.endObject();

            request = new IndexRequest(info.getIndexName(), info.name, getRsKey(key));

            //builder.flush();

            request.source(builder);
        }
	    return request;
    }

    @Override
	public void storeRecord(DataKey key, DataBlock data) {
        if(key.producerID == null || key.producerID.isEmpty()) {
            getLogger().info("Missing producerID data " + key.recordType);
            return;
        }
        try {
            IndexRequest request = storeRecordIndexRequest(key, data);
            if(request != null) {
                bulkProcessor.add(request);
            } else {
                log.error("Missing record store " + key.recordType);
            }
        } catch (IOException ex) {
            log.error("Cannot create json data storeRecord", ex);
        }

        storeChanged = System.currentTimeMillis();
    }

	@Override
	public void updateRecord(DataKey key, DataBlock data) {
        // Key handle duplicates
        storeRecord(key, data);
	}

	@Override
	public void removeRecord(DataKey key) {
        Map<String, EsRecordStoreInfo>  recordStoreInfoMap = getRecordStores();
        EsRecordStoreInfo info = recordStoreInfoMap.get(key.recordType);
        if(info != null) {
            // build the key as recordTYpe_timestamp_producerID
            String esKey = getRsKey(key);

            // prepare delete request
            DeleteRequest deleteRequest = new DeleteRequest(info.indexName, info.name, esKey);
            bulkProcessor.add(deleteRequest);
        }
	}

	private static String encodeEndPoint(String... params) throws IOException {
	    StringBuilder s = new StringBuilder();
	    for(String param : params) {
	        if(s.length() > 0) {
	            s.append("/");
            }
            s.append(URLEncoder.encode(param, "UTF-8"));
        }
	    return s.toString();
    }

    /**
     * Convert OSH filter to ElasticSearch filter
     * @param filter OSH filter
     * @return ElasticSearch query object
     */
    BoolQueryBuilder queryByFilter(IDataFilter filter) {
        double[] timeRange = getTimeRange(filter);

        BoolQueryBuilder query = QueryBuilders.boolQuery();

        if(config.filterByStorageId) {
            query.must(QueryBuilders.termQuery(STORAGE_ID_FIELD_NAME, config.id));
        }

        query.must(new RangeQueryBuilder(ESDataStoreTemplate.TIMESTAMP_FIELD_NAME)
                .from(ESDataStoreTemplate.toEpochMillisecond(timeRange[0]))
                .to(ESDataStoreTemplate.toEpochMillisecond(timeRange[1])).format("epoch_millis"));

        // check if any producerIDs
        if(filter.getProducerIDs() != null && !filter.getProducerIDs().isEmpty()) {
            query.must(QueryBuilders.termsQuery(ESDataStoreTemplate.PRODUCER_ID_FIELD_NAME, filter.getProducerIDs()));
        }

        return query;
    }

	@Override
	public int removeRecords(IDataFilter filter) {
		try {
            Map<String, EsRecordStoreInfo>  recordStoreInfoMap = getRecordStores();
            EsRecordStoreInfo info = recordStoreInfoMap.get(filter.getRecordType());
            if(info != null) {
                // Delete by query, currently not supported by High Level Api

                BoolQueryBuilder query = queryByFilter(filter);

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                XContentBuilder builder = XContentFactory.jsonBuilder(bos);
                builder.startObject();
                builder.rawField("query", new ByteArrayInputStream(query.toString().getBytes(StandardCharsets.UTF_8)), XContentType.JSON);
                builder.endObject();
                builder.flush();
                String json = bos.toString("UTF-8");
                HttpEntity entity = new NStringEntity(json, ContentType.APPLICATION_JSON);
                Response response = client.getLowLevelClient().performRequest("POST", encodeEndPoint(info.indexName, "_delete_by_query"),Collections.EMPTY_MAP, entity);
                String source = EntityUtils.toString(response.getEntity());
                Map<String, Object> content = XContentHelper.convertToMap(XContentFactory.xContent(XContentType.JSON), source, true);

                storeChanged = System.currentTimeMillis();

                return ((Number)content.get("total")).intValue();
            }
        } catch (IOException ex) {
		  log.error("Failed to removeRecords", ex);
        }
        return 0;
	}

	/**
	 * Get a serialized object from an object.
	 * The object is serialized using Kryo.
	 * @param object The raw object
	 * @return the serialized object
	 */
	protected static <T> byte[] getBlob(T object){
		return KryoSerializer.serialize(object);
	}

	/**
	 * Get an object from a base64 encoding String.
	 * The object is deserialized using Kryo.
	 * @param blob The base64 encoding String
	 * @return The deserialized object
	 */
	protected static <T> T getObject(Object blob) {
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
		return key.recordType+RS_KEY_SEPARATOR+Double.doubleToLongBits(key.timeStamp)+RS_KEY_SEPARATOR+key.producerID;
	}
	
	/**
	 * Transform the recordStorage data key into a DataKey by splitting <recordtype><SEPARATOR><timestamp><SEPARATOR><producerID>.
	 * @param rsKey the corresponding dataKey
	 * @return the dataKey. NULL if the length != 3 after splitting
	 */
	protected DataKey getDataKey(String rsKey, Map<String, Object> content) {
		DataKey dataKey = null;
		
		// split the rsKey using separator
		String [] split = rsKey.split(RS_KEY_SEPARATOR);
		
		// must find <recordtype><SEPARATOR><timestamp><SEPARATOR><producerID>
    	if(split.length == 3) {
    		dataKey = new DataKey(split[0], split[2], Double.longBitsToDouble(Long.parseLong(split[1])));
    	}
		return dataKey;
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
        bulkProcessor.flush();

        if(config.autoRefresh) {
            RefreshRequest refreshRequest = new RefreshRequest();
            try {
                client.indices().refresh(refreshRequest);
            } catch (IOException ex) {
                getLogger().error("Error while refreshIndex", ex);
            }
        }
	}


    @Override
    public boolean isReadSupported() {
        return true;
    }

    @Override
    public boolean isWriteSupported() {
        return true;
    }

    private static final class BulkListener implements BulkProcessor.Listener {
	    Logger logger = LoggerFactory.getLogger(BulkListener.class);
	    RestHighLevelClient client;
	    private int maxRetry;
	    private int retryDelay;

        public BulkListener(RestHighLevelClient client, int max_retry, int retryDelay) {
            this.client = client;
            this.maxRetry = max_retry;
            this.retryDelay = retryDelay;
        }

        private Map<Long, Integer> bulkRetries = new HashMap<>();


        @Override
        public void beforeBulk(long executionId, BulkRequest request) {

        }

        @Override
        public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {

        }



        @Override
        public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
            if((failure instanceof IOException || failure instanceof ElasticsearchStatusException) && request != null) {
                // Retry to send the bulk later
                int retries = bulkRetries.getOrDefault(executionId, 0);
                if(retries < maxRetry) {
                    log.info(String.format("Retry send bulk request id:%d",executionId));
                    bulkRetries.put(executionId, retries + 1);
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            client.bulkAsync(request, new ActionListener<BulkResponse>() {
                                @Override
                                public void onResponse(BulkResponse bulkItemResponses) {
                                    log.info(String.format("Successfully sent bulk request id:%d after a failure",executionId));
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    afterBulk(executionId, request, e);
                                }
                            });

                        }
                    }, retryDelay);
                    return;
                } else {
                    logger.error(String.format("Exception while pushing data id:%d to ElasticSearch after %d retries, data lost",executionId, retries), failure);
                }
            }
            if(request != null) {
                logger.error(String.format("Unprocessed exception while pushing data id:%d to ElasticSearch, data lost",executionId), failure);
            }
        }
    }
}