/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.persistence;


/**
 * <p><b>Title:</b>
 * IObservationKey
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Interface for keys used to index observations
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 6, 2010
 */
public class ObsKey extends DataKey
{
    /**
     * ID of feature of interest (i.e. the feature that was the target of the oservation)
     */
    public String foiID;
    
    
    public ObsKey(String producerID, String foiID, long timeStamp)
    {
        super(producerID, timeStamp);
        this.foiID = foiID;
    }
}
