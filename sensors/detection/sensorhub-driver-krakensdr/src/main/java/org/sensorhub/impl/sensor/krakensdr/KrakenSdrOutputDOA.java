/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.krakensdr;

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
public class KrakenSdrOutputDOA extends AbstractSensorOutput<KrakenSdrSensor> {
    static final String SENSOR_OUTPUT_NAME = "kraken_doa";
    static final String SENSOR_OUTPUT_LABEL = "DoA";
    static final String SENSOR_OUTPUT_DESCRIPTION = "This is the DoA output for the krakenSDR";

    // myNote:
    // Added Variables because there were in other templates
    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private int setCount = 0;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();
    private long lastSetTimeMillis = System.currentTimeMillis();

    private static final Logger logger = LoggerFactory.getLogger(KrakenSdrOutputDOA.class);

    private DataRecord dataStruct;
    private DataEncoding dataEncoding;

    /**
     * Creates a new output for the sensor driver.
     *
     * @param parentSensor Sensor driver providing this output.
     */
    public KrakenSdrOutputDOA(KrakenSdrSensor parentSensor) {
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
                .definition(SWEHelper.getPropertyUri("doaOutput"))
                .addField("time", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("KrakenSDR Collection Time")
                        .description("Timestamp of when KrakenSDR reading was generated")
                        .definition(SWEHelper.getPropertyUri("time")))
                .addField("raw_lob", sweFactory.createQuantity()
                        .uomCode("deg")
                        .label("Raw LOB")
                        .description("The LOB to the emitter in absolute (true north) value")
                        .definition(SWEHelper.getPropertyUri("lob")))
                .addField("confidence", sweFactory.createQuantity()
                        .label("Confidence")
                        .description("Confidence Value (0-99). The higher the better")
                        .definition(SWEHelper.getPropertyUri("confidence")))
                .addField("rssi", sweFactory.createQuantity()
                        .uomCode("dB")
                        .label("RSSI")
                        .description("Received Signal Strength Indicator value of the event")
                        .definition(SWEHelper.getPropertyUri("rssi")))
                .addField("frequency", sweFactory.createQuantity()
                        .uomCode("Hz")
                        .label("Frequency")
                        .dataType(DataType.LONG)
                        .description("The transmission frequency of the event in Hertz")
                        .definition(SWEHelper.getPropertyUri("frequency")))
                .addField("antenna_arrangement", sweFactory.createText()
                        .label("Antenna Arrangement")
                        .description("Antenna Array Arrangement : (\"UCA\"/\"ULA\"/\"Custom\")")
                        .definition(SWEHelper.getPropertyUri("antenna_arrangement")))
                .addField("time-delta", sweFactory.createQuantity()
                        .uomCode("ms")
                        .label("Latency")
                        .description("Latency in ms : (Time from signal arrival at antenna, to result. NOT including network latency.)")
                        .definition(SWEHelper.getPropertyUri("latency")))
                .addField("uuid", sweFactory.createText()
                        .label("UUID")
                        .description("Name of the KrakenSDR station inputted in the Station Information box in the Web GUI")
                        .definition(SWEHelper.getPropertyUri("id"))
                )
                .addField("location", geoFac.createLocationVectorLatLon().label(SWEHelper.getPropertyUri("location"))
                        .label("Location")
                        .description("Lat and Long of the Kraken's Position")
                )
                .addField("heading", sweFactory.createQuantity()
                        .label("Heading")
                        .uomCode("deg")
                        .description("heading")
                        .definition(SWEHelper.getPropertyUri("Heading")))
                .addField("used_heading", sweFactory.createText()
                        .label("Main Heading Sensor Used")
                        .description("Main Heading Sensor Used (\"GPS\"/\"Compass\")")
                        .definition(SWEHelper.getPropertyUri("Heading")))
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

            URL url = new URL(parentSensor.DOA_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // READ DoA CSV provided by KrakenSDR Software
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String doaCsv;
            String[] doaArray;

            doaCsv = in.readLine();
            if (!doaCsv.isEmpty()) {
                doaCsv = doaCsv.trim();
                // Create an array from the html csv data. If it's not the right amount of fields, go to next iteration.
                doaArray = doaCsv.split(",");
                if (doaArray.length < 13) {
                    return;
                }

                // Convert Unix Epoch time provided by GPS to OffsetDateTime
                Instant krakenTimeInstant = Instant.ofEpochMilli(Long.parseLong(doaArray[0]));
                OffsetDateTime odt = krakenTimeInstant.atOffset(ZoneOffset.UTC);

                dataBlock.setDateTime(0, odt);                                  // time
                dataBlock.setDoubleValue(1, Double.parseDouble(doaArray[1]));  // Line of Bearing (DoA)
                dataBlock.setDoubleValue(2, Double.parseDouble(doaArray[2]));  // confidence
                dataBlock.setDoubleValue(3, Double.parseDouble(doaArray[3]));  // rssi
                dataBlock.setDoubleValue(4, Double.parseDouble(doaArray[4]));  // frequency
                dataBlock.setStringValue(5, doaArray[5]);                      // antenna arrangement
                dataBlock.setDoubleValue(6, Double.parseDouble(doaArray[6]));  // time-delta / latency
                dataBlock.setStringValue(7, doaArray[7]);                      // uuid
                dataBlock.setDoubleValue(8, Double.parseDouble(doaArray[8]));  // location lat
                dataBlock.setDoubleValue(9, Double.parseDouble(doaArray[9]));  // location lon
                if (doaArray[12].equals("Compass")) {
                    dataBlock.setStringValue(10, doaArray[11]);                // compass heading
                } else {
                    dataBlock.setStringValue(10, doaArray[10]);                // gps heading
                }
                dataBlock.setStringValue(11, doaArray[12]);                    // hading used

                latestRecord = dataBlock;
                latestRecordTime = System.currentTimeMillis();

                eventHandler.publish(new DataEvent(latestRecordTime, KrakenSdrOutputDOA.this, dataBlock));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
