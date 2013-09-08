/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.persistence;

import org.vast.cdm.common.DataBlock;


/**
 * <p><b>Title:</b>
 * IDataRecord
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Interface encapsulating records composed of a key and a data block as value
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 12, 2010
 */
public interface IDataRecord<KeyType extends DataKey>
{
    public KeyType getKey();
    
    public DataBlock getData();
}
