/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Sensia Software LLC. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.axis;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;


/**
 * <p>
 * Implementation of sensor interface for generic Axis Cameras using IP
 * protocol
 * </p>
 *
 * <p>
 * Copyright (c) 2014
 * </p>
 * 
 * @author Mike Botts <mike.botts@botts-inc.com>
 * @since October 30, 2014
 */

public class AxisCameraDriver extends AbstractSensorModule<AxisCameraConfig>
{
    AxisVideoOutput videoDataInterface;
    AxisSettingsOutput ptzDataInterface;
    AxisVideoControl videoControlInterface;
    AxisPtzControl ptzControlInterface;


    /* *** here begins the specific sensor module stuff */

    public AxisCameraDriver()
    {
        videoDataInterface = new AxisVideoOutput(this);
        addOutput(videoDataInterface, false);

        ptzDataInterface = new AxisSettingsOutput(this);
        addOutput(ptzDataInterface, false);
    }


    @Override
    protected void updateSensorDescription() throws SensorException
    {
        synchronized (sensorDescription)
        {
            // parent class reads SensorML from config if provided
            // and then sets unique ID, outputs and control inputs
            super.updateSensorDescription();
            
            // add more stuff in SensorML here
        }
    }


    @Override
    public boolean isConnected()
    {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public void start() throws SensorHubException
    {
        ptzDataInterface.init();
        videoDataInterface.init();

        ptzDataInterface.startPolling();
        videoDataInterface.startStream();
    }


    @Override
    public void stop()
    {

    }


    @Override
    public void cleanup() throws SensorHubException
    {

    }

}
