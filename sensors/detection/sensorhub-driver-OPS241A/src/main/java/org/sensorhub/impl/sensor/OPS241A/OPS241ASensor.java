/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.OPS241A;

import org.sensorhub.impl.sensor.OPS241A.config.OPS241AConfig;
import org.sensorhub.impl.sensor.OPS241A.outputs.*;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Pi4J Variables:
import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;
import com.pi4j.io.i2c.I2CProvider;

import java.util.ArrayList;
import java.util.List;


/**
 * Driver implementation for the sensor.
 * <p>
 * This class is responsible for providing sensor information, managing output registration,
 * and performing initialization and shutdown for the driver and its outputs.
 */
public class OPS241ASensor extends AbstractSensorModule<OPS241AConfig>{
    static final String UID_PREFIX = "osh:OPS241A:";
    static final String XML_PREFIX = "OPS241A";

    private static final Logger logger = LoggerFactory.getLogger(OPS241ASensor.class);

    ///  REQUIRED VARIABLES FOR SENSOR OPERATION
    // I2C Initialization Variables from Pi4j
    I2C i2c;
    Context pi4j;
    I2CProvider i2CProvider;

    // Local variables
    private volatile boolean keepRunning = false;

    // Cosmetic debugging Variables Used to make font bold
    String BoldOn = "\033[1m";  // ANSI code to turn on bold
    String BoldOff = "\033[0m"; // ANSI code to turn on bold

    ///  INITIALIZE BNO085 SENSOR OVER I2C
    @Override
    public void doInit() throws SensorHubException {
        super.doInit();

        // Create I2C connection
        createI2cConnection();

        // Create SensorHub Identifiers using designated prefix and serial number from Admin Panel
        generateUniqueID(UID_PREFIX, config.serialNumber);
        generateXmlID(XML_PREFIX, config.serialNumber);

    }

    @Override
    public void doStart() throws SensorHubException, InterruptedException {
        super.doStart();
        keepRunning = true;
    }

    @Override
    public void doStop() throws SensorHubException, InterruptedException {
        super.doStop();
        keepRunning = false;
    }

    @Override
    public boolean isConnected() {
        return true; //output.isAlive()
    }

    public void createI2cConnection(){
        // Initialize pi4j and i2c configuration
        pi4j = Pi4J.newAutoContext();
        i2CProvider = pi4j.provider("linuxfs-i2c");

        I2CConfig i2cConfig = I2C.newConfigBuilder(pi4j)
                .bus(config.connection.I2C_BUS_NUMBER)
                .device(config.connection.SENSOR_ADDRESS)
                .name("bno055")
                .build();
        System.out.println("Building I2C Connection...");
        logger.info("Building I2C Connection...");
        try {
            this.i2c = i2CProvider.create(i2cConfig);
            System.out.println(BoldOn + "I2C Connection Established" + BoldOff + "\n\tBus/Port:" + config.connection.I2C_BUS_NUMBER + "\n\tSensor Address: 0x" + Integer.toHexString(config.connection.SENSOR_ADDRESS));
            logger.info("{}I2C Connection Established{}\n\tBus/Port:{}\n\tSensor Address: 0x{}", BoldOn, BoldOff, config.connection.I2C_BUS_NUMBER, Integer.toHexString(config.connection.SENSOR_ADDRESS));

        } catch (Exception e) {
            System.out.println("I2C Connection Failed...check I2C bus and Sensor Address");
            logger.error("I2C Connection Failed...check I2C bus and Sensor Address");
            throw new RuntimeException(e);
        }
    }

}
