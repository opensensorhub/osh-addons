### Building Ardupilot SITL Containers 

These scripts build a container for Ardupilot SITL 

The container is an Ubuntu image with the proper ardupilot stuff inside. It's a podman container that can run with 
Distrobox or Podman Desktop. This script currently builds the Distrobox container.

An example run.sh is provided that will run the container.

### Build
./ardupilot_distrobox.sh

### Run
./example.sh 

### Example
./run.sh -v ArduCopter -f quad --console --map --instance 0 --sysid 1 --out udp:127.0.0.1:14551
