package org.sensorhub.impl.sensor.meshtastic;

import com.botts.impl.comm.jssc.JsscSerialCommProviderConfig;
import com.geeksville.mesh.MeshProtos;
import com.geeksville.mesh.Portnums;
import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleRegistry;


import javax.validation.constraints.AssertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.Assert.*;

public class MeshtasticSensorTest {
    // TODO: Create some tests for the Meshtastic Sensor
}
