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
import java.util.List;
import org.bluez.Adapter;
import org.bluez.Adapter.DeviceFound;
import org.bluez.Manager;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.exceptions.DBusException;
import org.sensorhub.api.comm.CommConfig;
import org.sensorhub.api.comm.IDeviceInfo;
import org.sensorhub.api.comm.ICommNetwork;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.comm.IDeviceScanCallback;
import org.sensorhub.api.comm.IDeviceScanner;
import org.sensorhub.api.comm.INetworkInfo;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.comm.BluetoothConfig;
import org.sensorhub.impl.module.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Implementation of Bluetooth LE network service based on Kura library.
 * This is based on the 'hcitool' and 'gatttool' utilities so will only work on
 * Linux 
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 11, 2016
 */
public class BleDbusCommNetwork extends AbstractModule<BluetoothNetworkConfig> implements ICommNetwork<BluetoothNetworkConfig>
{
    private static final Logger log = LoggerFactory.getLogger(BleDbusCommNetwork.class); 
    
    DBusConnection dbus;
    Manager manager;
    Adapter btAdapter;
    BleDeviceScanner bleScanner;
    
    
    class BleDeviceScanner implements IDeviceScanner
    {
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
                    dbus.addSigHandler(Adapter.DeviceFound.class, new DBusSigHandler<DeviceFound>() {
                        @Override
                        public void handle(final DeviceFound devFound)
                        {
                            final BluetoothConfig btConfig = new BluetoothConfig();
                            btConfig.deviceAddress = devFound.address;
                            //for (String key: devFound.values.keySet())
                            //    System.out.println(key);
                            
                            // create device info
                            IDeviceInfo devInfo = new IDeviceInfo() {

                                @Override
                                public String getName()
                                {
                                    return (String)devFound.values.get("Name").getValue();
                                }

                                @Override
                                public String getType()
                                {
                                    return devFound.values.get("Class").getValue().toString();
                                }

                                @Override
                                public String getAddress()
                                {
                                    return btConfig.deviceAddress;
                                }

                                @Override
                                public String getSignalLevel()
                                {
                                    return devFound.values.get("RSSI").getValue().toString();
                                }

                                @Override
                                public CommConfig getCommConfig()
                                {
                                    return btConfig;
                                }
                                
                            };
                            
                            callback.onDeviceFound(devInfo);                        
                        }
                    });
                    
                    btAdapter.StartDiscovery();
                }
                catch (DBusException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }            
        }

        @Override
        public synchronized void stopScan()
        {
            log.debug("Stopping Bluetooth LE scan");
            if (isScanning())
                btAdapter.StopDiscovery();   
        }

        @Override
        public boolean isScanning()
        {
            return (boolean)btAdapter.GetProperties().get("Discovering").getValue();
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
            
            List<Path> adapters = (List<Path>)manager.GetProperties().get("Adapters").getValue();
            for (Path objPath: adapters)
            {
                String path = objPath.getPath();
                Adapter bt = dbus.getRemoteObject("org.bluez", path, Adapter.class);
                final String address = (String)bt.GetProperties().get("Address").getValue();
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
                Path objPath = (Path)manager.FindAdapter(config.deviceName);
                btAdapter = dbus.getRemoteObject("org.bluez", objPath.getPath(), Adapter.class);
            }
            //btAdapter = dbus.getRemoteObject("org.bluez", adapterObjPath, Adapter.class);
            
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
    
    
    protected void initBlueZDbus() throws SensorHubException
    {
        try
        {
            if (dbus == null)
                dbus = DBusConnection.getConnection(DBusConnection.SYSTEM);
            
            if (manager == null)
                manager = dbus.getRemoteObject("org.bluez", "/", Manager.class);
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
