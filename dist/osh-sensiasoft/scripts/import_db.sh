#!/bin/bash
java -Xmx384m -cp "build/*" org.sensorhub.tools.DbImport $@
