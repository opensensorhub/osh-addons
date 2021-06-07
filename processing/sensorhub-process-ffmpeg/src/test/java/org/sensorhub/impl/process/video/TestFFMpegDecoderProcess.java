/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.process.video;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Assert;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.processing.IProcessModule;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.processing.SMLProcessConfig;
import org.sensorhub.impl.processing.SMLProcessImpl;
import org.sensorhub.impl.sensor.v4l.V4LCameraConfig;
import org.sensorhub.impl.sensor.v4l.V4LCameraDriver;


public class TestFFMpegDecoderProcess
{
    static String CAM_SERIAL_NUMBER = "001";
    static final int SAMPLE_COUNT = 300;
    
    static ModuleRegistry registry;
    
    
    protected static ISensorModule<?> createSensorDataSource1() throws Exception
    {
        // create test sensor
        var sensorCfg = new V4LCameraConfig();
        sensorCfg.autoStart = false;
        sensorCfg.moduleClass = V4LCameraDriver.class.getCanonicalName();
        sensorCfg.id = "V4L_CAM";
        sensorCfg.name = "Camera1";
        sensorCfg.deviceName = "/dev/video4";
        sensorCfg.serialNumber = CAM_SERIAL_NUMBER;
        var sensor = registry.loadModule(sensorCfg);
        sensor.init();
        sensor.start();
        return (V4LCameraDriver)sensor;
    }
    
    
    protected static void runProcess(IProcessModule<?> process) throws Exception
    {
        AtomicInteger counter = new AtomicInteger();        
        
        for (IStreamingDataInterface output: process.getOutputs().values())
            output.registerListener(e -> {
                System.out.println(e.getTimeStamp() + ": " + ((DataEvent)e).getRecords()[0].getAtomCount());
                counter.incrementAndGet();
            });
        
        process.start();
                
        long t0 = System.currentTimeMillis();
        while (counter.get() < SAMPLE_COUNT)
        {
            if (System.currentTimeMillis() - t0 >= 100000L)
                Assert.fail("No data received before timeout");
            Thread.sleep(100);
        }
        
        System.out.println();
    }
    
    
    protected static IProcessModule<?> createSMLProcess(String smlUrl) throws Exception
    {
        SMLProcessConfig processCfg = new SMLProcessConfig();
        processCfg.autoStart = false;
        processCfg.name = "SensorML Process #1";
        processCfg.moduleClass = SMLProcessImpl.class.getCanonicalName();
        processCfg.sensorML = smlUrl;
        
        @SuppressWarnings("unchecked")
        IProcessModule<SMLProcessConfig> process = (IProcessModule<SMLProcessConfig>)registry.loadModule(processCfg);
        process.init();
        return process;
    }
    
    
    public static void main(String[] args) throws Exception
    {
        // init sensorhub with in-memory config
        var hub = new SensorHub();
        hub.start();
        registry = hub.getModuleRegistry();
        
        createSensorDataSource1();
        String smlUrl = TestFFMpegDecoderProcess.class.getResource("processchain-decode-ffmpeg.xml").getFile();
        IProcessModule<?> process = createSMLProcess(smlUrl);
        runProcess(process);
    }
    
        
    @After
    public void cleanup()
    {
        try
        {
            registry.shutdown(false, false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}
