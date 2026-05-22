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

import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.Point;
import net.opengis.gml.v32.impl.GMLFactory;
import net.opengis.sensorml.v20.ClassifierList;
import net.opengis.sensorml.v20.ContactList;
import net.opengis.sensorml.v20.IdentifierList;
import net.opengis.sensorml.v20.Term;
import org.isotc211.v2005.gmd.CIResponsibleParty;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.vast.ogc.gml.IFeature;
import org.vast.ogc.om.MovingFeature;
import org.vast.sensorML.SMLFactory;
import org.vast.sensorML.SMLHelper;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;

import java.io.*;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


public class AirNavADSBSensor extends AbstractSensorModule<AdsbConfig>
{
    static final String SENSOR_UID_PREFIX = "urn:osh:sensor:adsb:";

    ICommProvider<?> commProvider;
    AirNavADSBOutput output;
    InputStream msgIn;
    private BufferedReader reader;
    private final ConcurrentHashMap<String, AircraftState> aircraftMap = new ConcurrentHashMap<>();
    private final AtomicBoolean started = new AtomicBoolean(false);
    private Thread workerThread;
    private Process dump1090Process;
    String modelNumber;


    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
            super.updateSensorDescription();

            SMLFactory smlFac = new SMLFactory();

            if (!sensorDescription.isSetDescription())
                sensorDescription.setDescription("AirNav ADS-B FlightStick receiver, aircraft surveillance data ingested from dump1090 SBS TCP stream");

            IdentifierList identifierList = smlFac.newIdentifierList();
            sensorDescription.addIdentification(identifierList);

            Term term;
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("Manufacturer"));
            term.setLabel("Manufacturer");
            term.setValue("AirNav Systems");
            identifierList.addIdentifier(term);

            if (modelNumber != null) {
                term = smlFac.newTerm();
                term.setDefinition(SWEHelper.getPropertyUri("ModelNumber"));
                term.setLabel("Model Number");
                term.setValue("RadarBox FlightStick");
                term.setValue(modelNumber);
                identifierList.addIdentifier(term);
            }

            if (config.serialNumber != null) {
                term = smlFac.newTerm();
                term.setDefinition(SWEHelper.getPropertyUri("SerialNumber"));
                term.setLabel("Serial Number");
                term.setValue(config.serialNumber);
                identifierList.addIdentifier(term);

            }
        }
    }

    @Override
    protected void doInit() throws SensorHubException {
        generateUniqueID(SENSOR_UID_PREFIX, config.serialNumber);
        generateXmlID("AIRNAV_ADSB_", config.serialNumber);

        output = new AirNavADSBOutput(this);
        addOutput(output, false);
        output.init();
    }

    @Override
    protected void doStart() throws SensorHubException {
        startDump1090();

        if (commProvider == null) {
            try {
                if (config.commSettings == null)
                    throw new SensorHubException("No communication settings specified");

                var moduleReg = getParentHub().getModuleRegistry();
                commProvider = (ICommProvider<?>)moduleReg.loadSubModule(config.commSettings, true);
                commProvider.start();
            } catch (SensorHubException e) {
                commProvider = null;
                reportError("Failed to start AirNav ADS-B Sensor", e);
            }
        }

        try {
            msgIn = new BufferedInputStream(commProvider.getInputStream());
            reader = new BufferedReader(new InputStreamReader(msgIn));

            getLogger().info("Connected to ADS-B Sensor");
        } catch (IOException e) {
            throw new SensorHubException("Error while reading input stream", e);
        }

        started.set(true);

        workerThread = new Thread(() -> {
            while (started.get()) {
                handleMessage();
            }
        }, "adsb-reader-" + config.serialNumber);
        workerThread.setDaemon(true);
        workerThread.start();
    }


    public void handleMessage() {
        try {
            String line = reader.readLine();
            if (line != null)
                parseSbsLine(line);
        } catch (IOException e) {
            if (!started.get() || "Stream closed".equalsIgnoreCase(e.getMessage())) {
                return;
            }

            getLogger().error("Error reading dump1090 SBS stream", e);
        }
    }

    private void parseSbsLine(String line) {
        String[] fields = line.split(",", -1);
        if (fields.length < 11 || !"MSG".equals(fields[0]))
            return;

        String msgType = fields[1].trim();
        String icao = fields[4].trim();
        if (icao.isEmpty())
            return;

        AircraftState state = aircraftMap.computeIfAbsent(icao, k -> {
            AircraftState s = new AircraftState();
            s.icao = k;
            return s;
        });

        switch (msgType) {
            case "1": // callsign
                if (fields.length > 10 && !fields[10].trim().isEmpty())
                    state.callsign = fields[10].trim();
                break;

            case "3": // position — lat, lon, altitude
                if (fields.length > 15) {
                    if (!fields[11].trim().isEmpty())
                        state.altFt = Double.parseDouble(fields[11].trim());
                    if (!fields[14].trim().isEmpty())
                        state.lat = Double.parseDouble(fields[14].trim());
                    if (!fields[15].trim().isEmpty())
                        state.lon = Double.parseDouble(fields[15].trim());
                }
                state.lastUpdateTime = System.currentTimeMillis();
                if (state.hasPosition())
                    output.publishAircraftState(state);
                break;

            case "4": // velocity — ground speed, heading, vertical rate
                if (fields.length > 16) {
                    if (!fields[12].trim().isEmpty())
                        state.groundSpeed = Double.parseDouble(fields[12].trim());
                    if (!fields[13].trim().isEmpty())
                        state.heading = Double.parseDouble(fields[13].trim());
                    if (!fields[16].trim().isEmpty())
                        state.verticalRate = Double.parseDouble(fields[16].trim());
                }
                state.lastUpdateTime = System.currentTimeMillis();
                break;

            case "5": // surveillance Alt
                if (fields.length > 11 && !fields[11].trim().isEmpty())
                    state.altFt = Double.parseDouble(fields[11].trim());
                state.lastUpdateTime = System.currentTimeMillis();
                break;

            case "6": // surveillance ID — squawk
                if (fields.length > 17 && !fields[17].trim().isEmpty())
                    state.squawk = fields[17].trim();
                state.lastUpdateTime = System.currentTimeMillis();
                break;

            default:
                break;
        }
        if (fields.length > 18 && !fields[18].trim().isEmpty())
            state.alert = "-1".equals(fields[18].trim());
        if (fields.length > 19 && !fields[19].trim().isEmpty())
            state.emergency = "-1".equals(fields[19].trim());
        if (fields.length > 21 && !fields[21].trim().isEmpty())
            state.isOnGround = "-1".equals(fields[21].trim());
    }

    private void startDump1090() throws SensorHubException {
        try {
            var cmd = new ProcessBuilder(
                "dump1090",
                "--net",
                "--device-index", String.valueOf(0),
                "--quiet"
            );
            cmd.redirectErrorStream(true);
            dump1090Process = cmd.start();

            Thread.sleep(2000);

            if (!dump1090Process.isAlive())
                throw new SensorHubException("dump1090 exited immediately — check the path and RTL-SDR device");

            getLogger().info("Started dump1090 (pid {})", dump1090Process.pid());
        } catch (IOException e) {
            throw new SensorHubException("Failed to start dump1090", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SensorHubException("Interrupted while waiting for dump1090 to start", e);
        }
    }

    private void stopDump1090() {
        if (dump1090Process != null) {
            dump1090Process.destroy();
            try {
                if (!dump1090Process.waitFor(5, TimeUnit.SECONDS))
                    dump1090Process.destroyForcibly();
            } catch (InterruptedException e) {
                dump1090Process.destroyForcibly();
                Thread.currentThread().interrupt();
            }
            getLogger().info("Stopped dump1090");
            dump1090Process = null;
        }
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

        if (commProvider != null)
        {
            try {
                commProvider.stop();
            } finally {
                commProvider = null;
            }
        }

        if (reader != null) {
            try { reader.close(); } catch (IOException ignore) { }
            reader = null;
        }

        if (msgIn != null)
        {
            try {msgIn.close(); } catch (IOException ignore) { }
            msgIn = null;
        }

        stopDump1090();
        aircraftMap.clear();
    }

    @Override
    public boolean isConnected()
    {
        return commProvider != null && commProvider.isStarted();
    }

    String addFoi(String aircraftId, String callsign, Point point) {
        String foiUID = SENSOR_UID_PREFIX + aircraftId;

        if (!foiMap.containsKey(foiUID)) {
            MovingFeature foi = new MovingFeature();
            foi.setId(aircraftId);
            foi.setUniqueIdentifier(foiUID);

            if (callsign != null && !callsign.isEmpty())
                foi.setName(callsign);
            else
                foi.setName(aircraftId);
            foi.setDescription("Aircraft " + aircraftId);
            foi.setGeometry(point);

            addFoi(foi);
            logger.debug("New aircraft added as FOI: {}", foiUID);
        } else {
            IFeature existingFoi = foiMap.get(foiUID);
            if (existingFoi instanceof AbstractFeature) {
                ((AbstractFeature) existingFoi).setGeometry(point);
                logger.debug("Updated geometry for FOI: {}", foiUID);
            } else {
                logger.warn("FOI {} found in map but cannot be updated (null or wrong type)", foiUID);
            }
        }
        return foiUID;
    }

    public Point getGeometry(double lat, double lon){
        GMLFactory fac = new GMLFactory(true);
        Point p = fac.newPoint();

        p.setSrsDimension(2);
        p.setSrsName(SWEConstants.REF_FRAME_4326);
        p.setPos(new double[] { lat, lon } );

        return p;
    }
}
