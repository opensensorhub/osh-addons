/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.onvif;

import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;

import org.onvif.ver10.schema.FloatRange;
import org.onvif.ver10.schema.PTZPreset;
import org.onvif.ver10.schema.PTZVector;
import org.onvif.ver10.schema.Profile;
import org.onvif.ver10.schema.Vector1D;
import org.onvif.ver10.schema.Vector2D;
import org.sensorhub.api.common.CommandStatus;
import org.sensorhub.api.common.CommandStatus.StatusCode;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.sensorhub.impl.sensor.videocam.VideoCamHelper;
import org.sensorhub.impl.sensor.videocam.ptz.PtzConfig;
import org.sensorhub.impl.sensor.videocam.ptz.PtzPreset;
import org.sensorhub.impl.sensor.videocam.ptz.PtzPresetsHandler;
import org.vast.data.DataChoiceImpl;

import de.onvif.soap.OnvifDevice;

/**
 * <p>
 * Implementation of sensor interface for generic cameras using ONVIF 
 * protocol. This particular class provides control of the Pan-Tilt-Zoom
 * (PTZ) capabilities.
 * </p>
 * 
 * @author Joshua Wolfe <developer.wolfe@gmail.com>
 * @since June 13, 2017
 */

public class OnvifPtzControl extends AbstractSensorControl<OnvifCameraDriver>
{
    // define and set default values
    double minPan = -180.0;
    double maxPan = 180.0;
    double minTilt = -180.0;
    double maxTilt = 0.0;
    double minZoom = 1.0;
    double maxZoom = 9999;

    PtzPresetsHandler presetsHandler;
    Map<PtzPreset, String> presetsMap;
    URL optionsURL = null;
    
	DataChoice commandData;

    protected OnvifPtzControl(OnvifCameraDriver driver)
    {
        super(driver);
        
        FloatRange panSpaces = driver.camera.getPtz().getPanSpaces(driver.profile.getToken());
        minPan = panSpaces.getMin();
        maxPan = panSpaces.getMax();

        FloatRange tiltSpaces = driver.camera.getPtz().getPanSpaces(driver.profile.getToken());
        minTilt = tiltSpaces.getMin();
        maxTilt = tiltSpaces.getMax();

        FloatRange zoomSpaces = driver.camera.getPtz().getPanSpaces(driver.profile.getToken());
        minZoom = zoomSpaces.getMin();
        maxZoom = zoomSpaces.getMax();
        
        PtzConfig ptzConfig = new PtzConfig();
        presetsMap = new LinkedHashMap<PtzPreset, String>();
        List<PTZPreset> presets = driver.camera.getPtz().getPresets(driver.profile.getToken());
        for (PTZPreset p : presets) {
        	PtzPreset preset = new PtzPreset();
        	PTZVector ptzPos = p.getPTZPosition();

        	preset.name = p.getName();
        	Vector2D panTiltVec = ptzPos.getPanTilt();
        	preset.pan = panTiltVec.getX();
        	preset.tilt = panTiltVec.getY();
        	Vector1D zoomVec = ptzPos.getZoom();
        	preset.zoom = zoomVec.getX();
        	
        	ptzConfig.presets.add(preset);
        	presetsMap.put(preset, p.getToken());
        }
		presetsHandler = new PtzPresetsHandler(ptzConfig);
    }
    
    protected void init()
    {
        // build SWE data structure for the tasking parameters
        VideoCamHelper videoHelper = new VideoCamHelper();
        Collection<String> presetList = presetsHandler.getPresetNames();
        commandData = videoHelper.getPtzTaskParameters(getName(), minPan, maxPan, minTilt, maxTilt, minZoom, maxZoom, presetList);      
    }
    
    @Override
    public CommandStatus execCommand(DataBlock command) throws SensorException
    {
    	// associate command data to msg structure definition
        DataChoice commandMsg = (DataChoice) commandData.copy();
        commandMsg.setData(command);

        DataComponent component = ((DataChoiceImpl) commandMsg).getSelectedItem();
        String itemID = component.getName();
        DataBlock data = component.getData();

        try
        {
        	OnvifDevice camera = parentSensor.camera;
        	Profile profile = parentSensor.profile;
			PTZVector position = camera.getPtz().getPosition(profile.getToken());

        	if (itemID.equals(VideoCamHelper.TASKING_PAN))
        	{
        		float pan = data.getFloatValue();
        		float tilt = position.getPanTilt().getY();
        		float zoom = position.getZoom().getX();
        		
        		camera.getPtz().absoluteMove(profile.getToken(), pan, tilt, zoom);
        	}
        	else if (itemID.equals(VideoCamHelper.TASKING_TILT))
        	{
        		float pan = position.getPanTilt().getX();
        		float tilt = data.getFloatValue();
        		float zoom = position.getZoom().getX();

        		camera.getPtz().absoluteMove(profile.getToken(), pan, tilt, zoom);
        	}
        	else if (itemID.equals(VideoCamHelper.TASKING_ZOOM))
        	{
        		float pan = position.getPanTilt().getX();
        		float tilt = position.getPanTilt().getY();
        		float zoom = data.getFloatValue();

        		camera.getPtz().absoluteMove(profile.getToken(), pan, tilt, zoom);
        	}
        	else if (itemID.equals(VideoCamHelper.TASKING_RPAN))
        	{
        		float rpan = data.getFloatValue();
        		camera.getPtz().relativeMove(profile.getToken(), rpan, 0, 0);
        	}
        	else if (itemID.equals(VideoCamHelper.TASKING_RTILT))
        	{
        		float rtilt = data.getFloatValue();
        		camera.getPtz().relativeMove(profile.getToken(), 0, rtilt, 0);
        	}
        	else if (itemID.equals(VideoCamHelper.TASKING_RZOOM))
        	{
        		float rzoom = data.getFloatValue();
        		camera.getPtz().relativeMove(profile.getToken(), 0, 0, rzoom);
        	}
        	else if (itemID.equals(VideoCamHelper.TASKING_PTZPRESET))
        	{
        	    PtzPreset preset = presetsHandler.getPreset(data.getStringValue());

        	    camera.getPtz().gotoPreset(presetsMap.get(preset), profile.getToken());
        	}
        	else if (itemID.equalsIgnoreCase(VideoCamHelper.TASKING_PTZ_POS))
        	{
				float pan = component.getComponent("pan").getData().getFloatValue();
				float tilt = component.getComponent("tilt").getData().getFloatValue();
				float zoom = component.getComponent("zoom").getData().getFloatValue();

        		camera.getPtz().absoluteMove(profile.getToken(), pan, tilt, zoom);
        	}
	    }
	    catch (Exception e)
	    {	    	
	        throw new SensorException("Error sending PTZ command via ONVIF", e);
	    }        
       
        CommandStatus cmdStatus = new CommandStatus();
        cmdStatus.status = StatusCode.COMPLETED;        
        return cmdStatus;
    }
    
    @Override
    public String getName()
    {
        return "ptzControl";
    }
    
    @Override
    public DataComponent getCommandDescription()
    {    
        return commandData;
    }

    protected void start()
    {
    }

	public void stop()
	{
	}
}
