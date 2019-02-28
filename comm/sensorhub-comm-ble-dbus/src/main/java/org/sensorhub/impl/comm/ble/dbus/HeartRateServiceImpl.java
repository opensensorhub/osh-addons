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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bluez.HeartRate1;
import org.bluez.HeartRateManager1;
import org.bluez.HeartRateWatcher1;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.Properties;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;
import org.sensorhub.api.comm.ble.BleUtils;
import org.sensorhub.api.comm.ble.IGattCharacteristic;
import org.sensorhub.api.comm.ble.IGattDescriptor;
import org.sensorhub.api.comm.ble.IGattService;


/**
 * <p>
 * Implementation of GATT service wrapping DBus HeartRate1<br/>
 * This emulates a generic GATT service from the HeartRate1 interface since
 * BlueZ DBus implementation hides the generic service tree in favor of the
 * profile interface.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Mar 14, 2016
 */
public class HeartRateServiceImpl implements IGattService
{
    public final static int HEARTRATE_SERVICE = 0x180D;
    private final static String WATCHER_PATH = "/osh/hrwatcher";
    
    DBusConnection dbus;
    HeartRate1 hrService;
    HeartRateManager1 hrManager;
    Properties hrServiceProps;
    
    UUID uuid;
    List<IGattCharacteristic> characteristics = new ArrayList<IGattCharacteristic>(2);
    int heartRateValue;
    
    
    class HeartRateMeasurement implements IGattCharacteristic
    {
        @Override
        public IGattService getService()
        {
            return HeartRateServiceImpl.this;
        }

        @Override
        public int getHandle()
        {
            return 0;
        }

        @Override
        public UUID getType()
        {
            return BleUtils.getUUID(0x2A37);
        }

        @Override
        public int getProperties()
        {
            return 0;
        }

        @Override
        public int getPermissions()
        {
            return 0;
        }

        @Override
        public Map<UUID, IGattDescriptor> getDescriptors()
        {
            return Collections.EMPTY_MAP;
        }

        @Override
        public ByteBuffer getValue()
        {
            ByteBuffer buf = ByteBuffer.allocate(4);
            buf.putInt(heartRateValue);
            return buf;
        }

        @Override
        public boolean setValue(ByteBuffer value)
        {
            return false;
        }
        
        
        protected void setNotify(boolean enabled)
        {
            try
            {
                // unregister previous watcher if any
                // HACK exceptions will occur if no watcher was registered but that's ok
                try { dbus.unExportObject(WATCHER_PATH); }
                catch (Exception e) {}                
                try { hrManager.UnregisterWatcher(new Path(WATCHER_PATH)); }
                catch (Exception e) {}
                
                dbus.exportObject(WATCHER_PATH, new HeartRateWatcher1() {
                    public boolean isRemote() { return false; }                    
                    public void MeasurementReceived(Path device, Map<String, Variant<?>> measurement)
                    {
                        System.out.println(measurement);                    
                    }
                });
                
                hrManager.RegisterWatcher(new Path(WATCHER_PATH));
            }
            catch (DBusException e)
            {
                e.printStackTrace();
            }
        }
    }
    
    
    class BodySensorLocation implements IGattCharacteristic
    {
        @Override
        public IGattService getService()
        {
            return HeartRateServiceImpl.this;
        }

        @Override
        public int getHandle()
        {
            return 0;
        }

        @Override
        public UUID getType()
        {
            return BleUtils.getUUID(0x2A3);
        }

        @Override
        public int getProperties()
        {
            return 0;
        }

        @Override
        public int getPermissions()
        {
            return 0;
        }

        @Override
        public Map<UUID, IGattDescriptor> getDescriptors()
        {
            return Collections.EMPTY_MAP;
        }

        @Override
        public ByteBuffer getValue()
        {
            String loc = hrServiceProps.Get(HeartRate1.IFACE_NAME, HeartRate1.PROP_LOCATION);
            
            // convert to code
            byte[] b = new byte[1];            
            if ("other".equals(loc))
                b[0] = 0;
            else if ("chest".equals(loc))
                b[0] = 1;
            else if ("wrist".equals(loc))
                b[0] = 2;
            else if ("finger".equals(loc))
                b[0] = 3;
            else if ("hand".equals(loc))
                b[0] = 4;
            else if ("earlobe".equals(loc))
                b[0] = 5;
            else if ("foot".equals(loc))
                b[0] = 6;
            return ByteBuffer.wrap(b);
        }

        @Override
        public boolean setValue(ByteBuffer value)
        {
            return false;
        }
    }
    
    
    protected HeartRateServiceImpl(DBusConnection dbus, String devObjPath)
    {
        this.dbus = dbus;
        
        try
        {
            this.hrService = dbus.getRemoteObject(BleDbusCommNetwork.DBUS_BLUEZ, devObjPath, HeartRate1.class);
            this.hrServiceProps = dbus.getRemoteObject(BleDbusCommNetwork.DBUS_BLUEZ, devObjPath, Properties.class);
            
            String adaptObjPath = devObjPath.substring(0, devObjPath.lastIndexOf('/'));
            this.hrManager = dbus.getRemoteObject(BleDbusCommNetwork.DBUS_BLUEZ, adaptObjPath, HeartRateManager1.class);
                        
            // set type and handle
            this.uuid = BleUtils.getUUID(HEARTRATE_SERVICE);
            GattClientImpl.log.debug("Heart rate service {} ({})", uuid, devObjPath);
            
            // characteristics
            characteristics.add(new HeartRateMeasurement());
            if (hrServiceProps.GetAll(HeartRate1.IFACE_NAME).containsKey(HeartRate1.PROP_LOCATION))
                characteristics.add(new BodySensorLocation());
        }
        catch (DBusException e)
        {
            GattClientImpl.log.error("Error while retrieving heart rate service metadata " + devObjPath, e);
        }
    }
    
    
    @Override
    public int getHandle()
    {
        return 0;
    }


    @Override
    public UUID getType()
    {
        return uuid;
    }


    @Override
    public Collection<IGattCharacteristic> getCharacteristics()
    {
        return characteristics;
    }


    @Override
    public boolean addCharacteristic(IGattCharacteristic characteristic)
    {
        return false;
    }
}
