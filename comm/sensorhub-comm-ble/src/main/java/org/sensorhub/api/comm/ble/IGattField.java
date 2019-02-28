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

import java.nio.ByteBuffer;
import java.util.UUID;


/**
 * <p>
 * Base interface for GATT characteristics and descriptors
 * </p>
 *
 * @author Alex Robin
 * @since May 16, 2016
 */
public interface IGattField
{
    public static final int PERMISSION_READ = 1;
    public static final int PERMISSION_READ_ENCRYPTED = 2;
    public static final int PERMISSION_READ_ENCRYPTED_MITM = 4;
    public static final int PERMISSION_WRITE = 16;
    public static final int PERMISSION_WRITE_ENCRYPTED = 32;
    public static final int PERMISSION_WRITE_ENCRYPTED_MITM = 64;
    public static final int PERMISSION_WRITE_SIGNED = 128;
    public static final int PERMISSION_WRITE_SIGNED_MITM = 256;
    public static final int PROPERTY_BROADCAST = 1;
    public static final int PROPERTY_READ = 2;
    public static final int PROPERTY_WRITE_NO_ACK = 4;
    public static final int PROPERTY_WRITE_WITH_ACK = 8;
    public static final int PROPERTY_NOTIFY = 16;
    public static final int PROPERTY_INDICATE = 32;
    public static final int PROPERTY_SIGNED_WRITE = 64;
    public static final int PROPERTY_EXTENDED_PROPS = 128;


    /**
     * @return the UUID identifying the type of this field
     */
    public abstract UUID getType();


    /**
     * Gets the bit mask of permission flags for this field.
     * @return the permissions for this characteristic or descriptor.
     */
    public abstract int getPermissions();


    /**
     * Gets the cached value of the field.<br/>
     * This value is updated as a result of a read operation or if a
     * characteristic update notification has been received.
     * @return the stored value for this characteristic or descriptor.
     */
    public abstract ByteBuffer getValue();


    /**
     * Sets the cached value of this field.<br/>
     * This function only modifies the locally stored cached value of this
     * field. To send the value to the remote device, call
     * writeCharacteristic().
     * @param value
     * @return true if the locally stored value has been set.
     */
    public abstract boolean setValue(ByteBuffer value);

}