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
import java.util.Map;
import java.util.Map.Entry;
import org.bluez.Device1;
import org.bluez.GattCharacteristic1;
import org.bluez.GattService1;
import org.bluez.HeartRate1;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.ObjectManager;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.Properties;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.Properties.PropertiesChanged;
import org.freedesktop.dbus.exceptions.DBusException;
import org.sensorhub.api.comm.ble.GattCallback;
import org.sensorhub.api.comm.ble.IGattCharacteristic;
import org.sensorhub.api.comm.ble.IGattClient;
import org.sensorhub.api.comm.ble.IGattDescriptor;
import org.sensorhub.api.comm.ble.IGattService;
import org.sensorhub.impl.comm.ble.dbus.HeartRateServiceImpl.BodySensorLocation;
import org.sensorhub.impl.comm.ble.dbus.HeartRateServiceImpl.HeartRateMeasurement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * BlueZ D-Bus GATT client implementation
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 28, 2016
 */
public class GattClientImpl implements IGattClient
{
    static final Logger log = LoggerFactory.getLogger(GattClientImpl.class);
    private static final String IFACE = Device1.class.getCanonicalName();
    
    DBusConnection dbus;
    ObjectManager objManager;
    
    String devObjPath;
    Device1 btDevice;
    Properties btDeviceProps;
    GattCallback callback;
    DBusSigHandler<PropertiesChanged> sigHandler;
    
    volatile boolean serviceDiscoveryRequested;
    volatile boolean servicesDiscovered;
    Map<String, IGattService> services = new LinkedHashMap<String, IGattService>();
    
    
    GattClientImpl(DBusConnection dbus, ObjectManager objManager, String devObjPath, GattCallback callback)
    {
        this.dbus = dbus;
        this.objManager = objManager;
        this.devObjPath = devObjPath;
        this.callback = callback;
        
        try
        {
            this.btDevice = dbus.getRemoteObject(BleDbusCommNetwork.DBUS_BLUEZ, devObjPath, Device1.class);
            this.btDeviceProps = dbus.getRemoteObject(BleDbusCommNetwork.DBUS_BLUEZ, devObjPath, Properties.class);
        }
        catch (DBusException e)
        {
            log.error("Error while retrieving device metadata " + devObjPath, e);
        }
    }


    @Override
    public void connect()
    {
        try
        {
            // register signal handler to be notified when device state changes
            sigHandler = new DBusSigHandler<PropertiesChanged>() {
                @Override
                public void handle(PropertiesChanged event)
                {
                    String objPath = event.getPath();
                    if (objPath == null || !objPath.startsWith(devObjPath))
                        return;
                    
                    log.trace("Properties changed {}", objPath, event.changedProps);
                    
                    // connection status
                    if (event.changedProps.containsKey(Device1.CONNECTED))
                    {
                        boolean connected = (Boolean)event.changedProps.get(Device1.CONNECTED).getValue();
                        if (connected)
                        {
                            listKnownServices(devObjPath);
                            try { Thread.sleep(100); }
                            catch (InterruptedException e) { }
                            callback.onConnected(GattClientImpl.this, IGattClient.GATT_SUCCESS);
                        }
                        else
                        {
                            services.clear();
                            callback.onDisconnected(GattClientImpl.this, IGattClient.GATT_SUCCESS);
                        }
                    }
                    
                    // new services
                    if (event.changedProps.containsKey(GattService1.PROP_CHARACTERISTICS))
                    {
                        Variant<?> charList = event.changedProps.get(GattService1.PROP_CHARACTERISTICS);
                        GattServiceImpl s = new GattServiceImpl(dbus, objPath, charList);
                        services.put(objPath, s);
                    }
                    
                    // property value changes
                    if (event.changedProps.containsKey(GattCharacteristic1.PROP_VALUE))
                    {
                        log.debug("Property value changed {}", objPath, event.changedProps);                        
                        GattCharacteristicImpl c = getCharacteristic(objPath);
                        if (c.isNotifying())
                            callback.onCharacteristicChanged(GattClientImpl.this, c);
                        else
                            callback.onCharacteristicRead(GattClientImpl.this, c, GATT_SUCCESS);
                    }
                    
                    // end service discovery
                    if (event.changedProps.containsKey(Device1.GATT_SERVICES))
                    {
                        servicesDiscovered = true;
                        if (serviceDiscoveryRequested)
                            callback.onServicesDiscovered(GattClientImpl.this, GattClientImpl.GATT_SUCCESS);
                    }
                }                        
            };
            
            dbus.addSigHandler(PropertiesChanged.class, sigHandler);
            btDevice.Connect();
        }
        catch (Exception e)
        {
            log.error("Cannot connect to remote GATT server", e);
            callback.onConnected(this, IGattClient.GATT_FAILURE);
        }
    }
    
    
    @Override
    public void disconnect()
    {
        try
        {
            if ((boolean)btDeviceProps.Get(IFACE, Device1.CONNECTED))
                btDevice.Disconnect();
        }
        catch (Exception e)
        {
            log.error("Cannot disconnect from remote GATT server", e);
            callback.onDisconnected(this, IGattClient.GATT_FAILURE);
        }
    }
    
    
    protected void listKnownServices(String devObjPath)
    {
        try
        {
            // lookup services objects already in DBus tree
            // this happens when device was connected before
            Map<Path, Map<String, Map<String,Variant<?>>>> adapters = objManager.GetManagedObjects();
            for (Entry<Path, Map<String, Map<String,Variant<?>>>> objPath: adapters.entrySet())
            {
                String path = objPath.getKey().getPath();
                
                for (Entry<String, Map<String,Variant<?>>> obj: objPath.getValue().entrySet())
                {
                    if (obj.getKey().equals(HeartRate1.class.getCanonicalName()))
                    {
                        services.put(devObjPath, new HeartRateServiceImpl(dbus, devObjPath));
                    }
                    
                    else if (obj.getKey().equals(GattService1.class.getCanonicalName()))
                    {
                        Variant<?> charList = obj.getValue().get(GattService1.PROP_CHARACTERISTICS);
                        GattServiceImpl s = new GattServiceImpl(dbus, path, charList);
                        services.put(path, s);
                    }
                }
            }
            
            // add profiles if any
            
            if (!services.isEmpty())
                servicesDiscovered = true;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error while retrieving Bluetooth services", e);
        }
    }
    
    
    protected GattServiceImpl getService(String objPath)
    {
        return (GattServiceImpl)services.get(objPath);
    }
    
    
    protected GattCharacteristicImpl getCharacteristic(String objPath)
    {
        String servObjPath = objPath.substring(0, objPath.lastIndexOf('/'));
        GattServiceImpl s = getService(servObjPath);
        return (GattCharacteristicImpl)s.getCharacteristic(objPath);
    }
    
    
    @Override
    public boolean discoverServices()
    {
        serviceDiscoveryRequested = true;
        if (servicesDiscovered)
            callback.onServicesDiscovered(this, IGattClient.GATT_SUCCESS);        
        return true;
    }


    @Override
    public Collection<IGattService> getServices()
    {
        return services.values();
    }


    @Override
    public boolean readCharacteristic(IGattCharacteristic characteristic)
    {
        // special cases for heart rate profile loaded by BlueZ
        if (characteristic instanceof HeartRateMeasurement)
        {
            return false;
        }
        else if (characteristic instanceof BodySensorLocation)
        {
            callback.onCharacteristicRead(this, characteristic, GATT_SUCCESS);
            return true;
        }
        
        // generic case
        ((GattCharacteristicImpl)characteristic).gattChar.ReadValue();
        return true;
    }


    @Override
    public boolean setCharacteristicNotification(IGattCharacteristic characteristic, boolean enable)
    {
        // special case for heart rate measurement
        if (characteristic instanceof HeartRateMeasurement)
        {
            ((HeartRateMeasurement)characteristic).setNotify(enable);
            return true;
        }            
        
        // generic case
        if (characteristic instanceof GattCharacteristicImpl)
        {
            if (enable)
            {
                if (!((GattCharacteristicImpl)characteristic).isNotifying())
                    ((GattCharacteristicImpl)characteristic).gattChar.StartNotify();
            }            
            else
                ((GattCharacteristicImpl)characteristic).gattChar.StopNotify();
            
            return true;
        }
        
        return false;
    }


    @Override
    public boolean writeCharacteristic(IGattCharacteristic characteristic)
    {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean readDescriptor(IGattDescriptor descriptor)
    {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean writeDescriptor(IGattDescriptor descriptor)
    {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public void close()
    {
        // TODO Auto-generated method stub

    }

}
