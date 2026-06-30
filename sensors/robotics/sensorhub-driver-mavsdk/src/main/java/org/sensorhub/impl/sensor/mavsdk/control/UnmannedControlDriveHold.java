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

/**
 * <p>
 * Commands a ground rover or surface vessel into HOLD mode: it stops and holds position.
 * This is the non-flying counterpart to a copter "brake/loiter" style hold.
 * </p>
 *
 * <p>
 * <b>Why this is {@code UnmannedControlDriveHold} and not a shared {@code UnmannedControlHold}:</b>
 * the autopilot flight mode that stops a driven vehicle is a genuinely different mode from the
 * one that stops a flying vehicle. ArduPilot custom-mode numbers are per-vehicle-type, so the
 * same number means different things on Rover vs Copter:
 * </p>
 * <ul>
 *   <li>ArduRover: "stop and hold" is HOLD, custom_mode {@value #ROVER_MODE_HOLD}.</li>
 *   <li>ArduCopter: has no HOLD mode; its equivalents are BRAKE / LOITER / POSHOLD, each with a
 *       different custom_mode number.</li>
 * </ul>
 * <p>
 * A copter hold therefore belongs in its own control with its own mode constant (e.g. a future
 * {@code UnmannedControlHold} or {@code UnmannedControlBrake}); it is not just this class with a
 * different number.
 * </p>
 *
 * <p>
 * Works for both ground rovers (MAV_TYPE_GROUND_ROVER) and surface boats (MAV_TYPE_SURFACE_BOAT)
 * since both run ArduRover. The control refuses to run if the connected vehicle is positively
 * identified as airborne.
 * </p>
 *
 * @since Jun 2026
 */
public class UnmannedControlDriveHold extends AbstractSensorControl<UnmannedSystem>
{
    private DataRecord commandDataStruct;

    /**
     * Name of the control
     */
    private static final String SENSOR_CONTROL_NAME = "UnmannedControlDriveHold";

    /**
     * Label for the control
     */
    private static final String SENSOR_CONTROL_LABEL = "Drive Hold Control";

    /**
     * Control description
     */
    private static final String SENSOR_CONTROL_DESCRIPTION =
            "Commands a ground rover or surface vessel into HOLD mode (stop and hold position). " +
            "Rejected for airborne platforms.";

    /**
     * ROS Node name assigned at creation
     */
    private static final String NODE_NAME_STR = "/SensorHub/spot/drive_hold_control";

    // --- ArduPilot mode-change wire constants -------------------------------------------------
    // We set the mode explicitly via MAVLink rather than using MAVSDK's Action.hold(), because
    // Action.hold() is documented as PX4-specific: it switches to a PX4 custom mode that
    // ArduPilot does not interpret as HOLD. Sending DO_SET_MODE with the ArduRover custom-mode
    // number is the path ArduPilot actually honors, and it ACKs via COMMAND_ACK.

    /** MAV_CMD_DO_SET_MODE (set base/custom flight mode). */
    private static final int MAV_CMD_DO_SET_MODE = 176;

    /**
     * base_mode bit MAV_MODE_FLAG_CUSTOM_MODE_ENABLED. ArduPilot reads custom_mode only when this
     * bit is set, and otherwise ignores base_mode.
     */
    private static final int BASE_MODE_CUSTOM_ENABLED = 1;

    /**
     * ArduRover flight-mode number for HOLD.
     * CONFIRM against your firmware (Mission Planner mode list / ArduRover Mode enum) before
     * trusting on real hardware: mode numbers are per-vehicle-type and can change between major
     * firmware revisions. If a copter variant is added later, give it its own constant
     * (ArduCopter BRAKE / LOITER / POSHOLD are different numbers).
     */
    private static final int ROVER_MODE_HOLD = 4;

    // Single-vehicle assumption: the autopilot is system 1 / component 1. If you ever run more
    // than one vehicle through this driver, derive these from the connected System instead of
    // hard-coding them.
    private static final int TARGET_SYSTEM = 1;
    private static final int TARGET_COMPONENT = 1;

    private io.mavsdk.System system = null;

    public UnmannedControlDriveHold( UnmannedSystem parentSensor ) {
        super("mavDriveHoldControl", parentSensor);
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandDataStruct;
    }

    public void setSystem( io.mavsdk.System systemParam ) {
        // No telemetry subscription needed for a mode change. MavlinkDirect was already
        // initialized once on connection (see UnmannedSystem.receiveDrone), and this is the same
        // System instance, so getMavlinkDirect() here is the same initialized plugin.
        system = systemParam;
    }

    public void init() {

        SWEHelper factory = new SWEHelper();

        // Single boolean trigger. Sending the command with engageHold=true issues the HOLD mode
        // change; false is a no-op (lets an operator wire a momentary control without firing).
        commandDataStruct = factory.createRecord()
                .name(SENSOR_CONTROL_NAME)
                .label(SENSOR_CONTROL_LABEL)
                .description(SENSOR_CONTROL_DESCRIPTION)
                .definition(SWEHelper.getPropertyUri("Control"))
                .addField("engageHold", factory.createBoolean()
                        .definition(SWEHelper.getPropertyUri("Flag"))
                        .label("Engage Hold")
                        .description("Set true to command HOLD (stop and hold position)."))
                .build();
    }

    @Override
    protected boolean execCommand(DataBlock command) throws CommandException {

        VehicleDomain domain = parentSensor.getVehicleDomain();
        if ( !domain.permitsSurfaceCommand() ) {
            throw new CommandException("Refusing drive-hold: connected vehicle is airborne ("
                    + domain + "). Use the air hold/brake control instead.");
        }

        boolean engage = command.getBooleanValue(0);
        if ( !engage ) {
            log.debug("Drive-hold command received with engageHold=false; no action taken");
            return true;
        }

        if ( system == null ) {
            throw new CommandException("Unmanned System not initialized");
        }

        log.debug("Command received - Drive Hold (ArduRover HOLD, custom_mode=" + ROVER_MODE_HOLD + ")");
        holdPosition();

        return true;
    }

    private void holdPosition() {

        log.debug("Commanding HOLD mode...");

        // COMMAND_LONG / MAV_CMD_DO_SET_MODE.
        //   param1 = base_mode    -> MAV_MODE_FLAG_CUSTOM_MODE_ENABLED
        //   param2 = custom_mode  -> ArduRover HOLD
        // Numeric values are used throughout so MavlinkDirect does not have to resolve enum names.
        // The autopilot replies with COMMAND_ACK; a reject surfaces through the Completable's
        // onError path below.
        String fieldsJson = String.format(
                "{\"target_system\":%d,\"target_component\":%d,\"command\":%d,\"confirmation\":0," +
                "\"param1\":%d,\"param2\":%d,\"param3\":0,\"param4\":0,\"param5\":0,\"param6\":0,\"param7\":0}",
                TARGET_SYSTEM, TARGET_COMPONENT, MAV_CMD_DO_SET_MODE,
                BASE_MODE_CUSTOM_ENABLED, ROVER_MODE_HOLD);

        system.getMavlinkDirect()
                .sendMessage(new MavlinkDirect.MavlinkMessage("COMMAND_LONG", 0, 0, 0, 0, fieldsJson))
                .doOnComplete(() -> {

                    log.debug("HOLD mode change sent");

                })
                .doOnError(throwable -> {

                    log.debug("Failed to send HOLD mode change: " + throwable.getMessage());

                })
                .subscribe(
                        () -> { },
                        throwable -> log.error("Drive-hold error: " + throwable.getMessage())
                );
    }
}