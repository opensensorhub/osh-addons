#!/bin/bash
java -Xmx512m -Dlogback.configurationFile=./logback.xml -cp "lib/*" org.sensorhub.impl.SensorHub config.json db
