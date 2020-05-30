#!/bin/bash
java -Xmx384m -cp "build_old/*" org.sensorhub.tools.DbExport $@
