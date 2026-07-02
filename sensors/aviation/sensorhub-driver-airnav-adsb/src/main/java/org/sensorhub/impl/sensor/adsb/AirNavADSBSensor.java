/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.adsb;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.opengis.sensorml.v20.IdentifierList;
import net.opengis.sensorml.v20.Term;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.vast.ogc.gml.IFeature;
import org.vast.ogc.om.MovingFeature;
import org.vast.sensorML.SMLFactory;
import org.vast.swe.SWEHelper;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;


public class AirNavADSBSensor extends AbstractSensorModule<AirNavADSBConfig>
{
    static final String SENSOR_UID_PREFIX = "urn:osh:sensor:adsb:";

    ICommProvider<?> commProvider;
    AirNavADSBOutput output;
    InputStream msgIn;
    private MessageParser parser;
    private final ConcurrentHashMap<String, AircraftState> aircraftMap = new ConcurrentHashMap<>();
    private final AtomicBoolean started = new AtomicBoolean(false);
    private Thread workerThread;
    private ExecutorService lookupExecutor;

    HttpClient client;

    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
            super.updateSensorDescription();

            SMLFactory smlFac = new SMLFactory();

            if (!sensorDescription.isSetDescription())
                sensorDescription.setDescription("AirNav ADS-B FlightStick receiver, aircraft surveillance data ingested from dump1090 (SBS or Beast format)");

            IdentifierList identifierList = smlFac.newIdentifierList();
            sensorDescription.addIdentification(identifierList);

            Term term;
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("Manufacturer"));
            term.setLabel("Manufacturer");
            term.setValue("AirNav Systems");
            identifierList.addIdentifier(term);

            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("ModelNumber"));
            term.setLabel("Model Number");
            term.setValue("RadarBox FlightStick");
            identifierList.addIdentifier(term);
        }
    }

    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();

        generateUniqueID(SENSOR_UID_PREFIX, config.serialNumber);
        generateXmlID("AIRNAV_ADSB_", config.serialNumber);

        output = new AirNavADSBOutput(this);
        addOutput(output, false);
        output.init();
    }

    @Override
    protected void doStart() throws SensorHubException {
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        lookupExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "adsb-icao-lookup-" + config.serialNumber);
            t.setDaemon(true);
            return t;
        });

        if (commProvider == null) {
            if (config.commSettings == null)
                throw new SensorHubException("No communication settings specified");

            try {
                var moduleReg = getParentHub().getModuleRegistry();
                commProvider = (ICommProvider<?>)moduleReg.loadSubModule(config.commSettings, true);
                commProvider.start();
            } catch (Exception e) {
                commProvider = null;
                throw new SensorHubException("Failed to start AirNav ADS-B Sensor", e);
            }
        }

        try {
            msgIn = new BufferedInputStream(commProvider.getInputStream());
        } catch (IOException e) {
            throw new SensorHubException("Error while reading input stream", e);
        }

        if (config.inputFormat == AirNavADSBConfig.InputFormat.BEAST)
            parser = new BeastParser(msgIn, aircraftMap);
        else
            parser = new SbsParser(msgIn, aircraftMap);

        started.set(true);

        workerThread = new Thread(() -> {
            while (started.get()) {
                try {
                    AircraftState state = parser.readNext();

                    if (state != null)
                        output.publishAircraftState(state);
                    else if (!started.get())
                        break;
                } catch (IOException e) {
                    if (!started.get() || "Stream closed".equalsIgnoreCase(e.getMessage()))
                        return;
                    getLogger().error("Error reading ADS-B stream", e);
                }
            }
        }, "adsb-reader-" + config.serialNumber);
        workerThread.setDaemon(true);
        workerThread.start();
    }

    @Override
    protected void doStop() throws SensorHubException
    {
        started.set(false);

        if (workerThread != null)
        {
            workerThread.interrupt();
            workerThread = null;
        }

        if (lookupExecutor != null)
            lookupExecutor.shutdownNow();

        parser = null;

        if (commProvider != null)
        {
            try {
                commProvider.stop();
            } finally {
                commProvider = null;
            }
        }

        if (msgIn != null)
        {
            try {msgIn.close(); } catch (IOException ignore) { }
            msgIn = null;
        }

        aircraftMap.clear();
    }

    @Override
    public boolean isConnected()
    {
        return commProvider != null && commProvider.isStarted();
    }

    private String lookupICAOAddress(String icaoAddress) {
        String url = config.icaoAddressLookupUrl + icaoAddress;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return response.body();
            } else {
                logger.info("Status Code: " + response.statusCode());
            }

        } catch (Exception e) {
            logger.error("Error connecting to icao address " + icaoAddress, e);
        }
        return null;
    }

    String addFoi(String icaoAddress, String callsign) {
        String foiUID = SENSOR_UID_PREFIX + icaoAddress;

        synchronized (foiMap) {
            if (!foiMap.containsKey(foiUID)) {
                MovingFeature foi = new MovingFeature();
                foi.setId(icaoAddress);
                foi.setUniqueIdentifier(foiUID);
                foi.setDescription("Aircraft " + icaoAddress);

                if (callsign != null && !callsign.isEmpty())
                    foi.setName(callsign);
                else
                    foi.setName(icaoAddress);

                addFoi(foi);
                logger.debug("New aircraft added as FOI: {}", foiUID);

                lookupExecutor.submit(() -> updateFoi(foiUID, icaoAddress));
            }
        }
        return foiUID;
    }

    private void updateFoi(String foiUID, String icaoAddress) {
        String icaoLookup = lookupICAOAddress(icaoAddress);
        if (icaoLookup == null)
            return;

        try {
            JsonObject root = JsonParser.parseString(icaoLookup).getAsJsonObject();
            JsonObject response = root.getAsJsonObject("response");
            if (response == null)
                return;
            JsonObject aircraft = response.getAsJsonObject("aircraft");
            if (aircraft == null)
                return;

            String registration = getJsonString(aircraft, "registration");
            String manufacturer = getJsonString(aircraft, "manufacturer");
            String icaoTypeCode = getJsonString(aircraft, "icao_type");
            String aircraftType = getJsonString(aircraft, "type");
            String registeredOwnerCountryIso = getJsonString(aircraft, "registered_owner_country_iso_name");
            String registeredOwnerCountry = getJsonString(aircraft, "registered_owner_country_name");
            String registeredOperatorFlag = getJsonString(aircraft, "registered_owner_operator_flag_code");
            String registeredOwner = getJsonString(aircraft, "registered_owner");
            String urlPhoto = getJsonString(aircraft, "url_photo");

            synchronized (foiMap) {
                IFeature existingFoi = foiMap.get(foiUID);
                if (existingFoi instanceof MovingFeature) {
                    MovingFeature foi = (MovingFeature) existingFoi;
                    foi.setProperty("registration", registration);
                    foi.setProperty("manufacturer", manufacturer);
                    foi.setProperty("icaoTypeCode", icaoTypeCode);
                    foi.setProperty("aircraftType", aircraftType);
                    foi.setProperty("registeredOwnerCountryIso", registeredOwnerCountryIso);
                    foi.setProperty("registeredOwnerCountry", registeredOwnerCountry);
                    foi.setProperty("registeredOperatorFlag", registeredOperatorFlag);
                    foi.setProperty("registeredOwner", registeredOwner);
                    foi.setProperty("urlPhoto", urlPhoto);
                    foi.setDescription(registration + " (" + aircraftType + ") operated by " + registeredOwner);
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing ICAO lookup response for {}", icaoAddress, e);
        }
    }

    private String getJsonString(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull())
            return obj.get(key).getAsString();
        return "";
    }

}
