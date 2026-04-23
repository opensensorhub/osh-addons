#!/bin/bash
set -e
echo "Building ardupilot distrobox container..."
cd ardupilot
./ardupilot_distrobox.sh
echo "Building mavsdk distrobox container..."
cd ../mavsdk_native
./build_mavsdk_distrobox.sh
cd ..
echo "Done."
