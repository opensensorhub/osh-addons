#!/bin/bash
# run the ardupilot container
distrobox enter ardupilot -- bash -c 'export HOME=/home/ardupilot && source /home/ardupilot/venv-ardupilot/bin/activate && cd ardupilot && ./Tools/autotest/sim_vehicle.py "$@"' bash "$@"

