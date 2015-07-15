/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2014 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.process.trupulse;

import org.sensorhub.api.processing.StreamProcessConfig;


/**
 * <p>
 * Configuration for TruPulse geolocation process
 * </p>
 *
 * <p>Copyright (c) 2015 Sensia Software LLC</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Jun 21, 2015
 */
public class TargetGeolocConfig extends StreamProcessConfig
{
    public double[] fixedPosLLA;
}
