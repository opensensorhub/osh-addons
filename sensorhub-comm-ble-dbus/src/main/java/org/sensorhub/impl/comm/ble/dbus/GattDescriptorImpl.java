/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.comm.ble.dbus;

import java.nio.ByteBuffer;
import java.util.UUID;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.Properties;
import org.freedesktop.dbus.exceptions.DBusException;
import org.sensorhub.api.comm.ble.IGattDescriptor;
import org.sensorhub.api.comm.ble.IGattField;


/**
 * <p>
 * Implementation of GATT descriptor wrapping DBus GattDescriptor1
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Mar 15, 2016
 */
public class GattDescriptorImpl implements IGattDescriptor
{
    IGattField characteristic;
    DBusConnection dbus;
    String descObjPath;
    //GattCharacteristic1 gattDesc;
    Properties gattDescProps;
    
    int handle;
    UUID uuid;
    
    
    GattDescriptorImpl(IGattField parent, DBusConnection dbus, String descObjPath) throws DBusException
    {
        this.characteristic = parent;
        this.dbus = dbus;
        this.descObjPath = descObjPath;
    }
    
    
    @Override
    public IGattField getCharacteristic()
    {
        return characteristic;
    }


    @Override
    public UUID getType()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public int getPermissions()
    {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public ByteBuffer getValue()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public boolean setValue(ByteBuffer value)
    {
        // TODO Auto-generated method stub
        return false;
    }

}
