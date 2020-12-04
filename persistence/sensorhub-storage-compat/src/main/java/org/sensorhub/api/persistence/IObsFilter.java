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
 * Simple structure for defining filtering criteria when retrieving observations
 * from storage. These criteria correspond to properties of the O&M model.<br/>
 * There is an implicit logical AND between all criteria.
 * </p>
 *
 * @author Alex Robin
 * @since Nov 5, 2010
 */
public interface IObsFilter extends IDataFilter
{
    
    /**
     * Gets filter criteria for selecting observations based on phenomenon time.<br/>
     * Only observations whose phenomenon time (i.e sampling time for sensors) lies within
     * that range will be selected (range is inclusive).<br/>
     * If range is null, no filtering on phenomenon time will be applied.
     * @return Desired phenomenon time range
     */
    @Override
    public double[] getTimeStampRange();
    
    
    /**
     * Gets filter criteria for selecting observations based on result time.<br/>
     * Only observations whose result time (e.g. model run time) lies within
     * that range will be selected (range is inclusive).<br/>
     * If range is null, no filtering on result time will be applied.
     * @return Desired result time range
     */
    public double[] getResultTimeRange();
    
    
    /**
     * Gets filter criteria for selecting observations associated to certain features
     * of interest.<br/>
     * Only observations associated to one of the listed FoI IDs will be selected.<br/>
     * If the list is null or empty, no filtering on FoI ID will be applied.
     * @return List of desired feature of interest IDs
     */
    public Set<String> getFoiIDs();
    
    
    /**
     * Gets filter criteria for selecting observations based on their sampling geometry.<br/>
     * Only observations whose sampling geometry is included within the polygon will be selected.<br/>
     * If the polygon is null, no filtering on location will be applied.<br/>
     * The polygon must be expressed in the same coordinate reference system as the one used for storage.
     * @return Polygonal Region of Interest
     */
    public Polygon getRoi();
}
