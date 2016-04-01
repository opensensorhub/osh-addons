/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

import org.sensorhub.api.comm.IDeviceInfo;
import org.sensorhub.api.comm.IDeviceScanCallback;
import org.sensorhub.api.comm.IDeviceScanner;
import org.sensorhub.api.comm.ble.GattCallback;
import org.sensorhub.api.comm.ble.IGattClient;
import org.sensorhub.impl.comm.ble.dbus.BleDbusCommNetwork;
import org.sensorhub.impl.comm.ble.dbus.BluetoothNetworkConfig;


public class TestBleDbusCommNetwork
{

    public static void main(String[] args) throws Exception
    {
        BleDbusCommNetwork bleNet = new BleDbusCommNetwork();
        bleNet.init(new BluetoothNetworkConfig());
        bleNet.start();
        
        // start scan
        IDeviceScanner scanner = bleNet.getDeviceScanner();
        scanner.startScan(new IDeviceScanCallback()
        {
            @Override
            public void onDeviceFound(IDeviceInfo info)
            {
                System.out.println("Device detected");
                System.out.println("Name: " + info.getName());
                System.out.println("Address: " + info.getAddress());
                System.out.println("RSSI: " + info.getSignalLevel());
                System.out.println();
            }

            @Override
            public void onScanError(Throwable e)            
            {
                e.printStackTrace();                
            }            
        });
        
        // stop scanning after 5s
        Thread.sleep(5000);
        scanner.stopScan();
        
        // try to connect to GATT device
        String address = "EB:33:AC:60:77:7E";
        System.out.println("Connecting to " + address);
        bleNet.connectGatt(address, new GattCallback() {

            @Override
            public void onConnected(IGattClient gatt, int status)
            {
                if (status == IGattClient.GATT_SUCCESS)
                {
                    System.out.println("Connected to GATT server");
                    gatt.discoverServices();
                }
                else
                {
                    System.err.println("Error while connecting to GATT server");
                }
            }

            @Override
            public void onServicesDiscovered(IGattClient gatt, int status)
            {
                System.out.println("GATT services discovered");
            }
            
        });
    }
}
