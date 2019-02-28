/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.mavlink;

import java.io.IOException;
import java.util.EnumSet;
import net.opengis.swe.v20.AllowedValues;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Vector;
import org.sensorhub.api.common.CommandStatus;
import org.sensorhub.api.common.CommandStatus.StatusCode;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.mavlink.MavlinkConfig.CmdTypes;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;
import com.MAVLink.common.msg_command_long;
import com.MAVLink.enums.MAV_CMD;


/**
 * <p>
 * Implementation of camera control interface for MAVLink systems
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Jul 5, 2016
 */
public class MavlinkCameraControl extends MavlinkControlInput
{
    
    protected MavlinkCameraControl(MavlinkDriver driver)
    {
        super(driver);
    }
    
    
    @Override
    public String getName()
    {
        return "camCommands";
    }
    
    
    protected void init()
    {
        GeoPosHelper fac = new GeoPosHelper();
        
        // build command message structure
        commandData = fac.newDataChoice();
        commandData.setName(getName());
        commandData.setUpdatable(true);
        AllowedValues numConstraint;
        
        // get commands enabled in config
        EnumSet<CmdTypes> cmdSet = parentSensor.getConfiguration().activeCommands;
        
        // mount control
        if (cmdSet.contains(CmdTypes.MOUNT_CONTROL))
        {
            DataRecord cmd = fac.newDataRecord();
            
            Quantity pitch = fac.newQuantity(SWEHelper.getPropertyUri("Pitch"), "Gimbal Pitch", null, "deg", DataType.FLOAT);
            numConstraint = fac.newAllowedValues();
            numConstraint.addInterval(new double[] {-90.0, +90.0});
            pitch.setConstraint(numConstraint);
            cmd.addField("pitch", pitch);
            
            Quantity roll = fac.newQuantity(SWEHelper.getPropertyUri("Roll"), "Gimbal Roll", null, "deg", DataType.FLOAT);
            numConstraint = fac.newAllowedValues();
            numConstraint.addInterval(new double[] {-180.0, +180.0});
            pitch.setConstraint(numConstraint);
            cmd.addField("roll", roll);
            
            Quantity yaw = fac.newQuantity(SWEHelper.getPropertyUri("Yaw"), "Gimbal Yaw", null, "deg", DataType.FLOAT);
            numConstraint = fac.newAllowedValues();
            numConstraint.addInterval(new double[] {0.0, 360.});
            yaw.setConstraint(numConstraint);
            cmd.addField("yaw", yaw);
            
            commandData.addItem(CmdTypes.MOUNT_CONTROL.name(), cmd);
        }
        
        // mount target location
        if (cmdSet.contains(CmdTypes.MOUNT_TARGET))
        {
            Vector lla = fac.newLocationVectorLLA(SWEHelper.getPropertyUri("TargetLocation"));
            lla.setLabel("Pointing Target Location");
            commandData.addItem(CmdTypes.MOUNT_TARGET.name(), lla);
        }
    }


    @Override
    public CommandStatus execCommand(DataBlock command) throws SensorException
    {
        msg_command_long cmd = null;
        
        try
        {
            int cmdIndex = command.getIntValue(0);
            String cmdName = commandData.getComponent(cmdIndex).getName();
            
            // switch on command type
            CmdTypes cmdType = CmdTypes.valueOf(cmdName);
            parentSensor.getLogger().info("Sending {} command", cmdType);
            switch (cmdType)
            {
                case MOUNT_CONTROL:
                    cmd = new msg_command_long();
                    cmd.target_system = 1;
                    cmd.target_component = 1;
                    cmd.command = MAV_CMD.MAV_CMD_DO_MOUNT_CONTROL;
                    cmd.param1 = command.getFloatValue(1); // pitch (deg)
                    cmd.param2 = command.getFloatValue(2); // roll (deg)
                    cmd.param3 = command.getFloatValue(3); // yaw (deg)
                    parentSensor.sendCommand(cmd.pack());
                    break;
                    
                case MOUNT_TARGET:
                    cmd = new msg_command_long();
                    cmd.target_system = 1;
                    cmd.target_component = 1;
                    cmd.command = MAV_CMD.MAV_CMD_DO_SET_ROI;
                    cmd.param5 = (float)(command.getFloatValue(1)*1e7); // lat (deg)
                    cmd.param6 = (float)(command.getFloatValue(2)*1e7); // lon (deg)
                    cmd.param7 = command.getFloatValue(3); // alt (deg)
                    parentSensor.sendCommand(cmd.pack());
                    break;
                    
                default:
                    throw new SensorException("Unsupported command " + cmdType);
            }
        }
        catch (IOException e)
        {
            throw new SensorException("Cannot execute command", e);
        }
        
        CommandStatus cmdStatus = new CommandStatus();
        cmdStatus.status = StatusCode.COMPLETED;
        return cmdStatus;
    }

}
