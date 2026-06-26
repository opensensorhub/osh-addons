/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.nmeaais;

import dk.dma.ais.reader.AisReader;
import dk.dma.ais.reader.AisReaders;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.vast.ogc.gml.IFeature;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.nmeaais.helpers.AisCodeHelper;
import org.sensorhub.impl.sensor.nmeaais.outputs.NmeaAisOutputAidNavigation;
import org.sensorhub.impl.sensor.nmeaais.outputs.NmeaAisOutputBaseStation;
import org.sensorhub.impl.sensor.nmeaais.outputs.NmeaAisOutputRawMessages;
import org.sensorhub.impl.sensor.nmeaais.outputs.NmeaAisOutputVesselLocation;
import org.sensorhub.impl.sensor.nmeaais.outputs.NmeaAisOutputVoyageInfo;
import org.vast.ogc.gml.GenericFeatureImpl;
import org.vast.ogc.om.MovingFeature;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * Driver implementation for the NMEA AIS sensor.
 * <p>
 * Outputs are organized by semantic concern:
 * - Vessel Location (types 1/2/3/18/19) — dynamic position
 * - Voyage Info (type 5) — voyage-specific data
 * - Aid to Navigation (type 21) — navigation aids
 * - Base Station (types 4/11) — shore infrastructure
 * - Raw Messages (all) — raw NMEA sentences
 * <p>
 * Static vessel identity (name, callsign, ship type, dimensions, IMO, etc.) is stored
 * as properties on the vessel FOI. Navigational status is streamed in the vesselLocation output.
 */
public class NmeaAisDriver extends AbstractSensorModule<NmeaAisConfig> {
    static final String UID_PREFIX = "urn:osh:sensor:nmea:ais:";
    static final String XML_PREFIX = "nmea:ais:";

    NmeaAisHandler nmeaAisHandler;
    NmeaAisOutputRawMessages    nmeaAisOutputRawMessages;
    NmeaAisOutputVesselLocation nmeaAisOutputVesselLocation;
    NmeaAisOutputVoyageInfo     nmeaAisOutputVoyageInfo;
    NmeaAisOutputBaseStation    nmeaAisOutputBaseStation;
    NmeaAisOutputAidNavigation  nmeaAisOutputAidNavigation;

    ICommProvider<?> commProvider;
    AisReader aisReader;
    volatile boolean started;

    @Override
    public void doInit() throws SensorHubException {
        super.doInit();

        generateUniqueID(UID_PREFIX, config.serialNumber);
        generateXmlID(XML_PREFIX, config.serialNumber);

        nmeaAisOutputRawMessages = new NmeaAisOutputRawMessages(this);
        addOutput(nmeaAisOutputRawMessages, false);
        nmeaAisOutputRawMessages.doInit();

        nmeaAisOutputVesselLocation = new NmeaAisOutputVesselLocation(this);
        addOutput(nmeaAisOutputVesselLocation, false);
        nmeaAisOutputVesselLocation.doInit();

        nmeaAisOutputVoyageInfo = new NmeaAisOutputVoyageInfo(this);
        addOutput(nmeaAisOutputVoyageInfo, false);
        nmeaAisOutputVoyageInfo.doInit();

        nmeaAisOutputBaseStation = new NmeaAisOutputBaseStation(this);
        addOutput(nmeaAisOutputBaseStation, false);
        nmeaAisOutputBaseStation.doInit();

        nmeaAisOutputAidNavigation = new NmeaAisOutputAidNavigation(this);
        addOutput(nmeaAisOutputAidNavigation, false);
        nmeaAisOutputAidNavigation.doInit();

        nmeaAisHandler = new NmeaAisHandler(this);
    }

    @Override
    public void doStart() throws SensorHubException {
        super.doStart();

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
            getLogger().info("Connected to AIS data stream");
        } catch (IOException e) {
            throw new SensorHubException("Error opening AIS input stream", e);
        }

        PipedInputStream pipedIn;
        final PipedOutputStream pipedOut;
        try {
            pipedIn = new PipedInputStream(65536);
            pipedOut = new PipedOutputStream(pipedIn);
        } catch (IOException e) {
            throw new SensorHubException("Error creating AIS pipe", e);
        }

        aisReader = AisReaders.createReaderFromInputStream(pipedIn);
        aisReader.registerHandler(aisMessage ->
            nmeaAisHandler.handleAisMessage(aisMessage)
        );

        Thread readerThread = new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            try {
                int b;
                while (started && (b = inputStream.read()) != -1) {
                    if (b == '\n') {
                        String line = sb.toString().trim();
                        sb.setLength(0);
                        if (!line.isEmpty() &&
                                (line.startsWith("!AIVDM") || line.startsWith("!AIVDO"))) {
                            publishRawMessage(line);
                            byte[] bytes = (line + "\n").getBytes();
                            pipedOut.write(bytes);
                            pipedOut.flush();
                        }
                    } else if (b != '\r') {
                        sb.append((char) b);
                    }
                }
            } catch (IOException e) {
                if (started)
                    getLogger().error("Error reading AIS data stream", e);
            } finally {
                try { pipedOut.close(); } catch (IOException ignored) {}
            }
        });

        started = true;
        readerThread.setName("ais-reader");
        readerThread.setDaemon(true);
        readerThread.start();
        aisReader.start();
    }

    @Override
    public void doStop() throws SensorHubException {
        started = false;

        if (aisReader != null) {
            aisReader.stopReader();
            aisReader = null;
        }

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

    // -------------------------------------------------------------------------
    // FOI management
    // -------------------------------------------------------------------------

    /**
     * Registers a moving vessel FOI keyed by MMSI. Subsequent calls with the
     * same MMSI are no-ops; the existing FOI UID is returned.
     */
    public String addVesselFoi(String mmsi) {
        String foiUID = UID_PREFIX + "foi:vessel:" + mmsi;
        if (!foiMap.containsKey(foiUID)) {
            MovingFeature foi = new MovingFeature();
            foi.setId(mmsi);
            foi.setUniqueIdentifier(foiUID);
            foi.setDescription("AIS vessel with MMSI " + mmsi);
            addFoi(foi);
            getLogger().debug("New AIS vessel added as FOI: {}", foiUID);
        }
        return foiUID;
    }

    /**
     * Registers a fixed Aid-to-Navigation FOI keyed by MMSI.
     * Subsequent calls with the same MMSI are no-ops.
     */
    public String addAtonFoi(String mmsi) {
        String foiUID = UID_PREFIX + "foi:aton:" + mmsi;
        if (!foiMap.containsKey(foiUID)) {
            GenericFeatureImpl foi = new GenericFeatureImpl("http://www.opengis.net/ont/ais/AidToNavigation");
            foi.setId("aton_" + mmsi);
            foi.setUniqueIdentifier(foiUID);
            foi.setName("AtoN " + mmsi);
            foi.setDescription("AIS Aid-to-Navigation with MMSI " + mmsi);
            addFoi(foi);
            getLogger().debug("New AtoN added as FOI: {}", foiUID);
        }
        return foiUID;
    }

    /**
     * Registers a fixed Base Station FOI keyed by MMSI.
     * Subsequent calls with the same MMSI are no-ops.
     */
    public String addStationFoi(String mmsi) {
        String foiUID = UID_PREFIX + "foi:station:" + mmsi;
        if (!foiMap.containsKey(foiUID)) {
            GenericFeatureImpl foi = new GenericFeatureImpl("http://www.opengis.net/ont/ais/BaseStation");
            foi.setId("station_" + mmsi);
            foi.setUniqueIdentifier(foiUID);
            foi.setName("Base Station " + mmsi);
            foi.setDescription("AIS Base Station with MMSI " + mmsi);
            addFoi(foi);
            getLogger().debug("New AIS base station added as FOI: {}", foiUID);
        }
        return foiUID;
    }

    /**
     * Creates or updates a vessel FOI with static identity properties received
     * from AIS message types 5, 19, or 24. Only non-null / non-zero values
     * overwrite existing properties so properties accumulate across message types.
     *
     * @return FOI UID
     */
    public String updateFoiStaticData(String mmsi, String name, String callSign,
            int shipType, long imoNumber, int aisVersion, String vendorId,
            String epfd, boolean dte,
            int dimBow, int dimStern, int dimPort, int dimStarboard) {

        // add foiUID if it exists, otherwise, just return uid
        String foiUID = addVesselFoi(mmsi);

        synchronized (foiMap) {
            IFeature feature = foiMap.get(foiUID);
            if (!(feature instanceof MovingFeature foi)) return foiUID;

            if (name != null && !name.isBlank()) {
                foi.setName(name);
                foi.setProperty("vesselName", name);
            }
            if (callSign != null && !callSign.isBlank())
                foi.setProperty("callSign", callSign.trim());
            if (shipType != 0) {
                foi.setProperty("shipType", AisCodeHelper.ShipType.getDescription(shipType));
                foi.setProperty("shipTypeCode", String.valueOf(shipType));
            }
            if (imoNumber != 0)
                foi.setProperty("imoNumber", String.valueOf(imoNumber));
            if (aisVersion != 0)
                foi.setProperty("aisVersion", String.valueOf(aisVersion));
            if (vendorId != null && !vendorId.isBlank())
                foi.setProperty("vendorId", vendorId.trim());
            if (epfd != null && !epfd.isBlank())
                foi.setProperty("epfd", epfd);
            foi.setProperty("dte", String.valueOf(dte));
            if (dimBow != 0)
                foi.setProperty("dimBow", String.valueOf(dimBow) + " m");
            if (dimStern != 0)
                foi.setProperty("dimStern", String.valueOf(dimStern) + " m");
            if (dimPort != 0)
                foi.setProperty("dimPort", String.valueOf(dimPort) + " m");
            if (dimStarboard != 0)
                foi.setProperty("dimStarboard", String.valueOf(dimStarboard) + " m");

            // Re-register to propagate property changes to the system registry
            addFoi(foi);
        }

        return foiUID;
    }

    /**
     * Publishes a raw NMEA AIS sentence to the messages output.
     */
    void publishRawMessage(String nmeaAisMsg) {
        nmeaAisOutputRawMessages.setData(nmeaAisMsg);
    }
}
