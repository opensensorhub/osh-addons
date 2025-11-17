package com.sensorhub.impl.sensor.meshtastic;

import com.botts.impl.comm.jssc.JsscSerialCommProviderConfig;
import com.geeksville.mesh.MeshProtos;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;


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
    public void init() throws SensorHubException {
        config = new Config();
        config.id = UUID.randomUUID().toString();

        JsscSerialCommProviderConfig serialConfig = new JsscSerialCommProviderConfig();
        serialConfig.protocol.portName = "/dev/tty.usbserial-0001";
        serialConfig.protocol.baudRate = 115200;

        config.commSettings = serialConfig;

        sensor = new MeshtasticSensor();
        sensor.init(config);

    }
    // TEST 1: CHECK INITIALIZATION CREATES ALL OUTPUTS AND HANDLER SUCCESSFULLY
    @Test
    public void testInitCreatesOutputAndHandler() throws SensorHubException {
        sensor.doInit();

        //Verify that each output object was created:
        assertNotNull("Text output should be initialized", sensor.textOutput);
        assertNotNull("Position output should be initialized", sensor.posOutput);
        assertNotNull("Node info output should be initialized", sensor.nodeInfoOutput);
        assertNotNull("Generic output should be initialized", sensor.genericOutput);

        // Verify handler and control message objects exist
        assertNotNull("Handler should be initialized", sensor.meshtasticHandler);
        assertNotNull("Control text message should be initialized", sensor.meshtasticControlTextMessage);

    }

    // -----------------------------------------
    // Test 2: addFoi() registers a new node only once
    // -----------------------------------------
    @Test
    public void testAddFoiCreatesFoiOnlyOnce() throws SensorHubException {
        sensor.doInit();

        // Add FOI for a node ID
        String uid1 = sensor.addFoi("NODE123");

        // Add the same FOI again
        String uid2 = sensor.addFoi("NODE123");

        // The returned UID should be the same
        assertEquals("UIDs should be equal for same node", uid1, uid2);

        // Verify the UID format
        assertEquals("UID should match expected format", "urn:osh:Meshtastic_Node:NODE123", uid1);

    }



}
