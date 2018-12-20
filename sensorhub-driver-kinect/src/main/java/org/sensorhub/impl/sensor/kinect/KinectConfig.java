/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.kinect;

import org.openkinect.freenect.DepthFormat;
import org.openkinect.freenect.LedStatus;
import org.openkinect.freenect.Resolution;
import org.openkinect.freenect.VideoFormat;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.PositionConfig;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.sensor.PositionConfig.EulerOrientation;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;

public class KinectConfig extends SensorConfig {
	
	private KinectDeviceParams deviceParams;

	@DisplayInfo(desc = "Kinect geographic position")
	public PositionConfig position = new PositionConfig();

	@DisplayInfo(desc = "Serial number of KINECT device")
	public String serialNumber = "075440104338";
	
	

	@DisplayInfo(desc = "The video format of the camera on the KINECT device")
	public VideoFormat videoFormat = deviceParams.getCameraVideoFormat();
	
	@DisplayInfo(desc = "The video resolution of the camera on the KINECT device")
	public Resolution videoResolution = deviceParams.getCameraVideoResolution();

	@DisplayInfo(desc = "The format of the IR sensor on the KINECT device")
	public VideoFormat irFormat = deviceParams.getInfraredVideoFormat();

	@DisplayInfo(desc = "The resolution of the IR sensor on the KINECT device")
	public Resolution irResolution = deviceParams.getInfraredVideoResolution();

	@DisplayInfo(desc = "The format of the depth sensor on the KINECT device")
	public DepthFormat depthFormat = deviceParams.getDepthFormat();

	@DisplayInfo(desc = "The resolution of the depth sensor on the KINECT device")
	public Resolution depthResolution = deviceParams.getDepthSensorResolution();
	
	@DisplayInfo(desc = "Tilt angle of the Kinect")
	public double tiltAngle = deviceParams.getTiltAngle();

	@DisplayInfo(desc = "LED status indicator of the Kinect")
	public LedStatus ledStatus = deviceParams.getLedStatus();

	
	public KinectConfig() {

		deviceParams = new KinectDeviceParams();
	}
	
    @Override
    public LLALocation getLocation()
    {
        return position.location;
    }
    
    
    @Override
    public EulerOrientation getOrientation()
    {
        return position.orientation;
    }
}
