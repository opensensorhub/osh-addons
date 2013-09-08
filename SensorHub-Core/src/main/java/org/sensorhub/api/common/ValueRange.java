/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.common;


/**
 * <p><b>Title:</b>
 * ValueRange
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Immutable object for storing ranges of values of any types
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 12, 2010
 */
public class ValueRange<DataType>
{
    protected DataType low;
    protected DataType high;
    
    
    public ValueRange(DataType low, DataType high)
    {
        this.low = low;
        this.high = high;
    }


    /**
     * @return low value
     */
    public DataType getLow()
    {
        return low;
    }


    /**
     * @return high value
     */
    public DataType getHigh()
    {
        return high;
    }
}
