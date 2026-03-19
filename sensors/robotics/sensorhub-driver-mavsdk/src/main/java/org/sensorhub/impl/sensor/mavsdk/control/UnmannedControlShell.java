/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2025 the Initial Developer. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/


package org.sensorhub.impl.comm.mavsdk.control;

import io.mavsdk.shell.Shell;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.impl.comm.mavsdk.UnmannedSystem;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.swe.helper.GeoPosHelper;


/**
 * <p>
 * This particular class provides control stream  capabilities
 * </p>
 *
 * @since Jul 2025
 */
public class UnmannedControlShell extends AbstractSensorControl<UnmannedSystem>
{
    private DataRecord commandDataStruct;

    /**
     * Name of the control
     */
    private static final String SENSOR_CONTROL_NAME = "UnmannedControlShell";

    /**
     * Label for the control
     */
    private static final String SENSOR_CONTROL_LABEL = "System Shell Control";

    /**
     * Control description
     */
    private static final String SENSOR_CONTROL_DESCRIPTION =
            "Interfaces with MAVLINK and OSH to send System Shell Commands to the platform";

    /**
     * ROS Node name assigned at creation
     */
    private static final String NODE_NAME_STR = "/SensorHub/spot/shell_control";

    private io.mavsdk.System system = null;

    private static final String CMD_EXIT = "exit";

    public UnmannedControlShell( UnmannedSystem parentSensor) {
        super("mavShellControl", parentSensor);
    }


    @Override
    public DataComponent getCommandDescription() {
        return commandDataStruct;
    }

    public void setSystem( io.mavsdk.System systemParam ) {
        system = systemParam;
    }

    public void init() {

        GeoPosHelper factory = new GeoPosHelper();

        commandDataStruct = factory.createRecord()
                .name(SENSOR_CONTROL_NAME)
                .label(SENSOR_CONTROL_LABEL)
                .description(SENSOR_CONTROL_DESCRIPTION)
                .addField("command", factory.createText().value("")) //land
                .build();
    }


    @Override
    protected boolean execCommand(DataBlock command) throws CommandException {
        String shellCommand = command.getStringValue(0);

        System.out.println("Command received - Shell Command: " + command.getStringValue(0));

        if ( system != null ) {
            sendShellCommand( shellCommand );
        } else {
            throw new CommandException("Unmanned System not initialized");
        }

        return true;
    }


    private void sendShellCommand( String shellCommand ) {

        System.out.println("Sending Command...");

        // Logging shell response
        system.getShell().getReceive()
                .subscribe( throwable -> {
                    throw new CommandException(throwable);
                });


        system.getShell().send("command long 21 0 0 0 0 0 0 0" + "\n")
                .doOnError(throwable -> {
                    System.out.println("Error during shell");
                    throw new CommandException(throwable.getMessage());
                })
                .subscribe(() -> { }, throwable -> {
                    System.out.println("Failed to send: "
                            + ((Shell.ShellException) throwable).getCode());
                    throw new CommandException(throwable.getMessage());
                });

        //var test = MAV_CMD_NAV_LAND;
        //example: To Land:
        //message COMMAND_LONG 0 0 176 0 1 6 0 0 0 0 0

//        // Reading and sending command
//        while (true) {
//
//            if (shellCommand == null || shellCommand.isEmpty()) {
//                continue;
//            }
//            if (shellCommand.equalsIgnoreCase(CMD_EXIT)) {
//                break;
//            }
//
//            system.getShell().send(shellCommand)
//                    .subscribe(() -> { }, throwable -> {
//                        System.out.println("Failed to send: "
//                                + ((Shell.ShellException) throwable).getCode());
//                        throw new CommandException(throwable.getMessage());
//                    });
//        }
    }


    public void stop() {
        // TODO Auto-generated method stub
    }
}

