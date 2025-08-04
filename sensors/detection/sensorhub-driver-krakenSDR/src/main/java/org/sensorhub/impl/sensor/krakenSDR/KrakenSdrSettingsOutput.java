/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.krakenSDR;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.opengis.swe.v20.*;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEBuilders;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;


/**
 * Output specification and provider for {@link KrakenSdrSensor}.
 */
public class KrakenSdrSettingsOutput extends AbstractSensorOutput<KrakenSdrSensor> {
    static final String SENSOR_OUTPUT_NAME = "kraken_settings";
    static final String SENSOR_OUTPUT_LABEL = "Current Applicable Settings";
    static final String SENSOR_OUTPUT_DESCRIPTION = "This is the output for the krakenSDR's current settings";

    // myNote:
    // Added Variables because there were in other templates
    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private int setCount = 0;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();
    private long lastSetTimeMillis = System.currentTimeMillis();

    private static final Logger logger = LoggerFactory.getLogger(KrakenSdrSettingsOutput.class);

    private DataRecord dataStruct;
    private DataEncoding dataEncoding;

    /**
     * Creates a new output for the sensor driver.
     *
     * @param parentSensor Sensor driver providing this output.
     */
    public KrakenSdrSettingsOutput(KrakenSdrSensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering, and data types.
     */
    public void doInit() {
        logger.info("Initializing krakenSDR-Output");
        // Get an instance of SWE Factory suitable to build components
        SWEHelper sweFactory = new SWEHelper();
        GeoPosHelper geoFac = new GeoPosHelper();

        // Create the data record description
        SWEBuilders.DataRecordBuilder recordBuilder = sweFactory.createRecord()
                .name(SENSOR_OUTPUT_NAME)
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .definition(SWEHelper.getPropertyUri("settingsOutput"))
                .addField("time", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("OSH Collection Time")
                        .description("Timestamp of when OSH took a reading of the current settings")
                        .definition(SWEHelper.getPropertyUri("time")))
                .addField("en_remote_control", sweFactory.createBoolean()
                        .label("Remote Control Enabled")
                        .description("If 'false', the control for this node will not work. Adjust settings of KrakenSDR DSP application")
                        .definition(SWEHelper.getPropertyUri("en_remote_control")))
                .addField("center_freq", sweFactory.createQuantity()
                        .uomCode("MHz")
                        .label("Center Frequency")
                        .description("The transmission frequency of the event in MegaHertz")
                        .definition(SWEHelper.getPropertyUri("frequency")))
                .addField("uniform_gain", sweFactory.createQuantity()
                        .uomCode("dB")
                        .label("Receiver Gain")
                        .description("Current reciever gain settings in dB for the KrakenSDR")
                        .definition(SWEHelper.getPropertyUri("uniform_gain")))
                .addField("ant_spacing_meters", sweFactory.createQuantity()
                        .label("Antenna Array Radius")
                        .description("Current spacing of the Antenna Array")
                        .definition(SWEHelper.getPropertyUri("ant_spacing_meters")))
                ;

        dataStruct = recordBuilder.build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");
    }

    @Override
    public DataComponent getRecordDescription() {
        return dataStruct;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {
        long accumulator = 0;
        synchronized (histogramLock) {
            for (int idx = 0; idx < MAX_NUM_TIMING_SAMPLES; ++idx) {
                accumulator += timingHistogram[idx];
            }
        }
        return accumulator / (double) MAX_NUM_TIMING_SAMPLES;
    }

    public void SetData() {
               DataBlock dataBlock;
        try {
            if (latestRecord == null) {
                dataBlock = dataStruct.createDataBlock();
            } else {
                dataBlock = latestRecord.renew();
            }
            synchronized (histogramLock) {
                int setIndex = setCount % MAX_NUM_TIMING_SAMPLES;
                // Get a sampling time for latest set based on previous set sampling time
                timingHistogram[setIndex] = System.currentTimeMillis() - lastSetTimeMillis;
                // Set latest sampling time to now
                lastSetTimeMillis = timingHistogram[setIndex];
            }
            ++setCount;

            // RETRIEVE CURRENT JSON SETTINGS AS A JSON OBJECT
            JsonObject currentSettings = retrieveJSONObj(parentSensor.OUTPUT_URL + "/settings.json");

            dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000d);                                  // time
            dataBlock.setBooleanValue(1, currentSettings.get("en_remote_control").getAsBoolean());
            dataBlock.setDoubleValue(2, currentSettings.get("center_freq").getAsDouble());
            dataBlock.setDoubleValue(3, currentSettings.get("uniform_gain").getAsDouble());
            dataBlock.setDoubleValue(4, currentSettings.get("ant_spacing_meters").getAsDouble());

            latestRecord = dataBlock;
            latestRecordTime = System.currentTimeMillis();

            eventHandler.publish(new DataEvent(latestRecordTime, KrakenSdrSettingsOutput.this, dataBlock));

        } catch (Exception e) {
            System.err.println("Error reading from krakenSDR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public JsonObject retrieveJSONObj(String httpURL ){
        try {
            URL url = new URL(httpURL);

            System.out.println(url);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // Read the JSON response into a String
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder jsonString = new StringBuilder();
            String line;

            while ((line = in.readLine()) != null) {
                jsonString.append(line);
            }

            in.close();
            conn.disconnect();

            // Parse JSON string into a JSON object
            return JsonParser.parseString(jsonString.toString()).getAsJsonObject();

        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (ProtocolException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

}
