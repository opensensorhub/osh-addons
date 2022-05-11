package org.sensorhub.impl.sensor.uas;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.uas.common.SyncTime;
import org.sensorhub.impl.sensor.uas.config.UasConfig;
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
 * Base class for two sensors that parse an MPEG TS stream.
 * 
 * This collects the common functionality between the {@link UasSensor} and {@link UasOnDemandSensor}. Nearly
 * all of the code is shared, but just called in a different order.
 *
 * @param <UasConfigType> The concrete configuration class used by the sensor. Needed by the superclass, and the two
 *   subclasses have slightly different config, so we have to parameterize it.
 */
public abstract class UasSensorBase<UasConfigType extends UasConfig> extends AbstractSensorModule<UasConfigType> {
	/** Debug logger */
    private static final Logger logger = LoggerFactory.getLogger(UasSensorBase.class);

    /**
     * Thing that knows how to parse the data out of the bytes from the video stream.
     */
    protected MpegTsProcessor mpegTsProcessor;
    
    /**
     * Background thread manager. Used for image decoding. At the moment, this is a single thread.
     */
    protected ScheduledExecutorService executor;
    
    /**
     * Knows how to decode the STANAG 4609 tags from the MPEG data. The various outputs listen to events emitted by this
     * object.
     */
    protected SetDecoder setDecoder;

    /**
     * Sensor output for the video frames.
     */
    protected Video<UasConfigType> videoOutput;

    /**
     * Keeps track of the times in the data stream so that we can put an accurate phenomenon time in the data blocks.
     */
    protected SyncTime syncTime;

    /**
     * Lock to prevent simultaneous access to syncTime.
     */
    protected final Object syncTimeLock = new Object();

    /**
     * Feature for the UAS itself.
     */
    protected MovingFeature uasFoi;

    /**
     * Feature for the target (if available).
     */
    protected MovingFeature imagedFoi;

    /**
     * Performs initialization common to both sensors.
     */
    @Override
    protected void doInit() throws SensorHubException {
    	super.doInit();
    	
        generateIds();
        createFois();

        setDecoder = new SetDecoder();
        
        // Every time we init we have to tear down the mpegTsProcessor, just in case they changed some setting that
        // might cause the video output to be different.
        if (mpegTsProcessor != null) {
        	try {
        		mpegTsProcessor.closeStream();
        	} catch (Exception e) {
        		logger.warn("Could not close MPEG TS processor", e);
        	} finally {
        		// Regardless of exceptions, go ahead and set it to null. If there were severe problems we can hope
        		// that garbage collection will take care of it eventually.
        		mpegTsProcessor = null;
        	}
        }
        // We also have to clear out the video output since its settings may have changed (based on having a new input
        // video, for example).
        videoOutput = null;

        // The non-on-demand subclass will override this method to also open up the stream to get video frame size.
    }
    
    @Override
    protected void doStart() throws SensorHubException {
    	super.doStart();
        setupExecutor();
        
        // Both subclasses will override this method to also start the streaming from the video file/URI.
    }

    @Override
    protected void doStop() throws SensorHubException {
        super.doStop();
        
        stopStream();
        shutdownExecutor();
    }

    /**
     * Creates the background thread that'll handle video decoding, if it hasn't already been done.
     * Also tells the setDecoder and videoOutput about the executor. This can be called multiple times without causing
     * problems, and that's done on purpose so that the two subclasses could potentially call it at different times in
     * their life cycle.
     */
    protected void setupExecutor() {
    	if (executor == null) {
    		logger.debug("Executor was null, so creating a new one");
	        executor = Executors.newSingleThreadScheduledExecutor();
    	} else {
    		logger.debug("Already had an exector.");
    	}
    	if (setDecoder != null) {
    		setDecoder.setExecutor(executor);
    	}
		if (videoOutput != null) {
			videoOutput.setExecutor(executor);
		}
    }

    /**
     * Cleanly shuts down the background thread and sets it to null. If it's already null, doesn't do anything. This is
     * called when the sensor is stopped to clean up the background thread (hopefully).
     */
    protected void shutdownExecutor() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    /**
     * Creates the strings that uniquely identify this sensor in OpenSensorHub.
     */
    protected void generateIds() {
        generateUniqueID("urn:osh:sensor:uas:", config.serialNumber);
        generateXmlID("MISB_UAS_", config.serialNumber);
    }

    /**
     * Create and initialize the video output. The caller has to be careful not to call this if the video output has
     * already been created and added to the sensor.
     */
    protected void createVideoOutput(int[] videoDims) {
    	videoOutput = new Video<UasConfigType>(this, videoDims);
    	if (executor != null) {
    		videoOutput.setExecutor(executor);
    	}
        addOutput(videoOutput, false);
        videoOutput.init();
    }

    /**
     * Creates outputs other than the video output. This also initializes those outputs, adds them to the sensor, and
     * adds them as listeners to the mpeg decoder.
     */
    protected void createConfiguredOutputs() {
    	List<UasOutput<UasConfigType>> additionalOutputs = new ArrayList<>();
        if (config.outputs.enableIdentification) {
            additionalOutputs.add(new Identification<UasConfigType>(this));
        }
        
        if (config.outputs.enableAirframePosition) {
            additionalOutputs.add(new SensorLocation<UasConfigType>(this));
            additionalOutputs.add(new AirframeAttitude<UasConfigType>(this));
        }

        if (config.outputs.enableGimbalAttitude) {
            additionalOutputs.add(new GimbalAttitude<UasConfigType>(this));
        }

        if (config.outputs.enableSensorParams) {
            additionalOutputs.add(new SensorParams<UasConfigType>(this));
        }

        if (config.outputs.enableGeoRefImageFrame) {
            additionalOutputs.add(new GeoRefImageFrame<UasConfigType>(this));
        }

        if (config.outputs.enableSecurity) {
            additionalOutputs.add(new Security<UasConfigType>(this));
        }

        if (config.outputs.enableTargetIndicators) {
            additionalOutputs.add(new VmtiOutput<UasConfigType>(this));
        }

        if (config.outputs.enableFullTelemetry) {
            additionalOutputs.add(new FullTelemetry<UasConfigType>(this));
        }

        // For each configured output
        for (UasOutput<UasConfigType> output: additionalOutputs) {
            // Initialize the output
            output.init();

            // Register it as a listener to the set decoder
            setDecoder.addListener(output);

            // Add it as an output to the driver
            addOutput(output, false);
        }
    }
    
    /**
     * Overridden to set the definition of the sensor to "http://www.w3.org/ns/ssn/System"
     */
    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
            super.updateSensorDescription();
            sensorDescription.setDefinition(SWEConstants.DEF_SYSTEM);
        }
    }

    /**
     * Opens the connection to the upstream source but does not (yet) start reading any more than the initial
     * metadata necessary to get the video frame size. This can be called multiple times, but will only have any
     * effect the first time it's called after doInit() since it checks for a null mpegTsProcessor. This also has the
     * side effect of creating and adding the Video output if it hasn't already happened earlier.
     */
    protected void openStream() throws SensorHubException {
    	if (mpegTsProcessor == null) {
	    	logger.info("Opening MPEG TS connection for {} ...", getUniqueIdentifier());
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
	            	// In case we were waiting until we got video data to make the video frame output, we go ahead and do
	            	// that now.
	            	if (videoOutput == null) {
	            		int[] videoDimensions = mpegTsProcessor.getVideoStreamFrameDimensions();
	            		createVideoOutput(videoDimensions);
	            	}
	                // Set video stream packet listener to video output
	                mpegTsProcessor.setVideoDataBufferListener(videoOutput);
	            }
	        } else {
	        	throw new SensorHubException("Unable to open stream from data source");
	        }
	
	        // Set data packet listener to the set decoder
	        mpegTsProcessor.setMetaDataDataBufferListener(setDecoder);
		        
	    	logger.info("MPEG TS stream for {} opened.", getUniqueIdentifier());
    	}
    }

    /**
     * This causes the frames of the video to start being processed. If it's from a network stream, that means that
     * data will start flowing across the wire. If it's from a file stream, the frame are read from disk.
     */
    protected void startStream() throws SensorHubException {
        try {
        	if (mpegTsProcessor != null) {
        		mpegTsProcessor.processStream();
        	}
        } catch (IllegalStateException e) {
            String message = "Failed to start stream processor";
            logger.error(message);
            throw new SensorHubException(message, e);
        }
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
    public boolean isConnected() {
        return (mpegTsProcessor != null) && isStarted();
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
    
    protected void createFois() {
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

    public String getUasFoiUID()
    {
        return uasFoi.getUniqueIdentifier();
    }
    
    public String getImagedFoiUID()
    {
        return imagedFoi.getUniqueIdentifier();
    }
}
