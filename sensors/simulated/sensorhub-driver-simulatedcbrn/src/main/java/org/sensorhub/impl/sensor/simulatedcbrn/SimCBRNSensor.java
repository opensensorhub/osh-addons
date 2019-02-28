/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.simulatedcbrn;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;


/**
 * <p>
 *     Driver implementation that outputs simulated CBRN sensor data
 *     by randomly increasing or decreasing concentrations of toxic agents.
 *     Other outputs are then updated based on the randomized values.
 *     Serves as a simple, deployable sensor and as an example of
 *     a CBRN sensor driver.
 * </p>
 *
 * @author Ian Patterson
 * @since May 2, 2017
 */

public class SimCBRNSensor extends AbstractSensorModule<SimCBRNConfig>
{
    SimCBRNOutputID idDataInterface;
    SimCBRNOutputAlerts alertsDataInterface;
   // SimCBRNOutputReadings readingsDataInterface;
    SimCBRNOutputMaintenance maintDataInterface;
    SimCBRNOutputStatus statusDataInterface;

    // To create the simulated data for the sensor
    //CBRNSimulatedData simData = new CBRNSimulatedData(getConfiguration());


    @Override
    public void init() throws SensorHubException
    {
        super.init();

        // generate ID
        generateUniqueID("urn:osh:sensor:simcbrn:", config.serialNumber);
        generateXmlID("CBRN_SENSOR_", config.serialNumber);

        // Create output interfaces
        idDataInterface = new SimCBRNOutputID(this);
        addOutput(idDataInterface, false);
        idDataInterface.init();

        alertsDataInterface = new SimCBRNOutputAlerts(this);
        addOutput(alertsDataInterface, false);
        alertsDataInterface.init();

        //TODO: add this back in after testing if the duplication causes a problem, also add back to stop and start fcn's
//        readingsDataInterface = new SimCBRNOutputReadings(this);
//        addOutput(readingsDataInterface, false);
//        readingsDataInterface.init();

        maintDataInterface = new SimCBRNOutputMaintenance(this);
        addOutput(maintDataInterface,false);
        maintDataInterface.init();

        statusDataInterface = new SimCBRNOutputStatus(this);
        addOutput(statusDataInterface,false);
        statusDataInterface.init();
    }


    @Override
    public void start() throws SensorHubException
    {
        idDataInterface.start();
        alertsDataInterface.start();
        //readingsDataInterface.start();
        maintDataInterface.start();
        statusDataInterface.start();
    }


    @Override
    public void stop() throws SensorHubException
    {
        idDataInterface.stop();
        alertsDataInterface.stop();
       // readingsDataInterface.stop();
        maintDataInterface.stop();
        statusDataInterface.stop();
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
