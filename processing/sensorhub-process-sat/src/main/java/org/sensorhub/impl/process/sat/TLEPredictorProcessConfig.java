/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2014 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.process.sat;

import org.sensorhub.api.processing.StreamProcessConfig;


public class TLEPredictorProcessConfig extends StreamProcessConfig
{
    public String tleFetchUrl;
    public int satID;
}
