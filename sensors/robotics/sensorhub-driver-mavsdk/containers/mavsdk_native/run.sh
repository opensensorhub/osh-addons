#!/bin/bash
# run the mavsdk container
distrobox enter mavsdk -- bash -c 'source /home/user/MAVSDK/.bashrc && mavsdk_server "$@"' bash "$@"
