/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
Developer are Copyright (C) 2016 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.videocam;

import java.util.Collection;
import net.opengis.swe.v20.AllowedTokens;
import net.opengis.swe.v20.AllowedValues;
import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Text;
import net.opengis.swe.v20.Time;
import org.vast.swe.SWEHelper;


/**
 * <p>
 * Implementation of a helper class to support all video cameras with or without 
 * Pan-Tilt-Zoom (PTZ) control
 * </p>
 *
 * @author Mike Botts <mike.botts@botts-inc.com>
 * @since March 2016
 */
public class VideoCamHelper extends SWEHelper
{
    // PTZ tasking commands
    public static final String TASKING_PAN = "pan";
    public static final String TASKING_TILT = "tilt";
    public static final String TASKING_ZOOM = "zoom";
    public static final String TASKING_RPAN = "rpan";
    public static final String TASKING_RTILT = "rtilt";
    public static final String TASKING_RZOOM = "rzoom";
    public static final String TASKING_PTZPRESET = "preset";
    public static final String TASKING_PTZ_POS = "ptzPos";


    // system info	
    String cameraBrand = " ";
    String cameraModel = " ";
    String serialNumber = " ";

    //   TODO add Camera Settings   //

    // InfraRed
    //    boolean irEnabled = false;  // Can you set camera to IR mode
    //    //boolean irOn = false;		// Is camera currently in IR mode
    //    
    //    //Color Settings
    //    double minBrightness;
    //    double maxBrightness;
    //    //double brightness;
    //    
    //    double minContrast;
    //    double maxContrast;
    //    //double contrast;
    //    
    //    double minHue;
    //    double maxHue;
    //    //double hue;
    //    
    //    double minSaturation;
    //    double maxSaturation;
    //    //double saturation;
    //
    //    // Video 
    //    boolean videoEnabled = true;
    //    double videoHeight;	// Height of Video Frame (in contrast to snapshot image height)
    //    double videoWidth;	// Width of Video Frame
    //    double videoFPS;    // Frames per Second
    //    String videoCompression;  // e.g. MPEG4, MPEG2, MPEG1, MJPG, H263, H264
    //  
    
    
    public Quantity getPanComponent(double min, double max)
    {
        Quantity q = this.newQuantity(getPropertyUri("Pan"), "Pan", "Gimbal rotation (usually horizontal)", "deg", DataType.FLOAT);
        AllowedValues constraints = newAllowedValues();
        constraints.addInterval(new double[] { min, max });
        q.setConstraint(constraints);
        return q;
    }
    
    
    public Quantity getTiltComponent(double min, double max)
    {
        Quantity q = this.newQuantity(getPropertyUri("Tilt"), "Tilt", "Gimbal rotation (usually up-down)", "deg", DataType.FLOAT);
        AllowedValues constraints = newAllowedValues();
        constraints.addInterval(new double[] { min, max });
        q.setConstraint(constraints);
        return q;
    }
    
    
    public Quantity getZoomComponent(double min, double max)
    {
        Quantity q = this.newQuantity(getPropertyUri("ZoomFactor"), "Zoom Factor", "Camera specific zoom factor", "1", DataType.FLOAT);
        AllowedValues constraints = newAllowedValues();
        constraints.addInterval(new double[] { min, max });
        q.setConstraint(constraints);
        return q;
    }
    

    public DataRecord getPtzOutput(String name, double minPan, double maxPan, double minTilt, double maxTilt, double minZoom, double maxZoom)
    {
        // Build SWE Common Data structure for PTZ Output values
        // Settings output includes time, pan, tilt, zoom

        DataRecord settingsDataStruct = newDataRecord(4);
        settingsDataStruct.setName(name);

        // time needs to be in UTC !!!
        // either set camera and convert
        Time t = this.newTimeStampIsoUTC();
        settingsDataStruct.addComponent("time", t);

        // TODO: set localReferenceFrame for Z to be the pan axis into camera, 
        settingsDataStruct.addComponent("pan", getPanComponent(minPan, maxPan));
        settingsDataStruct.addComponent("tilt", getTiltComponent(minTilt, maxTilt));
        settingsDataStruct.addComponent("zoomFactor", getTiltComponent(minZoom, maxZoom));

        return settingsDataStruct;
    }


    public DataChoice getPtzTaskParameters(String name, double minPan, double maxPan, double minTilt, double maxTilt, double minZoom, double maxZoom, Collection<String> presetNames)
    {
        // NOTE: commands are individual and supported using DataChoice

        // PTZ command will consist of DataChoice with items:
        // pan, tilt, zoom, relPan, relTilt, relZoom, presetName

        DataChoice commandData = this.newDataChoice();
        commandData.setName(name);

        // Pan, Tilt, Zoom
        Quantity pan = getPanComponent(minPan, maxPan);
        commandData.addItem(TASKING_PAN, pan);
        Quantity tilt = getTiltComponent(minTilt, maxTilt);
        commandData.addItem(TASKING_TILT, tilt);
        Quantity zoom = getTiltComponent(minZoom, maxZoom);
        commandData.addItem(TASKING_ZOOM, zoom);
        
        // Relative Pan
        Quantity q = newQuantity(DataType.FLOAT);
        q.getUom().setCode("deg");
        q.setDefinition(getPropertyUri("RelativePan"));
        q.setLabel("Relative Pan");
        commandData.addItem(TASKING_RPAN, q);

        // Relative Tilt
        q = newQuantity(DataType.FLOAT);
        q.getUom().setCode("deg");
        q.setDefinition(getPropertyUri("RelativeTilt"));
        q.setLabel("Relative Tilt");
        commandData.addItem(TASKING_RTILT, q);

        // Relative Zoom
        Count c = newCount();
        c.setDefinition(getPropertyUri("RelativeZoomFactor"));
        c.setLabel("Relative Zoom Factor");
        commandData.addItem(TASKING_RZOOM, c);

        // PTZ Preset Positions
        Text preset = newText();
        preset.setDefinition(getPropertyUri("CameraPresetPositionName"));
        preset.setLabel("Preset Camera Position");
        AllowedTokens presetTokens = newAllowedTokens();
        for (String position : presetNames)
            presetTokens.addValue(position);
        preset.setConstraint(presetTokens);
        commandData.addItem(TASKING_PTZPRESET, preset);
        
        // PTZ Position (supports pan, tilt, and zoom simultaneously
        DataRecord ptzPos = newDataRecord(3);
        ptzPos.setName("ptzPosition");
        ptzPos.setDefinition(getPropertyUri("PtzPosition"));
        ptzPos.setLabel("Absolute PTZ Position");
        ptzPos.addComponent("pan", pan.copy());
        ptzPos.addComponent("tilt", tilt.copy());
        ptzPos.addComponent("zoom", zoom.copy());
        commandData.addItem(TASKING_PTZ_POS, ptzPos);       

        return commandData;

    }
}
