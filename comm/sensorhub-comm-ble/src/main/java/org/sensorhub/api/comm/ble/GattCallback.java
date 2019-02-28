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


/**
 * <p>
 * Abstract class used to implement IGattClient callbacks.<br/>
 * <i>This class was originally modeled on Android's Bluetooth LE API.</i>
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 28, 2016
 */
public abstract class GattCallback
{

    /**
     * Callback triggered when connected to the remote GATT server
     * @param gatt
     * @param status 
     */
    public void onConnected(IGattClient gatt, int status)
    {        
    }
    
    
    /**
     * Callback triggered when disconnected from the remote GATT server 
     * @param gatt 
     * @param status
     */
    public void onDisconnected(IGattClient gatt, int status)
    {        
    }
    
    
    /**
     * Callback invoked when the list of remote services, characteristics and
     * descriptors for the remote device have been updated, i.e. new services
     * have been discovered.
     * @param gatt
     * @param status
     */
    public void onServicesDiscovered(IGattClient gatt, int status)
    {        
    }
    
    
    /**
     * Callback triggered as a result of a remote characteristic notification.
     * @param gatt
     * @param characteristic
     */
    public void onCharacteristicChanged(IGattClient gatt, IGattField characteristic)
    {        
    }
    
    
    /**
     * Callback reporting the result of a characteristic read operation.
     * @param gatt
     * @param characteristic
     * @param status
     */
    public void onCharacteristicRead(IGattClient gatt, IGattField characteristic, int status)
    {        
    }
    
    
    /**
     * Callback indicating the result of a characteristic write operation.
     * @param gatt
     * @param characteristic
     * @param status
     */
    public void onCharacteristicWrite(IGattClient gatt, IGattField characteristic, int status)
    {        
    }
    
    
    /**
     * Callback reporting the result of a descriptor read operation.
     * @param gatt
     * @param descriptor
     * @param status
     */
    public void onDescriptorRead(IGattClient gatt, IGattDescriptor descriptor, int status)
    {        
    }
    
    
    /**
     * Callback indicating the result of a descriptor write operation.
     * @param gatt
     * @param descriptor
     * @param status
     */
    public void onDescriptorWrite(IGattClient gatt, IGattDescriptor descriptor, int status)
    {        
    }
    
    
    /**
     * Callback invoked when a reliable write transaction has been completed.
     * @param gatt
     * @param status
     */
    public void onReliableWriteCompleted(IGattClient gatt, int status)
    {        
    }
    
}
