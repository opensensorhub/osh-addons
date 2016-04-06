/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.bluez;

import org.freedesktop.dbus.DBusInterface;


/**
 * <p>
 * GATT Service Representation.
 * </p>
 * <br/>
 * <p>
 * <b>Service:</b> org.bluez<br/>
 * <b>Interface:</b> org.bluez.GattService1 [Experimental]<br/>
 * <b>Object path:</b> [variable prefix]/{hci0,hci1,...}/dev_XX_XX_XX_XX_XX_XX/serviceXX<br/>
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 28, 2016
 */
public interface GattService1 extends DBusInterface
{
    public final static String IFACE_NAME = GattService1.class.getCanonicalName();
    
    
    // Properties    
    
    /**
     * string UUID [read-only]<br/>
     * 128-bit service UUID.
     */
    public final static String PROP_UUID = "UUID";
    

    /**
     * boolean Primary [read-only]<br/>
     * Indicates whether or not this GATT service is a
     * primary service. If false, the service is secondary.
     */
    public final static String PROP_PRIMARY = "Primary";
    

    /**
     * object Device [read-only, optional]<br/>
     * Object path of the Bluetooth device the service
     * belongs to. Only present on services from remote
     * devices.
     */
    public final static String PROP_DEVICE = "Device";
    

    /**
     * array{object} Characteristics [read-only]<br/>
     * Array of object paths representing the characteristics
     * of this service. This property is set only when the
     * characteristic discovery has been completed, however the
     * characteristic objects will become available via
     * ObjectManager as soon as they get discovered.
     */
    public final static String PROP_CHARACTERISTICS = "Characteristics";
    

    /**
     * array{object} Includes [read-only]: Not implemented<br/>
     * Array of object paths representing the included
     * services of this service.<br/>
     */
    public final static String PROP_INCLUDES = "Includes";            

}
