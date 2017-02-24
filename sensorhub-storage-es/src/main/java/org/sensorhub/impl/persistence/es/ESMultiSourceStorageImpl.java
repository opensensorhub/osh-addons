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

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.IMultiSourceStorage;
import org.sensorhub.api.persistence.IObsStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opengis.swe.v20.DataBlock;

/**
 * <p>
 * ES implementation of {@link IObsStorage} for storing observations.
 * </p>
 *
 * @author Mathieu Dhainaut <mathieu.dhainaut@gmail.com>
 * @since 2017
 */
public class ESMultiSourceStorageImpl extends ESObsStorageImpl implements IMultiSourceStorage<IObsStorage> {

	/**
	 * Class logger
	 */
	private static final Logger log = LoggerFactory.getLogger(ESMultiSourceStorageImpl.class);  
	
	
	
	
	
	
	
	
	
	@Override
	public Collection<String> getProducerIDs() {
		return null;
	}

	@Override
	public IObsStorage getDataStore(String producerID) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IObsStorage addDataStore(String producerID) {
		// TODO Auto-generated method stub
		return null;
	}

	
}
