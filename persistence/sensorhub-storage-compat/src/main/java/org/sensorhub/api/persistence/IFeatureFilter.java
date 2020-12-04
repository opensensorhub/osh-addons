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
import com.vividsolutions.jts.geom.Polygon;


/**
 * <p>
 * Simple structure for defining filtering criteria when retrieving features
 * from storage.<br/> There is an implicit logical AND between all criteria.
 * </p>
 *
 * @author Alex Robin
 * @since May 13, 2015
 */
public interface IFeatureFilter
{
       
    /**
     * Gets filter criteria for selecting features by ID.<br/>
     * Only features identified with one of the listed IDs will be selected.<br/>
     * If the list is null or empty, no filtering on ID will be applied.
     * @return List of desired feature IDs
     */
    public Set<String> getFeatureIDs();
    
    
    /**
     * Gets filter criteria for selecting features based on their geometry.<br/>
     * Only features whose geometry intersects the polygon will be selected.<br/>
     * If the polygon is null, no filtering on location will be applied.<br/>
     * The polygon must be expressed in the same coordinate reference system as the one used for storage.
     * @return Polygonal Region of Interest
     */
    public Polygon getRoi();
}
