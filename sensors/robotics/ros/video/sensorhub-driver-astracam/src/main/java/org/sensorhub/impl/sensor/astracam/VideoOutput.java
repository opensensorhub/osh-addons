/*
 * The contents of this file are subject to the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one
 * at http://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the License.
 *
 * Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 *
 */
package org.sensorhub.impl.sensor.astracam;

import net.opengis.swe.v20.DataBlock;
import org.jboss.netty.buffer.ChannelBuffer;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.ros.config.RosMasterConfig;
import org.sensorhub.impl.ros.nodes.pubsub.RosSubscriberNode;
import org.sensorhub.impl.ros.output.RosVideoOutput;
import org.sensorhub.impl.ros.utils.RosUtils;
import org.sensorhub.impl.sensor.videocam.VideoCamHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataBlockMixed;
import sensor_msgs.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

/**
 * Output specification and provider for image/video output
 *
 * @author Nick Garay
 * @since Feb. 2, 2023
 */
public class VideoOutput extends RosVideoOutput<AstraCamSensor> {

    private static final String SENSOR_OUTPUT_NAME = "Image";

    private static final String NODE_NAME_STR = "/SensorHub/image";

    private static final String TOPIC_STR = "/camera/rgb/image_raw";

    private static final Logger logger = LoggerFactory.getLogger(VideoOutput.class);

    private static final int BYTES_PER_PIXEL = 3;

    private static final String STR_JPG_FORMAT_SPECIFIER = "jpg";

    private NodeMainExecutor nodeMainExecutor;

    private RosSubscriberNode subscriberNode;

    /**
     * Constructor
     *
     * @param parentAstraCamSensor Sensor driver providing this output
     */
    VideoOutput(AstraCamSensor parentAstraCamSensor) {

        super(SENSOR_OUTPUT_NAME, parentAstraCamSensor, logger);

        logger.debug("Output created");
    }

    @Override
    protected void defineRecordStructure() {

        // Get an instance of SWE Factory suitable to build components
        VideoCamHelper sweFactory = new VideoCamHelper();

        dataStream = sweFactory.newVideoOutputMJPEG("image", 640, 480);

        dataEncoding = dataStream.getEncoding();

        nodeMainExecutor = DefaultNodeMainExecutor.newDefault();

        subscriberNode = new RosSubscriberNode(NODE_NAME_STR, TOPIC_STR, Image._TYPE, this);
    }

    @Override
    public void onNewMessage(Object object) {

        updateSamplingPeriodHistogram();

        Image image = (Image) object;

        ChannelBuffer channelBuffer = image.getData();
        byte[] imageData = channelBuffer.array();

        BufferedImage bufferedImage = new BufferedImage(640, 480, BufferedImage.TYPE_3BYTE_BGR);

        byte[] channelData = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();

        for (short y = 0; y < 480; ++y) {

            for (short x = 0; x < 640; ++x) {

                int offset = BYTES_PER_PIXEL * (x + y * 640);

                // Kinect reports in BGRA
                byte r = imageData[offset + 2];
                byte g = imageData[offset + 1];
                byte b = imageData[offset];

                // Transpose as RGB
                channelData[offset] = b;
                channelData[offset + 1] = g;
                channelData[offset + 2] = r;
            }
        }

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        BufferedOutputStream bos = new BufferedOutputStream(byteStream);

        try {

            ImageIO.write(bufferedImage, STR_JPG_FORMAT_SPECIFIER, bos);

            byteStream.flush();

            byte[] newImage = byteStream.toByteArray();

            DataBlock dataBlock;

            if (latestRecord == null) {

                dataBlock = dataStream.getElementType().createDataBlock();

            } else {

                dataBlock = latestRecord.renew();
            }

            // Populate data block
            dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000.0);
            ((DataBlockMixed) dataBlock).getUnderlyingObject()[1].setUnderlyingObject(newImage);

            latestRecord = dataBlock;

            latestRecordTime = System.currentTimeMillis();

            eventHandler.publish(new DataEvent(latestRecordTime, VideoOutput.this, dataBlock));

        } catch (IOException e) {

            logger.error(e.getMessage());
        }
    }

    @Override
    public void doInit() {

        logger.debug("Initializing Output");

        defineRecordStructure();

        initSamplingTime();

        logger.debug("Initializing Output Complete");
    }

    @Override
    public void doStart() {

        RosMasterConfig config = parentSensor.getConfiguration().rosMaster;

        NodeConfiguration nodeConfiguration = RosUtils.getNodeConfiguration(
                config.localHostIp, NODE_NAME_STR, URI.create(config.uri));

        nodeMainExecutor.execute(subscriberNode, nodeConfiguration);
    }

    @Override
    public void doStop() {

        nodeMainExecutor.shutdownNodeMain(subscriberNode);

        nodeMainExecutor.shutdown();
    }

    @Override
    public boolean isAlive() {

        return !(nodeMainExecutor == null || nodeMainExecutor.getScheduledExecutorService().isShutdown());
    }
}
