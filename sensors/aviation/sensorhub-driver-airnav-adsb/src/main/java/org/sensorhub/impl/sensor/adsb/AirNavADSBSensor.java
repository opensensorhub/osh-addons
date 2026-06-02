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
import net.opengis.sensorml.v20.IdentifierList;
import net.opengis.sensorml.v20.Term;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.vast.ogc.gml.IFeature;
import org.vast.ogc.om.MovingFeature;
import org.vast.sensorML.SMLFactory;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
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
        try {
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
                            state.track = Double.parseDouble(fields[13].trim());
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
        } catch (NumberFormatException e) {
            getLogger().debug("Skipping malformed SBS line: {}", line, e);
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

        aircraftMap.clear();
    }

    @Override
    public boolean isConnected()
    {
        return commProvider != null && commProvider.isStarted();
    }

    String addFoi(String aircraftId, String callsign, Point point) {
        String foiUID = SENSOR_UID_PREFIX + aircraftId;

        synchronized (foiMap) {
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
                }
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
