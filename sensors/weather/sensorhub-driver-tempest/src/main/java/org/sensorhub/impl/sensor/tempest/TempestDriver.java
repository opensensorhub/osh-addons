/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.tempest;

import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.tempest.outputs.*;
import org.vast.ogc.om.MovingFeature;

import java.io.IOException;
import java.io.InputStream;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Driver implementation for the sensor.
 * <p>
 * This class is responsible for providing sensor information, managing output registration,
 * and performing initialization and shutdown for the driver and its outputs.
 */
public class TempestDriver extends AbstractSensorModule<TempestConfig> {
    static final String UID_PREFIX = "urn:osh:sensor:weather:tempest:";
    static final String XML_PREFIX = "weather:tempest:";

    // GLOBAL VARIABLES FOR SENSOR OPERATION
    TempestOutputAirObservation tempestOutputAirObservation;
    TempestOutputDeviceStatus tempestOutputDeviceStatus;
    TempestOutputHubStatus tempestOutputHubStatus;
    TempestOutputLightningStrikeEvent tempestOutputLightningStrikeEvent;
    TempestOutputObservation tempestOutputObservation;
    TempestOutputRainStartEvent tempestOutputRainStartEvent;
    TempestOutputRapidWind tempestOutputRapidWind;
    TempestOutputSkyObservation tempestOutputSkyObservation;


    ICommProvider<?> commProvider;
    volatile boolean started;
    private final ObjectMapper mapper = new ObjectMapper();
    private Thread readerThread;

    //  INITIALIZE
    @Override
    public void doInit() throws SensorHubException {
        super.doInit();

        // Create SensorHub Identifiers using designated prefix and serial number from Admin Panel
        generateUniqueID(UID_PREFIX, config.serialNumber);
        generateXmlID(XML_PREFIX, config.serialNumber);

        // INITIALIZE OUTPUT
        tempestOutputAirObservation = new TempestOutputAirObservation(this);
        addOutput(tempestOutputAirObservation, false);
        tempestOutputAirObservation.doInit();

        tempestOutputDeviceStatus = new TempestOutputDeviceStatus(this);
        addOutput(tempestOutputDeviceStatus, false);
        tempestOutputDeviceStatus.doInit();

        tempestOutputHubStatus = new TempestOutputHubStatus(this);
        addOutput(tempestOutputHubStatus, false);
        tempestOutputHubStatus.doInit();

        tempestOutputLightningStrikeEvent = new TempestOutputLightningStrikeEvent(this);
        addOutput(tempestOutputLightningStrikeEvent, false);
        tempestOutputLightningStrikeEvent.doInit();

        tempestOutputObservation = new TempestOutputObservation(this);
        addOutput(tempestOutputObservation, false);
        tempestOutputObservation.doInit();

        tempestOutputRainStartEvent = new TempestOutputRainStartEvent(this);
        addOutput(tempestOutputRainStartEvent, false);
        tempestOutputRainStartEvent.doInit();

        tempestOutputRapidWind = new TempestOutputRapidWind(this);
        addOutput(tempestOutputRapidWind, false);
        tempestOutputRapidWind.doInit();

        tempestOutputSkyObservation = new TempestOutputSkyObservation(this);
        addOutput(tempestOutputSkyObservation, false);
        tempestOutputSkyObservation.doInit();

    }

    @Override
    public void doStart() throws SensorHubException {
        super.doStart();

        // Init comm provider — recreated here so it picks up any config changes made via the UI
        if (commProvider == null) {
            if (config.commSettings == null)
                throw new SensorHubException("No communication settings specified");
            try {
                var moduleReg = getParentHub().getModuleRegistry();
                commProvider = (ICommProvider<?>) moduleReg.loadSubModule(config.commSettings, true);
                commProvider.start();
            } catch (Exception e) {
                commProvider = null;
                throw e;
            }
        }

        final InputStream inputStream;
        try {
            inputStream = commProvider.getInputStream();
            getLogger().info("Connected to Tempest Data Stream");
        } catch (IOException e) {
            throw new SensorHubException("Error opening Tempest input stream", e);
        }

        started = true;
        readerThread = new Thread(() -> {
            try {
                processStream(inputStream);
            } catch (Exception e) {
                getLogger().error("Tempest stream error", e);
            }
        }, "tempest-reader");
        readerThread.setDaemon(true);
        readerThread.start();

    }

    @Override
    public void doStop() throws SensorHubException {
        started = false;

        if (commProvider != null) {
            commProvider.stop();
            commProvider = null;
        }

        super.doStop();
    }

    @Override
    public boolean isConnected() {
        return commProvider != null;
    }

    /**
     * Registers a Tempest weather station as a Feature of Interest (FOI) keyed by its serial number.
     * Subsequent calls with the same serial number are no-ops; the existing FOI UID is returned.
     */
    public String addFoi(String serialNum) {
        String foiUID = UID_PREFIX + "foi:" + serialNum;

        if (!foiMap.containsKey(foiUID)) {
            MovingFeature foi = new MovingFeature();
            foi.setId(serialNum);
            foi.setUniqueIdentifier(foiUID);
            foi.setName("Tempest: " + serialNum);
            foi.setDescription("Tempest weather station: " + serialNum);
            addFoi(foi);
            getLogger().debug("New Tempest weather station added as FOI: {}", foiUID);
        }

        return foiUID;
    }

    private void processStream(InputStream inputStream)
    {
        StringBuilder sb = new StringBuilder();
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        try {
            int b;
            while (started && (b = inputStream.read()) != -1) {
                char c = (char) b;
                if (escape) {
                    escape = false;
                } else if (c == '\\' && inString) {
                    escape = true;
                } else if (c == '"') {
                    inString = !inString;
                } else if (!inString) {
                    if (c == '{') depth++;
                    else if (c == '}') {
                        depth--;
                        if (depth == 0) {
                            sb.append(c);
                            decodeMessage(sb.toString().trim());
                            sb.setLength(0);
                            continue;
                        }
                    }
                }
                sb.append(c);
            }
        } catch (IOException e) {
            if (started)
                getLogger().error("Error reading Tempest data stream", e);
        }
    }

    private void decodeMessage(String jsonString)
    {
        try
        {
            JsonNode tempestJsonMsg = mapper.readTree(jsonString);

            String type = tempestJsonMsg.path("type").asText();

            switch (type)
            {
                case "obs_air":
                    tempestOutputAirObservation.setData(tempestJsonMsg);
                    break;
                case "device_status":
                    tempestOutputDeviceStatus.setData(tempestJsonMsg);
                    break;
                case "evt_strike":
                    tempestOutputLightningStrikeEvent.setData(tempestJsonMsg);
                    break;
                case "obs_st":
                    tempestOutputObservation.setData(tempestJsonMsg);
                    break;
                case "evt_precip":
                    tempestOutputRainStartEvent.setData(tempestJsonMsg);
                    break;
                case "rapid_wind":
                    tempestOutputRapidWind.setData(tempestJsonMsg);
                    break;
                case "obs_sky":
                    tempestOutputSkyObservation.setData(tempestJsonMsg);
                    break;
                case "hub_status":
                    tempestOutputHubStatus.setData(tempestJsonMsg);
                    break;
                default:
                    getLogger().debug("Unhandled Tempest message type: {}", type);
            }
        }
        catch (Exception e)
        {
            getLogger().warn("Failed to decode Tempest JSON: {}", jsonString, e);
        }
    }

}