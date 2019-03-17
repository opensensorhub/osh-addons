java -Xmx128m -Dlogback.configurationFile=./logback.xml -cp "lib/*" -Djava.system.class.loader="org.sensorhub.utils.NativeClassLoader" org.sensorhub.impl.SensorHub config.json db
