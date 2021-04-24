/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2021 Nicolas Garay
 All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.audio.ml.nlp;

import org.sensorhub.impl.sensor.audio.ml.nlp.speech.AudioTranscriptListener;
import edu.cmu.sphinx.api.SpeechResult;
import net.opengis.swe.v20.DataBlock;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEHelper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Output specification and provider for ...
 *
 * @author Nick Garay
 * @since Mar. 5, 2021
 */
public class AudioTranscriberOutput extends BaseSensorOutput<AudioTranscriberSensor> implements Runnable, AudioTranscriptListener {

    private static final String SENSOR_OUTPUT_NAME = "AudioTranscriptOutput";
    private static final String SENSOR_OUTPUT_LABEL = "Audio Transcript";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "A transcript of a given audio stream";

    private static final Logger logger = LoggerFactory.getLogger(AudioTranscriberOutput.class);

    BlockingQueue<SpeechResult> dataBufferQueue = new LinkedBlockingQueue<>();

    /**
     * Constructor
     *
     * @param parentSensor Sensor driver providing this output
     */
    AudioTranscriberOutput(AudioTranscriberSensor parentSensor) {

        super(SENSOR_OUTPUT_NAME, parentSensor);

        logger.debug("Output created");
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering,
     * and data types.
     */
    @Override
    protected void init() {

        logger.debug("Initializing");

        // Get an instance of SWE Factory suitable to build components
        SWEHelper sweFactory = new SWEHelper();

        dataStruct = sweFactory.createDataRecord()
                .name(SENSOR_OUTPUT_NAME)
                .definition(SWEHelper.getPropertyUri("TranscribedText"))
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .label(SENSOR_OUTPUT_LABEL)
                .addField("SampleTime", sweFactory.createTime().asSamplingTimeIsoUTC()
                        .build())
                .addField("TextHypothesis", sweFactory.createText()
                        .definition(SWEHelper.getPropertyUri("TextHypothesis"))
                        .description("The hypothesis or best result of audio to text transcription provided by the analysis engine")
                        .label("Text Hypothesis")
                        .build())
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");

        logger.debug("Initialized");
    }

    /**
     * Begins processing data for output
     */
    @Override
    protected void start() {

        logger.debug("Starting");

        workerThread = new Thread(this, this.name);

        doWork.set(true);

        workerThread.start();

        logger.debug("Started");
    }

    /**
     * Terminates processing data for output
     */
    @Override
    protected void stop() {

        logger.debug("Stopping");

        super.stop();

//        try {

            doWork.set(false);

//            workerThread.join();
//
//        } catch (InterruptedException e) {
//
//            logger.error("Failed to stop {} thread due to exception {}", workerThread.getName(), e.getMessage());
//        }

        logger.debug("Stopped");
    }

    /**
     * Check to validate data processing is still running
     *
     * @return true if worker thread is active, false otherwise
     */
    public boolean isAlive() {

        return workerThread.isAlive();
    }

    @Override
    public void run() {

        logger.debug("Starting worker {}", Thread.currentThread().getName());

        try {

            while (doWork.get()) {

                SpeechResult result = dataBufferQueue.take();

                DataBlock dataBlock;
                if (latestRecord == null) {

                    dataBlock = dataStruct.createDataBlock();

                } else {

                    dataBlock = latestRecord.renew();
                }

                synchronized (histogramLock) {

                    int setIndex = dataFrameCount % MAX_NUM_TIMING_SAMPLES;

                    // Get a sampling time for latest set based on previous set sampling time
                    timingHistogram[setIndex] = System.currentTimeMillis() - lastDataFrameTimeMillis;

                    // Set latest sampling time to now
                    lastDataFrameTimeMillis = timingHistogram[setIndex];
                }

                ++dataFrameCount;

                dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000.0);
                dataBlock.setStringValue(1, result.getHypothesis());

                latestRecord = dataBlock;

                latestRecordTime = System.currentTimeMillis();

                eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, AudioTranscriberOutput.this, dataBlock));
            }

        } catch (Exception e) {

            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            logger.error("Error in worker thread: {} due to exception: {}", Thread.currentThread().getName(), stringWriter.toString());

        } finally {

            logger.debug("Terminating worker {}", Thread.currentThread().getName());
        }
    }

    @Override
    public void onTranscribedAudio(SpeechResult speechResult) {

        try {

            dataBufferQueue.put(speechResult);

        } catch (InterruptedException e) {

            logger.error("Error in worker thread: {} due to exception: {}", Thread.currentThread().getName(), e.toString());
        }
    }
}
