/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.krakensdr.outputs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.opengis.swe.v20.*;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.krakensdr.KrakenSdrDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataArrayImpl;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;


/**
 * Output specification and provider for KrakenSDR spectrum data.
 *
 * <p>Each published record contains the timestamp, the active antenna channel,
 * and parallel frequency/amplitude arrays.  Channels where every amplitude
 * value equals -200 dBFS are treated as inactive and are silently skipped;
 * the output is only published when at least one channel carries real data.
 */
public class KrakenSdrOutputSpectrum extends AbstractSensorOutput<KrakenSdrDriver> {

    static final String SENSOR_OUTPUT_NAME = "kraken_spectrum";
    static final String SENSOR_OUTPUT_LABEL = "Spectrum";
    static final String SENSOR_OUTPUT_DESCRIPTION = "RF spectrum data from the KrakenSDR — one frame per active channel";

    /** Amplitude value used by KrakenSDR to signal that a channel has no data. */
    private static final float INACTIVE_THRESHOLD = -199.9f;

    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private int setCount = 0;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();
    private long lastSetTimeMillis = System.currentTimeMillis();

    private static final Logger logger = LoggerFactory.getLogger(KrakenSdrOutputSpectrum.class);

    private DataRecord dataStruct;
    private DataEncoding dataEncoding;

    /** Kept so both arrays can be resized together before each {@link #setData} call. */
    private DataArray freqAxisArray;
    private DataArray amplitudeArray;

    /**
     * Creates a new spectrum output for the sensor driver.
     *
     * @param parentSensor Sensor driver providing this output.
     */
    public KrakenSdrOutputSpectrum(KrakenSdrDriver parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    /**
     * Initializes the SWE data structure.
     *
     * <p>Schema layout (flat DataBlock indices):
     * <pre>
     *   [0]         time           – sampling timestamp (ISO UTC)
     *   [1]         channel        – active channel label, e.g. "ch0"
     *   [2]         freq_count     – N, number of points in frequency_axis
     *   [3..3+N-1]  frequency_axis – center frequency of each point in Hz
     *   [3+N]       amp_count      – N (always equals freq_count)
     *   [3+N+1..]   amplitude      – signal amplitude at each frequency in dBFS
     * </pre>
     */
    public void doInit() {
        logger.info("Initializing KrakenSDR Spectrum Output");

        SWEHelper sweFactory = new SWEHelper();

        // Each variable-size DataArray requires a sibling Count field whose ID
        // it references.  freq_count and amp_count will always be equal in
        // practice, but OSH requires one Count per array.
        String freqCountId = "KRAKEN_FREQ_COUNT";
        String ampCountId  = "KRAKEN_AMP_COUNT";

        dataStruct = sweFactory.createRecord()
                .name(SENSOR_OUTPUT_NAME)
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .definition(SWEHelper.getPropertyUri("spectrumOutput"))
                .addField("time", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("KrakenSDR Collection Time")
                        .description("Timestamp of when the spectrum frame was generated")
                        .definition(SWEConstants.DEF_SAMPLING_TIME))
                .addField("channel", sweFactory.createText()
                        .label("Active Channel")
                        .description("Antenna channel carrying valid spectrum data (e.g. ch0)")
                        .definition(SWEHelper.getPropertyUri("ChannelIdentifier")))
                .addField("freq_count", sweFactory.createCount()
                        .id(freqCountId)
                        .label("Frequency Axis Size")
                        .description("Number of points in the frequency_axis array")
                        .definition(SWEHelper.getPropertyUri("NumberOfFrequencyPoints")))
                .addField("frequency_axis", sweFactory.createArray()
                        .withVariableSize(freqCountId)
                        .label("Frequency Axis")
                        .description("Center frequency of each sample point in Hz")
                        .definition(SWEConstants.QUDT_URI_PREFIX + "Frequency")
                        .withElement("frequency", sweFactory.createQuantity()
                                .uomCode("Hz")
                                .label("Frequency")
                                .dataType(DataType.FLOAT)
                                .definition(SWEConstants.QUDT_URI_PREFIX + "Frequency")
                                .build()))
                .addField("amp_count", sweFactory.createCount()
                        .id(ampCountId)
                        .label("Amplitude Array Size")
                        .description("Number of points in the amplitude array (equals freq_count)")
                        .definition(SWEHelper.getPropertyUri("NumberOfAmplitudePoints")))
                .addField("amplitude", sweFactory.createArray()
                        .withVariableSize(ampCountId)
                        .label("Amplitude")
                        .description("Signal amplitude at each frequency point in dBFS")
                        .definition(SWEHelper.getPropertyUri("SignalAmplitude"))
                        .withElement("amplitude", sweFactory.createQuantity()
                                .uomCode("dB")
                                .label("Amplitude")
                                .dataType(DataType.FLOAT)
                                .definition(SWEHelper.getPropertyUri("SignalAmplitude"))
                                .build()))
                .build();

        freqAxisArray  = (DataArray) dataStruct.getComponent("frequency_axis");
        amplitudeArray = (DataArray) dataStruct.getComponent("amplitude");

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

    /**
     * Called by {@link KrakenSdrDriver} when a WebSocket {@code "spectrum"} message arrives.
     *
     * <p>WS field → SWE output field mapping:
     * <ul>
     *   <li>{@code timestamp}                      → time</li>
     *   <li>first channel with any value > -200    → channel</li>
     *   <li>{@code freq_axis[i]}                   → frequency_axis[i] (Hz)</li>
     *   <li>{@code channels.<ch>[i]}               → amplitude[i] (dBFS)</li>
     * </ul>
     *
     * <p>If all channels are inactive (every sample == -200 dBFS) the method
     * returns without publishing a record.
     *
     * @param specMsg the parsed JSON object from the WebSocket frame
     */
    public void setData(JsonObject specMsg) {
        try {
            JsonArray freqAxis = specMsg.getAsJsonArray("freq_axis");
            JsonObject channels = specMsg.getAsJsonObject("channels");
            int N = freqAxis.size();

            // Find the first channel whose amplitudes are not all -200 dBFS.
            String activeChannel = null;
            JsonArray activeAmplitudes = null;
            for (String chKey : new String[]{"ch0", "ch1", "ch2", "ch3"}) {
                if (!channels.has(chKey)) continue;
                JsonArray chData = channels.getAsJsonArray(chKey);
                for (int i = 0; i < chData.size(); i++) {
                    if (chData.get(i).getAsFloat() > INACTIVE_THRESHOLD) {
                        activeChannel = chKey;
                        activeAmplitudes = chData;
                        break;
                    }
                }
                if (activeChannel != null) break;
            }

            // Skip frames with no active channel.
            if (activeChannel == null) return;

            // Resize both variable-size arrays to match this frame's point count.
            ((DataArrayImpl) freqAxisArray).updateSize(N);
            ((DataArrayImpl) amplitudeArray).updateSize(N);

            DataBlock dataBlock = dataStruct.createDataBlock();

            synchronized (histogramLock) {
                int setIndex = setCount % MAX_NUM_TIMING_SAMPLES;
                timingHistogram[setIndex] = System.currentTimeMillis() - lastSetTimeMillis;
                lastSetTimeMillis = timingHistogram[setIndex];
            }
            ++setCount;

            Instant tsInstant = Instant.ofEpochMilli(specMsg.get("timestamp").getAsLong());
            OffsetDateTime odt = tsInstant.atOffset(ZoneOffset.UTC);

            // Flat DataBlock layout:
            //   [0]           time
            //   [1]           channel
            //   [2]           freq_count  N
            //   [3..3+N-1]    frequency_axis[0..N-1]
            //   [3+N]         amp_count   N
            //   [3+N+1..3+2N] amplitude[0..N-1]
            dataBlock.setDateTime(0, odt);
            dataBlock.setStringValue(1, activeChannel);
            dataBlock.setIntValue(2, N);
            for (int i = 0; i < N; i++) {
                dataBlock.setFloatValue(3 + i, freqAxis.get(i).getAsFloat());
            }
            dataBlock.setIntValue(3 + N, N);
            for (int i = 0; i < N; i++) {
                dataBlock.setFloatValue(3 + N + 1 + i, activeAmplitudes.get(i).getAsFloat());
            }

            latestRecord = dataBlock;
            latestRecordTime = System.currentTimeMillis();

            eventHandler.publish(new DataEvent(latestRecordTime, KrakenSdrOutputSpectrum.this, dataBlock));

        } catch (Exception ex) {
            parentSensor.getLogger().error("Failed to set spectrum data", ex);
        }
    }
}
