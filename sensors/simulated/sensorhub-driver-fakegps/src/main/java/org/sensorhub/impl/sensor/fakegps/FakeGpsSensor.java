/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.fakegps;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.vast.sensorML.SMLHelper;
import org.vast.swe.SWEHelper;
import net.opengis.sensorml.v20.PhysicalSystem;


/**
 * <p>
 * Driver implementation outputing simulated GPS data after
 * requesting trajectories from Google Directions.
 * </p>
 *
 * @author Alex Robin
 * @since Nov 2, 2014
 */
public class FakeGpsSensor extends AbstractSensorModule<FakeGpsConfig>
{
    FakeGpsOutput dataInterface;
    
    
    public FakeGpsSensor()
    {
    }
    
    
    @Override
    protected void doInit() throws SensorHubException
    {
        super.doInit();
        
        // validate config
        config.validate();
        
        // generate IDs
        generateUniqueID("urn:osh:sensor:simgps:", null);
        generateXmlID("GPS_SENSOR_", null);
        
        // init main data interface
        dataInterface = new FakeGpsOutput(this);
        addOutput(dataInterface, false);
        dataInterface.init();
    }
    
    
    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
            super.updateSensorDescription();
            sensorDescription.setDescription("Simulated GPS sensor generating data along random itineraries obtained using Google Direction API");
            
            var sml = new SMLHelper();
            sml.edit((PhysicalSystem)sensorDescription)
            
                .addIdentifier(sml.identifiers.serialNumber("45AC78EDF"))
                
                .addClassifier(sml.classifiers.sensorType("Global Navigation Satellite System (GNSS) Receiver"))
                .addClassifier(sml.classifiers.sensorType(
                    "http://vocab.nerc.ac.uk/collection/D01/current/D0100002",
                    "http://vocab.nerc.ac.uk/collection/L05/current/POS02"))
                            
                .addCharacteristicList("operating_specs", sml.characteristics.operatingCharacteristics()
                    .add("voltage", sml.characteristics.operatingVoltageRange(3.3, 5., "V"))
                    .add("temperature", sml.conditions.temperatureRange(-10., 75., "Cel")))
            
                .addCapabilityList("system_caps", sml.capabilities.systemCapabilities()
                    .add("update_rate", sml.capabilities.reportingFrequency(1.0))
                    .add("accuracy", sml.capabilities.absoluteAccuracy(2.5, "m"))
                    .add("ttff_cold", sml.createQuantity()
                        .definition(SWEHelper.getDBpediaUri("Time_to_first_fix"))
                        .label("Cold Start TTFF")
                        .description("Time to first fix on cold start")
                        .uomCode("s")
                        .value(120))
                    .add("ttff_warm", sml.createQuantity()
                        .definition(SWEHelper.getDBpediaUri("Time_to_first_fix"))
                        .label("Warm Start TTFF")
                        .description("Time to first fix on warm start")
                        .uomCode("s")
                        .value(30))
                    .add("ttff_hot", sml.createQuantity()
                        .definition(SWEHelper.getDBpediaUri("Time_to_first_fix"))
                        .label("Hot Start TTFF")
                        .description("Time to first fix on hot start")
                        .uomCode("s")
                        .value(5))
                    .add("battery_life", sml.characteristics.batteryLifetime(72, "h")));
        }
    }


    @Override
    protected void doStart() throws SensorHubException
    {
        dataInterface.start();        
    }
    

    @Override
    protected void doStop() throws SensorHubException
    {
        if (dataInterface != null)
            dataInterface.stop();
    }
    

    @Override
    public void cleanup() throws SensorHubException
    {
       
    }
    
    
    @Override
    public boolean isConnected()
    {
        return true;
    }
}
