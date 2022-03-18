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
import java.util.concurrent.TimeUnit;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.event.EventUtils;
import org.sensorhub.api.event.IEventBus;
import org.sensorhub.impl.sensor.uas.config.UasOnDemandConfig;
import org.sensorhub.impl.sensor.uas.outputs.UasOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sensor driver that can read UAS data from a MISB transport stream.
 * <p>
 * This is one of two sensors defined in this package (the other being {@link UasSensor}). This one provides the same
 * functionality as the other, except that it does not stream data from the source when there are no OpenSensorHub
 * clients subscribed for its data.
 * <p>
 * Since it does not connect to the stream right away, the sensor cannot know video frame information (pixel
 * size and codec) at initialization time. So the user has to provide that info as part of its configuration.
 * 
 * @author Nick Garay
 * @author Chris Dillard
 * @since March 17, 2022
 */
public class UasOnDemandSensor extends UasSensorBase<UasOnDemandConfig> {
	/** Debug logger */
    private static final Logger logger = LoggerFactory.getLogger(UasOnDemandSensor.class);

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

        // Clear the list of data stream topics
        // NOTE: Hopefully this is a temporary workaround. See the javadoc for outputTopicIds above.
        outputTopicIds.clear();

        if (config.outputs.enableVideo) {
        	int[] videoDims = new int[] { config.video.videoFrameWidth, config.video.videoFrameHeight };
        	createVideoOutput(videoDims);
            // Add the video output's topic ID to the list we're watching for subscribers.
            // NOTE: Hopefully this is a temporary workaround. See the javadoc for outputTopicIds above.
            outputTopicIds.add(EventUtils.getDataStreamDataTopicID(videoOutput));
        }

        // Instantiate configured outputs
        createConfiguredOutputs();

        for (UasOutput<UasOnDemandConfig> output: uasOutputs) {
            // Add this output's topic ID to the list we're watching for subscribers.
            // NOTE: Hopefully this is a temporary workaround. See the javadoc for outputTopicIds above.
            outputTopicIds.add(EventUtils.getDataStreamDataTopicID(output));
        }
    }

    /**
     * Start up this sensor. In our case, nothing really happens right away. Instead we wait until there is a consumer
     * that is subscribed for data that we emit.
     */
    @Override
    protected void doStart() throws SensorHubException {
        super.doStart();

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
}
