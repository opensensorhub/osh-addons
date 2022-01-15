/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Sensia Software LLC. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.impl.sensor.onvif;

import java.util.Map;
import java.util.UUID;

import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.event.Event;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.IStreamingControlInterface;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.onvif.OnvifCameraConfig;
import org.sensorhub.impl.sensor.onvif.OnvifCameraDriver;
import org.vast.data.DataChoiceImpl;
import org.vast.sensorML.SMLUtils;
import org.vast.swe.SWEUtils;

import static org.junit.Assert.*;

/**
 * <p>
 * This is the test class for the ONVIF driver
 * </p>
 *
 * @author Joshua Wolfe <developer.wolfe@gmail.com>
 */


public class TestOnvifCameraDriver implements IEventListener
{
	final static int MAX_FRAMES = 300;
	int frameCount;

	OnvifCameraDriver driver;
    OnvifCameraConfig config;

    @Before
    public void init() throws Exception
    {
        config = new OnvifCameraConfig();
        config.id = UUID.randomUUID().toString();
        config.hostIp = "192.168.0.28";
        config.user = "admin";
        config.password = "op3nsaysam3";
        
        driver = new OnvifCameraDriver();
        driver.setConfiguration(config);
        driver.init();
    }

    @Test
    public void testGetOutputDesc() throws Exception
    {
        for (IStreamingDataInterface di: driver.getObservationOutputs().values())
        {
            DataComponent dataMsg = di.getRecordDescription();
            new SWEUtils(SWEUtils.V2_0).writeComponent(System.out, dataMsg, false, true);
        }
    }

    @Test
    public void testGetCommandDesc() throws Exception
    {
        for (IStreamingControlInterface ci: driver.getCommandInputs().values())
        {
            DataComponent commandMsg = ci.getCommandDescription();
            new SWEUtils(SWEUtils.V2_0).writeComponent(System.out, commandMsg, false, true);
        }
    }
        
    @Test
    public void testGetSensorDesc() throws Exception
    {
        AbstractProcess smlDesc = driver.getCurrentDescription();
        new SMLUtils(SMLUtils.V2_0).writeProcess(System.out, smlDesc, true);
    }
    
    @Test
    public void testSendPTZCommand() throws Exception
    {
    	Map<String, IStreamingControlInterface> cmdInputs = driver.getCommandInputs();
    	assertTrue(cmdInputs.containsKey("ptzControl"));
    	
    	Map<String, ? extends IStreamingDataInterface> outputs = driver.getOutputs();
    	assertTrue(outputs.containsKey("ptzOutput"));
    	
        // register listeners
    	IStreamingDataInterface di = driver.getObservationOutputs().get("ptzOutput");
        di.registerListener(this);
        
        // get ptz control interface
        IStreamingControlInterface ci = driver.getCommandInputs().get("ptzControl");
        DataComponent commandDesc = ci.getCommandDescription().copy();

        // instantiate command data block and status
        DataBlock commandData;
        
        // Absolute Pan
        ((DataChoiceImpl) commandDesc).setSelectedItem("pan");
        commandData = commandDesc.createDataBlock();
        commandData.setFloatValue(1, 0.0f);
        ci.submitCommand(new CommandData(1, commandData));

        // Absolute Tilt
        ((DataChoiceImpl) commandDesc).setSelectedItem("tilt");
        commandData = commandDesc.createDataBlock();
        commandData.setFloatValue(1, 0.0f);
        ci.submitCommand(new CommandData(1, commandData));

        // Absolute Zoom
        ((DataChoiceImpl) commandDesc).setSelectedItem("zoom");
        commandData = commandDesc.createDataBlock();
        commandData.setFloatValue(1, 0.0f);
        ci.submitCommand(new CommandData(1, commandData));

        // Absolute PTZ
        ((DataChoiceImpl) commandDesc).setSelectedItem("ptzPos");
        commandData = commandDesc.createDataBlock();
        commandData.setFloatValue(1, 0.25f);
        commandData.setFloatValue(2, -0.25f);
        commandData.setFloatValue(3, 0.50f);
        ci.submitCommand(new CommandData(1, commandData));
        
        // Relative Pan
        ((DataChoiceImpl) commandDesc).setSelectedItem("rpan");
        commandData = commandDesc.createDataBlock();
        commandData.setFloatValue(1, -0.25f);
        ci.submitCommand(new CommandData(1, commandData));

        // Relative Tilt
        ((DataChoiceImpl) commandDesc).setSelectedItem("rtilt");
        commandData = commandDesc.createDataBlock();
        commandData.setFloatValue(1, -0.25f);
        ci.submitCommand(new CommandData(1, commandData));

        // Relative Zoom
        ((DataChoiceImpl) commandDesc).setSelectedItem("rzoom");
        commandData = commandDesc.createDataBlock();
        commandData.setFloatValue(1, 0.50f);
        ci.submitCommand(new CommandData(1, commandData));
    }

    @Override
    public void handleEvent(Event e)
    {
        assertTrue(e instanceof DataEvent);
        
        synchronized (this) { this.notify(); }
    }
    
    @After
    public void cleanup() throws Exception
    {
        driver.stop();
    }
}
