/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.persistence;

import java.util.Set;


/**
 * <p>
 * Basic filter interface to get time series data records from storage.<br/>
 * There is an implicit logical AND between all criteria.
 * </p>
 *
 * @author Alex Robin
 * @since Nov 12, 2010
 */
public interface IDataFilter
{   
    
    /**
     * @return the record type that needs to be retrieved (cannot be null)
     */
    public String getRecordType();
    
    
    /**
     * Gets filter criteria for selecting data records based on time stamp.<br/>
     * Only records whose time stamp (usually sampling time for sensors) lies within
     * that range will be selected (range is inclusive).<br/>
     * It both ends of the time range have the value {@link Double#POSITIVE_INFINITY},
     * the latest record of each selected producer is retrieved.<br/>
     * If range is null, no filtering on time stamp will be applied.
     * @return Desired time stamp range
     */
    public double[] getTimeStampRange();
    
    
    /**
     * Gets filter criteria for selecting data records associated to certain producers.<br/>
     * Only data records associated to one of the listed producer IDs will be selected.<br/>
     * If the list is null or empty, no filtering on producer ID will be applied.
     * @return List of desired producer IDs
     */
    public Set<String> getProducerIDs();
}
