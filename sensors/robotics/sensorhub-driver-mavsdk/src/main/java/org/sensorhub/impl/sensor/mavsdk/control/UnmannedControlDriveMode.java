/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is GeoRobotix Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2026 the Initial Developer. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/


package org.sensorhub.impl.sensor.mavsdk.control;

import io.mavsdk.mavlink_direct.MavlinkDirect;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.impl.sensor.mavsdk.UnmannedSystem;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.swe.SWEHelper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * Sets the flight mode of a ground rover or surface vessel (ArduRover). The operator picks a
 * mode by name; it is mapped to the ArduRover custom-mode number and pushed to the autopilot
 * via MAV_CMD_DO_SET_MODE over the {@link MavlinkDirect} plugin.
 * </p>
 *
 * <p>
 * <b>About the "Received ack for not-existing command: 176" warning:</b> MavlinkDirect injects
 * raw MAVLink and bypasses MAVSDK's tracked command-sender, so when ArduPilot acks the
 * DO_SET_MODE, MAVSDK has no record of having sent it and logs that warning. It is benign — the
 * ack proves the command reached the autopilot. MAVSDK-Java has no MavlinkPassthrough plugin, so
 * this cannot be avoided from Java. Instead of relying on the (discarded) sender ack, this control
 * subscribes to COMMAND_ACK directly to report accept/deny, and watches HEARTBEAT to confirm the
 * mode actually became active.
 * </p>
 *
 * <p>
 * The mode numbers are ArduRover-specific, so the control refuses to run if the connected vehicle
 * is positively identified as airborne. DOCK (8) requires a {@code MODE_DOCK_ENABLED} firmware
 * build, otherwise the autopilot will NACK it (visible now in the COMMAND_ACK log).
 * </p>
 *
 * @since Jun 2026
 */
public class UnmannedControlDriveMode extends AbstractSensorControl<UnmannedSystem>
{
    private DataRecord commandDataStruct;

    private static final String SENSOR_CONTROL_NAME = "UnmannedControlDriveMode";
    private static final String SENSOR_CONTROL_LABEL = "Drive Mode Control";
    private static final String SENSOR_CONTROL_DESCRIPTION =
            "Sets the ArduRover flight mode of a ground rover or surface vessel. " +
                    "Rejected for airborne platforms.";

    private static final String NODE_NAME_STR = "/SensorHub/spot/drive_mode_control";

    /**
     * Mode name -> ArduRover custom-mode number. Insertion order is preserved, so the operator's
     * drop-down lists modes in this order. CONFIRM against your firmware before trusting on real
     * hardware.
     */
    private static final Map<String, Integer> ROVER_MODES = new LinkedHashMap<>();
    static {
        ROVER_MODES.put("MANUAL",    0);
        ROVER_MODES.put("ACRO",      1);
        ROVER_MODES.put("STEERING",  3);
        ROVER_MODES.put("HOLD",      4);
        ROVER_MODES.put("LOITER",    5);
        ROVER_MODES.put("FOLLOW",    6);
        ROVER_MODES.put("SIMPLE",    7);
        ROVER_MODES.put("DOCK",      8);   // requires MODE_DOCK_ENABLED firmware build
        ROVER_MODES.put("AUTO",      10);
        ROVER_MODES.put("RTL",       11);
        ROVER_MODES.put("SMART_RTL", 12);
        ROVER_MODES.put("GUIDED",    15);
    }

    /** MAV_CMD_DO_SET_MODE. */
    private static final int MAV_CMD_DO_SET_MODE = 176;
    /** base_mode bit MAV_MODE_FLAG_CUSTOM_MODE_ENABLED; ArduPilot reads custom_mode only when set. */
    private static final int BASE_MODE_CUSTOM_ENABLED = 1;
    private static final int TARGET_COMPONENT = 1;   // autopilot

    private io.mavsdk.System system = null;

    // Target vehicle system id; read from MAV_SYSID, defaults to 1 (typical single-vehicle SITL).
    private volatile int targetSysId = 1;

    // Last mode requested through this control, used to interpret acks and confirm via HEARTBEAT.
    private volatile int lastRequestedMode = -1;
    private volatile String lastRequestedModeName = null;
    private volatile int confirmedMode = -1;

    public UnmannedControlDriveMode( UnmannedSystem parentSensor ) {
        super("mavDriveModeControl", parentSensor);
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandDataStruct;
    }

    public void setSystem( io.mavsdk.System systemParam ) {

        system = systemParam;
        if ( system == null )
            return;

        // Resolve the vehicle's MAVLink system id (so commands target the right vehicle).
        system.getParam().getParamInt("MAV_SYSID")
                .subscribe(
                        id -> targetSysId = id,
                        err -> log.debug("Could not read MAV_SYSID; defaulting target system to "
                                + targetSysId + " (" + err.getMessage() + ")")
                );

        // Listen for the autopilot's reply to our DO_SET_MODE and report accept/deny. This is the
        // ack MAVSDK's command-sender discards (hence the "not-existing command: 176" warning).
        system.getMavlinkDirect().getMessage("COMMAND_ACK")
                .subscribe(
                        msg -> {
                            String json = msg.getFieldsJson();
                            String cmd = extractToken(json, "command");
                            boolean isSetMode = cmd != null
                                    && (cmd.equals(String.valueOf(MAV_CMD_DO_SET_MODE))
                                    || cmd.toUpperCase().contains("DO_SET_MODE"));
                            if ( !isSetMode )
                                return;

                            String result = describeResult(extractToken(json, "result"));
                            String ctx = (lastRequestedModeName != null)
                                    ? " for " + lastRequestedModeName : "";
                            if ( "ACCEPTED".equals(result) )
                                log.debug("Drive-mode set-mode ACK" + ctx + ": " + result);
                            else
                                log.warn("Drive-mode set-mode ACK" + ctx + ": " + result
                                        + " (autopilot did not accept the mode change)");
                        },
                        err -> log.debug("COMMAND_ACK subscription error: " + err.getMessage())
                );

        // Definitive confirmation: the autopilot's HEARTBEAT reports the currently active mode in
        // custom_mode (1 Hz). When it matches what we asked for, the change is confirmed live.
        system.getMavlinkDirect().getMessage("HEARTBEAT")
                .subscribe(
                        msg -> {
                            if ( msg.getComponentId() != 1 )
                                return;
                            int cm = extractInt(msg.getFieldsJson(), "custom_mode");
                            if ( cm != Integer.MIN_VALUE && cm == lastRequestedMode && cm != confirmedMode ) {
                                confirmedMode = cm;
                                log.debug("Drive mode confirmed active: " + lastRequestedModeName
                                        + " (custom_mode=" + cm + ")");
                            }
                        },
                        err -> log.debug("HEARTBEAT subscription error: " + err.getMessage())
                );
    }

    public void init() {

        SWEHelper factory = new SWEHelper();

        commandDataStruct = factory.createRecord()
                .name(SENSOR_CONTROL_NAME)
                .label(SENSOR_CONTROL_LABEL)
                .description(SENSOR_CONTROL_DESCRIPTION)
                .definition(SWEHelper.getPropertyUri("Control"))
                .addField("mode", factory.createCategory()
                        .definition(SWEHelper.getPropertyUri("OperationalMode"))
                        .label("Drive Mode")
                        .description("ArduRover flight mode to switch to")
                        .addAllowedValues(ROVER_MODES.keySet())
                        .value("HOLD"))
                .build();
    }

    @Override
    protected boolean execCommand(DataBlock command) throws CommandException {

        VehicleDomain domain = parentSensor.getVehicleDomain();
        if ( !domain.permitsSurfaceCommand() ) {
            throw new CommandException("Refusing drive-mode change: connected vehicle is airborne ("
                    + domain + "). These are ArduRover mode numbers.");
        }

        if ( system == null ) {
            throw new CommandException("Unmanned System not initialized");
        }

        String modeName = command.getStringValue(0);
        if ( modeName == null || modeName.trim().isEmpty() ) {
            throw new CommandException("No drive mode supplied");
        }
        modeName = modeName.trim().toUpperCase();

        Integer modeNum = ROVER_MODES.get(modeName);
        if ( modeNum == null ) {
            throw new CommandException("Unsupported drive mode: " + modeName
                    + ". Expected one of " + ROVER_MODES.keySet());
        }

        lastRequestedMode = modeNum;
        lastRequestedModeName = modeName;
        confirmedMode = -1;   // re-arm HEARTBEAT confirmation for this request

        log.debug("Command received - Drive Mode: " + modeName + " (custom_mode=" + modeNum + ")");
        setMode(modeNum);

        return true;
    }

    private void setMode( int customMode ) {

        int sysId = targetSysId;

        log.debug("Commanding drive mode (custom_mode=" + customMode + ", target_system=" + sysId + ")...");

        // COMMAND_LONG / MAV_CMD_DO_SET_MODE: param1 = base_mode (custom enabled), param2 = mode.
        String fieldsJson = String.format(
                "{\"target_system\":%d,\"target_component\":%d,\"command\":%d,\"confirmation\":0," +
                        "\"param1\":%d,\"param2\":%d,\"param3\":0,\"param4\":0,\"param5\":0,\"param6\":0,\"param7\":0}",
                sysId, TARGET_COMPONENT, MAV_CMD_DO_SET_MODE, BASE_MODE_CUSTOM_ENABLED, customMode);

        // Envelope: sender = GCS (255/1), target = the vehicle (sysId/1). A 0 sender id is invalid
        // in MAVLink and muddies ack routing, which is why the old 0/0/0/0 version looked messy.
        system.getMavlinkDirect()
                .sendMessage(new MavlinkDirect.MavlinkMessage("COMMAND_LONG", 255, 1, sysId, 1, fieldsJson))
                .doOnComplete(() -> log.debug("Drive-mode change sent (custom_mode=" + customMode + ")"))
                .doOnError(throwable -> log.debug("Failed to send drive-mode change: " + throwable.getMessage()))
                .subscribe(
                        () -> { },
                        throwable -> log.error("Drive-mode error: " + throwable.getMessage())
                );
    }

    /** Pull a scalar token (number or quoted enum name) out of a flat JSON object. */
    private static String extractToken(String json, String key) {
        if ( json == null )
            return null;
        Matcher m = Pattern
                .compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"?([A-Za-z0-9_\\-]+)\"?")
                .matcher(json);
        return m.find() ? m.group(1) : null;
    }

    /** Parse a numeric field, returning Integer.MIN_VALUE if absent/non-numeric. */
    private static int extractInt(String json, String key) {
        String tok = extractToken(json, key);
        if ( tok == null )
            return Integer.MIN_VALUE;
        try {
            return Integer.parseInt(tok.trim());
        } catch (NumberFormatException e) {
            return Integer.MIN_VALUE;
        }
    }

    /** Map a MAV_RESULT (numeric or enum-name form) to a readable label. */
    private static String describeResult(String token) {
        if ( token == null )
            return "UNKNOWN";
        String up = token.trim().toUpperCase();
        if ( up.contains("ACCEPTED") )             return "ACCEPTED";
        if ( up.contains("TEMPORARILY_REJECTED") ) return "TEMPORARILY_REJECTED";
        if ( up.contains("DENIED") )               return "DENIED";
        if ( up.contains("UNSUPPORTED") )          return "UNSUPPORTED";
        if ( up.contains("FAILED") )               return "FAILED";
        if ( up.contains("IN_PROGRESS") )          return "IN_PROGRESS";
        if ( up.contains("CANCELLED") )            return "CANCELLED";
        switch (up) {
            case "0": return "ACCEPTED";
            case "1": return "TEMPORARILY_REJECTED";
            case "2": return "DENIED";
            case "3": return "UNSUPPORTED";
            case "4": return "FAILED";
            case "5": return "IN_PROGRESS";
            case "6": return "CANCELLED";
            default:  return "result=" + token;
        }
    }
}