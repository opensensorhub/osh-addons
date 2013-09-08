/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.sensor;

import org.vast.cdm.common.DataBlock;
import org.vast.cdm.common.DataComponent;


/**
 * <p><b>Title:</b>
 * SensorDataEvent
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Type of event generated when new data is avaible from sensors.
 * It is immutable and carries sensor data by reference
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 5, 2010
 */
public class SensorDataEvent extends SensorEvent
{
	private static final long serialVersionUID = 2124599187504793797L;
    
	
	/**
	 * Description of data records contained in this event (by reference) 
	 */
	protected DataComponent recordDescription;
	
	
	/**
	 * Array of available data records (by reference)
	 * Data is NOT automatically cleared from sensor buffer and may still
	 * be available through the polling API.
	 */
	protected DataBlock[] records;
	
	
	/**
	 * Constructor from list of records with their descriptor
	 * @param desc
	 * @param records 
	 */
	public SensorDataEvent(String sensorId, DataComponent desc, DataBlock... records)
	{
		super(sensorId, Type.NEW_DATA_AVAILABLE);
		this.recordDescription = desc;
		this.records = records;
	}


    public DataComponent getRecordDescription()
    {
        return recordDescription;
    }


    public DataBlock[] getRecords()
    {
        return records;
    }
}
