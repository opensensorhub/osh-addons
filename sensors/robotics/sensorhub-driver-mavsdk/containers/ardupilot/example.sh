#!/bin/bash
# run the ardupilot container
bash ./run.sh -v ArduCopter -f quad --console --map --instance 0 --sysid 1 --out udp:127.0.0.1:14551
