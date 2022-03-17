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

import org.sensorhub.impl.sensor.uas.common.ITimeSynchronizedUasDataProducer;
import org.sensorhub.impl.sensor.uas.common.SyncTime;
import org.sensorhub.impl.sensor.uas.config.UasConfig;
import org.sensorhub.impl.sensor.uas.klv.SetDecoder;
import org.sensorhub.impl.sensor.uas.outputs.*;
import org.sensorhub.misb.stanag4609.comm.MpegTsProcessor;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.ogc.om.MovingFeature;
import org.vast.swe.SWEConstants;
import org.vast.util.Asserts;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sensor driver for the UAS providing sensor description, output registration,
 * initialization and shutdown of driver and outputs.
 *
 * @author Nick Garay
 * @since Feb. 6, 2020
 */
public class UasSensor extends AbstractSensorModule<UasConfig> implements ITimeSynchronizedUasDataProducer {

    private static final Logger logger = LoggerFactory.getLogger(UasSensor.class);

    MpegTsProcessor mpegTsProcessor;
    ExecutorService executor;
    SetDecoder setDecoder;
    Video videoOutput;
    SyncTime syncTime;
    final Object syncTimeLock = new Object();
    ArrayList<UasOutput> uasOutputs = new ArrayList<>();
    MovingFeature uasFoi;
    MovingFeature imagedFoi;
    

    @Override
    protected void doInit() throws SensorHubException {

        super.doInit();

        // Generate identifiers
        generateUniqueID("urn:osh:sensor:uas:", config.serialNumber);
        generateXmlID("MISB_UAS_", config.serialNumber);

        this.executor = Executors.newSingleThreadExecutor();
        boolean streamOpened = false;

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

        // Attempt to open the stream
        if (mpegTsProcessor.openStream()) {

            streamOpened = true;

            // Query the stream for available sub-stream types
            mpegTsProcessor.queryEmbeddedStreams();

            // If there is a video content in the stream
            if (config.outputs.enableVideo && mpegTsProcessor.hasVideoStream()) {

                int[] videoFrameDimensions = mpegTsProcessor.getVideoStreamFrameDimensions();

                // Create and initialize video output
                videoOutput = new Video(this, videoFrameDimensions);

                // Set video stream packet listener to video output
                mpegTsProcessor.setVideoDataBufferListener(videoOutput);

                addOutput(videoOutput, false);

                videoOutput.init();
            }

            // If there is metadata content in the stream
            if (mpegTsProcessor.hasDataStream()) {

                // Create a new MISB-TS STANAG 4609 ST0601.16 UAS Metadata data set decoder
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
                }

                // Set data packet listener to the set decoder
                mpegTsProcessor.setMetaDataDataBufferListener(setDecoder);
            }
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
        
        //foiMap.put(this.uniqueID, new GenericFeatureImpl(new QName("Feature")));

        // If the stream failed to open throw an exception indicating problem with configuration
        if (!streamOpened) {

            String message = "Stream failed to open, verify stream configuration parameters";

            logger.error(message);

            throw new SensorHubException(message);
        }
    }

    private void createConfiguredOutputs() {

        if(config.outputs.enableIdentification) {

            uasOutputs.add(new Identification(this));
        }
        
        if(config.outputs.enableAirframePosition) {

            uasOutputs.add(new SensorLocation(this));
            uasOutputs.add(new AirframeAttitude(this));
        }

        if(config.outputs.enableGimbalAttitude) {

            uasOutputs.add(new GimbalAttitude(this));
        }

        if(config.outputs.enableSensorParams) {

            uasOutputs.add(new SensorParams(this));
        }

        if(config.outputs.enableGeoRefImageFrame) {

            uasOutputs.add(new GeoRefImageFrame(this));
        }

        if(config.outputs.enableSecurity) {

            uasOutputs.add(new Security(this));
        }

        if(config.outputs.enableTargetIndicators) {

            uasOutputs.add(new VmtiOutput(this));
        }

        if(config.outputs.enableFullTelemetry) {

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

    @Override
    protected void doStart() throws SensorHubException {

        super.doStart();

        this.executor = Executors.newSingleThreadExecutor();
        
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
    }

    @Override
    protected void doStop() throws SensorHubException {

        super.doStop();

        if (null != mpegTsProcessor) {

            mpegTsProcessor.stopProcessingStream();

            try {

                // Wait for thread to finish
                mpegTsProcessor.join();

            } catch (InterruptedException e) {

                String message = "Exception occurred waiting for MpegTsProcessor" +
                        " to terminate before closing stream:\n" + e.toString();

                logger.error(message);

                throw new SensorHubException(message);

            } finally {

                // Close stream and cleanup resources
                mpegTsProcessor.closeStream();
                
                if (null != executor) {
                    
                    executor.shutdownNow();
                }
            }
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
