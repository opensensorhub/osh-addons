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
import org.eclipse.jetty.util.ArrayUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEHelper;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Output specification and provider for {@link Sensor}.
 *
 * @author your_name
 * @since date
 */
public class SleepOutput extends AbstractSensorOutput<Sensor> implements Runnable{

    private static final String SENSOR_OUTPUT_NAME = "Sleep";
    private static final String SENSOR_OUTPUT_LABEL = "Sleep";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Daily Sleep Data";

    private static final Logger logger = LoggerFactory.getLogger(SleepOutput.class);

    private DataRecord dataStruct;
    private DataEncoding dataEncoding;

    private Boolean stopProcessing = false;
    private final Object processingLock = new Object();

    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private int setCount = 0;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();

    private Thread worker;
    private Date startTime, endTime;

    /**
     * Constructor
     *
     * @param parentSensor Sensor driver providing this output
     */
    SleepOutput(Sensor parentSensor) {

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
                .definition("urn:osh:data:oura:sleep")
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
                .addField("sleepScore", sweFactory.createQuantity()
                        .definition(SWEHelper.getCfUri("sleep_score"))
                        .label("Sleep Score"))
                .addField("restfulness", sweFactory.createQuantity()
                        .definition(SWEHelper.getCfUri("restfulness"))
                        .label("Restfulness"))
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

            String requestString = "https://api.ouraring.com/v2/usercollection/daily_sleep?start_date=";
            requestString += startTime.toInstant().atZone(zoneOffset).toLocalDate().toString();
            requestString += "&end_date=";
            requestString += endTime.toInstant().atZone(zoneOffset).toLocalDate().toString();

            System.out.println("Number of Rings Monitored: " + records.size());
            JSONObject[] sum_sleep_jsons = new JSONObject[0];
            for (List<String> record : records) {
                System.out.println(record.get(0) + " | " + record.get(1) + " | " + record.get(2)); // ID | Name | Token
                String sleep_response = makeRequest(requestString, record.get(2)); // record(2) = Token
                JSONObject[] sleep_jsons = getDataRecord(sleep_response, record);
                System.out.println("Number of Sleep Records for " + record.get(1) + " : " + sleep_jsons.length);
                ArrayList<JSONObject> list = new ArrayList<>(Arrays.asList(sum_sleep_jsons));
                list.addAll(Arrays.asList(sleep_jsons));
                JSONObject[] result = list.toArray(new JSONObject[0]);
                sum_sleep_jsons = result;
            }

            List<String> dayList = new ArrayList<>(); // Maintain a list of days to detect duplicates

            int i = 0;
            while (i < sum_sleep_jsons.length) {

                DataBlock dataBlock;
                if (latestRecord == null) {

                    dataBlock = dataStruct.createDataBlock();

                } else {

                    dataBlock = latestRecord.renew();
                }

                if (!sum_sleep_jsons[i].isNull("score") && !sum_sleep_jsons[i].getJSONObject("contributors").isNull("restfulness")) {
                    LocalDateTime date = LocalDateTime.parse(sum_sleep_jsons[i].getString("day") + "T07:00:00");
                    if(dayList.contains(sum_sleep_jsons[i].getString("day"))) {
                        date = date.plusSeconds(1); // Add 1 second if duplicate day detected among user data
                    }
                    dayList.add(sum_sleep_jsons[i].getString("day"));
                    dataBlock.setDoubleValue(0, date.toEpochSecond(zoneOffset));
                    dataBlock.setStringValue(1, sum_sleep_jsons[i].getString("oid"));
                    dataBlock.setStringValue(2, sum_sleep_jsons[i].getString("oname"));
                    dataBlock.setIntValue(3, sum_sleep_jsons[i].getInt("score"));
                    dataBlock.setIntValue(4, sum_sleep_jsons[i].getJSONObject("contributors").getInt("restfulness"));
                    latestRecord = dataBlock;
                    latestRecordTime = date.toEpochSecond(zoneOffset);
                    eventHandler.publish(new DataEvent(latestRecordTime, SleepOutput.this, dataBlock));
                } else System.out.println("Record REJECTED due to NULL value");


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
