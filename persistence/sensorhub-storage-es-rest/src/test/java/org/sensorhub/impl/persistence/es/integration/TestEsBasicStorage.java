/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.es.integration;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.persistence.es.ESBasicStorageImpl;
import org.sensorhub.impl.persistence.es.ESBasicStorageConfig;
import org.sensorhub.test.persistence.AbstractTestBasicStorage;


public class TestEsBasicStorage extends AbstractTestBasicStorage<ESBasicStorageImpl> {

	protected static final String CLUSTER_NAME = "elasticsearch";

	@Before
	public void init() throws Exception {
		ESBasicStorageConfig config = new ESBasicStorageConfig();
		config.autoStart = true;
		config.clusterName = CLUSTER_NAME;
		List<String> nodes = new ArrayList<String>();
		nodes.add("localhost:9200");
		nodes.add("localhost:9201");

		config.nodeUrls = nodes;
		config.scrollFetchSize = 200;
		config.bulkConcurrentRequests = 0;
		config.id = "junit_" + UUID.randomUUID().toString();
		storage = new ESBasicStorageImpl();
		storage.init(config);
		storage.start();
	}

	@After
	public void closeStorage() throws SensorHubException {
		storage.stop();
	}
	
	@Override
	protected void forceReadBackFromStorage() throws Exception {
		// Let the time to ES to write the data
    	// if some tests are not passed,  try to increase this value first!!
		storage.commit();
	}

	@AfterClass
	public static void cleanup() throws UnknownHostException {
//		// add transport address(es)
//		Settings settings = Settings.builder()
//		        .put("cluster.name", CLUSTER_NAME).build();
//
//		TransportClient client = new PreBuiltTransportClient(settings);
//		client.addTransportAddress(new TransportAddress(InetAddress.getByName("localhost"), 9300));
//
//		String idxName = "junit_*";
//
//		DeleteIndexResponse delete = client.admin().indices().delete(new DeleteIndexRequest(idxName)).actionGet();
//		if (!delete.isAcknowledged()) {
//			System.err.println("Index wasn't deleted");
//		}
//
//		client.close();
	}
}
