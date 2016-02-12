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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.bluetooth.BluetoothAdapter;
import org.eclipse.kura.bluetooth.BluetoothDevice;
import org.eclipse.kura.bluetooth.BluetoothLeScanListener;
import org.eclipse.kura.linux.bluetooth.BluetoothAdapterImpl;
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
public class KuraBleCommNetwork extends AbstractModule<BluetoothNetworkConfig> implements ICommNetwork<BluetoothNetworkConfig>
{
    private static final Logger log = LoggerFactory.getLogger(KuraBleCommNetwork.class); 
    
    BluetoothAdapter btAdapter;
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
                
                btAdapter.startLeScan(new BluetoothLeScanListener() {
                    @Override
                    public void onScanFailed(int errCode)
                    {
                        callback.onScanError(new Exception("Error during Bluetooth LE Scan (Code " + errCode + ")"));                    
                    }

                    @Override
                    public void onScanResults(List<BluetoothDevice> devList)
                    {
                        log.debug("Received Bluetooth LE scan results ");
                        
                        for (BluetoothDevice btDev: devList)
                        {
                            final BluetoothConfig btConfig = new BluetoothConfig();
                            btConfig.deviceAddress = btDev.getAdress();
                            
                            // create device info
                            IDeviceInfo devInfo = new IDeviceInfo() {

                                @Override
                                public String getName()
                                {
                                    return btConfig.deviceName;
                                }

                                @Override
                                public String getType()
                                {
                                    return null;
                                }

                                @Override
                                public String getAddress()
                                {
                                    return btConfig.deviceAddress;
                                }

                                @Override
                                public String getSignalLevel()
                                {
                                    return null;
                                }

                                @Override
                                public CommConfig getCommConfig()
                                {
                                    return btConfig;
                                }
                                
                            };
                            
                            callback.onDeviceFound(devInfo);
                        }
                    }                    
                });
            }            
        }

        @Override
        public synchronized void stopScan()
        {
            btAdapter.killLeScan();            
        }

        @Override
        public boolean isScanning()
        {
            return btAdapter.isScanning();
        }     
    }
    
    
    @Override
    public Collection<INetworkInfo> getAvailableNetworks()
    {
        // for now always return config for hci0 adapter
        INetworkInfo netInfo;
        try
        {
            if (btAdapter == null)
                btAdapter = new BluetoothAdapterImpl("hci0");
            
            netInfo = new INetworkInfo() {
                @Override
                public String getInterfaceName()
                {
                    return "hci0";
                }

                @Override
                public NetworkType getNetworkType()
                {
                    return NetworkType.BLUETOOTH;
                }

                @Override
                public String getHardwareAddress()
                {
                    return btAdapter.getAddress();
                }

                @Override
                public String getLogicalAddress()
                {
                    return null;
                }            
            };
            
            return Arrays.asList(netInfo);
        }
        catch (KuraException e)
        {
            throw new RuntimeException();
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
            if (btAdapter == null)
                btAdapter = new BluetoothAdapterImpl(config.deviceName);
                        
            if (config.advertisementName != null)
            {
                btAdapter.setBeaconAdvertisingData(config.advertisementName, 1, 0, "0002", 0, false, true, true, true, true);
                btAdapter.setBeaconAdvertisingInterval(100, 1000);
                btAdapter.startBeaconAdvertising();
            }
        }
        catch (KuraException e)
        {
            throw new SensorHubException("Error while initializing Bluetooth adapter", e);
        }
    }


    @Override
    public void stop() throws SensorHubException
    {
        if (btAdapter != null)
        {
            btAdapter.stopBeaconAdvertising();
            btAdapter.killLeScan();
        }
        
        btAdapter = null;
        bleScanner = null;
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
