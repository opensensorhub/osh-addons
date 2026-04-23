#### Ardupilot SITL and MAVSDK Containers 

### Building
./build.sh

This script builds both containers and the binaries inside the containers

The containers are each an Ubuntu image with the proper stuff inside. The are podman containers that can run with 
Distrobox or Podman Desktop. 

The containers are best run inside of a Linux environment.

### Running
Examples are provided for each

./ardupilot/example.sh 

./mavsdk_native/example.sh 

### Example
./ardupilot/run.sh -v ArduCopter -f quad --console --map --instance 0 --sysid 1 --out udp:127.0.0.1:14551

./mavsdk_native/run.sh -p 50051 udp://:14551


### Tested on a VM of Fedora Kinoite ARM64 (Mac)
NAME="Fedora Linux"

VERSION="42.20250723.0 (Kinoite)"

RELEASE_TYPE=stable

ID=fedora

VERSION_ID=42

VERSION_CODENAME=""

PLATFORM_ID="platform:f42"

PRETTY_NAME="Fedora Linux 42.20250723.0 (Kinoite)"

VARIANT="Kinoite"

VARIANT_ID=kinoite

OSTREE_VERSION='42.20250723.0'


