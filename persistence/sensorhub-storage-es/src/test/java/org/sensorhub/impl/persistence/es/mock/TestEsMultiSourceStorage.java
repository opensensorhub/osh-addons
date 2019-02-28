/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.es.mock;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.support.AbstractClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeValidationException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.sensorhub.impl.persistence.es.ESBasicStorageImpl;
import org.sensorhub.impl.persistence.es.ESBasicStorageConfig;
import org.sensorhub.impl.persistence.es.ESMultiSourceStorageImpl;
import org.sensorhub.impl.persistence.es.ESObsStorageImpl;
import org.sensorhub.test.persistence.AbstractTestBasicStorage;
import org.sensorhub.utils.FileUtils;

public class TestEsMultiSourceStorage extends AbstractTestBasicStorage<ESMultiSourceStorageImpl> {

    protected static final String CLUSTER_NAME = "elasticsearch";
    private static Node node;
	private static File tmpDir;
	
	static {
		tmpDir = new File(System.getProperty("java.io.tmpdir")+"/es/"+UUID.randomUUID().toString());
		tmpDir.mkdirs();
		try {
			node = getNode(tmpDir);
		} catch (NodeValidationException e) {
			e.printStackTrace();
		}
	}
	
	@Before
	public void init() throws Exception {
		
		
		ESBasicStorageConfig config = new ESBasicStorageConfig();
		config.autoStart = true;
		config.clusterName = CLUSTER_NAME;
		List<String> nodes = new ArrayList<String>();
		nodes.add("localhost:9300");

		config.nodeUrls = nodes;
		config.scrollFetchSize = 2000;
		config.bulkConcurrentRequests = 0;
		config.id = "junit_" + UUID.randomUUID().toString();
		
		storage = new ESMultiSourceStorageImpl((AbstractClient) node.client());
		storage.init(config);
		storage.start();
	}

	@Override
	protected void forceReadBackFromStorage() throws Exception {
		// Let the time to ES to write the data
    	// if some tests are not passed,  try to increase this value first!!
		storage.commit();
		
	}

	public static Node getNode(File outputDir) throws NodeValidationException {
		Settings settings = Settings.builder()
	            .put("path.home", tmpDir.getAbsolutePath())
	            .put("transport.type", "local")
	            .put("http.enabled", false)
	            .put("processors",Runtime.getRuntime().availableProcessors())
	            .put("node.max_local_storage_nodes", 15)
                .put("thread_pool.bulk.size", Runtime.getRuntime().availableProcessors())
                // default is 50 which is too low
                .put("thread_pool.bulk.queue_size", 16 * Runtime.getRuntime().availableProcessors())
	            .build();

	    return new Node(settings).start();
	}
	
	@AfterClass
    public static void cleanup() throws Exception {
		if(node != null) {
			if(node.client() != null) {
				node.client().close();
			}
			node.close();
			node = null;
		}
		
		if(tmpDir.exists()) {
			FileUtils.deleteRecursively(tmpDir);
		}
	}
}
	
