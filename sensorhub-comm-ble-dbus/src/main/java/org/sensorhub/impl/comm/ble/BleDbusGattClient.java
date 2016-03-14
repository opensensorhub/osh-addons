/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.comm.ble;

import java.util.List;
import org.bluez.Device1;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.Properties;
import org.freedesktop.dbus.Properties.PropertiesChanged;
import org.freedesktop.dbus.exceptions.DBusException;
import org.sensorhub.api.comm.ble.GattCallback;
import org.sensorhub.api.comm.ble.IGattCharacteristic;
import org.sensorhub.api.comm.ble.IGattClient;
import org.sensorhub.api.comm.ble.IGattDescriptor;
import org.sensorhub.api.comm.ble.IGattService;
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
public class BleDbusGattClient implements IGattClient
{
    private static final Logger log = LoggerFactory.getLogger(BleDbusGattClient.class);
    private static final String IFACE = Device1.class.getCanonicalName();
    
    DBusConnection dbus;
    String devObjPath;
    Device1 btDevice;
    Properties btDeviceProps;
    GattCallback callback;
    
    volatile boolean serviceDiscoveryRequested;
    volatile boolean servicesDiscovered;
    List<IGattService> services;
    
    
    BleDbusGattClient(DBusConnection dbus, String devObjPath, GattCallback callback)
    {
        this.dbus = dbus;
        this.devObjPath = devObjPath;
        this.callback = callback;
        
        try
        {
            this.btDevice = dbus.getRemoteObject(BleDbusCommNetwork.DBUS_BLUEZ, devObjPath, Device1.class);
            this.btDeviceProps = dbus.getRemoteObject(BleDbusCommNetwork.DBUS_BLUEZ, devObjPath, Properties.class);
        }
        catch (DBusException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    @Override
    public void connect()
    {
        try
        {
            // register signal handler to be notified when remote GATT
            // services have been discovered
            final DBusSigHandler connectHandler;
            dbus.addSigHandler(PropertiesChanged.class, connectHandler = new DBusSigHandler<PropertiesChanged>() {
                @Override
                public void handle(PropertiesChanged event)
                {
                    String objPath = event.getPath();
                    if (objPath == null || !objPath.startsWith(devObjPath))
                        return;
                    
                    log.debug("Properties changed {}: {}", objPath, event.changedProps);
                    
                    if (event.changedProps.containsKey(Device1.CONNECTED))
                        callback.onConnected(BleDbusGattClient.this, BleDbusGattClient.GATT_SUCCESS);
                    
                    if (event.changedProps.containsKey(Device1.GATT_SERVICES))
                    {
                        servicesDiscovered = true;
                        
                        // build service list
                        //event.changedProps.get(Device1.GATT_SERVICES)
                        
                        if (serviceDiscoveryRequested)
                            callback.onServicesDiscovered(BleDbusGattClient.this, BleDbusGattClient.GATT_SUCCESS);
                    }
                }                        
            });
            
            btDevice.Connect();
        }
        catch (Exception e)
        {
            log.error("Error while connecting to remove GATT server", e);
            callback.onConnected(this, IGattClient.GATT_FAILURE);
        }
    }
    
    
    @Override
    public void disconnect()
    {
        if ((boolean)btDeviceProps.Get(IFACE, Device1.CONNECTED))
            btDevice.Disconnect();
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
    public List<IGattService> getServices()
    {
        // generate service list
        
        
        return null;
    }


    @Override
    public boolean readCharacteristic(IGattCharacteristic characteristic)
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
    public boolean setCharacteristicNotification(IGattCharacteristic characteristic, boolean enable)
    {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean writeCharacteristic(IGattCharacteristic characteristic)
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
