/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.videocam;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;


/**
 * <p>
 * generic configuration class for the video cameras
 * </p>
 *
 * @author Mike Botts <mike.botts@botts-inc.com>
 * @since March 2016
 */
public class VideoCamConfig
{    
    
    @DisplayInfo(desc="Remote host (camera IP address or host name) where camera commands are sent")
    public String remoteHost;
        
    @DisplayInfo(desc="Width of video frames in pixels")
    public int frameWidth;
    
    @DisplayInfo(desc="Height of video frames in pixels")
    public int frameHeight;
    
    @DisplayInfo(desc="Frame rate in Hz")
    public int frameRate;
    
    @DisplayInfo(desc="Set to true if video is grayscale, false is RGB")
    public boolean grayscale = false;
    
    //@DisplayInfo(desc="Set frame dimension in pixels")
    
 }
