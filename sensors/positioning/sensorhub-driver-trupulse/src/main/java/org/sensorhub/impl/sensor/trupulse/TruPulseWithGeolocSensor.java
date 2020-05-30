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
import org.sensorhub.api.processing.DataSourceConfig.InputLinkConfig;
import org.sensorhub.api.processing.StreamingDataSourceConfig;
import org.sensorhub.impl.process.trupulse.TargetGeolocConfig;
import org.sensorhub.impl.process.trupulse.TargetGeolocProcess;


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
    TargetGeolocProcess geolocProcess;
    TruPulseGeolocOutput geolocOutput;
    
    
    @Override
    public void init() throws SensorHubException
    {
        super.init();
        
        // create geoloc processing config
        TargetGeolocConfig geolocProcessConfig = new TargetGeolocConfig();
        
        StreamingDataSourceConfig trupulseSrc = new StreamingDataSourceConfig();
        trupulseSrc.producerID = getLocalID();
        InputLinkConfig conn1 = new InputLinkConfig();
        conn1.source = dataInterface.getName();
        conn1.destination = dataInterface.getName();
        trupulseSrc.inputConnections.add(conn1);
        geolocProcessConfig.dataSources.add(trupulseSrc);
        
        StreamingDataSourceConfig locationSrc = new StreamingDataSourceConfig();
        locationSrc.producerID = ((TruPulseWithGeolocConfig)config).locationSourceID;
        InputLinkConfig conn2 = new InputLinkConfig();
        conn2.source = ((TruPulseWithGeolocConfig)config).locationOutputName;
        conn2.destination = "sensorLocation";
        locationSrc.inputConnections.add(conn2);
        geolocProcessConfig.dataSources.add(locationSrc);
        
        // add geoloc processing
        geolocProcess = new TargetGeolocProcess();        
        geolocProcess.init(geolocProcessConfig);
                
        // add geoloc output
        geolocOutput = new TruPulseGeolocOutput(this, geolocProcess.getAllOutputs().get("targetLocation"));
        addOutput(geolocOutput, false);
    }


    @Override
    public void start() throws SensorHubException
    {
        super.start();
        geolocProcess.start();
    }


    @Override
    public void stop() throws SensorHubException
    {
        geolocProcess.stop();
        super.stop();        
    }
}
