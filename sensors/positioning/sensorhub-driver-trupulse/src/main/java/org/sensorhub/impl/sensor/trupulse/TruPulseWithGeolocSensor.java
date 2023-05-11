/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2019 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.trupulse;

import org.sensorhub.api.common.SensorHubException;


/**
 * <p>
 * Extended TruPulse driver also providing the geolocated output in case 
 * a location source is available locally (e.g. when run on a smartphone)
 * </p>
 *
 * @author Alex Robin
 * @since Apr 10, 2019
 */
public class TruPulseWithGeolocSensor extends TruPulseSensor
{
    TruPulseGeolocOutput geolocOutput;
    
    
    @Override
    protected void doInit() throws SensorHubException
    {
        super.doInit();
        
        // add geoloc output
        geolocOutput = new TruPulseGeolocOutput(this);
        addOutput(geolocOutput, false);
    }


    @Override
    protected void doStart() throws SensorHubException
    {
        super.doStart();
        if (geolocOutput != null)
            geolocOutput.start();
    }


    @Override
    protected void doStop() throws SensorHubException
    {
        if (geolocOutput != null)
            geolocOutput.stop();
        super.doStop();
    }
}
