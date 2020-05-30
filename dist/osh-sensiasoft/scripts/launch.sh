#!/bin/bash
java -Xmx128m -Dlogback.configurationFile=./logback.xml -cp "lib/*" org.sensorhub.impl.SensorHub config.json db
