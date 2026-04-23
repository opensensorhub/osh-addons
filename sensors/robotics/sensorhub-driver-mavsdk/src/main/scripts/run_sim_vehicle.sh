#!/bin/bash

# Find the Default Gateway IP (the Windows Host) via eth0
# This looks for the 'default' route and grabs the IP address
HOST_IP=$(ip route show dev eth0 | grep default | awk '{print $3}')

if [ -z "$HOST_IP" ]; then
    echo "Error: Could not detect the Host Gateway IP."
    exit 1
fi

echo "Detected Windows Host IP: $HOST_IP"

# Execute the simulation sending data TO the Host
~/builds/ardupilot/Tools/autotest/sim_vehicle.py \
    -v ArduCopter \
    -f quad \
    --console \
    --map \
    --out "$HOST_IP:14550" \
    --out "$HOST_IP:14551" \
    --out "$HOST_IP:14552"

