/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.misb.stanag4609.comm;

import org.sensorhub.impl.sensor.ffmpeg.klv.UasDataLinkSet;
import org.sensorhub.misb.stanag4609.tags.Tag;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.MappedH264ES;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import org.junit.Assert;
import org.junit.Test;
import org.sensorhub.mpegts.MpegTsProcessor;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit test suite for {@link MpegTsProcessor}
 *
 * @author Nick Garay
 * @since Feb. 6, 2020
 */
public class MpegTsProcessorTest {

    //static final String TEST_MPEGTS_FILE = "/home/alex/Projects/OGC/Testbed17/Video_Data/S06.ts";
    static final String TEST_MPEGTS_FILE = "src/test/resources/org/sensorhub/impl/sensor/uas/sample-stream.ts";
    static final int SLEEP_DURATION_MS = 1000;
    
    @Test
    public void testOpenStreamFails() {

        MpegTsProcessor mpegTsProcessor = new MpegTsProcessor("");

        assertFalse(mpegTsProcessor.openStream());
    }

    @Test
    public void testOpenStreamFilePath() {

        MpegTsProcessor mpegTsProcessor = new MpegTsProcessor(TEST_MPEGTS_FILE);

        assertTrue(mpegTsProcessor.openStream());

        mpegTsProcessor.closeStream();
    }

    @Test
    public void testQueryStreams() {

        MpegTsProcessor mpegTsProcessor = new MpegTsProcessor(TEST_MPEGTS_FILE);

        assertTrue(mpegTsProcessor.openStream());

        mpegTsProcessor.queryEmbeddedStreams();

        assertTrue(mpegTsProcessor.hasDataStream());

        assertTrue(mpegTsProcessor.hasVideoStream());

        mpegTsProcessor.closeStream();
    }

    @Test
    public void testGetVideoFrameDimensions() {

        MpegTsProcessor mpegTsProcessor = new MpegTsProcessor(TEST_MPEGTS_FILE);

        mpegTsProcessor.openStream();

        mpegTsProcessor.queryEmbeddedStreams();

        if (mpegTsProcessor.hasVideoStream()) {

            int[] dimensions = mpegTsProcessor.getVideoStreamFrameDimensions();

            assertEquals(2, dimensions.length);

            assertEquals(640, dimensions[0]);

            assertEquals(480, dimensions[1]);

        } else {

            fail("Video sub-stream not present");
        }

        mpegTsProcessor.closeStream();
    }

    @Test
    public void testGetVideoAvgFrameRate() {

        MpegTsProcessor mpegTsProcessor = new MpegTsProcessor(TEST_MPEGTS_FILE);

        mpegTsProcessor.openStream();

        mpegTsProcessor.queryEmbeddedStreams();

        if (mpegTsProcessor.hasVideoStream()) {

            double rate = mpegTsProcessor.getVideoStreamAvgFrameRate();

            assertNotEquals(0.0, rate);

        } else {

            fail("Video sub-stream not present");
        }

        mpegTsProcessor.closeStream();
    }

    @Test
    public void testStreamProcessing() {

        MpegTsProcessor mpegTsProcessor = new MpegTsProcessor(TEST_MPEGTS_FILE);

        mpegTsProcessor.openStream();

        mpegTsProcessor.queryEmbeddedStreams();

        if (mpegTsProcessor.hasVideoStream()) {

            mpegTsProcessor.setVideoDataBufferListener(Assert::assertNotNull);
        }

        if (mpegTsProcessor.hasDataStream()) {

            mpegTsProcessor.setMetaDataDataBufferListener(Assert::assertNotNull);
        }

        mpegTsProcessor.processStream();

        try {

            Thread.sleep(SLEEP_DURATION_MS);

        } catch (Exception e) {

            System.out.println(e.toString());

        } finally {

            mpegTsProcessor.stopProcessingStream();

            try {

                mpegTsProcessor.join();

            } catch (InterruptedException e) {

                e.printStackTrace();
            }

            mpegTsProcessor.closeStream();
        }
    }

    @Test
    public void testStreamProcessingDecodeVideo() {

        final JFrame window = new JFrame();
        window.setSize(640, 480);
        window.setVisible(true);
        window.setResizable(false);

        MpegTsProcessor mpegTsProcessor =
                new MpegTsProcessor(TEST_MPEGTS_FILE, 30, true);

        mpegTsProcessor.openStream();

        mpegTsProcessor.queryEmbeddedStreams();

        if (mpegTsProcessor.hasVideoStream()) {

            mpegTsProcessor.setVideoDataBufferListener(dataBufferRecord -> {

                try {
                    MappedH264ES es = new MappedH264ES(NIOUtils.from(ByteBuffer.wrap(dataBufferRecord.getDataBuffer()), 0));
                    Picture out = Picture.create(640, 480, ColorSpace.YUV420);
                    H264Decoder decoder = new H264Decoder();
                    Packet packet;

                    while(null != (packet = es.nextFrame()))  {

                            ByteBuffer data = packet.getData();
                            Picture res = decoder.decodeFrame(data, out.getData());
                            BufferedImage bi = AWTUtil.toBufferedImage(res);
                            window.getContentPane().getGraphics().drawImage(bi, 0, 0, null);
                    }

                } catch (Exception ignored) {

                }
            });
        }

        mpegTsProcessor.processStream();

        try {

            Thread.sleep(SLEEP_DURATION_MS*10);

        } catch (Exception e) {

            System.out.println(e.toString());

        } finally {

            mpegTsProcessor.stopProcessingStream();

            try {

                mpegTsProcessor.join();

            } catch (InterruptedException e) {

                e.printStackTrace();
            }

            mpegTsProcessor.closeStream();

            window.dispose();
        }
    }

    @Test
    public void testStreamProcessingMISB() {

        MpegTsProcessor mpegTsProcessor =
                new MpegTsProcessor(TEST_MPEGTS_FILE);

        mpegTsProcessor.openStream();

        mpegTsProcessor.queryEmbeddedStreams();

        if (mpegTsProcessor.hasDataStream()) {

            mpegTsProcessor.setMetaDataDataBufferListener(dataBufferRecord -> {

                UasDataLinkSet uasDataLinkSet = new UasDataLinkSet(dataBufferRecord.getDataBuffer().length, dataBufferRecord.getDataBuffer());


                List<String> acceptedDesignators = new ArrayList<>();
                acceptedDesignators.add(UasDataLinkSet.UAS_LOCAL_SET.getDesignator());

                if (uasDataLinkSet.validateChecksum() && uasDataLinkSet.validateDesignator(acceptedDesignators)) {

                    HashMap<Tag, Object> valuesMap = uasDataLinkSet.decode();

                    assertFalse(valuesMap.isEmpty());

                    for (Map.Entry<Tag, Object> entry : valuesMap.entrySet()) {

                        assertNotNull(entry.getValue());
                    }
                }
            });
        }

        mpegTsProcessor.processStream();

        try {

            Thread.sleep(SLEEP_DURATION_MS);

        } catch (Exception e) {

            System.out.println(e.toString());

        } finally {

            mpegTsProcessor.stopProcessingStream();

            try {

                mpegTsProcessor.join();

            } catch (InterruptedException e) {

                e.printStackTrace();
            }

            mpegTsProcessor.closeStream();
        }
    }
}
