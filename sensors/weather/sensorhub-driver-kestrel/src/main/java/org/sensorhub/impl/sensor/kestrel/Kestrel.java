/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2025 the Initial Developer. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.kestrel;

import org.sensorhub.api.comm.ble.GattCallback;
import org.sensorhub.api.comm.ble.IBleNetwork;
import org.sensorhub.api.comm.ble.IGattCharacteristic;
import org.sensorhub.api.comm.ble.IGattClient;
import org.sensorhub.api.comm.ble.IGattDescriptor;
import org.sensorhub.api.comm.ble.IGattField;
import org.sensorhub.api.comm.ble.IGattService;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;


/**
 *
 * @author Kalyn Stricklin
 * @since Dec 1, 2025
 */
public class Kestrel extends AbstractSensorModule<KestrelConfig> {
    static final Logger logger = LoggerFactory.getLogger(Kestrel.class.getSimpleName());
    EnvironmentalOutput environmentalOutput;
    private static final UUID ENVIRONMENTAL_SERVICE = UUID.fromString("03290000-eab4-dea1-b24e-44ec023874db");
    private static final UUID SENSOR_MEASUREMENTS_CHAR = UUID.fromString("03290310-eab4-dea1-b24e-44ec023874db");
    private static final UUID DERIVED_MEASUREMENTS_1_CHAR = UUID.fromString("03290320-eab4-dea1-b24e-44ec023874db");
    private static final UUID DERIVED_MEASUREMENTS_2_CHAR = UUID.fromString("03290330-eab4-dea1-b24e-44ec023874db");
    private static final UUID DERIVED_MEASUREMENTS_3_CHAR = UUID.fromString("03290340-eab4-dea1-b24e-44ec023874db");
    private static final UUID DERIVED_MEASUREMENTS_4_CHAR = UUID.fromString("03290350-eab4-dea1-b24e-44ec023874db");
    private static final UUID CLIENT_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private boolean btConnected;
    private IGattCharacteristic sensorMeasurements;
    private IGattCharacteristic derivedMeasurementsOne;
    private IGattCharacteristic derivedMeasurementsTwo;
    private IGattCharacteristic derivedMeasurementsThree;
    private IGattCharacteristic derivedMeasurementsFour;

    IGattClient gattClient;

    WeakReference<IBleNetwork<?>> bleNetRef;
    KestrelEnvData env = new KestrelEnvData();


    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();

        // generate IDs
        generateUniqueID("urn:osh:sensor:kestrel:", config.deviceAddress);
        generateXmlID("KESTREL_WEATHER_", config.deviceAddress);

        // create output interfaces
        addOutputs();
    }

    private void addOutputs() {
        environmentalOutput = new EnvironmentalOutput(this);
        environmentalOutput.doInit();
        addOutput(environmentalOutput, false);
    }

    @Override
    public void doStart() throws SensorHubException {

        if (bleNetRef == null) {
            var moduleRegistry = getParentHub().getModuleRegistry();
            bleNetRef = moduleRegistry.getModuleRef(config.networkID);
            if (bleNetRef != null) {
                bleNetRef.get().connectGatt(config.deviceAddress, gattCallback);
            }

        }
    }

    private GattCallback gattCallback = new GattCallback() {
        @Override
        public void onConnected(IGattClient gatt, int status) {
            if (status == IGattClient.GATT_SUCCESS) {
                gattClient = gatt;
                notifyConnectionStatus(true, "Kestrel (" + config.deviceAddress + ")");

                btConnected = true;
                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(final IGattClient gatt, int status){
            gatt.getServices().forEach(service -> {
                logger.info("Kestrel --" + "Service Type: " + service.getType());
                for (IGattCharacteristic characteristic : service.getCharacteristics()) {
                    logger.info("Characteristics: " + characteristic.getType() + " props=" + characteristic.getProperties() + " perms=" +characteristic.getPermissions());
                }
            });

            IGattService enviroService = null;
            for (IGattService service : gatt.getServices()) {
                UUID uuid = service.getType();
                if (uuid.equals(ENVIRONMENTAL_SERVICE)) {
                    enviroService = service;
                }
            }

            for (IGattCharacteristic ch : enviroService.getCharacteristics()) {
                if (ch.getType().equals(SENSOR_MEASUREMENTS_CHAR)) {
                    sensorMeasurements = ch;
                    if (sensorMeasurements != null)
                        enableNotification(gatt, sensorMeasurements);
                } else if (ch.getType().equals(DERIVED_MEASUREMENTS_1_CHAR)) {
                    derivedMeasurementsOne = ch;
                    if (derivedMeasurementsOne != null)
                        enableNotification(gatt, derivedMeasurementsOne);
                } else if (ch.getType().equals(DERIVED_MEASUREMENTS_2_CHAR)) {
                    derivedMeasurementsTwo = ch;
                    if (derivedMeasurementsTwo != null)
                        enableNotification(gatt, derivedMeasurementsTwo);
                } else if (ch.getType().equals(DERIVED_MEASUREMENTS_3_CHAR)) {
                    derivedMeasurementsThree = ch;
                    if (derivedMeasurementsThree != null)
                        enableNotification(gatt, derivedMeasurementsThree);
                } else if (ch.getType().equals(DERIVED_MEASUREMENTS_4_CHAR)) {
                    derivedMeasurementsFour = ch;
                    if (derivedMeasurementsFour != null)
                        enableNotification(gatt, derivedMeasurementsFour);
                }
            }
        }


        @Override
        public void onDisconnected(IGattClient gatt, int status)
        {
            notifyConnectionStatus(false, "Kestrel Weather Sensor (" + config.deviceAddress + ")");
        }

        @Override
        public void onCharacteristicChanged(IGattClient gatt, IGattField characteristic)
        {
            byte[] val = characteristic.getValue().array();
            handleCharacteristicBytes(characteristic.getType(), val);
        }

        public void onCharacteristicRead(IGattClient gatt, IGattField characteristic, int status)
        {
            logger.debug("Characteristic Read: {}", new String(characteristic.getValue().array()));
        }

        private void enableNotification(IGattClient gatt, IGattCharacteristic ch) {
            gatt.setCharacteristicNotification(ch, true);

            IGattDescriptor cccd = ch.getDescriptors().get(CLIENT_CONFIG);
            if (cccd != null) {
                gatt.writeDescriptor(cccd);
            }
        }
    };

    private void handleCharacteristicBytes(UUID uuid, byte[] raw) {
        if (uuid.equals(SENSOR_MEASUREMENTS_CHAR)) {
            double windSpeedRaw = (raw[0] & 0xFF) | ((raw[1] & 0xFF) << 8);
            env.windSpeed = (windSpeedRaw == (short)0xFFFF) ? 0.0 : windSpeedRaw / 1000.0;

            double dryBulbTempRaw = (((raw[2] & 0xFF) | ((raw[3] & 0xFF) << 8)));
            if (dryBulbTempRaw >= 0x8000)
                dryBulbTempRaw -= 0x10000;
            env.dryBulbTemp = (dryBulbTempRaw == (short)0x8001) ? 0.0 : dryBulbTempRaw / 100.0;

            double globeTempRaw = (raw[4] & 0xFF) | ((raw[5] & 0xFF) << 8);
            if (globeTempRaw >= 0x8000)
                globeTempRaw -= 0x10000;
            env.globeTemp = (globeTempRaw == (short)0x8001) ? 0.0 : globeTempRaw / 100.0;

            env.relativeHumidity = ((raw[6] & 0xFF) | ((raw[7] & 0xFF) << 8)) / 100.0;
            env.stationPress = ((raw[8] & 0xFF) | ((raw[9] & 0xFF) << 8)) / 10.0;
            env.magDirection = (raw[10] & 0xFF) | ((raw[11] & 0xFF) << 8);
            env.airSpeed = ((raw[12] & 0xFF) | ((raw[13] & 0xFF) << 8)) / 1000.0;
            env.markSensorMeasurementsReceived();
        } else if (uuid.equals(DERIVED_MEASUREMENTS_1_CHAR)) {
            double trueDirectionRaw =  (raw[0] & 0xFF) | ((raw[1] & 0xFF) << 8);
            env.trueDirection = (trueDirectionRaw == (short)0xFFFF) ? 0.0 : trueDirectionRaw;

            double airDensityRaw = (raw[2] & 0xFF) | ((raw[3] & 0xFF) << 8);
            env.airDensity = (airDensityRaw == (short)0xFFFF) ? 0.0 : airDensityRaw / 1000.0;

            int altitudeRaw = (raw[4] & 0xFF) | ((raw[5] & 0xFF) << 8) | ((raw[6] & 0xFF) << 16);
            altitudeRaw = (altitudeRaw << 8) >> 8;
            env.altitude = (altitudeRaw == 0x800001) ? 0.0 : altitudeRaw / 10.0;

            double pressureRaw = (raw[7] & 0xFF) | ((raw[8] & 0xFF) << 8);
            env.pressure = (pressureRaw == (short)0xFFFF) ? 0.0 : pressureRaw / 10.0;

            double crosswindRaw = (raw[9] & 0xFF) | ((raw[10] & 0xFF) << 8);
            env.crosswind = (crosswindRaw == (short)0xFFFF) ? 0.0 : crosswindRaw / 1000.0;

            int headwindRaw = (raw[11] & 0xFF) | ((raw[12] & 0xFF) << 8) | ((raw[13] & 0xFF) << 16);
            headwindRaw = (headwindRaw << 8) >> 8;
            env.headwind = (headwindRaw == 0x800001) ? 0.0 :  headwindRaw / 1000.0;

            int densityAltitudeRaw = (((raw[14] & 0xFF) | ((raw[15] & 0xFF) << 8)) | ((raw[16] & 0xFF) << 16));
            densityAltitudeRaw = (densityAltitudeRaw << 8) >> 8;
            env.densityAlt = (densityAltitudeRaw == 0x80001) ? 0.0 :  densityAltitudeRaw / 10.0;

            double relAirDensityRaw = (raw[17] & 0xFF) | ((raw[18] & 0xFF) << 8);
            env.relativeAirDensity = (relAirDensityRaw == (short)0xFFFF) ? 0.0 : relAirDensityRaw / 10.0;

            env.markDerived1Received();
        } else if (uuid.equals(DERIVED_MEASUREMENTS_2_CHAR)) {

            double dewPointRaw = (raw[0] & 0xFF) | ((raw[1] & 0xFF) << 8);
            env.dewPoint = (dewPointRaw == (short)0x8001) ? 0.0 : dewPointRaw / 100.0;

            int heatIndexRaw = (((raw[2] & 0xFF) | ((raw[3] & 0xFF) << 8)) | ((raw[4] & 0xFF) << 16));
            heatIndexRaw = (heatIndexRaw << 8) >> 8;
            env.heatIndex = (heatIndexRaw == 0x80001) ? 0.0 : heatIndexRaw / 100.0;

            double wetBulb = (raw[16] & 0xFF) | ((raw[17] & 0xFF) << 8);
            env.wetBulb = (wetBulb == (short)0x8001) ? 0.0 : wetBulb / 100.0;

            double windChillRaw = (raw[18] & 0xFF) | ((raw[19] & 0xFF) << 8);
            env.chill = (windChillRaw == (short)0x8001) ? 0.0 : windChillRaw / 100.0;

            env.markDerived2Received();
        }

        if (env.isComplete()) {
            environmentalOutput.setData(env.snapshot());
            env.reset();
        }
    }


    @Override
    public void doStop() throws SensorHubException {
        if (gattClient != null)
            gattClient.close();

        btConnected = false;
    }

    @Override
    public boolean isConnected() {
        return btConnected;
    }

}