/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.comm.providers;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.ValueRange;
import org.sensorhub.impl.comm.IPConfig;

/**
 * <p>
 * Driver configuration options for the UDP network protocol
 * </p>
 *
 * @author Alex Robin
 * @since Dec 12, 2015
 */
public class UDP2Config extends IPConfig
{
    
    @DisplayInfo(desc="Port number to connect to on remote host (0 to automatically select a port)")
    @ValueRange(min=0, max=65535)
    public int remotePort = PORT_AUTO;

    @DisplayInfo(desc="Local port number to use on the local host")
    @ValueRange(min=0, max=65535)
    public int localPort;
}
