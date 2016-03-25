/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
Developer are Copyright (C) 2016 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.dahua;

import org.sensorhub.impl.sensor.rtpcam.RTPVideoOutput;


/**
 * <p>
 * Implementation of sensor interface for generic Axis Cameras using IP
 * protocol. This particular class provides time-tagged video output from the video
 * camera capabilities.
 * </p>
 *
 * @author Mike Botts <mike.botts@botts-inc.com>
 * @since March 2016
 */
public class DahuaVideoOutput extends RTPVideoOutput
{
    // TODO Need a separate class for outputting camera settings (e.g. imageSize, brightness, iris, IR, focus, imageRotation)
    
    
	protected DahuaVideoOutput(DahuaCameraDriver driver)
	{
		super(driver,
		      driver.getConfiguration().video,
		      driver.getConfiguration().net,
		      driver.getConfiguration().rtsp);
	}
}
