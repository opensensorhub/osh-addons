/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.persistence;


/**
 * <p><b>Title:</b>
 * IDataFilter
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Base interface for filter objects allowing to get records by criteria
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 12, 2010
 */
public interface IDataFilter
{
    /**
     * Sets the range of time stamps for which data should be retrieved
     * @param start
     * @param stop
     */
    public void setTimeStampRange(long start, long stop);
    
    
    /**
     * @return the time stamp range to filter on
     */
    public long[] getTimeStampRange();
    
    
    /**
     * Sets the producer ID to filter on
     * @param localID
     */
    public void setProducerID(String localID);
    
    
    /**
     * @return the producer ID to filter on
     */
    public String getProducerID();
}
