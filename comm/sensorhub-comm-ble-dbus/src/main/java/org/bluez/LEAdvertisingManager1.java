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

import java.util.Map;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.Variant;


/**
 * <p>
 * The Advertising Manager allows external applications to register Advertisement
 * Data which should be broadcast to devices.  Advertisement Data elements must
 * follow the API for LE Advertisement Data described above.
 * </p>
 * <br/>
 * <p>
 * <b>Service:</b> org.bluez<br/>
 * <b>Interface:</b> org.bluez.LEAdvertisingManager1 [Experimental]<br/>
 * <b>Object path:</b> /org/bluez/{hci0,hci1,...}<br/>
 * </p>
 * 
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 26, 2016
 */
public interface LEAdvertisingManager1 extends DBusInterface
{
    /**
     * Registers an advertisement object to be sent over the LE
     * Advertising channel.  The service must be exported
     * under interface LEAdvertisement1. InvalidArguments
     * indicates that the object has invalid or conflicting
     * properties.  InvalidLength indicates that the data
     * provided generates a data packet which is too long.
     * The properties of this object are parser when it is
     * registered, and any changes are ignored.
     * Currently only one advertisement at a time is supported,
     * attempting to register two advertisements will result in
     * an AlreadyExists error.
     *
     * Possible errors: org.bluez.Error.InvalidArguments
     *                  org.bluez.Error.AlreadyExists
     *                  org.bluez.Error.InvalidLength
     *                
     * @param advertisement
     * @param options
     */
    public void RegisterAdvertisement(LEAdvertisement1 advertisement, Map<String,Variant<?>> options);
    
    
    /**
     * This unregisters an advertisement that has been
     * prevously registered.  The object path parameter must
     * match the same value that has been used on registration.
     *
     * Possible errors: org.bluez.Error.InvalidArguments
     *                  org.bluez.Error.DoesNotExist
     * 
     * @param advertisement
     */
    public void UnregisterAdvertisement(LEAdvertisement1 advertisement);
}
