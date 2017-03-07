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

package org.sensorhub.impl.sensor.waterdata;

import net.opengis.sensorml.v20.IdentifierList;
import net.opengis.sensorml.v20.Term;
import org.sensorhub.impl.module.RobustConnection;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.api.common.SensorHubException;
import org.vast.sensorML.SMLFactory;
import org.vast.swe.SWEHelper;


/**
 * <p>
 * Implementation of sensor interface for USGS Water Data using IP
 * protocol. This particular class stores configuration parameters.
 * </p>
 * 
 * @author Lee Butler <labutler10@gmail.com>
 * @since October 30, 2014
 */

public class WaterDataDriver extends AbstractSensorModule <WaterDataConfig> {
    RobustConnection connection;
    WaterDataOutput waterOut;
    
    String jsonURL;
    String serialNumber;
    String modelNumber;
    String longName;
    String shortName;
    
    public WaterDataDriver() {

    }
    
    @Override
    public void setConfiguration(final WaterDataConfig config) {
        super.setConfiguration(config);
    };


    @Override
    public void init() throws SensorHubException {
        // reset internal state in case init() was already called
        super.init();
        
        // init main data interface
        waterOut = new WaterDataOutput(this);
        addOutput(waterOut, false);

        // generate identifiers
        generateUniqueID("urn:usgs:water:", config.siteCode);
        generateXmlID("USGS_WATER_DATA_", config.siteCode);
        
        jsonURL = config.getUrlBase() + "&sites=" + config.getSiteCode() + "&parameterCd=00060,00065";
        waterOut.init();
    }


    @Override
    public void start() throws SensorHubException {
    	waterOut.start();
    }


    @Override
    protected void updateSensorDescription() {
        synchronized(sensorDescLock) {
            // parent class reads SensorML from config if provided
            // and then sets unique ID, outputs and control inputs
            super.updateSensorDescription();

            SMLFactory smlFac = new SMLFactory();

            if (!sensorDescription.isSetDescription())
                sensorDescription.setDescription("USGS Water Data");

            IdentifierList identifierList = smlFac.newIdentifierList();
            sensorDescription.addIdentification(identifierList);

            Term term;
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("Manufacturer"));
            term.setLabel("Manufacturer Name");
            term.setValue("USGS");
            identifierList.addIdentifier2(term);

            if (modelNumber != null) {
                term = smlFac.newTerm();
                term.setDefinition(SWEHelper.getPropertyUri("ModelNumber"));
                term.setLabel("Model Number");
                term.setValue(modelNumber);
                identifierList.addIdentifier2(term);
            }

            if (serialNumber != null) {
                term = smlFac.newTerm();
                term.setDefinition(SWEHelper.getPropertyUri("SerialNumber"));
                term.setLabel("Serial Number");
                term.setValue(serialNumber);
                identifierList.addIdentifier2(term);
            }

            if (longName != null) {
                term = smlFac.newTerm();
                term.setDefinition(SWEHelper.getPropertyUri("LongName"));
                term.setLabel("Long Name");
                term.setValue(longName);
                identifierList.addIdentifier2(term);
            }

            if (shortName != null) {
                term = smlFac.newTerm();
                term.setDefinition(SWEHelper.getPropertyUri("ShortName"));
                term.setLabel("Short Name");
                term.setValue(shortName);
                identifierList.addIdentifier2(term);
            }
        }
    }


    @Override
    public boolean isConnected() {
        if (connection == null)
            return false;

        return connection.isConnected();
    }


    @Override
    public void stop() {
        if (connection != null)
            connection.cancel();

        if (waterOut != null)
        	waterOut.stop();
    }


    @Override
    public void cleanup() {}
}