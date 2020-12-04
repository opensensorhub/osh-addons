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

import java.io.Serializable;
import org.vast.util.Asserts;
import org.vast.util.DateTimeFormat;

/**
 * <p>
 * Base class for all keys associated to data records in SensorHub.
 * Keys are used to store time stamped data records in storage.
 * </p>
 *
 * @author Alex Robin
 * @since Nov 6, 2010
 */
public class DataKey implements Serializable
{
    private static final long serialVersionUID = 4804063766602286733L;


    /**
     * ID of record type (i.e. maps to a record structure definition).<br/>
     * This field cannot be null.
     */
    public String recordType;
    
    
    /**
     * Time stamp of data record (e.g. measurement sampling time).<br/>
     * If value is NaN, data record will never be selected when filtering on time stamp
     */
    public double timeStamp = Double.NaN;
    
    
    /**
     * ID of data producer (i.e. the entity that produced the data record).<br/>
     * If value is null, data record will never be selected when filtering on producer ID
     */
    public String producerID = null;
    
    
    /**
     * Default constructor providing basic indexing metadata
     * @param recordType {@link #recordType}
     * @param timeStamp {@link #timeStamp}
     */
    public DataKey(String recordType, double timeStamp)
    {
        Asserts.checkNotNull(recordType, "recordType");
        this.recordType = recordType;
        this.timeStamp = timeStamp;
    }
    
    
    /**
     * Constructor with specific producer ID
     * @param recordType {@link #recordType}
     * @param producerID {@link #producerID}
     * @param timeStamp {@link #timeStamp}
     */
    public DataKey(String recordType, String producerID, double timeStamp)
    {
        this(recordType, timeStamp);
        this.producerID = producerID;
    }
    
    
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append(getClass().getSimpleName()).append(" {");
        toString(buf);
        buf.append('}');
        return buf.toString();
    }
    
    
    protected void toString(StringBuilder buf)
    {
        buf.append("producer=").append(producerID).append(", ");
        buf.append("recordType=").append(recordType).append(", ");
        buf.append("time=").append(new DateTimeFormat().formatIso(timeStamp, 0));
    }
}
