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
import org.sensorhub.impl.comm.ble.BleDbusCommNetworkV5;
import org.sensorhub.impl.comm.ble.BluetoothNetworkConfig;


public class TestBleDbusCommNetwork
{

    public static void main(String[] args) throws Exception
    {
        BleDbusCommNetworkV5 bleNet = new BleDbusCommNetworkV5();
        bleNet.init(new BluetoothNetworkConfig());
        bleNet.start();
        
        IDeviceScanner scanner = bleNet.getDeviceScanner();
        scanner.startScan(new IDeviceScanCallback()
        {
            @Override
            public void onDeviceFound(IDeviceInfo info)
            {
                System.out.println("\nDevice detected");
                System.out.println("Name: " + info.getName());
                System.out.println("Address: " + info.getAddress());
                System.out.println("RSSI: " + info.getSignalLevel());
            }

            @Override
            public void onScanError(Throwable e)
            
            {
                e.printStackTrace();                
            }            
        });
    }
}
