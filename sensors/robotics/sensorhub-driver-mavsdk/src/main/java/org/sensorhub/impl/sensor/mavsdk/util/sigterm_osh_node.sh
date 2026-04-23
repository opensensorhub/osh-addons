#!/bin/bash

#send SIGTERM to OSH node so Shutdown Hooks run cleanly. This is needed to close down the mavsdk_server
# in a clean way rather than the Stop button in the IDE debugger.
# Can add this script to your external tools in the IDE and make a button that can run the script to stop debugging.

ps -ef | grep sensorhub | grep -v grep | grep -v mavsdk_server | awk '{print $2}' | xargs kill -TERM
