/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.process.utils;

import java.util.concurrent.atomic.AtomicInteger;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.processing.IProcessModule;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.processing.SMLProcessConfig;
import org.sensorhub.impl.processing.SMLProcessImpl;


public class TestArrayIterator
{
    
    protected static void runProcess(IProcessModule<?> process) throws Exception
    {
        AtomicInteger counter = new AtomicInteger();        
        
        for (IStreamingDataInterface output: process.getOutputs().values())
            output.registerListener(e -> {
                System.out.println(output.getName() + ": " + ((DataEvent)e).getRecords()[0].getDoubleValue());
                counter.incrementAndGet();
            });
        
        process.start();
        
        while (counter.get() < 85-12+1)
            Thread.sleep(100);
        
        process.stop();
        System.out.println();
    }
    
    
    protected static IProcessModule<?> createSMLProcess(String smlUrl) throws Exception
    {
        var hub = new SensorHub();
        hub.start();
        var registry = hub.getModuleRegistry();
        
        var config = new SMLProcessConfig();
        config.autoStart = false;
        config.name = "SensorML Process #1";
        config.moduleClass = SMLProcessImpl.class.getCanonicalName();
        config.sensorML = smlUrl;
        
        @SuppressWarnings("unchecked")
        IProcessModule<SMLProcessConfig> process = (IProcessModule<SMLProcessConfig>)registry.loadModule(config);
        process.init();
        
        return process;
    }
    
    
    public static void main(String[] args) throws Exception
    {
        String smlUrl = TestArrayIterator.class.getResource("test_array_looper.xml").getFile();
        IProcessModule<?> process = createSMLProcess(smlUrl);
        runProcess(process);
    }

}
