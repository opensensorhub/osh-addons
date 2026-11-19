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

package com.botts.impl.driver.f20.rain;

import com.botts.impl.driver.f20.F20Driver;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.helper.GeoPosHelper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class RainOutput extends AbstractSensorOutput<F20Driver> {

    private static final String SENSOR_OUTPUT_NAME = "rainSensorOutput";
    private static final String SENSOR_OUTPUT_LABEL = "Rain Sensor Output";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Output of rain gauge sensor connected to F20";

    private static final Logger logger = LoggerFactory.getLogger(RainOutput.class);

    private DataRecord dataStruct;
    private DataEncoding dataEncoding;

    private long lastMessageTime = -1;
    private double totalInterval = 0;
    private int intervalCount = 0;

    /**
     * Constructor
     *
     * @param parentSensor Sensor driver providing this output
     */
    public RainOutput(F20Driver parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);

        logger.debug("Output created");
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering,
     * and data types.
     */
    public void doInit() {

        logger.debug("Initializing Output");

        // Get an instance of SWE Factory suitable to build components
        GeoPosHelper sweFactory = new GeoPosHelper();

        // Create data record description
        dataStruct = sweFactory.createRecord()
                .name(SENSOR_OUTPUT_NAME)
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("sampleTime", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("Time of data collection"))
                .addField("phenomenonTime", sweFactory.createTime()
                        .asPhenomenonTimeIsoUTC()
                        .label("Phenomenon Time")
                        .description("Time of reported data collection"))
                .addField("rainLevel", sweFactory.createQuantity()
                        .label("Rain level")
                        .description("Rain level from rain gauge sensor")
                        // Find rain level ontology
                        .definition("https://ontology-repo.com/RainLevel")
                        .uom("cm"))
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");

        logger.debug("Initializing Output Complete");
    }

    @Override
    public DataComponent getRecordDescription() { return dataStruct; }

    @Override
    public DataEncoding getRecommendedEncoding() { return dataEncoding; }

    /**
     * Computes average sampling period based on time between received messages
     * @return Average time in seconds between messages
     */
    @Override
    public double getAverageSamplingPeriod() {
        // Average time between messages
        return intervalCount == 0 ? 0: totalInterval / intervalCount;
    }

    /**
     * Callback to handle new MQTT messages
     * @param topic Topic id of subscription
     * @param message Latest message from topic
     */
    public void handleMessage(String topic, MqttMessage message) {
        long currentTime = System.currentTimeMillis();

        if (lastMessageTime != -1) {
            // Calculate interval between messages and update count to average sampling period
            long interval = currentTime - lastMessageTime;
            totalInterval += interval / 1000.0;
            intervalCount++;
        }

        // Update when last message was received
        lastMessageTime = currentTime;

        DataBlock dataBlock;
        if (latestRecord == null)
            dataBlock = dataStruct.createDataBlock();
        else
            dataBlock = latestRecord.renew();

        double timestamp = currentTime / 1000d;

        RainObject messageObject = parentSensor.gson.fromJson(new String(message.getPayload()), RainObject.class);

        LocalDateTime localDateTime = LocalDateTime.parse(messageObject.time);
        Instant messageTime = localDateTime.atZone(ZoneId.systemDefault()).toInstant();

        // Populate output with current time
        dataBlock.setDoubleValue(0, timestamp);
        // Populate output with time reported from sensor observation
        dataBlock.setDoubleValue(1, messageTime.toEpochMilli() / 1000d);
        // Populate output with sensor observation
        dataBlock.setDoubleValue(2, Double.parseDouble(messageObject.value.get(0)));

        latestRecord = dataBlock;
        latestRecordTime = currentTime;

        eventHandler.publish(new DataEvent(latestRecordTime, RainOutput.this, dataBlock));
    }
}
