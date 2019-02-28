/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.videocam;

import org.sensorhub.api.config.DisplayInfo;


/**
 * <p>
 * Generic configuration class for IP video cameras
 * </p>
 *
 * @author Mike Botts <mike.botts@botts-inc.com>
 * @since March 2016
 */
public abstract class BasicVideoConfig
{           
    @DisplayInfo(desc="Frame rate in Hz")
    public int frameRate;
    
    @DisplayInfo(desc="Set to true to get grayscale video, false for RGB")
    public boolean grayscale = false;
    
    @DisplayInfo(desc="Path to local file to backup raw video stream")
    public String backupFile = null;
    
    
    public abstract VideoResolution getResolution();

 }
