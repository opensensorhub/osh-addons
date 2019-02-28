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

import java.util.Iterator;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.support.AbstractClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;

/**
 * <p>
 * Iterator wrapper for ES scroll iterator.
 * </p>
 *
 * @author Mathieu Dhainaut <mathieu.dhainaut@gmail.com>
 * @since 2017
 */
public class ESIterator implements Iterator<SearchHit>{

	/**
	 * Default scroll fetch size.
	 */
	private static final int DEFAULT_FETCH_SIZE = 10;
	
	/**
	 * The scroll search request.
	 */
	private SearchRequestBuilder scrollSearchResponse;
	
	/**
	 * The scroll search response.
	 */
	private SearchResponse scrollResp;
	
	/**
	 * The first total hits number got from the first scroll request.
	 */
	private long totalHits = -1;
	
	/**
	 * The current number of fetch hits got from the current iterator.
	 */
	private int currentNbFetch = 0;
	
	/**
	 * The current search iterator.
	 */
	private Iterator<SearchHit> searchHitIterator;
	
	/**
	 * The shared transport client.
	 */
	private AbstractClient client;
	
	/**
	 * The current fetch size.
	 */
	private int fetchSize;
	
	/**
	 * The total number of fetched hits since the first request.
	 */
	private long nbHits;
	
	public ESIterator(AbstractClient client,SearchRequestBuilder scrollSearchResponse,int fetchSize) {
		this.fetchSize = fetchSize;
		this.client = client;
		this.scrollSearchResponse = scrollSearchResponse;
		this.nbHits = 0;
	}
	
	public ESIterator(AbstractClient client, SearchRequestBuilder scrollSearchResponse) {
		this(client,scrollSearchResponse,DEFAULT_FETCH_SIZE);
	}

	/**
	 * Inits the scroll response and gets the current iterator.
	 */
	private void init() {
		// init response
		// override the size to keep the size consistent
		scrollResp = scrollSearchResponse.setSize(this.fetchSize).get();
		
		// get totalHits
		totalHits = scrollResp.getHits().getTotalHits();
		
		// init current iterator
		searchHitIterator = scrollResp.getHits().iterator();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean hasNext() {
		// first call
		if(totalHits == -1) {
			init();
		}
		return nbHits < totalHits;
	}

	/**
	 * Makes a new scroll response, re-init the scroll response and
	 * the search iterator.
	 */
	private void makeNewScrollRequest() {
		// build and execute the next scroll request
		scrollResp = client.prepareSearchScroll(scrollResp.getScrollId())
	    		.setScroll(new TimeValue(6000))
	    		.execute()
	    		.actionGet();
		
		// re-init the search iterator
		searchHitIterator = scrollResp.getHits().iterator();
		
		// reset the current number of fetched hits
		currentNbFetch = 0;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public SearchHit next() {
		// get the next hit 
		SearchHit hit = searchHitIterator.next();
		nbHits++;
		currentNbFetch++;
		
		// if we have to make a new request
		// we compare the number of current fetched hit to the allowed fetch size
		if(currentNbFetch >= fetchSize) {
			makeNewScrollRequest();
		}
		return hit;
	}

}
