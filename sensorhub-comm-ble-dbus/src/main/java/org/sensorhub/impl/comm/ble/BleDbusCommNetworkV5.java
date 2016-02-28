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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import org.bluez.Adapter1;
import org.bluez.Device1;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.ObjectManager;
import org.freedesktop.dbus.ObjectManager.InterfacesAdded;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.Properties;
import org.freedesktop.dbus.Properties.PropertiesChanged;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;
import org.sensorhub.api.comm.CommConfig;
import org.sensorhub.api.comm.IDeviceInfo;
import org.sensorhub.api.comm.ICommNetwork;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.comm.IDeviceScanCallback;
import org.sensorhub.api.comm.IDeviceScanner;
import org.sensorhub.api.comm.INetworkInfo;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.module.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Implementation of Bluetooth LE network service based on BlueZ 5.X library.
 * This uses the DBus java implementation so it will only work on Linux.
 * Linux 
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 11, 2016
 */
public class BleDbusCommNetworkV5 extends AbstractModule<BluetoothNetworkConfig> implements ICommNetwork<BluetoothNetworkConfig>
{
    private static final Logger log = LoggerFactory.getLogger(BleDbusCommNetworkV5.class);
    private final static String DBUS_BLUEZ = "org.bluez";
    private final static String DBUS_BLUEZ_PATH = "/org/bluez/";
    private final static Pattern DBUS_BLUEZ_DEV_REGEX = Pattern.compile(DBUS_BLUEZ_PATH + ".*/dev.*");
    
    DBusConnection dbus;
    ObjectManager manager;
    Adapter1 btAdapter;
    Properties btAdapterProps;
    BleDeviceScanner bleScanner;
    
    
    class BleDeviceScanner implements IDeviceScanner
    {
        DBusSigHandler<InterfacesAdded> scanHandler1;
        DBusSigHandler<PropertiesChanged> scanHandler2;
        
        @Override
        public void startScan(IDeviceScanCallback callback)
        {
            startScan(callback, null);
        }

        @Override
        public synchronized void startScan(final IDeviceScanCallback callback, String idRegex)
        {
            if (!isScanning())
            {
                log.debug("Starting Bluetooth LE scan");
                
                try
                {
                    // listen for new connected devices
                    dbus.addSigHandler(InterfacesAdded.class, scanHandler1 = new DBusSigHandler<InterfacesAdded>() {
                        @Override
                        public void handle(final InterfacesAdded event)
                        {
                            if (!DBUS_BLUEZ_DEV_REGEX.matcher(event.objPath).matches())
                                return;
                                                        
                            try
                            {
                                log.debug("New interfaces: " + event.interfaces);
                                
                                Map<String, Variant<?>> props = event.interfaces.get(Device1.class.getCanonicalName());
                                log.debug("New device found {}: {}", event.objPath, props);
                                if (props != null)
                                    sendDeviceInfo(event.objPath, props, callback);
                            }
                            catch (Exception e)
                            {
                                log.error("Error while scanning for Bluetooth devices", e);
                            }
                        }
                    });
                    
                    // listen for changes in already connected devices
                    dbus.addSigHandler(PropertiesChanged.class, scanHandler2 = new DBusSigHandler<PropertiesChanged>() {
                        @Override
                        public void handle(PropertiesChanged event)
                        {
                            String objPath = event.getPath();
                            if (!DBUS_BLUEZ_DEV_REGEX.matcher(objPath).matches())
                                return;
                            
                            try
                            {
                                log.debug("Properties changed {}: {}", objPath, event.changedProps);
                                
                                Properties propIface = dbus.getRemoteObject(DBUS_BLUEZ, objPath, Properties.class);
                                Map<String, Variant<?>> props = propIface.GetAll(Device1.class.getCanonicalName());
                                if (!props.isEmpty()) // some props are empty at end of scan
                                    sendDeviceInfo(objPath, props, callback);
                            }
                            catch (DBusException e)
                            {
                                log.error("Error while scanning for Bluetooth devices", e);
                            }
                        }                        
                    });
                    
                    btAdapter.StartDiscovery();
                }
                catch (DBusException e)
                {
                    log.error("Error while starting Bluetooth scan", e);
                }
            }            
        }

        @Override
        public synchronized void stopScan()
        {
            try
            {
                if (isScanning())
                    btAdapter.StopDiscovery();
                
                if (scanHandler1 != null)
                {
                    dbus.removeSigHandler(InterfacesAdded.class, scanHandler1);
                    scanHandler1 = null;
                }
                
                if (scanHandler2 != null)
                {
                    dbus.removeSigHandler(PropertiesChanged.class, scanHandler2);
                    scanHandler2 = null;
                }
                
                log.debug("Stopping Bluetooth LE scan");
            }
            catch (DBusException e)
            {
                log.error("Error while stopping Bluetooth scan", e);
            }
        }

        @Override
        public boolean isScanning()
        {
            return btAdapterProps.Get(Adapter1.class.getCanonicalName(), "Discovering");
        }     
    }
    
    
    protected void sendDeviceInfo(String objPath, final Map<String, Variant<?>> props, final IDeviceScanCallback callback)
    {
        Variant<?> val;
        
        final String address = props.get("Address").getValue().toString();
        
        final String name;
        val = props.get("Name");
        if (val != null)
            name = val.getValue().toString();
        else
            name = null;
        
        final String type;
        val = props.get("Class");
        if (val != null)
            type = val.getValue().toString();
        else
            type = null;
        
        final String rssi;
        val = props.get("RSSI");
        if (val != null)
            rssi = val.getValue().toString();
        else
            rssi = null;
        
        // create device info
        IDeviceInfo devInfo = new IDeviceInfo() {

            @Override
            public String getName()
            {
                return name;
            }

            @Override
            public String getType()
            {
                return type;
            }

            @Override
            public String getAddress()
            {
                return address;
            }

            @Override
            public String getSignalLevel()
            {
                return rssi;
            }

            @Override
            public CommConfig getCommConfig()
            {
                return null;
            }
            
        };
        
        callback.onDeviceFound(devInfo);
        
        try
        {
            if (address.equals("EB:33:AC:60:77:7E") && ((Boolean)props.get("Connected").getValue()) == false)
            {
                System.out.println("Connecting to " + objPath);
                Device1 device = dbus.getRemoteObject(DBUS_BLUEZ, objPath, Device1.class);
                
                final DBusSigHandler connectHandler;
                dbus.addSigHandler(PropertiesChanged.class, connectHandler = new DBusSigHandler<PropertiesChanged>() {
                    @Override
                    public void handle(PropertiesChanged event)
                    {
                        String objPath = event.getPath();
                        if (!DBUS_BLUEZ_DEV_REGEX.matcher(objPath).matches())
                            return;
                        
                        try
                        {
                            log.debug("Properties changed {}: {}", objPath, event.changedProps);
                            
                            Properties propIface = dbus.getRemoteObject(DBUS_BLUEZ, objPath, Properties.class);
                            Map<String, Variant<?>> props = propIface.GetAll(Device1.class.getCanonicalName());
                            if (!props.isEmpty()) // some props are empty at end of scan
                                sendDeviceInfo(objPath, props, callback);
                        }
                        catch (DBusException e)
                        {
                            log.error("Error while scanning for Bluetooth devices", e);
                        }
                    }                        
                });
                
                
                device.Connect();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    
    @Override
    public Collection<INetworkInfo> getAvailableNetworks()
    {
        INetworkInfo netInfo;
        try
        {
            initBlueZDbus();
            
            ArrayList<INetworkInfo> btNets = new ArrayList<INetworkInfo>();
            
            Map<Path, Map<String, Map<String,Variant<?>>>> adapters = manager.GetManagedObjects();
            for (Entry<Path, Map<String, Map<String,Variant<?>>>> objPath: adapters.entrySet())
            {
                String path = objPath.getKey().getPath();
                
                for (Entry<String, Map<String,Variant<?>>> obj: objPath.getValue().entrySet())
                {
                    if (obj.getKey().equals(Adapter1.class.getCanonicalName()))
                    {                    
                        final String address = (String)obj.getValue().get("Address").getValue();
                        final String name = path.substring(path.lastIndexOf('/')+1);
                        
                        netInfo = new INetworkInfo() {
                            @Override
                            public String getInterfaceName()
                            {
                                return name;
                            }
            
                            @Override
                            public NetworkType getNetworkType()
                            {
                                return NetworkType.BLUETOOTH;
                            }
            
                            @Override
                            public String getHardwareAddress()
                            {
                                return address;
                            }
            
                            @Override
                            public String getLogicalAddress()
                            {
                                return null;
                            }            
                        };
                        
                        btNets.add(netInfo);
                    }
                }
            }
            
            return btNets;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error while retrieving Bluetooth adapters", e);
        }
    }


    @Override
    public ICommProvider<?> newCommProvider(CommConfig config)
    {
        return null;
    }


    @Override
    public void start() throws SensorHubException
    {
        try
        {
            initBlueZDbus();
            
            if (btAdapter == null)
            {
                btAdapter = dbus.getRemoteObject(DBUS_BLUEZ, "/org/bluez/"+config.deviceName, Adapter1.class);
                btAdapterProps = dbus.getRemoteObject(DBUS_BLUEZ, "/org/bluez/"+config.deviceName, Properties.class);
            }
            // will only be possible with bluez 5
            /*if (config.advertisementName != null)
            {
                btAdapter.setBeaconAdvertisingData(config.advertisementName, 1, 0, "0002", 0, false, true, true, true, true);
                btAdapter.setBeaconAdvertisingInterval(100, 1000);
                btAdapter.startBeaconAdvertising();
            }*/
        }
        catch (Exception e)
        {
            throw new SensorHubException("Error while initializing Bluetooth adapter", e);
        }
    }
    
    
    protected synchronized void initBlueZDbus() throws SensorHubException
    {
        try
        {
            if (dbus == null)
                dbus = DBusConnection.getConnection(DBusConnection.SYSTEM);
            
            if (manager == null)
                manager = dbus.getRemoteObject(DBUS_BLUEZ, "/", ObjectManager.class);
        }
        catch (DBusException e)
        {
            throw new SensorHubException("Error while connecting to BlueZ D-Bus service", e);
        }
    }


    @Override
    public void stop() throws SensorHubException
    {
        if (btAdapter != null)
            btAdapter.StopDiscovery();
        btAdapter = null;
        bleScanner = null;
        
        dbus.disconnect();
        dbus = null;
        manager = null;        
    }


    @Override
    public String getInterfaceName()
    {
        return config.deviceName;
    }


    @Override
    public NetworkType getNetworkType()
    {
        return NetworkType.BLUETOOTH;
    }
    
    
    @Override
    public IDeviceScanner getDeviceScanner()
    {
        if (bleScanner == null)
            bleScanner = new BleDeviceScanner();
        return bleScanner;
    }
    
    
    @Override
    public void cleanup() throws SensorHubException
    {
    }    
}
