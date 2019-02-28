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

import org.openkinect.freenect.Device;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorOutput;

public abstract class KinectOutputInterface extends AbstractSensorOutput<KinectSensor> {

	protected static final int NUM_DATA_COMPONENTS = 2;
	protected static final int IDX_TIME_DATA_COMPONENT = 0;
	protected static final int IDX_PAYLOAD_DATA_COMPONENT = 1;
	protected static final double MS_PER_S = 1000.0;

	protected Device device = null;
	
	public KinectOutputInterface(KinectSensor parentSensor, Device kinectDevice) {
		
		super(parentSensor);
		
		device = kinectDevice;
	}

	public abstract void init() throws SensorException;

	public abstract void start();
	
	@Override
	protected void stop() {

		device.stopDepth();
	}
	
	@Override
	public double getAverageSamplingPeriod() {

		return device.getDepthMode().framerate;
	}
}
