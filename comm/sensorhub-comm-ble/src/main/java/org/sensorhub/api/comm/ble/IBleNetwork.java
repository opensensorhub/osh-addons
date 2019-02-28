/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.comm.ble;

import org.sensorhub.api.comm.ICommNetwork;
import org.sensorhub.api.comm.NetworkConfig;


public interface IBleNetwork<ConfigType extends NetworkConfig> extends ICommNetwork<ConfigType>
{
    
    /**
     * Starts the pairing process with the remote device.
     * @param address
     * @return false on immediate error, true if bonding will begin
     */
    boolean startPairing(String address);
    
    
    /**
     * Initiates connection to the remote device's GATT server.<br/>
     * This is an asynnchronous call and the returned GATT client may not
     * yet be connected on return. Listen to onConnected() callback method
     * to be notified when the connection is ready to be used.
     * @param address Address of device to connect to
     * @param callback Callback to received client events
     */
    void connectGatt(String address, GattCallback callback);
}
