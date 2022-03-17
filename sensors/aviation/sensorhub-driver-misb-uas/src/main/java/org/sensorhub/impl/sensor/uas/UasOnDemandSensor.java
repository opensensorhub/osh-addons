/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.uas;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.event.EventUtils;
import org.sensorhub.api.event.IEventBus;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.uas.common.ITimeSynchronizedUasDataProducer;
import org.sensorhub.impl.sensor.uas.common.SyncTime;
import org.sensorhub.impl.sensor.uas.config.Outputs;
import org.sensorhub.impl.sensor.uas.config.UasOnDemandConfig;
import org.sensorhub.impl.sensor.uas.klv.SetDecoder;
import org.sensorhub.impl.sensor.uas.outputs.AirframeAttitude;
import org.sensorhub.impl.sensor.uas.outputs.FullTelemetry;
import org.sensorhub.impl.sensor.uas.outputs.GeoRefImageFrame;
import org.sensorhub.impl.sensor.uas.outputs.GimbalAttitude;
import org.sensorhub.impl.sensor.uas.outputs.Identification;
import org.sensorhub.impl.sensor.uas.outputs.Security;
import org.sensorhub.impl.sensor.uas.outputs.SensorLocation;
import org.sensorhub.impl.sensor.uas.outputs.SensorParams;
import org.sensorhub.impl.sensor.uas.outputs.UasOutput;
import org.sensorhub.impl.sensor.uas.outputs.Video;
import org.sensorhub.impl.sensor.uas.outputs.VmtiOutput;
import org.sensorhub.misb.stanag4609.comm.MpegTsProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.ogc.om.MovingFeature;
import org.vast.swe.SWEConstants;
import org.vast.util.Asserts;

/**
 * A sensor that has almost the same functionality as {@link UasSensor}, except that it does not actually start up the
 * stream processor to connect to the upstream source until after it detects that a client is actually subscribed for
 * data from it.
 * <p>
 * As currently written, this class duplicates a large amount of code in the {@link UasSensor} class, but just done in
 * a different order. Some thought needs to be given to factoring out shared functionality into a common superclass.
 * <p>
 * Since it does not connect to the stream right away, the sensor cannot know video frame information (pixel
 * size and codec) at initialization time. So the user has to provide that info (in configuration).
 */
public class UasOnDemandSensor extends AbstractSensorModule<UasOnDemandConfig> implements ITimeSynchronizedUasDataProducer {
	/** Debug logger */
    private static final Logger logger = LoggerFactory.getLogger(UasOnDemandSensor.class);

    /**
     * Thing that knows how to parse the data out of the bytes from the video stream.
     */
    private MpegTsProcessor mpegTsProcessor;
    
    /**
     * Background thread manager. Used for image decoding. At the moment, this is a single thread.
     */
    private ScheduledExecutorService executor;
    
    /**
     * Knows how to decode the STANAG 4609 tags from the MPEG data. The various outputs listen to events emitted by this
     * object.
     */
    private SetDecoder setDecoder;

    /**
     * Sensor output for the video frames.
     */
    private Video videoOutput;

    /**
     * Keeps track of the times in the data stream so that we can put an accurate phenomenon time in the data blocks.
     */
    private SyncTime syncTime;

    /**
     * Lock to prevent simultaneous access to syncTime.
     */
    private final Object syncTimeLock = new Object();

    /**
     * All of the various outputs of this sensor. The list's length varies based on the {@link Outputs} configuration.
     */
    private List<UasOutput> uasOutputs = new ArrayList<>();

    /**
     * Feature for the UAS itself.
     */
    private MovingFeature uasFoi;

    /**
     * Feature for the target (if available).
     */
    private MovingFeature imagedFoi;
    
    /**
     * The only way we can know whether someone is subscribed to our sensor data is to periodically ask the sensor hub's
     * event bus whether there are subscribers for each output's topic. This list here contains those topic names.
     * <p>
     * Hopefully this is a temporary workaround, and eventually we will be able to receive proper events from the
     * sensor hub rather than manually polling for the topic subscriber counts.
     */
    private List<String> outputTopicIds = new ArrayList<>();
    
    /**
     * Initialize outputs and FOIs, as configured.
     */
    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();

        // Generate identifiers
        generateUniqueID("urn:osh:sensor:uas:", config.serialNumber);
        generateXmlID("MISB_UAS_", config.serialNumber);

        // Clear the list of data stream topics
        // NOTE: Hopefully this is a temporary workaround. See the javadoc for outputTopicIds above.
        outputTopicIds.clear();

        if (config.outputs.enableVideo) {
        	int[] videoDims = new int[] { config.video.videoFrameWidth, config.video.videoFrameHeight };
        	videoOutput = new Video(this, videoDims);
            addOutput(videoOutput, false);
            videoOutput.init();
            // Add the video output's topic ID to the list we're watching for subscribers.
            // NOTE: Hopefully this is a temporary workaround. See the javadoc for outputTopicIds above.
            outputTopicIds.add(EventUtils.getDataStreamDataTopicID(videoOutput));
        }

        setDecoder = new SetDecoder();
        // Instantiate configured outputs
        createConfiguredOutputs();

        // For each configured output
        for (UasOutput output: uasOutputs) {
            // Initialize the output
            output.init();

            // Register it as a listener to the set decoder
            setDecoder.addListener(output);

            // Add it as an output to the driver
            addOutput(output, false);
            
            // Add this output's topic ID to the list we're watching for subscribers.
            // NOTE: Hopefully this is a temporary workaround. See the javadoc for outputTopicIds above.
            outputTopicIds.add(EventUtils.getDataStreamDataTopicID(output));
        }
        
        // create features of interest
        uasFoi = new MovingFeature();
        uasFoi.setUniqueIdentifier(this.uniqueID);
        uasFoi.setName("UAS " + config.serialNumber + " Imaging Sensor");
        uasFoi.setDescription("Imaging sensor on-board platform " + config.serialNumber);
        addFoi(uasFoi);
        
        imagedFoi = new MovingFeature();
        imagedFoi.setUniqueIdentifier("urn:osh:foi:imagedArea:" + config.serialNumber);
        imagedFoi.setName("UAS " + config.serialNumber + " Imaged Area");
        imagedFoi.setDescription("Area viewed by imaging sensor on-board platform " + config.serialNumber);
        addFoi(imagedFoi);
    }

    private void createConfiguredOutputs() {
        if (config.outputs.enableIdentification) {
            uasOutputs.add(new Identification(this));
        }
        
        if (config.outputs.enableAirframePosition) {
            uasOutputs.add(new SensorLocation(this));
            uasOutputs.add(new AirframeAttitude(this));
        }

        if (config.outputs.enableGimbalAttitude) {
            uasOutputs.add(new GimbalAttitude(this));
        }

        if (config.outputs.enableSensorParams) {
            uasOutputs.add(new SensorParams(this));
        }

        if (config.outputs.enableGeoRefImageFrame) {
            uasOutputs.add(new GeoRefImageFrame(this));
        }

        if (config.outputs.enableSecurity) {
            uasOutputs.add(new Security(this));
        }

        if (config.outputs.enableTargetIndicators) {
            uasOutputs.add(new VmtiOutput(this));
        }

        if (config.outputs.enableFullTelemetry) {
            uasOutputs.add(new FullTelemetry(this));
        }
    }
    
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
            super.updateSensorDescription();
            sensorDescription.setDefinition(SWEConstants.DEF_SYSTEM);
        }
    }

    /**
     * Start up this sensor. In our case, nothing really happens right away. Instead we wait until there is a consumer
     * that is subscribed for data that we emit.
     */
    @Override
    protected void doStart() throws SensorHubException {
        super.doStart();

        executor = Executors.newSingleThreadScheduledExecutor();
        
        // For now we just have to periodically manually check to see if anyone is subscribed.
        executor.scheduleWithFixedDelay(() -> checkForSubscriptions(),
        		500, 500, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Check to see if anyone is subscribed to data we provide. Hopefully this is a temporary workaround, until we get
     * the ability to register for new subscriptions.
     */
    private void checkForSubscriptions() {
    	logger.trace("Checking for subscriptions for {}", getUniqueIdentifier());
        IEventBus eventBus = getParentHub().getEventBus();
        boolean haveSubscribers = false;
        for (String topicId : outputTopicIds) {
        	if (eventBus.getNumberOfSubscribers(topicId) > 0) {
        		haveSubscribers = true;
        		break;
        	}
        }
        if (haveSubscribers) {
        	logger.trace("Subscribers detected.");
        	// Someone needs the data. Is the stream going?
        	if (mpegTsProcessor == null) {
        		try {
        			startStream();
        		}
        		catch (Exception e) {
        			logger.error("Failed to start stream", e);
        			executor.execute(() -> {
        				try {
        					stop();
        				} catch (Exception f) {
        					logger.error("Unable to stop", f);
        				}
        			});
        		}
        	}
        } else {
        	logger.trace("No subscribers detected.");
        	if (mpegTsProcessor != null) {
        		try {
        			stopStream();
        		} catch (Exception e) {
        			logger.error("Failed to stop stream", e);
        		}
        	}
        }
    }
    
    private void startStream() throws SensorHubException {
    	logger.info("Starting MPEG TS processor for {} ...", getUniqueIdentifier());
        // Initialize the MPEG transport stream processor from the source named in the configuration.
        // If neither the file source nor a connection string is specified, throw an exception so the user knows that
        // they have to provide at least one of them.
        if ((null != config.connection.transportStreamPath) && (!config.connection.transportStreamPath.isBlank())) {
            Asserts.checkArgument(config.connection.fps >= 0, "FPS must be >= 0");
            mpegTsProcessor = new MpegTsProcessor(config.connection.transportStreamPath, config.connection.fps, config.connection.loop);
        } else if ((null != config.connection.connectionString) && (!config.connection.connectionString.isBlank())) {
            mpegTsProcessor = new MpegTsProcessor(config.connection.connectionString);
        } else {
        	throw new SensorHubException("Either the input file path or the connection string must be set");
        }
        
        if (mpegTsProcessor.openStream()) {
        	logger.info("Stream opened for {}", getUniqueIdentifier());
        	mpegTsProcessor.queryEmbeddedStreams();
            // If there is a video content in the stream
            if (config.outputs.enableVideo && mpegTsProcessor.hasVideoStream()) {
                // Set video stream packet listener to video output
                mpegTsProcessor.setVideoDataBufferListener(videoOutput);
            }
        } else {
        	throw new SensorHubException("Unable to open stream from data source");
        }

        // Set data packet listener to the set decoder
        mpegTsProcessor.setMetaDataDataBufferListener(setDecoder);
        
        if (null != setDecoder) {
            setDecoder.setExecutor(executor);
        }

        if (null != videoOutput) {
            videoOutput.setExecutor(executor);
        }

        try {
            mpegTsProcessor.processStream();

        } catch (IllegalStateException e) {
            String message = "Failed to start stream processor";
            logger.error(message);
            throw new SensorHubException(message, e);
        }
        
    	logger.info("MPEG TS processor for {} started.", getUniqueIdentifier());
    }
    
    protected void stopStream() throws SensorHubException {
    	logger.info("Stopping MPEG TS processor for {}", getUniqueIdentifier());

    	if (null != mpegTsProcessor) {
            mpegTsProcessor.stopProcessingStream();

            try {
                // Wait for thread to finish
                mpegTsProcessor.join();
            } catch (InterruptedException e) {
                logger.error("Interrupted waiting for stream processor to stop", e);
                throw new SensorHubException("Interrupted waiting for stream processor to stop", e);
            } finally {
                // Close stream and cleanup resources
                mpegTsProcessor.closeStream();
                mpegTsProcessor = null;
            }
        }
    }

    @Override
    protected void doStop() throws SensorHubException {
        super.doStop();
        
        stopStream();

        if (null != executor) {
            executor.shutdownNow();
        }
    }

    @Override
    public boolean isConnected() {
        return isStarted();
    }

    /**
     * Sets a synchronization time element used to synchronize telemetry with video stream data
     *
     * @param syncTime the most recent synchronization data from telemetry stream
     */
    public void setStreamSyncTime(SyncTime syncTime) {
        synchronized (syncTimeLock) {
            this.syncTime = syncTime;
        }
    }

    /**
     * Returns the latest {@link SyncTime} record or null if not yet received from {@link FullTelemetry}
     *
     * @return latest sync time or null
     */
    public SyncTime getSyncTime() {
        SyncTime currentSyncTime;

        synchronized (syncTimeLock) {
            currentSyncTime = syncTime;
        }

        return currentSyncTime;
    }
    
    public String getUasFoiUID()
    {
        return uasFoi.getUniqueIdentifier();
    }
    
    public String getImagedFoiUID()
    {
        return imagedFoi.getUniqueIdentifier();
    }
}
