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

import java.util.Collection;


/**
 * <p>
 * Client interface exposing GATT capabilities of a remote device.<br/>
 * <i>This interface was originally modeled on Android's Bluetooth LE API.</i>
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 28, 2016
 */
public interface IGattClient
{
    public static final int GATT_SUCCESS = 0;
    public static final int GATT_FAILURE = 257;
    public static final int GATT_READ_NOT_PERMITTED = 2;
    public static final int GATT_WRITE_NOT_PERMITTED = 3;    
    
    
    /**
     * Disconnects an established connection with the remote device or cancels
     * a connection attempt currently in progress
     */
    void disconnect();
    
    
    /**
     * This method is used to re-connect to a remote device after the
     * connection has been dropped. If the device is not in range, the
     * re-connection will be triggered once the device is back in range.
     */
    void connect();
    
    
    /**
     * Discovers services offered by a remote device as well as their
     * characteristics and descriptors.<br/>
     * This is an asynchronous operation. Once service discovery is completed,
     * the onServicesDiscovered(...) callback is triggered.<br/>
     * If the discovery was successful, the remote services can be retrieved
     * using the getServices() function.
     * @return true, if the remote service discovery has been started
     */
    boolean discoverServices();
    
    
    /**
     * Gets the list of GATT services after service discovery has been
     * completed for the given device.
     * @return the list of GATT services offered by the remote device
     */
    Collection<? extends IGattService> getServices();
        
    
    /**
     * Reads the requested characteristic from the associated remote device.<br/>
     * This is an asynchronous operation. The result of the read operation
     * is reported by the {@link GattCallback#onCharacteristicRead} callback.
     * @param characteristic
     * @return true, if the read operation was initiated successfully
     */
    boolean readCharacteristic(IGattCharacteristic characteristic);
    
    
    /**
     * Enable or disable notifications/indications for a given characteristic.<br/>
     * Once notifications are enabled for a characteristic, the onCharacteristicChanged()
     * callback will be triggered when the remote device indicates that the given
     * characteristic has changed.
     * @param characteristic The characteristic for which to enable notifications
     * @param enable Set to true to enable notifications/indications
     * @return true, if the read operation was initiated successfully
     */
    boolean setCharacteristicNotification(IGattCharacteristic characteristic, boolean enable);
    
    
    /**
     * Writes the cached value of a given characteristic to the associated remote device.<br/>
     * Once the write operation has been completed, the onCharacteristicWrite() callback
     * is invoked, reporting the result of the operation.
     * @param characteristic
     * @return true, if the write operation was initiated successfully
     */
    boolean writeCharacteristic(IGattCharacteristic characteristic);
    
    
    /**
     * Reads the value of a descriptor from the associated remote device.<br/>
     * This is an asynchronous operation. The result of the read operation
     * is reported by the {@link GattCallback#onDescriptorRead} callback.
     * @param descriptor
     * @return true, if the read operation was initiated successfully
     */
    boolean readDescriptor(IGattDescriptor descriptor);
    
    
    /**
     * Writes the value of a given descriptor to the associated remote device.<br/>
     * Once the write operation has been completed, the onDescriptorWrite() callback
     * is invoked, reporting the result of the operation.
     * @param descriptor
     * @return true, if the write operation was initiated successfully
     */
    boolean writeDescriptor(IGattDescriptor descriptor);
    
    
    /**
     * Definitely closes this GATT client and frees any resources associated with it.
     */
    void close();
    
}
