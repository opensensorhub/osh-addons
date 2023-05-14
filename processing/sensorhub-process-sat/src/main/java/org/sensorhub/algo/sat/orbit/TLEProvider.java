/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2014 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.algo.sat.orbit;

import java.io.IOException;


public interface TLEProvider
{
    public TLEInfo getClosestTLE(String satID, double desiredTime) throws IOException;
}
