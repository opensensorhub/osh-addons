/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.persistence;


/**
 * <p><b>Title:</b>
 * IDataKey
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Base class for all data storage keys used in SensorHub.
 * Keys are used to store and retrieve data records from a storage.
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 6, 2010
 */
public class DataKey
{
    /**
     * Local ID of record to uniquely identify it in storage
     */
    public long recordID;
    
    
    /**
     * ID of data producer (i.e. the entity that produced the values in the data block)
     */
    public String producerID;
    
    
    /**
     * Time stamp of record
     */
    public long timeStamp;
    
    
    /**
     * Default constructor providing basic indexing metadata
     * @param producerID
     * @param timeStamp
     */
    public DataKey(String producerID, long timeStamp)
    {
        this.producerID = producerID;
        this.timeStamp = timeStamp;
    }
}
