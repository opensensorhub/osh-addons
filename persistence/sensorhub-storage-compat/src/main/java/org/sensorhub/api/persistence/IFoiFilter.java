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
 * Simple structure for defining filtering criteria when retrieving features
 * of interest from storage.<br/> There is an implicit logical AND between all criteria.
 * </p>
 *
 * @author Alex Robin
 * @since May 25, 2015
 */
public interface IFoiFilter extends IFeatureFilter
{

    /**
     * Gets filter criteria for selecting features of interest associated to
     * certain producers.<br/>
     * Only features associated to one of the listed producer IDs will be selected.<br/>
     * If the list is null or empty, no filtering on producer ID will be applied.
     * @return List of desired producer IDs
     */
    public Set<String> getProducerIDs();
}
