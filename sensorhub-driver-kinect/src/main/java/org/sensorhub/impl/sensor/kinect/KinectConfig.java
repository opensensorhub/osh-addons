/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.
Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2019 Botts Innovative Research, Inc. All Rights Reserved. 

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.kinect;

import org.openkinect.freenect.DepthFormat;
import org.openkinect.freenect.LedStatus;
import org.openkinect.freenect.VideoFormat;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.PositionConfig;
import org.sensorhub.api.sensor.PositionConfig.EulerOrientation;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;
import org.sensorhub.api.sensor.SensorConfig;

public class KinectConfig extends SensorConfig {
	
	@DisplayInfo(desc = "Kinect geographic position")
	public PositionConfig position = new PositionConfig();

	@DisplayInfo(desc = "Serial number of KINECT device")
	public String serialNumber = "075440104338";
	
	@DisplayInfo(desc = "Reduces the number of points in the cloud by this factor")
	public int pointCloudDecimationFactor = 10;
		
	@DisplayInfo(desc = "Mode of operation of the Kinect")
	public Mode videoMode = Mode.DEPTH;
	
	@DisplayInfo(desc = "The width of the image frame supported by this version of the Kinect device")
	public int frameWidth = 640;

	@DisplayInfo(desc = "The height of the image frame supported by this version of the Kinect device")
	public int frameHeight = 480;

	@DisplayInfo(desc = "The video format of the ir camera on the KINECT device")
	public VideoFormat irFormat = VideoFormat.IR_8BIT;

	@DisplayInfo(desc = "The video format of the camera on the KINECT device")
	public VideoFormat rgbFormat = VideoFormat.RGB;

//	@DisplayInfo(desc = "The video resolution of the camera on the KINECT device")
//	public Resolution resolution = Resolution.HIGH;

	@DisplayInfo(desc = "The format of the depth sensor on the KINECT device")
	public final DepthFormat depthFormat = DepthFormat.D11BIT;
	
	@DisplayInfo(desc = "Tilt angle of the Kinect")
	public double tiltAngle = 0.0;

	@DisplayInfo(desc = "LED status indicator of the Kinect")
	public LedStatus ledStatus = LedStatus.GREEN;
	
	enum Mode {
		
		DEPTH,
		VIDEO,
		IR
	};
	
	public KinectConfig() {

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
