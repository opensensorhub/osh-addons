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

import java.util.Map;
import java.util.UUID;


/**
 * <p>
 * Representation of a Bluetooth GATT Characteristic.<br/>
 * <i>This interface was originally modeled on Android's Bluetooth LE API.</i>
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 28, 2016
 */
public interface IGattCharacteristic extends IGattField
{
    /**
     * @return the service this characteristic belongs to.
     */
    IGattService getService();
    
    
    /**
     * @return the local ID of the characteristic (handle)
     */
    int getHandle();
    
    
    /**
     * Gets the bit mask of property flags for this characteristic.
     * @return the properties of this characteristic.
     */
    int getProperties();
    
    
    /**
     * @return the list of descriptors for this characteristic.
     */
    Map<UUID, ? extends IGattDescriptor> getDescriptors();
    
}
