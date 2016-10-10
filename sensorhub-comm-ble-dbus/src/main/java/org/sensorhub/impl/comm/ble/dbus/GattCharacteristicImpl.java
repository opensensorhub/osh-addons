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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bluez.GattCharacteristic1;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.Properties;
import org.freedesktop.dbus.exceptions.DBusException;
import org.sensorhub.api.comm.ble.IGattCharacteristic;
import org.sensorhub.api.comm.ble.IGattDescriptor;
import org.sensorhub.api.comm.ble.IGattService;


/**
 * <p>
 * Implementation of GATT service wrapping DBus GattCharacteristic1
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Mar 14, 2016
 */
public class GattCharacteristicImpl implements IGattCharacteristic
{
    IGattService service;
    DBusConnection dbus;
    String charObjPath;
    GattCharacteristic1 gattChar;
    Properties gattCharProps;
    
    int handle;
    UUID uuid;
    Map<String, IGattDescriptor> descriptors = new LinkedHashMap<String, IGattDescriptor>();
        
    
    protected GattCharacteristicImpl(IGattService service, DBusConnection dbus, String charObjPath) throws DBusException
    {
        this.service = service;
        
        this.dbus = dbus;
        this.charObjPath = charObjPath;
        
        this.gattChar = dbus.getRemoteObject(BleDbusCommNetwork.DBUS_BLUEZ, charObjPath, GattCharacteristic1.class);
        this.gattCharProps = dbus.getRemoteObject(BleDbusCommNetwork.DBUS_BLUEZ, charObjPath, Properties.class);
                
        // read type and handle
        this.uuid = UUID.fromString((String)gattCharProps.Get(GattCharacteristic1.IFACE_NAME, GattCharacteristic1.PROP_UUID));
        this.handle = Integer.valueOf(charObjPath.substring(charObjPath.length()-4), 16);        
        GattClientImpl.log.debug("GATT characteristic {} ({})", uuid, charObjPath);
                
        // descriptors
        List<Path> descPaths = gattCharProps.Get(GattCharacteristic1.IFACE_NAME, GattCharacteristic1.PROP_DESCRIPTORS);
        for (Path descObjPath: descPaths)
        {
            String objPath = descObjPath.getPath();
            GattDescriptorImpl d = new GattDescriptorImpl(this, dbus, objPath);
            descriptors.put(objPath, d);
        }
    }


    @Override
    public IGattService getService()
    {
        return service;
    }


    @Override
    public int getHandle()
    {
        return handle;
    }


    @Override
    public UUID getType()
    {
        return uuid;
    }


    @Override
    public int getProperties()
    {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public int getPermissions()
    {
        //String[] perm = gattCharProps.Get(GattCharacteristic1.IFACE_NAME, GattCharacteristic1.PROP_FLAGS);
        // TODO convert string to integer code
        return 0;
    }


    @Override
    public Map<UUID, IGattDescriptor> getDescriptors()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    
    protected IGattDescriptor getDescriptor(String objPath)
    {
        return descriptors.get(objPath);
    }


    @Override
    public ByteBuffer getValue()
    {
        byte[] data = gattCharProps.Get(GattCharacteristic1.IFACE_NAME, GattCharacteristic1.PROP_VALUE);
        return ByteBuffer.wrap(data);
    }


    @Override
    public boolean setValue(ByteBuffer value)
    {
        gattCharProps.Set(GattCharacteristic1.IFACE_NAME, GattCharacteristic1.PROP_VALUE, value.array());
        return true;
    }
    
    
    protected boolean isNotifying()
    {
        return (boolean)gattCharProps.Get(GattCharacteristic1.IFACE_NAME, GattCharacteristic1.PROP_NOTIFYING);
    }
}
