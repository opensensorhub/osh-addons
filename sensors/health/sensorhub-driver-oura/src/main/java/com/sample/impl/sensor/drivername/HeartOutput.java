/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.sample.impl.sensor.drivername;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Output specification and provider for {@link Sensor}.
 *
 * @author your_name
 * @since date
 */
public class HeartOutput extends AbstractSensorOutput<Sensor> implements Runnable {

    private static final String SENSOR_OUTPUT_NAME = "Heart";
    private static final String SENSOR_OUTPUT_LABEL = "Heart";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Heart Data";

    private static final Logger logger = LoggerFactory.getLogger(HeartOutput.class);

    private DataRecord dataStruct;
    private DataEncoding dataEncoding;

    private Boolean stopProcessing = false;
    private final Object processingLock = new Object();

    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private int setCount = 0;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();

    private Thread worker;

    private String bearerToken;
    private Date startTime, endTime;

    /**
     * Constructor
     *
     * @param parentSensor Sensor driver providing this output
     */
    HeartOutput(Sensor parentSensor) {

        super(SENSOR_OUTPUT_NAME, parentSensor);

        logger.debug("Output created");
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering,
     * and data types.
     */
    void doInit(Date start, Date end) {

        logger.debug("Initializing Output");

        // Get an instance of SWE Factory suitable to build components
        SWEHelper sweFactory = new SWEHelper();

        // TODO: Create data record description
        dataStruct = sweFactory.createRecord()
                .name(SENSOR_OUTPUT_NAME)
                .definition("urn:osh:data:oura:heart")
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("sampleTime", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("Time of data collection"))
                .addField("ownerID", sweFactory.createText()
                        .definition(SWEHelper.getCfUri("owner_id"))
                        .label("Owner ID"))
                .addField("ownerName", sweFactory.createText()
                        .definition(SWEHelper.getCfUri("owner_name"))
                        .label("Owner Name"))
                .addField("heartRate", sweFactory.createQuantity()
                        .definition(SWEHelper.getCfUri("heart_rate"))
                        .label("Heart Rate"))
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");

        startTime = start;
        endTime = end;

        logger.debug("Initializing Output Complete");
    }

    /**
     * Begins processing data for output
     */
    public void doStart() {

        // Instantiate a new worker thread
        worker = new Thread(this, this.name);

        // TODO: Perform other startup

        logger.info("Starting worker thread: {}", worker.getName());

        // Start the worker thread
        worker.start();
    }

    /**
     * Terminates processing data for output
     */
    public void doStop() {

        synchronized (processingLock) {

            stopProcessing = true;
        }

        // TODO: Perform other shutdown procedures
    }

    /**
     * Check to validate data processing is still running
     *
     * @return true if worker thread is active, false otherwise
     */
    public boolean isAlive() {

        return worker.isAlive();
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




    @Override
    public void run() {

        boolean processSets = true;

        long lastSetTimeMillis = System.currentTimeMillis();

        try {
            // TODO: Need to use getters & setters for timeFilter?
            Config config = new Config();

            LocalDateTime now = LocalDateTime.now();
            ZoneId zone = ZoneId.of("America/Chicago");
            ZoneOffset zoneOffset = zone.getRules().getOffset(now);

            String csvFile = "Oura.csv";
            String line;
            String csvSplitBy = ",";
            List<List<String>> records = new ArrayList<>();

            try (InputStream is = getClass().getClassLoader().getResourceAsStream(csvFile); BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                while ((line = br.readLine()) != null) {
                    String[] values = line.split(csvSplitBy);
                    records.add(Arrays.asList(values));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            String requestString = "https://api.ouraring.com/v2/usercollection/heartrate?start_datetime=";
            requestString += startTime.toInstant().atZone(zoneOffset).toLocalDate().toString();
            requestString += "T00:00:00-08:00";
            requestString += "&end_datetime=";
            requestString += endTime.toInstant().atZone(zoneOffset).toLocalDate().toString();
            requestString += "T00:00:00-08:00";

            System.out.println("Number of Rings Monitored: " + records.size());
            JSONObject[] sum_heart_jsons = new JSONObject[0];

            for (List<String> record : records) {
                System.out.println(record.get(0) + " | " + record.get(1) + " | " + record.get(2)); // ID | Name | Token
                String heart_response = makeRequest(requestString, record.get(2)); // record(2) = Token
                JSONObject[] heart_jsons = getDataRecord(heart_response, record);
                System.out.println("Number of Heart Records for " + record.get(1) + " : " + heart_jsons.length);
                ArrayList<JSONObject> list = new ArrayList<>(Arrays.asList(sum_heart_jsons));
                list.addAll(Arrays.asList(heart_jsons));
                JSONObject[] result = list.toArray(new JSONObject[0]);
                sum_heart_jsons = result;
            }

            int i = 0;
            while (i < sum_heart_jsons.length) {

                DataBlock dataBlock;
                if (latestRecord == null) {

                    dataBlock = dataStruct.createDataBlock();

                } else {

                    dataBlock = latestRecord.renew();
                }

                if (!sum_heart_jsons[i].isNull("bpm")) {
                    // TODO: Populate data block
                    String raw_datetime = sum_heart_jsons[i].getString("timestamp");
                    LocalDateTime dateTime = LocalDateTime.parse(raw_datetime.substring(0, raw_datetime.lastIndexOf("+")));
                    dataBlock.setDoubleValue(0, dateTime.toEpochSecond(zoneOffset));
                    dataBlock.setStringValue(1, sum_heart_jsons[i].getString("oid"));
                    dataBlock.setStringValue(2, sum_heart_jsons[i].getString("oname"));
                    dataBlock.setIntValue(3, sum_heart_jsons[i].getInt("bpm"));
                    latestRecord = dataBlock;
                    latestRecordTime = dateTime.toEpochSecond(zoneOffset);
                    eventHandler.publish(new DataEvent(latestRecordTime, HeartOutput.this, dataBlock));
                }
                else System.out.println("Record REJECTED due to NULL value");

                synchronized (histogramLock) {

                    int setIndex = setCount % MAX_NUM_TIMING_SAMPLES;

                    // Get a sampling time for latest set based on previous set sampling time
                    timingHistogram[setIndex] = System.currentTimeMillis() - lastSetTimeMillis;

                    // Set latest sampling time to now
                    lastSetTimeMillis = timingHistogram[setIndex];
                }

                ++setCount;

                i++;
            }

        } catch (Exception e) {

            logger.error("Error in worker thread: {}", Thread.currentThread().getName(), e);

        } finally {

            // Reset the flag so that when driver is restarted loop thread continues
            // until doStop called on the output again
            stopProcessing = false;

            logger.debug("Terminating worker thread: {}", this.name);
        }
    }

    public static String makeRequest(String requestString, String cloudToken) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestString))
                .header("Authorization", "Bearer " + cloudToken)
                .GET() // GET is default
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    public static JSONObject[] getDataRecord(String response, List<String> rec) {
        JSONObject jsonResponse = new JSONObject(response);
        JSONArray temp_array = jsonResponse.getJSONArray("data");
        ArrayList<JSONObject> arrays = new ArrayList<>();
        for (int i = 0; i < temp_array.length(); i++) {
            JSONObject array = temp_array.getJSONObject(i);
            array.put("oid",rec.get(0)); // Add ID & Name to object
            array.put("oname",rec.get(1));
            arrays.add(array);
        }
        JSONObject[] jsons = new JSONObject[arrays.size()];
        arrays.toArray(jsons);
        return jsons;
    }

}
