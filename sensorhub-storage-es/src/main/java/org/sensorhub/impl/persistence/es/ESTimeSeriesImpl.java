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

import org.elasticsearch.action.index.IndexRequestBuilder;
import org.sensorhub.api.persistence.IRecordStoreInfo;

import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;

/**
 * <p>
 * ES implementation of an observation series data store for a single
 * record type
 * </p>
 *
 * @author Mathieu Dhainaut <mathieu.dhainaut@gmail.com>
 * @since 2017
 */
public class ESTimeSeriesImpl implements IRecordStoreInfo {

	// current rsInfo index
	private String rsInfoIdx;
	
	// index request builder to make request onto the record storage
	private IndexRequestBuilder recordStoreIdx;
	
	// index request builder to make request onto the record info storage
	private IndexRequestBuilder recordStoreInfoIdx;
	
	public ESTimeSeriesImpl(IndexRequestBuilder recordStoreIdx,IndexRequestBuilder recordStoreInfoIdx,String rsInfoIdx) {
		this.rsInfoIdx = rsInfoIdx;
		this.recordStoreIdx = recordStoreIdx;
		this.recordStoreInfoIdx = recordStoreInfoIdx;
	}
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataComponent getRecordDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		// TODO Auto-generated method stub
		return null;
	}

}
