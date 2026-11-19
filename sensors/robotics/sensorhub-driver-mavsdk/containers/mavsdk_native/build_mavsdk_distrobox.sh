#!/bin/bash
# ***************************** BEGIN LICENSE BLOCK ***************************
# 
#  The contents of this file are subject to the Mozilla Public License, v. 2.0.
#  If a copy of the MPL was not distributed with this file, You can obtain one
#  at http://mozilla.org/MPL/2.0/.
#  
#  Software distributed under the License is distributed on an "AS IS" basis,
#  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
#  for the specific language governing rights and limitations under the License.
# 
#  Copyright (C) 2025 Botts Innovative Research. All Rights Reserved.
# 
# ******************************* END LICENSE BLOCK ***************************
git clone --recursive https://github.com/mavlink/MAVSDK.git
podman build -f MAVSDK/docker/Dockerfile-ubuntu-22.04 -t mavsdk:v0.1 . &&
distrobox create --name mavsdk --image localhost/mavsdk:v0.1

distrobox enter mavsdk -- bash -c '
  cp ~/.bashrc /home/user/MAVSDK
  export HOME=/home/user/MAVSDK &&
  cd MAVSDK && 
  git submodule update --init --recursive && 
  cmake -DCMAKE_INSTALL_PREFIX=$HOME/.local -DCMAKE_BUILD_TYPE=Release -DBUILD_MAVSDK_SERVER=ON -Bbuild -S. && 
  cmake --build build -j8 && 
  cmake --build build --target install && 
  echo '\''export LD_LIBRARY_PATH=$HOME/.local/lib:$LD_LIBRARY_PATH'\'' >> /home/user/MAVSDK/.bashrc'
