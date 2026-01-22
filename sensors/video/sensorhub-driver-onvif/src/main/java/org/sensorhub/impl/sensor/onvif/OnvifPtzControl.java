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
	//OnvifCameraDriver parentDriver;

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

		try {
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
						//speed = devicePtzConfig.getDefaultPTZSpeed();
						speed = null;
				}
			}
		} catch (Exception e) {
			log.warn("Could not determine pan PTZ limits.", e);
		}

		// Update the ptz presets before finishing init,
		// used for allowed values constraint.
		initPresetsMap();

        // build SWE data structure for the tasking parameters
        VideoCamHelper videoHelper = new VideoCamHelper();

		commandData = videoHelper.getPtzTaskParameters(getName(), minPan, maxPan, minTilt, maxTilt, minZoom, maxZoom, presetsMap.values());
		commandData.setUpdatable(true);

		// Replace presets with list instead of text entry.
		commandData.removeComponent(VideoCamHelper.TASKING_PTZPRESET);
		Category presetComp = helper.createCategory().addAllowedValues(mapToAllowedValues()).build();
		commandData.addItem(VideoCamHelper.TASKING_PTZPRESET, presetComp);

		var presetAdd = helper.createText().name("presetAdd").label("Add a Preset").dataType(DataType.UTF_STRING).build();
		commandData.addItem(presetAdd.getName(), presetAdd);

		var presetRemove = helper.createCategory().name("presetRemove").label("Remove a Preset").addAllowedValues(mapToAllowedValues()).build();
		commandData.addItem(presetRemove.getName(), presetRemove);

		// Remove components for commands that are not supported
		if (devicePtzConfig.getDefaultAbsolutePantTiltPositionSpace() == null) {
			// Remove absolute PTZ
			commandData.removeComponent(VideoCamHelper.TASKING_PAN);
			commandData.removeComponent(VideoCamHelper.TASKING_TILT);
			commandData.removeComponent(VideoCamHelper.TASKING_PTZ_POS);
			log.debug("Removed absolute pt");
		}
		if (devicePtzConfig.getDefaultAbsoluteZoomPositionSpace() == null) {
			commandData.removeComponent(VideoCamHelper.TASKING_ZOOM);
			// Zoom is nested in the ptz pos item
			commandData.getItem(VideoCamHelper.TASKING_PTZ_POS).removeComponent(VideoCamHelper.TASKING_ZOOM);
			log.debug("Removed absolute z");
		}
		if (devicePtzConfig.getDefaultRelativePanTiltTranslationSpace() == null
				// If absolute PTZ available, we can use simple logic to simulate relative PTZ
				&& devicePtzConfig.getDefaultAbsolutePantTiltPositionSpace() == null) {
			// Remove relative PT
			commandData.removeComponent(VideoCamHelper.TASKING_RPAN);
			commandData.removeComponent(VideoCamHelper.TASKING_RTILT);
			log.debug("Removed relative pt");
		}
		if (devicePtzConfig.getDefaultRelativeZoomTranslationSpace() == null) {
			// Remove relative zoom
			commandData.removeComponent(VideoCamHelper.TASKING_RZOOM);
			log.debug("Removed relative z");
		}

		// Added support for continuous move
		if (devicePtzConfig.getDefaultContinuousPanTiltVelocitySpace() != null) {
			//Vector speedComponent = videoHelper.createVelocityVector(null).build();
			//speedComponent.getCoordinate("vx").;
			DataRecord ptzPos = new DataRecordImpl(3);
			ptzPos.setName("ptzCont");
			//ptzPos.setDefinition(getPropertyUri("PtzPosition"));
			ptzPos.setLabel("Continuous PTZ Movement Vector");
			float panSpeed = (speed == null || speed.getPanTilt() == null) ? 1 : speed.getPanTilt().getX();
			float tiltSpeed = (speed == null || speed.getPanTilt() == null) ? 1 : speed.getPanTilt().getY();
			float zoomSpeed = (speed == null || speed.getZoom() == null) ? 1 : speed.getZoom().getX();

			// Making these from scratch to avoid allowed ranges. Speed values differ significantly from camera to camera.
			//var panComp = videoHelper.getPanComponent(-panSpeed, panSpeed);

			var panComp = helper.createQuantity().name("Pan").description("Gimbal rotation speed (usually horizontal)")
					.label("Pan").definition(getPropertyUri("Pan")).uom("1").dataType(DataType.FLOAT).build();
			panComp.setValue(0.0);
			//var tiltComp = videoHelper.getTiltComponent(-tiltSpeed, tiltSpeed);
			var tiltComp = helper.createQuantity().name("Tilt").description("Gimbal rotation speed (usually up-down)")
					.label("Tilt").definition(getPropertyUri("Tilt")).uom("1").dataType(DataType.FLOAT).build();
			tiltComp.setValue(0.0);

			//var zoomComp = videoHelper.getZoomComponent(-zoomSpeed, zoomSpeed);
			var zoomComp = helper.createQuantity().name("ZoomFactor").description("Camera specific zoom factor")
					.label("Zoom").definition(getPropertyUri("ZoomFactor")).uom("1").dataType(DataType.FLOAT).build();
			zoomComp.setValue(0.0);
			ptzPos.addComponent("cpan", panComp);
			ptzPos.addComponent("ctilt", tiltComp);
			ptzPos.addComponent("czoom", zoomComp);
			ptzPos.setUpdatable(true);
			commandData.addItem(ptzPos.getName(), ptzPos);
		}
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
			PTZVector position = null;
			try {
				PTZStatus status = ptz.getStatus(ptzProfile.getToken());
				position = status.getPosition();
			} catch (Exception e) {
				position = new PTZVector();
			}

			// Note: Some tasking is not supported for certain cameras
			// ABSOLUTE PAN
        	if (itemID.equals(VideoCamHelper.TASKING_PAN))
        	{
				parent.absolutePan(data.getFloatValue());
        	}
			// ABSOLUTE TILT
        	else if (itemID.equals(VideoCamHelper.TASKING_TILT))
        	{
				parent.absoluteTilt(data.getFloatValue());
        	}
			// ABSOLUTE ZOOM
        	else if (itemID.equals(VideoCamHelper.TASKING_ZOOM))
        	{
        		float zoom = parent.degtoGeneric(data.getFloatValue(), 2);
				Vector1D zoomVec = new Vector1D();

				zoomVec.setSpace(config.getDefaultAbsoluteZoomPositionSpace());
				zoomVec.setX(zoom);
				position.setZoom(zoomVec);

        		camera.getPtz().absoluteMove(ptzProfile.getToken(), position, speed);
        	}
			// RELATIVE PAN
        	else if (itemID.equals(VideoCamHelper.TASKING_RPAN))
        	{
				float rpan = data.getFloatValue();
				try {
					Vector2D targetPanTilt = new Vector2D();

					targetPanTilt.setX(rpan);
					targetPanTilt.setY(0.0f);
					targetPanTilt.setSpace(config.getDefaultRelativePanTiltTranslationSpace());
					position.setPanTilt(targetPanTilt);

					camera.getPtz().relativeMove(ptzProfile.getToken(), position, speed);
				} catch (Exception e) {
					if (devicePtzConfig.getDefaultAbsolutePantTiltPositionSpace() != null) {
						float pan = parent.ptzPosOutput.getLatestRecord().getFloatValue(1) + rpan;
						parent.absolutePan(pan);
					}
				}
        	}
			// RELATIVE TILT
        	else if (itemID.equals(VideoCamHelper.TASKING_RTILT))
        	{
				float rtilt = data.getFloatValue();
				try {
					Vector2D targetPanTilt = new Vector2D();

					targetPanTilt.setX(0.0f);
					targetPanTilt.setY(rtilt);
					targetPanTilt.setSpace(config.getDefaultRelativePanTiltTranslationSpace());
					position.setPanTilt(targetPanTilt);

					camera.getPtz().relativeMove(ptzProfile.getToken(), position, speed);
				} catch (Exception e) {
					if (devicePtzConfig.getDefaultAbsolutePantTiltPositionSpace() != null) {
						float tilt = parent.ptzPosOutput.getLatestRecord().getFloatValue(2) + rtilt;
						parent.absoluteTilt(tilt);
					}
				}
        	}
			// RELATIVE ZOOM
        	else if (itemID.equals(VideoCamHelper.TASKING_RZOOM))
        	{
				float rzoom = parent.degtoGeneric(data.getFloatValue(),2);
				Vector1D zoomVec = new Vector1D();

				zoomVec.setX(rzoom);
				zoomVec.setSpace(config.getDefaultRelativeZoomTranslationSpace());
				position.setZoom(zoomVec);

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

				float pan = 0;
				try { pan = parent.degtoGeneric(component.getComponent("pan").getData().getFloatValue(), 0); } catch (Exception ignored) {}

				float tilt = 0;
				try { tilt = parent.degtoGeneric(component.getComponent("tilt").getData().getFloatValue(), 1); } catch (Exception ignored) {}

				float zoom = 0;
				try { zoom = parent.degtoGeneric(component.getComponent("zoom").getData().getFloatValue(), 2); } catch (Exception ignored) {}

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
				try {
					panTiltSpeed.setX(data.getFloatValue(0) / (parent.panMax - parent.panMin));
				} catch (Exception e) {
					panTiltSpeed.setX(0.0f);
				}
				try {
					panTiltSpeed.setY(data.getFloatValue(1) / (parent.tiltMax - parent.tiltMin));
				} catch (Exception e) {
					panTiltSpeed.setY(0.0f);
				}
				panTiltSpeed.setSpace(config.getDefaultContinuousPanTiltVelocitySpace());
				speedVec.setPanTilt(panTiltSpeed);

				Vector1D zoomSpeed = new Vector1D();
				zoomSpeed.setX(0.0f);
				try {
					zoomSpeed.setX(parent.degtoGeneric(data.getFloatValue(2), 2));
				} catch (Exception e) {
					zoomSpeed.setX(0.0f);
				}
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
