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

git clone --recursive https://github.com/micstinso/ardupilot.git
podman build -f ardupilot/Dockerfile -t ardupilot:v0.1 ./ardupilot &&
distrobox-create --name ardupilot --image localhost/ardupilot:v0.1
distrobox enter ardupilot -- bash -c "export HOME=/home/ardupilot && cd / && ./ardupilot/Tools/environment_install/install-prereqs-ubuntu.sh -y"

distrobox enter ardupilot -- bash -c "export HOME=/home/ardupilot && mkdir -p /home/ardupilot/.mavproxy && touch ~/.mavproxy/mavinit.scr && echo 'output add 127.0.0.1:14540' >> ~/.mavproxy/mavinit.scr && echo 'output add 127.0.0.1:14560' >> ~/.mavproxy/mavinit.scr && echo 'output add 127.0.0.1:14550' >> ~/.mavproxy/mavinit.scr"
