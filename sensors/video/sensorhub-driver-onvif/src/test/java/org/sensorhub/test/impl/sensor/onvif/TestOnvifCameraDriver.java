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

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.ws.EndpointReference;

import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;

import org.apache.cxf.ws.discovery.WSDiscoveryClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onvif.ver10.schema.FloatRange;
import org.onvif.ver10.schema.PTZConfiguration;
import org.onvif.ver10.schema.PTZVector;
import org.onvif.ver10.schema.Profile;
import org.onvif.ver10.schema.VideoSource;
import org.sensorhub.api.common.CommandStatus;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.sensor.ISensorControlInterface;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.security.ClientAuth;
import org.sensorhub.impl.sensor.onvif.OnvifCameraConfig;
import org.sensorhub.impl.sensor.onvif.OnvifCameraDriver;
import org.sensorhub.test.sensor.videocam.VideoTestHelper;
import org.vast.data.DataChoiceImpl;
import org.vast.sensorML.SMLUtils;
import org.vast.swe.SWEUtils;

import de.onvif.discovery.OnvifDiscovery;
import de.onvif.discovery.OnvifPointer;
import de.onvif.soap.OnvifDevice;
import de.onvif.soap.SOAP;
import de.onvif.soap.devices.PtzDevices;

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
        for (ISensorDataInterface di: driver.getObservationOutputs().values())
        {
            DataComponent dataMsg = di.getRecordDescription();
            new SWEUtils(SWEUtils.V2_0).writeComponent(System.out, dataMsg, false, true);
        }
    }

    @Test
    public void testGetCommandDesc() throws Exception
    {
        for (ISensorControlInterface ci: driver.getCommandInputs().values())
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
    	Map<String, ISensorControlInterface> cmdInputs = driver.getCommandInputs();
    	assertTrue(cmdInputs.containsKey("ptzControl"));
    	
    	Map<String, ISensorDataInterface> outputs = driver.getAllOutputs();
    	assertTrue(outputs.containsKey("ptzOutput"));
    	
        // register listeners
    	ISensorDataInterface di = driver.getObservationOutputs().get("ptzOutput");
        di.registerListener(this);
        
        // get ptz control interface
        ISensorControlInterface ci = driver.getCommandInputs().get("ptzControl");
        DataComponent commandDesc = ci.getCommandDescription().copy();

        // instantiate command data block and status
        DataBlock commandData;
        CommandStatus cs;

        // Absolute Pan
        ((DataChoiceImpl) commandDesc).setSelectedItem("pan");
        commandData = commandDesc.createDataBlock();
        commandData.setFloatValue(1, 0.0f);
        cs = ci.execCommand(commandData);
        assertEquals(CommandStatus.StatusCode.COMPLETED, cs.status);

        // Absolute Tilt
        ((DataChoiceImpl) commandDesc).setSelectedItem("tilt");
        commandData = commandDesc.createDataBlock();
        commandData.setFloatValue(1, 0.0f);
        cs = ci.execCommand(commandData);
        assertEquals(CommandStatus.StatusCode.COMPLETED, cs.status);

        // Absolute Zoom
        ((DataChoiceImpl) commandDesc).setSelectedItem("zoom");
        commandData = commandDesc.createDataBlock();
        commandData.setFloatValue(1, 0.0f);
        cs = ci.execCommand(commandData);
        assertEquals(CommandStatus.StatusCode.COMPLETED, cs.status);

        // Absolute PTZ
        ((DataChoiceImpl) commandDesc).setSelectedItem("ptzPos");
        commandData = commandDesc.createDataBlock();
        commandData.setFloatValue(1, 0.25f);
        commandData.setFloatValue(2, -0.25f);
        commandData.setFloatValue(3, 0.50f);
        cs = ci.execCommand(commandData);
        assertEquals(CommandStatus.StatusCode.COMPLETED, cs.status);
        
        // Relative Pan
        ((DataChoiceImpl) commandDesc).setSelectedItem("rpan");
        commandData = commandDesc.createDataBlock();
        commandData.setFloatValue(1, -0.25f);
        cs = ci.execCommand(commandData);
        assertEquals(CommandStatus.StatusCode.COMPLETED, cs.status);

        // Relative Tilt
        ((DataChoiceImpl) commandDesc).setSelectedItem("rtilt");
        commandData = commandDesc.createDataBlock();
        commandData.setFloatValue(1, -0.25f);
        cs = ci.execCommand(commandData);
        assertEquals(CommandStatus.StatusCode.COMPLETED, cs.status);

        // Relative Zoom
        ((DataChoiceImpl) commandDesc).setSelectedItem("rzoom");
        commandData = commandDesc.createDataBlock();
        commandData.setFloatValue(1, 0.50f);
        cs = ci.execCommand(commandData);
        assertEquals(CommandStatus.StatusCode.COMPLETED, cs.status);
    }

    @Override
    public void handleEvent(Event<?> e)
    {
        assertTrue(e instanceof SensorDataEvent);
        
        synchronized (this) { this.notify(); }
    }
    
    @After
    public void cleanup()
    {
        if (driver != null)
            driver.stop();
    }
}
