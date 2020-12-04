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

import java.util.Iterator;
import org.vast.util.Bbox;
import net.opengis.gml.v32.AbstractFeature;


/**
 * <p>
 * Interface for feature data storage implementations. This type of storage
 * provides spatial filtering capabilities.
 * </p>
 *
 * @author Alex Robin
 * @since Nov 6, 2010
 */
public interface IFeatureStorage
{
    
    /**
     * @return The total number of features in this storage
     */
    public int getNumFeatures();
    
    
    /**
     * @param filter filtering parameters
     * @return Number of features matching the filter
     */
    public int getNumMatchingFeatures(IFeatureFilter filter);
    
    
    /**
     * @return Bounding rectangle of all features contained in this storage
     */
    public Bbox getFeaturesSpatialExtent();
    
    
    /**
     * Gets IDs of features matching the given filter
     * @param filter filtering parameters
     * @return an iterator over IDs of all matching features
     */
    public Iterator<String> getFeatureIDs(IFeatureFilter filter);
    
    
    /**
     * Gets features matching the specified filter
     * @param filter filtering parameters
     * @return an iterator over features matching the filter, sorted by ID
     */
    public Iterator<AbstractFeature> getFeatures(IFeatureFilter filter);
    
    
    /**
     * Stores a new feature object in this data store
     * @param f feature object
     */
    void store(AbstractFeature f);
}
