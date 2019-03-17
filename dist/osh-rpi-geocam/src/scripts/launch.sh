#!/bin/bash
java -Xmx384m -cp "lib/*" -Djava.system.class.loader="org.sensorhub.utils.NativeClassLoader" -Dlogback.configurationFile=./logback.xml -Djava.security.policy=./java.policy.ext.rpi -Djdk.dio.registry=./dio.properties-raspberrypi -Djdk.dio.uart.ports=ttyAMA0 -Dgnu.io.rxtx.SerialPorts=/dev/ttyAMA0:/dev/ttyUSB0 org.sensorhub.impl.SensorHub config.json db
