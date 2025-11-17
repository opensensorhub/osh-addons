package org.sensorhub.impl.sensor.meshtastic;

import com.botts.impl.comm.jssc.JsscSerialCommProviderConfig;
import com.geeksville.mesh.MeshProtos;
import com.geeksville.mesh.Portnums;
import com.google.protobuf.ByteString;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.common.SensorHubException;


import javax.validation.constraints.AssertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.Assert.*;

public class MeshtasticSensorTest {

    MeshtasticSensor sensor;
    Config config;


    /**
     * Runs before every test.
     * Sets up a Config object, configures the comm provider settings,
     * and initializes the sensor with this configuration.
     */
    @Before
    public void setup() throws SensorHubException {
        config = new Config();
        config.id = UUID.randomUUID().toString();

        // Configure serial comm provider
        JsscSerialCommProviderConfig serialConfig = new JsscSerialCommProviderConfig();
        serialConfig.protocol.portName = "/dev/tty.usbserial-0001";
        serialConfig.protocol.baudRate = 115200;

        config.commSettings = serialConfig;

        // Initialize Sensor
        sensor = new MeshtasticSensor();
        sensor.init(config);
        sensor.doInit();

    }
    // TEST 1: Test if driver can Connect
    @Test
    public void testDriverCanConnect() throws SensorHubException {
        boolean connected = sensor.isConnected();
        assertTrue("Driver is Connected", connected);
    }

    // -----------------------------------------
    // Test 2: See if messages send successfully
    // -----------------------------------------
    @Test
    public void testDriverCanSendMessage() {
        // CREATE A MESHTASTIC PROTO PACKET TO BE SENT AS TEXT MESSAGE
        String text = "Meshtastic Test to Primary Channel";
        int destinationID = 0xFFFFFFFF; // Broadcast to open channel

        MeshProtos.MeshPacket packet = MeshProtos.MeshPacket.newBuilder()
                .setDecoded(MeshProtos.Data.newBuilder()
                        .setPortnum(Portnums.PortNum.internalGetValueMap().findValueByNumber(Portnums.PortNum.TEXT_MESSAGE_APP_VALUE))
                        .setPayload(ByteString.copyFrom(text, StandardCharsets.UTF_8))
                        .build()
                )
                .setChannel(0)
                .setTo(destinationID) // Primary Channel
                .setWantAck(false) // Acknowledge direct messages to display history
                .build();

        // Create ToRadio
        MeshProtos.ToRadio message = MeshProtos.ToRadio.newBuilder()
                .setPacket(packet)
                .build();

        // Test
        boolean result = sensor.sendMessage(message);
        assertTrue("Message should be sent succesfully", result);
    }

    // -----------------------------------------
    // Test 3: See if messages are received correctly through the handler
    // -----------------------------------------
    @Test
    public void testDriverCanReceive() {
        String text = "Is a text message received";

        MeshProtos.MeshPacket testPacket = MeshProtos.MeshPacket.newBuilder()
                .setDecoded(MeshProtos.Data.newBuilder()
                        .setPortnum(Portnums.PortNum.internalGetValueMap().findValueByNumber(Portnums.PortNum.TEXT_MESSAGE_APP_VALUE))
                        .setPayload(ByteString.copyFrom(text, StandardCharsets.UTF_8))
                        .build()
                )
                .setWantAck(false) // Acknowledge direct messages to display history
                .build();

        // This test should run through the handler and provide MeshtasticOutputTextMessage
        sensor.meshtasticHandler.handlePacket(testPacket);

        // TEST OUTPUT
        assertEquals(
                "Handler should route text payload to MeshtasticOutputTextMessage",
                text,
                sensor.textOutput.getLastText()
        );

    }



}
