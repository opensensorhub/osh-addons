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
 * GATT Characteristic Representation
 * </p>
 * <br/>
 * <p>
 * <b>Service:</b> org.bluez<br/>
 * <b>Interface:</b> org.bluez.GattCharacteristic1 [Experimental]<br/>
 * <b>Object path:</b> [variable prefix]/{hci0,hci1,...}/dev_XX_XX_XX_XX_XX_XX/serviceXX/charYYYY<br/>
 * </p>
 * 
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 28, 2016
 */
public interface GattCharacteristic1 extends DBusInterface
{
    public final static String IFACE_NAME = GattCharacteristic1.class.getCanonicalName();
    
    
    // Properties

    /**
     * string UUID [read-only]<br/>
     * 128-bit characteristic UUID.
     **/
    public final static String PROP_UUID = "UUID";

    /**
     * object Service [read-only]<br/>
     * Object path of the GATT service the characteristc
     * belongs to.
     */
    public final static String PROP_SERVICE = "Service";
    

    /**
     * array{byte} Value [read-only, optional]<br/>
     * The cached value of the characteristic. This property
     * gets updated only after a successful read request and
     * when a notification or indication is received, upon
     * which a PropertiesChanged signal will be emitted.
     */
    public final static String PROP_VALUE = "Value";
    

    /**
     * boolean Notifying [read-only]<br/>
     * True, if notifications or indications on this
     * characteristic are currently enabled.
     */
    public final static String PROP_NOTIFYING = "Notifying";
    

    /**
     * array{string} Flags [read-only]<br/>
     * Defines how the characteristic value can be used. See
     * Core spec "Table 3.5: Characteristic Properties bit
     * field", and "Table 3.8: Characteristic Extended
     * Properties bit field". Allowed values:<br/>
     * 
     * "broadcast"<br/>
     * "read"<br/>
     * "write-without-response"<br/>
     * "write"<br/>
     * "notify"<br/>
     * "indicate"<br/>
     * "authenticated-signed-writes"<br/>
     * "reliable-write"<br/>
     * "writable-auxiliaries"<br/>
     * "encrypt-read"<br/>
     * "encrypt-write"<br/>
     * "encrypt-authenticated-read"<br/>
     * "encrypt-authenticated-write"
     */
    public final static String PROP_FLAGS = "Flags";
    

    /**
     * array{object} Descriptors [read-only]<br/>
     * Array of object paths representing the descriptors
     * of this service. This property is set only when the
     * descriptor discovery has been completed, however the
     * descriptor objects will become available via
     * ObjectManager as soon as they get discovered.
     */
    public final static String PROP_DESCRIPTORS = "Descriptors";


    // Methods

    /**
     * Issues a request to read the value of the characteristic
     * @return the value of the characteristic if the operation was successful.
     */
    public byte[] ReadValue();


    /**
     * Issues a request to write the value of the characteristic.
     * @param value
     */
    public void WriteValue(byte[] value);


    /**
     * Starts a notification session from this characteristic if it
     * supports value notifications or indications.
     */
    public void StartNotify();


    /**
     * This method will cancel any previous StartNotify transaction.
     * Note that notifications from a characteristic are shared between sessions
     * thus calling StopNotify will release a single session.
     */
    public void StopNotify();
}
