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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bluez.GattService1;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.Properties;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;
import org.sensorhub.api.comm.ble.IGattCharacteristic;
import org.sensorhub.api.comm.ble.IGattField;
import org.sensorhub.api.comm.ble.IGattService;


/**
 * <p>
 * Implementation of GATT service wrapping DBus GattService1
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Mar 14, 2016
 */
public class GattServiceImpl implements IGattService
{
    DBusConnection dbus;
    String servObjPath;
    GattService1 gattService;
    Properties gattServiceProps;
    
    int handle;
    UUID uuid;
    Map<String, IGattCharacteristic> characteristics = new LinkedHashMap<String, IGattCharacteristic>();
    
    
    protected GattServiceImpl(DBusConnection dbus, String servObjPath, Variant<?> charList)
    {
        this.dbus = dbus;
        this.servObjPath = servObjPath;
        
        try
        {
            this.gattService = dbus.getRemoteObject(BleDbusCommNetwork.DBUS_BLUEZ, servObjPath, GattService1.class);
            this.gattServiceProps = dbus.getRemoteObject(BleDbusCommNetwork.DBUS_BLUEZ, servObjPath, Properties.class);        
                        
            // read type and handle
            this.uuid = UUID.fromString((String)gattServiceProps.Get(GattService1.IFACE_NAME, GattService1.PROP_UUID));
            this.handle = Integer.valueOf(servObjPath.substring(servObjPath.length()-4), 16);
            GattClientImpl.log.debug("GATT service {} ({})", uuid, servObjPath);
            
            // characteristics
            for (Path charObjPath: (List<Path>)charList.getValue())
            {
                String objPath = charObjPath.getPath();
                GattCharacteristicImpl c = new GattCharacteristicImpl(this, dbus, objPath);
                characteristics.put(objPath, c);
            }
        }
        catch (DBusException e)
        {
            GattClientImpl.log.error("Error while retrieving service metadata " + servObjPath, e);
        }
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
    public Collection<IGattCharacteristic> getCharacteristics()
    {
        return characteristics.values();
    }


    @Override
    public boolean addCharacteristic(IGattCharacteristic characteristic)
    {
        // no support for adding characteristics for now
        return false;
    }
    
    
    protected IGattField getCharacteristic(String objPath)
    {
        return characteristics.get(objPath);
    }

}
