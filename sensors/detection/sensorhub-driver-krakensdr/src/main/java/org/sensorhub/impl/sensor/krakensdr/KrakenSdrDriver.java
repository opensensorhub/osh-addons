/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.krakensdr;

import org.sensorhub.api.command.CommandException;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.sensorhub.impl.sensor.krakensdr.controls.KrakenSdrControlDoA;
import org.sensorhub.impl.sensor.krakensdr.controls.KrakenSdrControlReceiver;
import org.sensorhub.impl.sensor.krakensdr.controls.KrakenSdrControlStation;
import org.sensorhub.impl.sensor.krakensdr.controls.KrakenSdrControlVfo;
import org.sensorhub.impl.sensor.krakensdr.outputs.KrakenSdrOutputDoA;
import org.sensorhub.impl.sensor.krakensdr.outputs.KrakenSdrOutputSettings;
import org.sensorhub.impl.sensor.krakensdr.outputs.KrakenSdrOutputSpectrum;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;


/**
 * Driver implementation for the sensor.
 * <p>
 * This class is responsible for providing sensor information, managing output registration,
 * and performing initialization and shutdown for the driver and its outputs.
 */
public class KrakenSdrDriver extends AbstractSensorModule<KrakenSdrConfig> {
    static final String UID_PREFIX = "urn:osh:sensor:krakensdr:";
    static final String XML_PREFIX = "krakenSdr";

    KrakenSdrOutputSettings krakenSdrOutputSettings;
    KrakenSdrOutputDoA krakenSdrOutputDoA;
    KrakenSdrOutputSpectrum krakenSdrOutputSpectrum;

    KrakenSdrControlReceiver krakenSdrControlReceiver;
    KrakenSdrControlVfo krakenSdrControlVfo;
    KrakenSdrControlDoA krakenSdrControlDoA;
    KrakenSdrControlStation krakenSdrControlStation;

    String OUTPUT_URL;
    String SETTINGS_URL;
    String WS_URL;

    private volatile boolean keepRunning = false;
    private HttpClient httpClient;
    private WebSocket webSocket;

    //Accumulates partial websocket text frames before dispatching
    private final StringBuilder wsFrameBuffer = new StringBuilder();

    //  INITIALIZE
    @Override
    public void doInit() throws SensorHubException {
        super.doInit();

        // Create SensorHub Identifiers using designated prefix and serial number from Admin Panel
        generateUniqueID(UID_PREFIX, config.serialNumber);
        generateXmlID(XML_PREFIX, config.serialNumber);

        // THE KRAKEN GUI APPLICATION SERVES IT'S _SHARE DIRECTORY TO A SPECIFIC PORT. DEFINE STRUCTURE IN CONFIG TO USE IN APP
        OUTPUT_URL  = "http://" + config.krakenIPaddress + ":" + config.krakenWsPort;
        SETTINGS_URL = OUTPUT_URL + "/settings.json";
        WS_URL = "ws://" + config.krakenIPaddress + ":" + config.krakenWsPort + "/ws/kraken";

        // INITIALIZE CONTROLS
        krakenSdrControlReceiver = new KrakenSdrControlReceiver(this);
        addControlInput(krakenSdrControlReceiver);
        krakenSdrControlReceiver.doInit();

        krakenSdrControlDoA = new KrakenSdrControlDoA(this);
        addControlInput(krakenSdrControlDoA);
        krakenSdrControlDoA.doInit();

        krakenSdrControlVfo = new KrakenSdrControlVfo(this);
        addControlInput(krakenSdrControlVfo);
        krakenSdrControlVfo.doInit();

        krakenSdrControlStation = new KrakenSdrControlStation(this);
        addControlInput(krakenSdrControlStation);
        krakenSdrControlStation.doInit();


        // INITIALIZE OUTPUTS
        // CURRENT SETTINGS OUTPUT
        krakenSdrOutputSettings = new KrakenSdrOutputSettings(this);
        addOutput(krakenSdrOutputSettings, false);
        krakenSdrOutputSettings.doInit();

        // DOA INFO SETTINGS
        krakenSdrOutputDoA = new KrakenSdrOutputDoA(this);
        addOutput(krakenSdrOutputDoA, false);
        krakenSdrOutputDoA.doInit();

        // SPECTRUM OUTPUT
        krakenSdrOutputSpectrum = new KrakenSdrOutputSpectrum(this);
        addOutput(krakenSdrOutputSpectrum, false);
        krakenSdrOutputSpectrum.doInit();
    }

    @Override
    public void doStart() throws SensorHubException {
        super.doStart();

        // Set variable to continue readings
        keepRunning = true;

        httpClient = HttpClient.newHttpClient();
        connectWebSocket();
    }

    @Override
    public void doStop() throws SensorHubException {
        keepRunning = false;
        if (webSocket != null && !webSocket.isOutputClosed()){
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");
        }
        super.doStop();
    }

    @Override
    public boolean isConnected() {
        return webSocket != null && !webSocket.isInputClosed();
    }

    void sendWsMessage(JsonObject message) throws CommandException {
        if (webSocket == null || webSocket.isOutputClosed()) {
            throw new CommandException("Cannot send command - KrakenSDR WebSocket is not connected");
        }
        webSocket.sendText(message.toString(), true)
                .exceptionally(err -> {
                    getLogger().error("Failed to send KrakenSDR WebSocket message", err);
                    return null;
                });
    }

    public void updateKrakenSettings(JsonObject data) throws CommandException {
        // Prepare Web socket message
        JsonObject command = new JsonObject();
        command.addProperty("type", "command");
        command.addProperty("action", "update_settings");
        command.add("data", data);

        this.sendWsMessage(command);
    }

    // WebSocket Connection
    private void connectWebSocket() {
        if (!keepRunning) return;
        try {
            getLogger().info("Connecting KrakenSDR WebSocket to {}", WS_URL);
            httpClient.newWebSocketBuilder()
                    .buildAsync(URI.create(WS_URL), new KrakenWsListener())
                    .whenComplete((ws, err) -> {
                        if (err != null) {
                            getLogger().error("KrakenSDR WebSocket connect failed", err);
                            scheduleReconnect();
                        } else {
                            webSocket = ws;
                            getLogger().info("KrakenSDR WebSocket connected");
                        }
                    });
        } catch (Exception e) {
            getLogger().error("KrakenSDR WebSocket connect error", e);
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if(!keepRunning) return;
        Thread t = new Thread(() -> {
           try {
               Thread.sleep(5_000);
           } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
               return;
           }
           connectWebSocket();
        }, "KrakenSDR-Reconnect");
        t.setDaemon(true);
        t.start();
    }


    // Inner Class WebSocket Listener to keep reference clean
    private class KrakenWsListener implements WebSocket.Listener {
        private void handleWsMessage (String raw) {
            try {
                JsonObject msg = JsonParser.parseString(raw).getAsJsonObject();
                String type = msg.has("type") ? msg.get("type").getAsString() : "";
                switch (type) {
                    case "doa":
                        krakenSdrOutputDoA.setData(msg);
                        break;
                    case "settings":
                        krakenSdrOutputSettings.setData(msg);
                        break;
                    case "spectrum":
                        krakenSdrOutputSpectrum.setData(msg);
                        break;
                    default:
                        getLogger().debug("Unknown KrakenSdr WS message type: '{}'", type);
                }
            } catch (Exception e) {
                getLogger().error("Error processing KrakenSdr WebSocket Message", e);
            }
        }

        @Override
        public void onOpen(WebSocket ws) {
            getLogger().debug("KrakenSDR WS onOpen");
            ws.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            wsFrameBuffer.append(data);
            if (last) {
                String complete = wsFrameBuffer.toString();
                wsFrameBuffer.setLength(0);
                handleWsMessage(complete);
            }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            getLogger().info("KrakenSDR WebSocket closed ({}) {}", statusCode, reason);
            if (keepRunning) {
                scheduleReconnect();
            }
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            getLogger().error("KrakenSDR WebSocket error", error);
            if (keepRunning) {
                scheduleReconnect();
            }
        }
    }

}
