#### Building MAVSDK Native Containers 

These scripts build a container for mavsdk_server (the Native C++ version of MAVSDK that is required by MAVSDK Java)
MAVSDK Java connects to mavsdk_server via gRPC so it can execute mavlink commands.

The container is an Ubuntu image with the proper mavsdk stuff inside. It's a podman container that can run with 
Distrobox or Podman Desktop. An example run.sh is provided that will run the container.

./build_mavsdk_distrobox.sh
./run.sh -p 50051 udp://:14551
