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
 * Specifies the Advertisement Data to be broadcast and some advertising
 * parameters. Properties which are not present will not be included in the
 * data. Required advertisement data types will always be included.
 * All UUIDs are 128-bit versions in the API, and 16 or 32-bit
 * versions of the same UUID will be used in the advertising data as appropriate.
 * </p>
 * <br/>
 * <p>
 * <b>Service:</b> org.bluez<br/>
 * <b>Interface:</b> org.bluez.LEAdvertisement1<br/>
 * <b>Object path:</b> freely definable
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 26, 2016
 */
public interface LEAdvertisement1 extends DBusInterface
{
       
    
    /**
     * This method gets called when the service daemon
     * removes the Advertisement. A client can use it to do
     * cleanup tasks. There is no need to call
     * UnregisterAdvertisement because when this method gets
     * called it has already been unregistered.
     */
    public void Release();
}
