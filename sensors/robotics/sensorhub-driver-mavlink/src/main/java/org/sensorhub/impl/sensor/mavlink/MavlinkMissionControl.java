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
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Vector;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.mavlink.MavlinkConfig.CmdTypes;
import org.sensorhub.impl.sensor.mavlink.MavlinkDriver.CopterModes;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.common.msg_command_int;
import com.MAVLink.common.msg_command_long;
import com.MAVLink.common.msg_mission_item;
import com.MAVLink.common.msg_mission_item_int;
import com.MAVLink.common.msg_set_position_target_global_int;
import com.MAVLink.common.msg_set_position_target_local_ned;
import com.MAVLink.enums.MAV_CMD;
import com.MAVLink.enums.MAV_COMPONENT;
import com.MAVLink.enums.MAV_FRAME;


/**
 * <p>
 * Implementation of navigation control interface for MAVLink systems
 * 
 * WARNING: WIP, not fully tested
 * </p>
 *
 * @author Alex Robin
 * @since Jul 5, 2016
 */
public class MavlinkMissionControl extends MavlinkControlInput
{
  
    
    protected MavlinkMissionControl(MavlinkDriver driver, DataChoice commandChoice)
    {
        super("missionPlan", driver);
    }
    
    
    protected void init()
    {
        GeoPosHelper fac = new GeoPosHelper();
        
        // build command message structure
        commandData = fac.createChoice().build();
        commandData.setName(getName());
        commandData.setUpdatable(true);
        
        // get commands enabled in config
        EnumSet<CmdTypes> cmdSet = parentSensor.getConfiguration().activeCommands;
        
        // takeoff
        if (cmdSet.contains(CmdTypes.TAKEOFF))
        {
            commandData.addItem(CmdTypes.TAKEOFF.name(), fac.createQuantity()
                .definition(GeoPosHelper.DEF_ALTITUDE_GROUND)
                .label("Take-Off Altitude")
                .uomCode("m")
                .dataType(DataType.FLOAT)
                .build());
        }
        
        // goto LLA
        if (cmdSet.contains(CmdTypes.GOTO_LLA))
        {
            commandData.addItem(CmdTypes.GOTO_LLA.name(), fac.createRecord()
                .addField("location", fac.createLocationVectorLLA()
                    .definition(GeoPosHelper.DEF_LOCATION)
                    .label("Goto Location"))
                .addField("yaw", fac.createQuantity()
                    .definition(GeoPosHelper.DEF_YAW_ANGLE)
                    .label("Yaw Angle")
                    .uomCode("deg")
                    .dataType(DataType.FLOAT))
                .build());
        }
        
        // goto ENU
        if (cmdSet.contains(CmdTypes.GOTO_ENU))
        {
            DataRecord cmd = fac.newDataRecord();
            Vector xyz = fac.newLocationVectorXYZ(SWEHelper.getPropertyUri("Location"), SWEConstants.REF_FRAME_ENU, "m");
            xyz.setLabel("Goto Location");
            cmd.addComponent("location", xyz);
            cmd.addComponent("yaw", fac.newQuantity(SWEHelper.getPropertyUri("Yaw"), "Yaw Angle", null, "deg", DataType.FLOAT));
            commandData.addItem(CmdTypes.GOTO_ENU.name(), cmd);
        }
        
        // velocity
        if (cmdSet.contains(CmdTypes.VELOCITY))
        {
            Vector xyz = fac.newVelocityVector(SWEHelper.getPropertyUri("Velocity"), SWEConstants.REF_FRAME_ENU, "m/s");
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
        
        // loiter in place for a specific amount of time
        if (cmdSet.contains(CmdTypes.LOITER))
        {
            commandData.addItem(CmdTypes.LOITER.name(), fac.createRecord()
                .addField("location", fac.createLocationVectorLLA()
                    .definition(GeoPosHelper.DEF_LOCATION)
                    .label("Goto Location"))
                .addField("time", fac.createQuantity()
                    .definition(GeoPosHelper.getPropertyUri("Duration"))
                    .label("Loiter Time")
                    .description("Time to loiter at waypoint")
                    .uomCode("s")
                    .dataType(DataType.FLOAT))
                .build());
        }
        
        // loiter in circle for a specific number of turns
        if (cmdSet.contains(CmdTypes.ORBIT))
        {
            commandData.addItem(CmdTypes.ORBIT.name(), fac.createRecord()
                .addField("location", fac.createLocationVectorLLA()
                    .definition(GeoPosHelper.DEF_LOCATION)
                    .label("Goto Location"))
                .addField("radius", fac.createQuantity()
                    .definition(GeoPosHelper.DEF_DISTANCE)
                    .label("Orbit Radius")
                    .uomCode("m")
                    .dataType(DataType.FLOAT))
                .addField("turns", fac.createCount()
                    .definition(SWEHelper.getPropertyUri("Count"))
                    .label("Number of Turns"))
                .build());
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
    protected boolean execCommand(DataBlock command) throws CommandException
    {
        try
        {
            int cmdIndex = command.getIntValue(0);
            String cmdName = commandData.getComponent(cmdIndex).getName();
            
            // switch on command type
            CmdTypes cmdType = CmdTypes.valueOf(cmdName);
            
            switch (cmdType)
            {
                case TAKEOFF:
                    var takeOffCmd = new msg_command_long();
                    takeOffCmd.target_system = 1;
                    takeOffCmd.target_component = 1;//MAV_COMPONENT.MAV_COMP_ID_SYSTEM_CONTROL;
                    takeOffCmd.command = MAV_CMD.MAV_CMD_NAV_TAKEOFF;
                    takeOffCmd.param7 = Math.min(10, command.getFloatValue(1)); // alt (m), max to 10m
                    parentSensor.getLogger().info("Sending {} command: {}", cmdType, takeOffCmd.command);
                    parentSensor.setMode(CopterModes.GUIDED.ordinal());
                    parentSensor.armMotors();
                    parentSensor.sendCommand(takeOffCmd.pack());
                    break;
                    
                case GOTO_LLA:
                    var wptCmd = new msg_mission_item_int();
                    wptCmd.target_system = 1;
                    wptCmd.target_component = 1;
                    wptCmd.seq = 0;
                    wptCmd.current = 2;
                    wptCmd.autocontinue = 1;
                    wptCmd.frame = MAV_FRAME.MAV_FRAME_GLOBAL_RELATIVE_ALT_INT;
                    wptCmd.command = MAV_CMD.MAV_CMD_NAV_WAYPOINT;
                    wptCmd.param1 = 0; // hold time in seconds
                    wptCmd.param2 = 0; // acceptance radius in meters
                    wptCmd.param3 = 0; // pass radius in meters
                    wptCmd.param4 = 0; // yaw angle (0 means vehicle heading)
                    wptCmd.x = (int)(command.getDoubleValue(1)*1e7); // latitude in degrees, scaled to int for precision
                    wptCmd.y = (int)(command.getDoubleValue(2)*1e7); // longitude in degrees, scaled to int for precision
                    wptCmd.z = command.getFloatValue(3); // altitude in meters relative to take-off altitude
                    parentSensor.getLogger().info("Sending {} command: {}", cmdType, wptCmd.command);
                    parentSensor.sendCommand(wptCmd.pack());
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
                    var velcmd = new msg_set_position_target_local_ned();
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
                    var headingCmd = new msg_command_long();
                    headingCmd.target_system = 1;
                    headingCmd.target_component = MAV_COMPONENT.MAV_COMP_ID_SYSTEM_CONTROL;
                    headingCmd.command = MAV_CMD.MAV_CMD_CONDITION_YAW;
                    headingCmd.param1 = command.getFloatValue(1); // yaw (deg)
                    headingCmd.param2 = command.getFloatValue(2); // yaw rate (deg/s)
                    headingCmd.param4 = 0;
                    parentSensor.getLogger().info("Sending {} command: {}", cmdType, headingCmd.command);
                    parentSensor.sendCommand(headingCmd.pack());
                    break;
                    
                case LOITER:
                    var loiterCmd = new msg_command_long();
                    loiterCmd.target_system = 1;
                    loiterCmd.target_component = MAV_COMPONENT.MAV_COMP_ID_SYSTEM_CONTROL;
                    loiterCmd.command = MAV_CMD.MAV_CMD_NAV_LOITER_UNLIM;
                    loiterCmd.param5 = (float)(command.getDoubleValue(1)*1e7); // lat (deg)
                    loiterCmd.param6 = (float)(command.getDoubleValue(2)*1e7); // lon (deg)
                    loiterCmd.param7 = command.getFloatValue(3); // alt (m)
                    parentSensor.getLogger().info("Sending {} command: {}", cmdType, loiterCmd.command);
                    parentSensor.sendCommand(loiterCmd.pack());
                    break;
                    
                case ORBIT:
                    var orbitCmd = new msg_mission_item_int();
                    orbitCmd.target_system = 1;
                    orbitCmd.target_component = 1;
                    orbitCmd.seq = 0;
                    orbitCmd.current = 2;
                    orbitCmd.autocontinue = 1;
                    orbitCmd.frame = MAV_FRAME.MAV_FRAME_GLOBAL_RELATIVE_ALT_INT;
                    //orbitCmd.command = MAV_CMD.MAV_CMD_NAV_LOITER_TURNS;
                    orbitCmd.command = MAV_CMD.MAV_CMD_NAV_WAYPOINT;
                    orbitCmd.param1 = 0;//command.getFloatValue(5); // number of turns
                    orbitCmd.param2 = 0; // unused
                    orbitCmd.param3 = 0;//command.getFloatValue(4); // orbit radius in meters
                    orbitCmd.param4 = 0; // unused
                    orbitCmd.x = (int)(command.getDoubleValue(1)*1e7); // latitude in degrees, scaled to int for precision
                    orbitCmd.y = (int)(command.getDoubleValue(2)*1e7); // longitude in degrees, scaled to int for precision
                    orbitCmd.z = command.getFloatValue(3); // altitude in meters relative to take-off altitude
                    parentSensor.getLogger().info("Sending {} command: {}", cmdType, orbitCmd.command);
                    parentSensor.sendCommand(orbitCmd.pack());
                    break;
                    
                case RTL:
                    var rtlCmd = new msg_command_long();
                    rtlCmd.target_system = 1;
                    rtlCmd.target_component = MAV_COMPONENT.MAV_COMP_ID_SYSTEM_CONTROL;
                    rtlCmd.command = MAV_CMD.MAV_CMD_NAV_RETURN_TO_LAUNCH;
                    parentSensor.getLogger().info("Sending {} command: {}", cmdType, rtlCmd.command);
                    parentSensor.sendCommand(rtlCmd.pack());
                    break;
                    
                case LAND:
                    var landCmd = new msg_command_long();
                    landCmd.target_system = 1;
                    landCmd.target_component = MAV_COMPONENT.MAV_COMP_ID_SYSTEM_CONTROL;
                    landCmd.command = MAV_CMD.MAV_CMD_NAV_LAND;
                    landCmd.param5 = (float)(command.getDoubleValue(1)*1e7); // lat (deg)
                    landCmd.param6 = (float)(command.getDoubleValue(2)*1e7); // lon (deg)
                    parentSensor.getLogger().info("Sending {} command: {}", cmdType, landCmd.command);
                    parentSensor.sendCommand(landCmd.pack());
                    break;
                    
                default:
                    throw new SensorException("Unsupported command " + cmdType);
            }
        }
        catch (Exception e)
        {
            throw new CommandException("Cannot execute command", e);
        }
        
        return true;
    }
    
    
    public void notifyMissionMessage(MAVLinkMessage msg)
    {
        
    }


    public void stop()
    {
        
    }

}
