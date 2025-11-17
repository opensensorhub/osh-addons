/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.meshtastic;

import com.geeksville.mesh.MeshProtos;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.ogc.om.MovingFeature;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Driver implementation for the sensor.
 * <p>
 * This class is responsible for providing sensor information, managing output registration,
 * and performing initialization and shutdown for the driver and its outputs.
 */
public class MeshtasticSensor extends AbstractSensorModule<Config> {
    static final String UID_PREFIX = "urn:osh:sensor:meshtastic:";
    static final String XML_PREFIX = "meshtastic";

    private static final Logger logger = LoggerFactory.getLogger(MeshtasticSensor.class);
    private ICommProvider<?> commProvider;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    AtomicBoolean isProcessing = new AtomicBoolean(false);

    // MESHTASTIC MESSAGE VARIABLES
    private static final int START1 = 0x94;
    private static final int START2 = 0xC3;

    // DEFINE meshtastic protobuf handler
    MeshtasticHandler meshtasticHandler;

    // DEFINE OUTPUTS
    MeshtasticOutputTextMessage textOutput;
    MeshtasticOutputPosition posOutput;
    MeshtasticOutputNodeInfo nodeInfoOutput;
    MeshtasticOutputGeneric genericOutput;

    // DEFINE CONTROL
    MeshtasticControlTextMessage meshtasticControlTextMessage;


    @Override
    public void doInit() throws SensorHubException {
        super.doInit();

        // Generate identifiers
        generateUniqueID(UID_PREFIX, config.serialNumber);
        generateXmlID(XML_PREFIX, config.serialNumber);

        if (config.commSettings != null)
            commProvider = (ICommProvider<?>) getParentHub().getModuleRegistry().loadSubModule(config.commSettings, true);

        // CREATE AND INITIALIZE OUTPUTS
        textOutput = new MeshtasticOutputTextMessage(this);
        addOutput(textOutput, false);
        textOutput.doInit();

        posOutput = new MeshtasticOutputPosition(this);
        addOutput(posOutput, false);
        posOutput.doInit();

        nodeInfoOutput = new MeshtasticOutputNodeInfo(this);
        addOutput(nodeInfoOutput, false);
        nodeInfoOutput.doInit();

        genericOutput = new MeshtasticOutputGeneric(this);
        addOutput(genericOutput, false);
        genericOutput.doInit();
        
        // INITIALIZE HANDLER
        meshtasticHandler = new MeshtasticHandler(this);

        // INITIALIZE CONTROL
        meshtasticControlTextMessage = new MeshtasticControlTextMessage(this);
        addControlInput(meshtasticControlTextMessage);

    }

    @Override
    public void doStart() throws SensorHubException {
        super.doStart();

        // CHECK COMM PROVIDER HAS BEEN SELECTED IN ADMIN PANEL
        if (commProvider != null){
            commProvider.start();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            // send "handshake" to start receiving protobufs
            MeshProtos.ToRadio handshake = MeshProtos.ToRadio.newBuilder()
                    // TODO: Verify response ID in future if needed
                    .setWantConfigId(0)
                    .build();
            try {
                sendMessage(handshake);
            } catch (IOException e) {
                getLogger().error("Failed to send handshake message", e);
                throw new SensorHubException("Handshake failed, cannot start sensor", e);
            }
            // BEGIN PROCESSING DATA
            startProcessing();
        } else {
            throw new SensorHubException("No communication provider configured");
        }
    }

    @Override
    public void doStop() throws SensorHubException {
        super.doStop();

        //Stop processing the loop
        isProcessing.set(false);

        // Shutdown executor
        executor.shutdownNow();

        // Stop comm
        if(commProvider != null){
            commProvider.stop();
        }


    }

    @Override
    public boolean isConnected() {
        return isProcessing.get();
    }

    // THIS METHOD IS UTILIZED IN ALL THE OUTPUTS. IT CREATES A FOI USING PARENT SENSOR METHOD
    String addFoi( String meshNodeId) {
        String foiUID = UID_PREFIX + meshNodeId;

        if (!foiMap.containsKey(foiUID))
        {
            // Generate small SensorML for FOI (in this case the system is the FOI)
            MovingFeature foi = new MovingFeature();
            foi.setId(meshNodeId);
            foi.setUniqueIdentifier(foiUID);
            foi.setName(meshNodeId);
            foi.setDescription("meshtastic Node " + meshNodeId);

            // REGISTER FOI
            addFoi(foi);

            getLogger().debug("New meshtastic Node added as FOI: {}", foiUID);

        }

        return foiUID;
    }


    // PROCESSING THREAD
    private void startProcessing() {
        executor.execute(() -> {
            try {
                InputStream in = commProvider.getInputStream();
                isProcessing.set(true);
                while (isProcessing.get()) {
                    int b;
                    // find START1 in Input Stream, indicating that a Protobuf messages is being sent
                    do {
                        b = in.read();
                        if (b == -1) return;
                        if (b != START1) {
//                            optional: treat as debug ASCII
//                            System.out.print((char) b);
                        }
                    } while (b != START1);

                    // If START1(0x94) is found, expect START2
                    b = in.read();
                    // invalid header
                    if (b != START2) {
                        getLogger().warn("Invalid header");
                        continue;
                    }

                    // GET LENGTH OF PROTOBUF MESSAGE
                    int lenMSB = in.read();
                    int lenLSB = in.read();
                    if (lenMSB == -1 || lenLSB == -1) break;

                    int length = ((lenMSB & 0xFF) << 8) | (lenLSB & 0xFF);
                    if (length <= 0 || length > 512) {
                        getLogger().info("Invalid length, resyncing...");
                        continue;
                    }

                    // PROTOBUF DATA
                    byte[] payload = new byte[length];
                    int read = 0;
                    while (read < length) {
                        int r = in.read(payload, read, length - read);
                        if (r == -1) break;
                        read += r;
                    }

                    if (read < length) {
                        getLogger().info("Truncated packet, resyncing...");
                        continue;
                    }

                    // parse protobuf
                    try {
                        MeshProtos.FromRadio msg = MeshProtos.FromRadio.parseFrom(payload);
                        if(msg.hasPacket()){
                            MeshProtos.MeshPacket packet = msg.getPacket();
                            meshtasticHandler.handlePacket(packet);
                        }
//                        getLogger().info("New message: " + msg);
                    } catch (Exception e) {
                        getLogger().error("Invalid protobuf: " + e.getMessage());
                    }
                }

            } catch (IOException e) {
            }
        });
    }

    public void sendMessage(MeshProtos.ToRadio message) throws IOException {
        byte[] bytes = message.toByteArray();

        int len = bytes.length;
        byte[] header = new byte[4];
        header[0] = (byte) START1;
        header[1] = (byte) START2;
        header[2] = (byte) ((len >> 8) & 0xFF);
        header[3] = (byte) (len & 0xFF);

        var os = commProvider.getOutputStream();
        os.write(header);
        os.write(bytes);
        os.flush();
    }

}
