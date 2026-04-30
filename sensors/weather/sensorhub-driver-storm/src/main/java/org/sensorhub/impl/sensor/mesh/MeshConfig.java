/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2018 Delta Air Lines, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.mesh;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.Required;
import org.sensorhub.api.sensor.SensorConfig;


public class MeshConfig extends SensorConfig
{
    
    @Required
    @DisplayInfo(desc="Directory to watch for MESH data files")
    public String dataPath;
    
    
    @Required
    @DisplayInfo(desc="File name pattern for MESH data files")
    public String fileNamePattern = ".*MESH.*\\.grb2";
    
    
    @DisplayInfo(desc="Name of file containing the path of the latest available data file (null if no such file is available)")
    public String latestPointerFileName;
    
    
}
