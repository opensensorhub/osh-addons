/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.angel;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.UUID;
import net.opengis.sensorml.v20.ClassifierList;
import net.opengis.sensorml.v20.Term;
import org.sensorhub.api.comm.ble.BleUtils;
import org.sensorhub.api.comm.ble.GattCallback;
import org.sensorhub.api.comm.ble.IBleNetwork;
import org.sensorhub.api.comm.ble.IGattCharacteristic;
import org.sensorhub.api.comm.ble.IGattClient;
import org.sensorhub.api.comm.ble.IGattField;
import org.sensorhub.api.comm.ble.IGattService;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.sensorML.SMLFactory;
import org.vast.swe.SWEHelper;


/**
 * <p>
 * Driver for Angel Sensor
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Mar 12, 2016
 */
public class AngelSensor extends AbstractSensorModule<AngelSensorConfig>
{
    static final Logger log = LoggerFactory.getLogger(AngelSensor.class);

    private final static int DEVICE_INFO_SERVICE = 0x180A;
    private final static int MANUFACTURER_NAME = 0x2A29;
    private final static int MODEL_NUMBER = 0x2A24;
    private final static int SERIAL_NUMBER = 0x2A25;

    private final static int BATTERY_SERVICE = 0x180F;
    private final static int BATTERY_LEVEL = 0x2A19;

    private final static int HEART_RATE_SERVICE = 0x180D;
    private final static int HEART_RATE_MEAS = 0x2A37;

    private final static int HEALTH_TEMP_SERVICE = 0x1809;
    private final static int HEALTH_TEMP_MEAS = 0x2A1C;

    //private final static String BLOOD_OXY_SERVICE = "902dcf38-ccc0-4902-b22c-70cab5ee5df2";
    //private final static String BLOOD_OXY = "b269c33f-df6b-4c32-801d-1b963190bc71";

    WeakReference<IBleNetwork<?>> bleNetRef;
    AngelSensorCallback gattCallback;
    IGattClient gattClient;
    IGattCharacteristic manufacturerName;
    IGattCharacteristic modelNumber;
    IGattCharacteristic serialNumber;
    IGattCharacteristic batteryLevel;
    IGattCharacteristic bodyLocation;
    IGattCharacteristic heartRate;
    IGattCharacteristic bodyTemp;
    IGattCharacteristic bloodOxygen;

    HealthMetricsOutput healthOutput;
    //ActivityOutput activityOutput;
    DeviceStatusOutput statusOutput;
    
    
    public AngelSensor()
    {
    }


    @Override
    public void init() throws SensorHubException
    {
        super.init();
        
        // generate IDs
        generateUniqueID("urn:osh:angelsensor:", config.btAddress);
        generateXmlID("ANGEL_SENSOR_", config.btAddress);

        // create output interfaces
        healthOutput = new HealthMetricsOutput(this);
        addOutput(healthOutput, false);
        healthOutput.init();

        /*activityOutput = new ActivityOutput(this);
        addOutput(activityOutput, false);
        activityOutput.init();*/

        statusOutput = new DeviceStatusOutput(this);
        addOutput(statusOutput, true);
        statusOutput.init();
    }


    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
            super.updateSensorDescription();

            SMLFactory smlFac = new SMLFactory();
            sensorDescription.setId("ANGEL_SENSOR");
            sensorDescription.setDescription("Angel Sensor health monitoring wrist band");

            ClassifierList classif = smlFac.newClassifierList();
            sensorDescription.getClassificationList().add(classif);
            Term term;

            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("Manufacturer"));
            term.setLabel("Manufacturer Name");
            term.setValue("Seraphim Sense Ltd.");
            classif.addClassifier(term);

            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("ModelNumber"));
            term.setLabel("Model Number");
            term.setValue("Angel Sensor M1");
            classif.addClassifier(term);

            // retrieve BLE device info
        }
    }


    @Override
    public void start() throws SensorHubException
    {
        // connect to BLE network
        if (bleNetRef == null)
        {
            ModuleRegistry reg = SensorHub.getInstance().getModuleRegistry();
            bleNetRef = (WeakReference<IBleNetwork<?>>) reg.getModuleRef(config.networkID);

            // connect to sensor
            gattCallback = new AngelSensorCallback();
            bleNetRef.get().connectGatt(config.btAddress, gattCallback);
        }
    }


    @Override
    public void stop() throws SensorHubException
    {
        if (gattClient != null)
            gattClient.close();
    }


    @Override
    public void cleanup() throws SensorHubException
    {
    }


    @Override
    public boolean isConnected()
    {
        return (gattClient != null);
    }


    protected IGattCharacteristic findCharacteristic(IGattClient gatt, int serviceID, int charID)
    {
        UUID serviceUUID = BleUtils.getUUID(serviceID);
        UUID charUUID = BleUtils.getUUID(charID);
        return findCharacteristic(gatt, serviceUUID, charUUID);
    }


    protected IGattCharacteristic findCharacteristic(IGattClient gatt, UUID serviceUUID, UUID charUUID)
    {
        for (IGattService s : gatt.getServices())
        {
            if (s.getType().equals(serviceUUID))
            {
                for (IGattCharacteristic c : s.getCharacteristics())
                {
                    if (c.getType().equals(charUUID))
                        return c;
                }
            }
        }

        return null;
    }

    /*
     * Callback for receiving all BLE events
     */
    public class AngelSensorCallback extends GattCallback
    {
        @Override
        public void onConnected(IGattClient gatt, int status)
        {
            if (status == IGattClient.GATT_SUCCESS)
            {
                AngelSensor.this.gattClient = gatt;
                notifyConnectionStatus(true, "Angel Sensor (" + config.btAddress + ")");
                gatt.discoverServices();
            }
        }


        @Override
        public void onDisconnected(IGattClient gatt, int status)
        {
            AngelSensor.this.gattClient = null;
            notifyConnectionStatus(false, "Angel Sensor (" + config.btAddress + ")");
        }


        @Override
        public void onServicesDiscovered(final IGattClient gatt, int status)
        {
            // lookup manufacturer info
            manufacturerName = findCharacteristic(gatt, DEVICE_INFO_SERVICE, MANUFACTURER_NAME);
            modelNumber = findCharacteristic(gatt, DEVICE_INFO_SERVICE, MODEL_NUMBER);
            serialNumber = findCharacteristic(gatt, DEVICE_INFO_SERVICE, SERIAL_NUMBER);
            gatt.readCharacteristic(manufacturerName);
            gatt.readCharacteristic(modelNumber);
            gatt.readCharacteristic(serialNumber);

            // subscribe to measurement properties
            batteryLevel = findCharacteristic(gatt, BATTERY_SERVICE, BATTERY_LEVEL);
            gatt.setCharacteristicNotification(batteryLevel, true);
            heartRate = findCharacteristic(gatt, HEART_RATE_SERVICE, HEART_RATE_MEAS);
            gatt.setCharacteristicNotification(heartRate, true);
            bodyTemp = findCharacteristic(gatt, HEALTH_TEMP_SERVICE, HEALTH_TEMP_MEAS);
            gatt.setCharacteristicNotification(bodyTemp, true);
            //bloodOxygen = findCharacteristic(gatt, UUID.fromString(BLOOD_OXY_SERVICE), UUID.fromString(BLOOD_OXY));
            //gatt.setCharacteristicNotification(bloodOxygen, true);
        }


        @Override
        public void onCharacteristicChanged(IGattClient gatt, IGattField characteristic)
        {
            if (characteristic == batteryLevel)
            {
                int val = characteristic.getValue().get();
                log.debug("Battery Level: {}%", val);
                statusOutput.newBatLevel(val);
            }
            
            else if (characteristic == heartRate)
            {
                ByteBuffer data = characteristic.getValue();
                log.debug("Flags: {}", Integer.toBinaryString(data.get())); // flags
                int val = data.get() & 0xFF;
                log.debug("Heart Rate: {} BPM", val);
                healthOutput.newHeartRate(val);
            }
            
            else if (characteristic == bodyTemp)
            {
                ByteBuffer data = characteristic.getValue();
                log.debug("Flags: {}", Integer.toBinaryString(data.get())); // flags
                float val = BleUtils.readHealthFloat32(data);
                log.debug("Body Temp: {} °C", val);
                healthOutput.newBodyTemp(val);
            }
            
            /*else if (characteristic == bloodOxygen)
            {
                ByteBuffer data = characteristic.getValue();
                log.debug("Blood Oxygen: {}%", BleUtils.readHealthFloat32(data));
            }*/
        }


        @Override
        public void onCharacteristicRead(IGattClient gatt, IGattField characteristic, int status)
        {
            if (characteristic == batteryLevel)
                log.debug("Characteristic Read: {}", (int) characteristic.getValue().get());
            else
                log.debug("Characteristic Read: {}", new String(characteristic.getValue().array()));
        }
    }
}