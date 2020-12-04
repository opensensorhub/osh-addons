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

import com.vividsolutions.jts.geom.Polygon;


/**
 * <p>
 * Class for keys associated to observations in SensorHub.
 * This type of key is used to store observations in storage.
 * </p>
 *
 * @author Alex Robin
 * @since Nov 6, 2010
 */
public class ObsKey extends DataKey
{
    private static final long serialVersionUID = 8685703972784653606L;


    /**
     * ID of feature of interest (i.e. feature whose properties were observed/measured).<br/>
     * If value is null, observation will never be selected when filtering on FoI ID
     */
    public String foiID;
    
    
    /**
     * Observation result time (i.e. time at which the observation result was obtained).<br/>
     * If value is NaN, observation will never be selected when filtering on result time
     */
    public double resultTime = Double.NaN;
    
    
    /**
     * Sampling Geometry (i.e. area or volume in 2D or 3D space, where the observation was made).<br/>
     * If value is null, FoI geometry is used instead when provided. If neither geometry is provided,
     * observation will never be selected when filtering on geometry.<br/>
     * In a given data store, all geometries must be expressed in the same coordinate reference system.
     */
    public Polygon samplingGeometry;
    
    
    /**
     * Default constructor providing mandatory indexing metadata
     * @param recordType {@link #recordType}
     * @param foiID {@link #foiID}
     * @param timeStamp {@link #timeStamp}
     */
    public ObsKey(String recordType, String foiID, double timeStamp)
    {
        super(recordType, timeStamp);
        this.foiID = foiID;
    }
    
    
    /**
     * DConstructor with specific entity ID
     * @param recordType {@link #recordType}
     * @param producerID {@link #producerID}
     * @param foiID {@link #foiID}
     * @param timeStamp {@link #timeStamp}
     */
    public ObsKey(String recordType, String producerID, String foiID, double timeStamp)
    {
        this(recordType, foiID, timeStamp);
        this.producerID = producerID;
    }
    
    
    @Override
    protected void toString(StringBuilder buf)
    {
        super.toString(buf);
        buf.append(", foiID=").append(foiID).append(", ");
        buf.append("resultTime=").append(resultTime).append(", ");
        buf.append("samplingGeometry=").append(samplingGeometry);
    }
}
