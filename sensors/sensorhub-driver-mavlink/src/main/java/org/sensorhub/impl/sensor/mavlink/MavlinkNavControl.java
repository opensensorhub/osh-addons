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

import java.util.EnumSet;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Vector;
import org.sensorhub.api.common.CommandStatus;
import org.sensorhub.api.common.CommandStatus.StatusCode;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.mavlink.MavlinkConfig.CmdTypes;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;
import com.MAVLink.common.msg_command_long;
import com.MAVLink.common.msg_set_position_target_global_int;
import com.MAVLink.common.msg_set_position_target_local_ned;
import com.MAVLink.enums.MAV_CMD;
import com.MAVLink.enums.MAV_COMPONENT;
import com.MAVLink.enums.MAV_FRAME;


/**
 * <p>
 * Implementation of navigation control interface for MAVLink systems
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Jul 5, 2016
 */
public class MavlinkNavControl extends MavlinkControlInput
{
  
    
    protected MavlinkNavControl(MavlinkDriver driver)
    {
        super(driver);
    }
    
    
    @Override
    public String getName()
    {
        return "navCommands";
    }
    
    
    protected void init()
    {
        GeoPosHelper fac = new GeoPosHelper();
        
        // build command message structure
        commandData = fac.newDataChoice();
        commandData.setName(getName());
        commandData.setUpdatable(true);
        
        // get commands enabled in config
        EnumSet<CmdTypes> cmdSet = parentSensor.getConfiguration().activeCommands;
        
        // takeoff
        if (cmdSet.contains(CmdTypes.TAKEOFF))
        {
            Quantity alt = fac.newQuantity(SWEHelper.getPropertyUri("AltitudeAboveGround"), "Take-Off Altitude", null, "m", DataType.FLOAT);
            commandData.addItem(CmdTypes.TAKEOFF.name(), alt);
        }
        
        // goto LLA
        if (cmdSet.contains(CmdTypes.GOTO_LLA))
        {
            DataRecord cmd = fac.newDataRecord();
            Vector lla = fac.newLocationVectorLLA(SWEHelper.getPropertyUri("PlatformLocation"));
            lla.setLabel("Goto Location");
            cmd.addComponent("location", lla);
            cmd.addComponent("yaw", fac.newQuantity(SWEHelper.getPropertyUri("Yaw"), "Yaw Angle", null, "deg", DataType.FLOAT));
            commandData.addItem(CmdTypes.GOTO_LLA.name(), cmd);
        }
        
        // goto ENU
        if (cmdSet.contains(CmdTypes.GOTO_ENU))
        {
            DataRecord cmd = fac.newDataRecord();
            Vector xyz = fac.newLocationVectorXYZ(SWEHelper.getPropertyUri("PlatformLocation"), SWEConstants.REF_FRAME_ENU, "m");
            xyz.setLabel("Goto Location");
            cmd.addComponent("location", xyz);
            cmd.addComponent("yaw", fac.newQuantity(SWEHelper.getPropertyUri("Yaw"), "Yaw Angle", null, "deg", DataType.FLOAT));
            commandData.addItem(CmdTypes.GOTO_ENU.name(), cmd);
        }
        
        // velocity
        if (cmdSet.contains(CmdTypes.VELOCITY))
        {
            Vector xyz = fac.newVelocityVector(SWEHelper.getPropertyUri("PlatformVelocity"), SWEConstants.REF_FRAME_ENU, "m/s");
            commandData.addItem(CmdTypes.VELOCITY.name(), xyz);
        }
        
        // heading
        if (cmdSet.contains(CmdTypes.HEADING))
        {
            DataRecord cmd = fac.newDataRecord();
            cmd.addComponent("yaw", fac.newQuantity(SWEHelper.getPropertyUri("Yaw"), "Yaw Angle", null, "deg", DataType.FLOAT));
            cmd.addComponent("yawRate", fac.newQuantity(SWEHelper.getPropertyUri("YawRate"), "Yaw Rate", null, "deg/s", DataType.FLOAT));
            commandData.addItem(CmdTypes.HEADING.name(), cmd);
        }
        
        // loiter in place
        if (cmdSet.contains(CmdTypes.LOITER))
        {
            Vector lla = fac.newLocationVectorLLA(SWEHelper.getPropertyUri("PlatformLocation"));
            lla.setLabel("Loiter Location");
            commandData.addItem(CmdTypes.LOITER.name(), lla);
        }
        
        // loiter in circle
        if (cmdSet.contains(CmdTypes.ORBIT))
        {
            DataRecord cmd = fac.newDataRecord();
            Vector lla = fac.newLocationVectorLLA(SWEHelper.getPropertyUri("PlatformLocation"));
            lla.setLabel("Orbit Center");
            cmd.addComponent("location", lla);
            cmd.addComponent("radius", fac.newQuantity(SWEHelper.getPropertyUri("CircleRadius"), "Orbit Radius", null, "m", DataType.FLOAT));
            commandData.addItem(CmdTypes.ORBIT.name(), cmd);
        }
        
        // return to launch
        if (cmdSet.contains(CmdTypes.RTL))
        {
            net.opengis.swe.v20.Boolean rtl = fac.newBoolean(null, "Return To Launch", "Return to launch location");
            commandData.addItem(CmdTypes.RTL.name(), rtl);
        }
        
        // land
        if (cmdSet.contains(CmdTypes.LAND))
        {
            Vector latLon = fac.newLocationVectorLatLon(SWEHelper.getPropertyUri("PlatformLocation"));
            latLon.setLabel("Landing Location");
            latLon.setDescription("Landing location or NaN to land at the current location");
            commandData.addItem(CmdTypes.LAND.name(), latLon);
        }
    }
    

    @Override
    public CommandStatus execCommand(DataBlock command) throws SensorException
    {
        try
        {
            int cmdIndex = command.getIntValue(0);
            String cmdName = commandData.getComponent(cmdIndex).getName();
            
            // switch on command type
            msg_command_long cmd;
            CmdTypes cmdType = CmdTypes.valueOf(cmdName);
            
            switch (cmdType)
            {
                case TAKEOFF:
                    cmd = new msg_command_long();
                    cmd.target_system = 1;
                    cmd.target_component = MAV_COMPONENT.MAV_COMP_ID_SYSTEM_CONTROL;
                    cmd.command = MAV_CMD.MAV_CMD_NAV_TAKEOFF;
                    cmd.param7 = Math.min(10, command.getFloatValue(1)); // alt (m), max to 10m
                    parentSensor.getLogger().info("Sending {} command: {}", cmdType,cmd.command);
                    parentSensor.sendCommand(cmd.pack());   
                    break;
                    
                case GOTO_LLA:
                    msg_set_position_target_global_int llacmd = new msg_set_position_target_global_int();
                    llacmd.target_system = 1;
                    llacmd.target_component = MAV_COMPONENT.MAV_COMP_ID_SYSTEM_CONTROL;
                    llacmd.coordinate_frame = MAV_FRAME.MAV_FRAME_GLOBAL_RELATIVE_ALT_INT;
                    llacmd.type_mask = 0x1F8;
                    llacmd.lat_int = (int)(command.getFloatValue(1)*1e7); // lat (deg)
                    llacmd.lon_int = (int)(command.getFloatValue(2)*1e7); // lon (deg)
                    llacmd.alt = 5;//command.getFloatValue(3); // alt (m)
                    llacmd.yaw = (float)(command.getDoubleValue(4)/180.*Math.PI); // yaw (deg)
                    parentSensor.getLogger().info("Sending {} command: {}", cmdType, llacmd.msgid);
                    parentSensor.sendCommand(llacmd.pack());
                    break;
                    
                case GOTO_ENU:
                    msg_set_position_target_local_ned enucmd = new msg_set_position_target_local_ned();
                    enucmd.target_system = 1;
                    enucmd.target_component = MAV_COMPONENT.MAV_COMP_ID_SYSTEM_CONTROL;
                    enucmd.coordinate_frame = MAV_FRAME.MAV_FRAME_LOCAL_OFFSET_NED;
                    enucmd.type_mask = 0x1F8;
                    enucmd.x = command.getFloatValue(2); // x_NED = y_ENU (m)
                    enucmd.y = command.getFloatValue(1); // y_NED = x_ENU (m)
                    enucmd.z = -command.getFloatValue(3); // z_NED = -z_ENU (m)
                    enucmd.yaw = (float)(command.getDoubleValue(4)/180.*Math.PI); // yaw (deg)
                    parentSensor.getLogger().info("Sending {} command: {}", cmdType, enucmd.msgid);
                    parentSensor.sendCommand(enucmd.pack());   
                    break;
                    
                case VELOCITY:
                    msg_set_position_target_local_ned velcmd = new msg_set_position_target_local_ned();
                    velcmd.target_system = 1;
                    velcmd.target_component = MAV_COMPONENT.MAV_COMP_ID_SYSTEM_CONTROL;
                    velcmd.coordinate_frame = MAV_FRAME.MAV_FRAME_LOCAL_NED;
                    velcmd.type_mask = 0x1C7;
                    velcmd.vx = command.getFloatValue(2); // vx_NED = vy_ENU (m/s)
                    velcmd.vy = command.getFloatValue(1); // vy_NED = vx_ENU (m/s)
                    velcmd.vz = -command.getFloatValue(3); // vz_NED = -vz_ENU (m/s)
                    parentSensor.getLogger().info("Sending {} command: {}", cmdType, velcmd.msgid);
                    parentSensor.sendCommand(velcmd.pack());   
                    break;
                    
                case HEADING:
                    cmd = new msg_command_long();
                    cmd.target_system = 1;
                    cmd.target_component = MAV_COMPONENT.MAV_COMP_ID_SYSTEM_CONTROL;
                    cmd.command = MAV_CMD.MAV_CMD_CONDITION_YAW;
                    cmd.param1 = command.getFloatValue(1); // yaw (deg)
                    cmd.param2 = command.getFloatValue(2); // yaw rate (deg/s)
                    cmd.param4 = 0;
                    parentSensor.getLogger().info("Sending {} command: {}", cmdType,cmd.command);
                    parentSensor.sendCommand(cmd.pack());  
                    break;
                    
                case LOITER:
                    cmd = new msg_command_long();
                    cmd.target_system = 1;
                    cmd.target_component = MAV_COMPONENT.MAV_COMP_ID_SYSTEM_CONTROL;
                    cmd.command = MAV_CMD.MAV_CMD_NAV_LOITER_UNLIM;
                    cmd.param5 = (float)(command.getFloatValue(1)*1e7); // lat (deg)
                    cmd.param6 = (float)(command.getFloatValue(2)*1e7); // lon (deg)
                    cmd.param7 = command.getFloatValue(3); // alt (m)
                    parentSensor.getLogger().info("Sending {} command: {}", cmdType,cmd.command);
                    parentSensor.sendCommand(cmd.pack());   
                    break;
                    
                case ORBIT:
                    // set circle radius param
                    parentSensor.setParam("CIRCLE_RADIUS", command.getFloatValue(4)*100f);
                    // send loiter command
                    cmd = new msg_command_long();
                    cmd.target_system = 1;
                    cmd.target_component = MAV_COMPONENT.MAV_COMP_ID_SYSTEM_CONTROL;
                    cmd.command = MAV_CMD.MAV_CMD_NAV_LOITER_TURNS;
                    cmd.param5 = (float)(command.getFloatValue(1)*1e7); // lat (deg)
                    cmd.param6 = (float)(command.getFloatValue(2)*1e7); // lon (deg)
                    cmd.param7 = command.getFloatValue(3); // alt (m)
                    parentSensor.getLogger().info("Sending {} command: {}", cmdType,cmd.command);
                    parentSensor.sendCommand(cmd.pack());   
                    break;
                    
                case RTL:
                    cmd = new msg_command_long();
                    cmd.target_system = 1;
                    cmd.target_component = MAV_COMPONENT.MAV_COMP_ID_SYSTEM_CONTROL;
                    cmd.command = MAV_CMD.MAV_CMD_NAV_RETURN_TO_LAUNCH;
                    parentSensor.getLogger().info("Sending {} command: {}", cmdType,cmd.command);
                    parentSensor.sendCommand(cmd.pack());
                    break;
                    
                case LAND:
                    cmd = new msg_command_long();
                    cmd.target_system = 1;
                    cmd.target_component = MAV_COMPONENT.MAV_COMP_ID_SYSTEM_CONTROL;
                    cmd.command = MAV_CMD.MAV_CMD_NAV_LAND;
                    cmd.param5 = (float)(command.getDoubleValue(1)*1e7); // lat (deg)
                    cmd.param6 = (float)(command.getDoubleValue(2)*1e7); // lon (deg)
                    parentSensor.getLogger().info("Sending {} command: {}", cmdType,cmd.command);
                    parentSensor.sendCommand(cmd.pack());   
                    break;
                    
                default:
                    throw new SensorException("Unsupported command " + cmdType);
            }
        }
        catch (Exception e)
        {
            throw new SensorException("Cannot execute command", e);
        }
        
        CommandStatus cmdStatus = new CommandStatus();
        cmdStatus.status = StatusCode.COMPLETED;
        return cmdStatus;
    }


    public void stop()
    {
        
    }

}
