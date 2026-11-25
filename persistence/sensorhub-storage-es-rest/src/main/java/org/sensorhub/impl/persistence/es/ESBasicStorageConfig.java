/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.es;

import java.util.*;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.Required;

/**
 * <p>
 * Configuration class for ES basic storage
 * </p>
 *
 * @author Mathieu Dhainaut <mathieu.dhainaut@gmail.com>
 * @author Nicolas Fortin <nicolas.fortin at - ifsttar.fr>
 * @since 2017
 */
public class ESBasicStorageConfig extends org.sensorhub.api.persistence.ObsStorageConfig {

	public static final String DEFAULT_INDEX_NAME_METADATA = "osh_meta_record_store";

    @DisplayInfo(desc="ES cluster name")
    public String clusterName = "elasticsearch";

    @DisplayInfo(desc = "ElasticSearch user for authentication (leave blank if not required)")
    public String user = "";

    @DisplayInfo(desc = "Refresh store on commit. Require indices:admin/refresh rights")
    public boolean autoRefresh = true;

	@DisplayInfo(desc = "Multiple storage instance can use the same index. If the filtering is disabled this driver will see all sensors (should be used only for read-only SOS service)")
	public boolean filterByStorageId = true;

	@DisplayInfo(desc="List of additional SSL certificates", label = "Certificates")
	public List<String> certificatesPath = new ArrayList<>();

	@DisplayInfo(desc = "ElasticSearch password for authentication")
    @DisplayInfo.FieldType(DisplayInfo.FieldType.Type.PASSWORD)
    public String password = "";
        
    @DisplayInfo(desc="List of nodes")
    public List<String> nodeUrls = new ArrayList<>(Arrays.asList("localhost:9200","localhost:9201"));

    @DisplayInfo(desc="String to add in index name before the data name")
    public String indexNamePrepend = "";

	@DisplayInfo(desc="Index name of the OpenSensorHub metadata")
	public String indexNameMetaData = DEFAULT_INDEX_NAME_METADATA;
            
    @DisplayInfo(desc="When scrolling, the maximum duration ScrollableResults will be usable if no other results are fetched from, in ms")
    public int scrollMaxDuration = 6000;
	
	@DisplayInfo(desc="MWhen scrolling, the number of results fetched by each Elasticsearch call")
    public int scrollFetchSize = 10;
	
	@DisplayInfo(desc="Determines the timeout in milliseconds until a connection is established. A timeout value of zero is interpreted as an infinite timeout.")
	@DisplayInfo.ValueRange(min = 0)
	public int connectTimeout = 5000;

	@DisplayInfo(desc="Defines the socket timeout (SO_TIMEOUT) in milliseconds, which is the timeout for waiting for data or, put differently, a maximum period inactivity between two consecutive data packets).")
	@DisplayInfo.ValueRange(min = 0)
	public int socketTimeout = 60000;

	@DisplayInfo(desc="Sets the maximum timeout (in milliseconds) to honour in case of multiple retries of the same request.")
	@DisplayInfo.ValueRange(min = 0)
	public int maxRetryTimeout = 60000;

	@DisplayInfo(desc="Set the number of concurrent requests")
	public int bulkConcurrentRequests = 10;
	
	@DisplayInfo(desc="We want to execute the bulk every n requests")
	public int bulkActions = 10000;
	
	@DisplayInfo(desc="We want to flush the bulk every n mb")
	public int bulkSize = 10;
	
	@DisplayInfo(desc="We want to flush the bulk every n seconds whatever the number of requests")
	public int bulkFlushInterval = 10;

    @Override
    public void setStorageIdentifier(String name)
    {
        indexNamePrepend = name;
    }

    @DisplayInfo(desc = "Bulk insertion may fail, client will resend in case of TimeOut exception. Retry is disabled by default in order to avoid overflow of ElasticSearch cluster")
	public int maxBulkRetry = 0;

}
