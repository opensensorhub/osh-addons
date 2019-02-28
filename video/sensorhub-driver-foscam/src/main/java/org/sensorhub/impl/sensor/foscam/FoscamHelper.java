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

package org.sensorhub.impl.sensor.foscam;

import java.util.Collection;
import org.sensorhub.impl.sensor.videocam.VideoCamHelper;
import net.opengis.swe.v20.AllowedTokens;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.Text;


/**
 * <p>
 * Implementation of a helper class to support Foscam video cameras with or without 
 * Pan-Tilt-Zoom (PTZ) control
 * </p>
 *
 * @author Lee Butler <labutler10@gmail.com>
 * @since September 2016
 */
public class FoscamHelper extends VideoCamHelper
{
    // PTZ tasking commands
    public static final String TASKING_PTZPRESET = "preset";
    public static final String TASKING_PTZREL = "relMove";


    public DataChoice getPtzTaskParameters(String name, Collection<String> relMoveNames, Collection<String> presetNames)
    {
        DataChoice commandData = this.newDataChoice();
        commandData.setName(name);
        
        // PTZ Preset Positions
        Text preset = newText();
        preset.setDefinition(getPropertyUri("CameraPresetPositionName"));
        preset.setLabel("Preset Camera Position");
        AllowedTokens presetTokens = newAllowedTokens();
        for (String position : presetNames)
            presetTokens.addValue(position);
        preset.setConstraint(presetTokens);
        commandData.addItem(TASKING_PTZPRESET, preset);
        
        // PTZ Relative Movements
        Text relMove = newText();
        relMove.setDefinition(getPropertyUri("CameraRelativeMovementName"));
        relMove.setLabel("Camera Relative Movements");
        AllowedTokens relMoveTokens = newAllowedTokens();
        for (String position2 : relMoveNames)
        	relMoveTokens.addValue(position2);
        relMove.setConstraint(relMoveTokens);
        commandData.addItem(TASKING_PTZREL, relMove);
        
        return commandData;
    }
}
