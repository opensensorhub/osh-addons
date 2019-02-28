/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.process.cam;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.processing.StreamProcessConfig;


/**
 * <p>
 * Configuration for PTZcamera geo-targeting process
 * </p>
 *
 * <p>Copyright (c) 2015 Sensia Software LLC</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Aug 9, 2015
 */
public class CamPtzGeoPointingConfig extends StreamProcessConfig
{    
    @DisplayInfo(desc="Unique identifier of PTZ camera")
    public String camSensorUID;
    
    @DisplayInfo(desc="Camera location when camera is static (lat, lon, alt as in EPSG:4979)")
    public double[] fixedCameraPosLLA;    
    
    @DisplayInfo(desc="Camera reference orientation in ENU frame when camera is static (pitch, roll, yaw in deg)")
    public double[] fixedCameraRotENU;
    
    @DisplayInfo(desc="Camera focal length at zoom factor 0 (in mm)")
    public double cameraMinFocalLength = 4.5;
    
    @DisplayInfo(desc="Camera focal length at zoom factor 1 (in mm)")
    public double cameraMaxFocalLength = 135.0;
    
    @DisplayInfo(desc="Camera sensor size (in mm)")
    public double cameraSensorSize = 8.47;
    
    @DisplayInfo(desc="Desired distance visible in image (in m)")
    public double desiredViewSize = 20;

    @DisplayInfo(desc="SPS endpoint for tasking camera")
    public String camSpsEndpointUrl;
}
