/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.ffmpeg.outputs;

import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensor;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;
import org.vast.data.DataBlockList;
import org.vast.data.DataBlockMixed;
import org.vast.swe.SWEHelper;
import net.opengis.swe.v20.DataBlock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.data.DataEvent;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class VmtiTest {

    private FFMPEGSensor driver = null;

    private final Object syncObject = new Object();

    @Before
    public void init() throws Exception {

        URL resource = FFMPEGSensor.class.getResource("sample-stream.ts");
        FFMPEGConfig config = new FFMPEGConfig();

        assert resource != null;
        config.connection.transportStreamPath = new File(resource.toURI()).getPath();
        config.outputs.enableTargetIndicators = true;

        driver = new FFMPEGSensor();
        driver.setConfiguration(config);
        driver.init();
    }

    @After
    public void cleanup() throws Exception {

        driver.stop();
    }

    @Test
    public void testVmti() throws Exception {

        driver.start();

        // register listener on data interface
        IStreamingDataInterface di = driver.getObservationOutputs().values().iterator().next();
        var dataWriter = SWEHelper.createDataWriter(di.getRecommendedEncoding());
        dataWriter.setDataComponents(di.getRecordDescription());
        dataWriter.setOutput(System.out);

        AtomicInteger count = new AtomicInteger();
        IEventListener listener = event -> {

            assertTrue(event instanceof DataEvent);
            DataEvent newDataEvent = (DataEvent) event;

            DataBlock dataBlock = newDataEvent.getRecords()[0];
            try {
                dataWriter.write(dataBlock);
                dataWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
                fail("Error writing data");
            }
            
            try {
                assertTrue(dataBlock.getDoubleValue(0) > 0); // time stamp
                
                var frameNum = dataBlock.getIntValue(1);
                var numTargets = dataBlock.getIntValue(2);
                assertTrue(frameNum >= 0);
                assertTrue(numTargets >= 0);
                
                if (frameNum > 0 && numTargets > 0) {
                    var targetSeries = (DataBlockList) ((DataBlockMixed)dataBlock).getUnderlyingObject()[3];
                    assertEquals(numTargets, targetSeries.getListSize());
                    
                    for (int i=0; i<targetSeries.getListSize(); i++) {
                        var targetData = targetSeries.get(i);
                        
                        int targetID = targetData.getIntValue(0);
                        assertTrue(targetID > 0);
                        
                        int centroidX = targetData.getIntValue(1);
                        int centroidY = targetData.getIntValue(2);
                        assertTrue(centroidX >= 0 && centroidX < 640);
                        assertTrue(centroidY >= 0 && centroidY < 480);
                    }
                }
            } finally {
                synchronized (syncObject) {
                    if (count.incrementAndGet() > 20)
                        syncObject.notify();
                }
            }
        };

        di.registerListener(listener);

        // start capture and wait until we receive the first frame
        synchronized (syncObject) {

            syncObject.wait();
        }

        di.unregisterListener(listener);
    }
}
