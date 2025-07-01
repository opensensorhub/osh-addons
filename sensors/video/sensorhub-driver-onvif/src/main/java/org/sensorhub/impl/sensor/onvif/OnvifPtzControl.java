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
import java.util.*;

import jakarta.xml.ws.Holder;
import net.opengis.swe.v20.*;

import org.onvif.ver10.schema.*;
import org.onvif.ver20.ptz.wsdl.PTZ;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.sensorhub.impl.sensor.videocam.VideoCamHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.*;

import de.onvif.soap.OnvifDevice;
import org.vast.swe.SWEHelper;

import javax.xml.datatype.DatatypeFactory;

import static org.vast.swe.SWEHelper.getPropertyUri;

/**
 * <p>
 * Implementation of sensor interface for generic cameras using ONVIF 
 * protocol. This particular class provides control of the Pan-Tilt-Zoom
 * (PTZ) capabilities.
 * </p>
 * 
 * @author Kyle Fitzpatrick, Joshua Wolfe <developer.wolfe@gmail.com>
 * @since June 13, 2017
 */

public class OnvifPtzControl extends AbstractSensorControl<OnvifCameraDriver>
{
	private static int controlCount = 0;
	private static final Logger log = LoggerFactory.getLogger(OnvifPtzControl.class);
    // define and set default values
    double minPan = -180.0;
    double maxPan = 180.0;
    double minTilt = -180.0;
    double maxTilt = 0.0;
    double minZoom = 1.0;
    double maxZoom = 9999;
	PTZSpeed speed = new PTZSpeed();
	SWEHelper helper = new SWEHelper();
    //PtzPresetsHandler presetsHandler;
	PTZ ptz = null;
	Profile ptzProfile;
	PTZConfiguration devicePtzConfig;
	OnvifDevice camera;

	// Stores presets as TOKEN, NAME pairs
    Map<String, String> presetsMap;

    URL optionsURL = null;
	//PTZConfiguration devicePtzConfig = null;
	//PtzConfig ptzConfig = new PtzConfig();
    
	DataChoice commandData = null;

    protected OnvifPtzControl(OnvifCameraDriver driver)
    {
        //super("ptzControl" + ++controlCount, driver);
		super("ptzControl", driver);
    }

	/**
	 * Inverts preset mappings and returns as a list of Strings.
	 * @return ArrayList of presets in the form "value: key" (name: token)
	 */
	protected List<String> mapToAllowedValues() {
		ArrayList<String> nameValues = new ArrayList<String>();
		// Create a list of presets in the form "value: key" (name: token)
		// Inverts mappings, returns as list of Strings
		for (String key: presetsMap.keySet()) {
			nameValues.add("Name: " + presetsMap.get(key) + ", Token: " + key);
		}
		return nameValues;
	}

	/**
	 * Initializes the ptz presets. Adds the presets to the presetMap. The presetMap is later used to set the
	 * allowed values constraint for the preset command. This should be called before using the VideoHelper to set the
	 * ptz commands. This method does not modify the command structure or constraints.
	 */
	protected void initPresetsMap() {
		// Contains token, name pairs for presets.
		// Camera itself stores position info.
		presetsMap = new LinkedHashMap<String, String>();
		List<PTZPreset> presets = parentSensor.camera.getPtz().getPresets(parentSensor.ptzProfile.getToken());
		// Iterate through the list of presets, adding the name
		for (PTZPreset preset : presets) {
			presetsMap.put(preset.getToken(), preset.getName());
		}
	}

	/**
	 * Adds a new ptz preset to the map and allowed values constraint.
	 * DO NOT CALL THIS BEFORE INITIALIZING (I think)
	 *
	 * @param presetName Name & token of the preset to add
	 */
	protected void addPreset(String presetName, String presetToken) throws CommandException {
		initPresetsMap(); // Refresh the presets map
		// Add preset to map
		//presetsMap.put(presetToken,presetName);

		// Add preset to allowed values constraint
		// TODO: No idea if this works. Feels like it should(?)

		AllowedTokens presetNames = helper.newAllowedTokens();
		for (String position : mapToAllowedValues())
			presetNames.addValue(position);

		var item = (CategoryImpl)this.commandData.getItem(VideoCamHelper.TASKING_PTZPRESET);
		item.setConstraint(presetNames);

		item = (CategoryImpl)this.commandData.getItem("presetRemove");
		item.setConstraint(presetNames);
	}

	/**
	 * Removes a preset from the map and allowed values constraint.
	 *
	 * @param presetToken
	 */
	protected void removePreset(String presetToken) throws CommandException {
		initPresetsMap();
		/*
		if (!presetsMap.containsKey(presetToken)) {
			throw new CommandException("Ptz preset token to remove is not valid.");
		}
		presetsMap.remove(presetToken);*/

		// Update preset allowed values constraint
		// TODO: No idea if this works. Feels like it should(?)
		AllowedTokens presetNames = helper.newAllowedTokens();
		for (String position : mapToAllowedValues())
			presetNames.addValue(position);

		var item = (CategoryImpl)this.commandData.getItem(VideoCamHelper.TASKING_PTZPRESET);
		item.setConstraint(presetNames);

		item = (CategoryImpl)this.commandData.getItem("presetRemove");
		item.setConstraint(presetNames);
	}

    protected void init()
    {
		camera = parentSensor.camera;
		ptz = camera.getPtz();
		ptzProfile = parentSensor.ptzProfile;
		devicePtzConfig = parentSensor.ptzProfile.getPTZConfiguration();

		if (parentSensor.ptzProfile != null) {
			if (devicePtzConfig != null) {
				PanTiltLimits panTiltLimits = devicePtzConfig.getPanTiltLimits();
				if (panTiltLimits != null) {
					minPan = panTiltLimits.getRange().getXRange().getMin();
					maxPan = panTiltLimits.getRange().getXRange().getMax();
					minTilt = panTiltLimits.getRange().getYRange().getMin();
					maxTilt = panTiltLimits.getRange().getYRange().getMax();
				}
				ZoomLimits zoomLimits = devicePtzConfig.getZoomLimits();
				if (zoomLimits != null) {
					minZoom = zoomLimits.getRange().getXRange().getMin();
					maxZoom = zoomLimits.getRange().getXRange().getMax();
				}
				if (devicePtzConfig.getDefaultPTZSpeed() != null)
					speed = devicePtzConfig.getDefaultPTZSpeed();
			}
		}

		// Update the ptz presets before finishing init,
		// used for allowed values constraint.
		initPresetsMap();

        // build SWE data structure for the tasking parameters
        VideoCamHelper videoHelper = new VideoCamHelper();

		DataChoice commandData = helper.newDataChoice();
		commandData.setName(name);

		// Pan, Tilt, Zoom
		Quantity pan = videoHelper.getPanComponent(minPan, maxPan);
		//commandData.addItem(VideoCamHelper.TASKING_PAN, pan);
		Quantity tilt = videoHelper.getTiltComponent(minTilt, maxTilt);
		//commandData.addItem(VideoCamHelper.TASKING_TILT, tilt);
		Quantity zoom = videoHelper.getZoomComponent(minZoom, maxZoom);
		//commandData.addItem(VideoCamHelper.TASKING_ZOOM, zoom);

		// Add only the commands supported by this camera
		// PTZ Position (supports pan, tilt, and zoom simultaneously
		// TODO Have unique units for each type of movement
		DataRecord ptzPos = helper.newDataRecord();
		ptzPos.setName("ptzPosition");
		ptzPos.setDefinition(getPropertyUri("PtzPosition"));
		ptzPos.setLabel("Absolute PTZ Position");
		if (devicePtzConfig.getDefaultAbsolutePantTiltPositionSpace() != null) {
			// Add absolute PTZ
			ptzPos.addComponent("pan", pan.copy());
			ptzPos.addComponent("tilt", tilt.copy());
			log.debug("Add absolute pt");
		}
		if (devicePtzConfig.getDefaultAbsoluteZoomPositionSpace() != null) {
			ptzPos.addComponent("zoom", zoom.copy());
			log.debug("Add absolute z");
		}
		if (ptzPos.getComponentCount() > 0) {
			commandData.addItem("Absolute Movement", ptzPos);
		}

		// Relative PTZ Position (simultaneous ptz pos)
		DataRecord ptzRelPos = helper.newDataRecord();
		ptzRelPos.setName("ptzRelPosition");
		ptzRelPos.setDefinition(getPropertyUri("PtzRelPosition")); // This probably doesn't resolve to anything
		ptzRelPos.setLabel("Relative PTZ Position");
		if (devicePtzConfig.getDefaultRelativePanTiltTranslationSpace() != null) {
			// Add relative PT
			ptzRelPos.addComponent("pan", pan.copy());
			ptzRelPos.addComponent("tilt", tilt.copy());
			log.debug("Add relative pt");
		}
		if (devicePtzConfig.getDefaultRelativeZoomTranslationSpace() != null) {
			// Add relative zoom
			ptzRelPos.addComponent("zoom", zoom.copy());
			log.debug("Add relative z");
		}
		if (ptzRelPos.getComponentCount() > 0) {
			commandData.addItem("Relative Movement", ptzRelPos);
		}

		// Continuous PTZ Position (simultaneous ptz pos)
		DataRecord ptzContPos = helper.newDataRecord();
		ptzContPos.setName("ptzContinuousPosition");
		ptzContPos.setDefinition(getPropertyUri("PtzContPosition")); // This probably doesn't resolve to anything
		ptzContPos.setLabel("Continuous PTZ Position");
		if (devicePtzConfig.getDefaultContinuousPanTiltVelocitySpace() != null) {
			ptzContPos.addComponent("pan", pan.copy());
			ptzContPos.addComponent("tilt", tilt.copy());
		}
		if (devicePtzConfig.getDefaultContinuousZoomVelocitySpace() != null) {
			ptzContPos.addComponent("zoom", zoom.copy());
		}
		if (ptzContPos.getComponentCount() > 0) {
			commandData.addItem("Continuous Movement", ptzPos);
		}

		//commandData = videoHelper.getPtzTaskParameters(getName(), minPan, maxPan, minTilt, maxTilt, minZoom, maxZoom, presetsMap.values());
		//commandData.setUpdatable(true);

		// Replace presets with list instead of text entry.
		// PTZ Presets
		Category presetComp = helper.createCategory().name("Preset").addAllowedValues(mapToAllowedValues()).build();
		commandData.addItem(presetComp.getName(), presetComp);

		var presetAdd = helper.createText().name("Add Preset").label("Add a Preset").dataType(DataType.UTF_STRING).build();
		commandData.addItem(presetAdd.getName(), presetAdd);

		var presetRemove = helper.createCategory().name("Remove Preset").label("Remove a Preset").addAllowedValues(mapToAllowedValues()).build();
		commandData.addItem(presetRemove.getName(), presetRemove);

	}

    @Override
    protected boolean execCommand(DataBlock command) throws CommandException
    {
    	// associate command data to msg structure definition
        DataChoice commandMsg = (DataChoice) commandData.copy();

		if (command == null)
			return false;
        commandMsg.setData(command);

        DataComponent component = ((DataChoiceImpl) commandMsg).getSelectedItem();
        String itemID = component.getName();
		DataBlock data = component.getData();

        try
        {

			PTZConfiguration config = ptzProfile.getPTZConfiguration();
			PTZStatus status = ptz.getStatus(ptzProfile.getToken());
			PTZVector position = status.getPosition();

			// Note: Some tasking is not supported for certain cameras

			// RELATIVE MOVEMENT
        	if (itemID.equals("Relative Movement"))
        	{
				Float pan;
				try {
					 pan = component.getComponent("pan").getData().getFloatValue();
				} catch (Exception e) {
					pan = Float.NaN;
				}
				Float tilt;
				try {
					tilt = component.getComponent("tilt").getData().getFloatValue();
				} catch (Exception e) {
					tilt = Float.NaN;
				}
				Float zoom;
				try {
					zoom = component.getComponent("zoom").getData().getFloatValue();
				} catch (Exception e) {
					zoom = Float.NaN;
				}

				if (!pan.isNaN() && !tilt.isNaN()) {
					Vector2D targetPanTilt = new Vector2D();
					targetPanTilt.setX(pan);
					targetPanTilt.setY(tilt);
					targetPanTilt.setSpace(config.getDefaultRelativePanTiltTranslationSpace());
					position.setPanTilt(targetPanTilt);
				}

				if (!zoom.isNaN()) {
					Vector1D zoomVec = new Vector1D();
					zoomVec.setX(zoom);
					zoomVec.setSpace(config.getDefaultRelativeZoomTranslationSpace());
					position.setZoom(zoomVec);
				}

				camera.getPtz().relativeMove(ptzProfile.getToken(), position, speed);
        	}

			// GO TO PRESET
        	else if (itemID.equals(VideoCamHelper.TASKING_PTZPRESET))
        	{
				String preset = data.getStringValue(); // This is in the form "Name: {name}, Token: {token}"
				String presetToken = preset.substring(preset.lastIndexOf("Token:") + "Token: ".length());
				if (presetsMap.containsKey(presetToken)) {
					ptz.gotoPreset(ptzProfile.getToken(), presetToken, speed);
				} else {// This should never happen. May indicate issue when parsing selected string.
					throw new CommandException("Could not find this preset to execute.");
				}
        	}

			// ADD PRESET
			else if (itemID.equalsIgnoreCase("presetAdd")) {
				String presetName = data.getStringValue();
				//var ptzStatus = camera.getPtz().getStatus(profile.getToken());

				if (presetName == null || presetName.contains("Token: ") || presetsMap.containsValue(presetName))
					return false;

				// Update preset
				Holder<String> tokenHolder = new Holder<>(presetName);
				ptz.setPreset(ptzProfile.getToken(), presetName, tokenHolder);
				addPreset(presetName, tokenHolder.value);

			}
			// REMOVE PRESET
			else if (itemID.equalsIgnoreCase("presetRemove")) {
				String preset = data.getStringValue(); // This is in the form "Name: {name}, Token: {token}"
				String presetToken = preset.substring(preset.lastIndexOf("Token:") + "Token: ".length());

				if (presetsMap.containsKey(presetToken)) {
					removePreset(presetToken);
					ptz.removePreset(ptzProfile.getToken(), presetToken);
				}
			}
			// ABSOLUTE PTZ
        	else if (itemID.equalsIgnoreCase(VideoCamHelper.TASKING_PTZ_POS))
        	{
				float pan = component.getComponent("pan").getData().getFloatValue();
				float tilt = component.getComponent("tilt").getData().getFloatValue();
				float zoom = component.getComponent("zoom").getData().getFloatValue();

				Vector2D targetPanTilt = new Vector2D();
				targetPanTilt.setX(pan);
				targetPanTilt.setY(tilt);
				targetPanTilt.setSpace(config.getDefaultAbsolutePantTiltPositionSpace());
				position.setPanTilt(targetPanTilt);

				Vector1D zoomVec = new Vector1D();
				zoomVec.setX(zoom);
				zoomVec.setSpace(config.getDefaultAbsoluteZoomPositionSpace());
				position.setZoom(zoomVec);

        		camera.getPtz().absoluteMove(ptzProfile.getToken(), position, speed);
        	}
			// CONTINUOUS PTZ
			else if (itemID.equalsIgnoreCase("ptzCont"))
			{
				PTZSpeed speedVec = new PTZSpeed();
				Vector2D panTiltSpeed = new Vector2D();
				panTiltSpeed.setX(0.0f);
				panTiltSpeed.setY(0.0f);
				panTiltSpeed.setX(data.getFloatValue(0));
				panTiltSpeed.setY(data.getFloatValue(1));
				panTiltSpeed.setSpace(config.getDefaultContinuousPanTiltVelocitySpace());
				speedVec.setPanTilt(panTiltSpeed);

				Vector1D zoomSpeed = new Vector1D();
				zoomSpeed.setX(0.0f);
				zoomSpeed.setX(data.getFloatValue(2));
				zoomSpeed.setSpace(config.getDefaultContinuousZoomVelocitySpace());
				speedVec.setZoom(zoomSpeed);
				// Note: Duration does not seem to work (at least on camera used for testing).
				camera.getPtz().continuousMove(ptzProfile.getToken(), speedVec, DatatypeFactory.newInstance().newDuration(500));
			}
	    }
	    catch (Exception e)
	    {	    	
	        throw new CommandException("Error sending PTZ command via ONVIF", e);
	    }
        return true;
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
