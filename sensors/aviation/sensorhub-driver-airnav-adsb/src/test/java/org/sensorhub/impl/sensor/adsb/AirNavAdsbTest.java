package org.sensorhub.impl.sensor.adsb;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.comm.TCPCommProviderConfig;
import org.sensorhub.impl.comm.TCPConfig;
import org.sensorhub.impl.module.ModuleRegistry;

public class AirNavAdsbTest {
    private static final String REMOTE_HOST = "localhost";
    private static final int REMOTE_PORT = 30003;
    ISensorHub hub;
    ModuleRegistry reg;

    @Before
    public void setUp() throws SensorHubException {
        hub = new SensorHub();
        hub.start();
        reg = hub.getModuleRegistry();
    }

    private AirNavADSBConfig createConfig(String host, int port) throws SensorHubException {
        AirNavADSBConfig config = (AirNavADSBConfig) reg.createModuleConfig(new Descriptor());
        config.commSettings = new TCPCommProviderConfig();
        var commConfig = new TCPConfig();
        commConfig.remoteHost = host;
        commConfig.remotePort = port;
        config.commSettings.protocol = commConfig;
        return config;
    }

    public void testLoadAndStart(AirNavADSBConfig config) throws SensorHubException {
        AirNavADSBSensor sensor = (AirNavADSBSensor) reg.loadModule(config);
        reg.startModule(sensor.getLocalID());
        boolean isStarted = sensor.waitForState(ModuleEvent.ModuleState.STARTED, 10000);
        Assert.assertTrue(isStarted);
    }

    @Test
    public void testSensor() throws SensorHubException {
        var config = createConfig(REMOTE_HOST, REMOTE_PORT);
        testLoadAndStart(config);
    }


    @After
    public void cleanup() {
        if (hub != null)
            hub.stop();
    }
}
