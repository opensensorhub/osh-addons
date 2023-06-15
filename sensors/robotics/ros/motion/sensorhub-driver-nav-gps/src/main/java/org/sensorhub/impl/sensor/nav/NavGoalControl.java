/*
 *  The contents of this file are subject to the Mozilla Public License, v. 2.0.
 *  If a copy of the MPL was not distributed with this file, You can obtain one
 *  at http://mozilla.org/MPL/2.0/.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the License.
 *
 *  Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 */
package org.sensorhub.impl.sensor.nav;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.Vector;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.impl.ros.config.RosMasterConfig;
import org.sensorhub.impl.ros.nodes.pubsub.RosPublisherNode;
import org.sensorhub.impl.ros.utils.RosUtils;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;
import sensor_msgs.NavSatFix;

import java.net.URI;

public class NavGoalControl extends AbstractSensorControl<NavGoalSensor> {

    private static final String SENSOR_CONTROL_NAME = "NavGoalControl";

    private static final String SENSOR_CONTROL_LABEL = "NavGoalControl";

    private static final String SENSOR_CONTROL_DESCRIPTION =
            "Sends remote operation commands in the form of GPS location goals to the robot to navigate to given location";

    private static final String NODE_NAME_STR = "/SensorHub/navGoal";

    private static final String NAVSAT_TOPIC_STR = "gps_goal_fix";

    private DataRecord commandDataStruct;

    private NodeMainExecutor nodeMainExecutor;

    private RosPublisherNode<NavSatFix> navSatFixPublisherNode;

    public NavGoalControl(NavGoalSensor parentNavGoalSensor) {

        super(SENSOR_CONTROL_NAME, parentNavGoalSensor);
    }

    private void defineRecordStructure() {

        GeoPosHelper factory = new GeoPosHelper();

        Vector location = factory.createLocationVectorLatLon()
                .name("location")
                .updatable(true)
                .label("Location")
                .description("The target location to travel to")
                .build();

        commandDataStruct = factory.createRecord()
                .name(getName())
                .updatable(true)
                .definition(SWEHelper.getPropertyUri("NavGoalControls"))
                .label(SENSOR_CONTROL_LABEL)
                .description(SENSOR_CONTROL_DESCRIPTION)
                .addField(location.getName(), location)
                .build();
    }

    public void doInit() {

        defineRecordStructure();

        nodeMainExecutor = DefaultNodeMainExecutor.newDefault();

        navSatFixPublisherNode = new RosPublisherNode<>(NODE_NAME_STR, NAVSAT_TOPIC_STR, NavSatFix._TYPE);
    }

    public void doStart() {

        RosMasterConfig config = parentSensor.getConfiguration().rosMaster;

        NodeConfiguration nodeConfiguration = RosUtils.getNodeConfiguration(
                config.localHostIp, NODE_NAME_STR, URI.create(config.uri));

        nodeMainExecutor.execute(navSatFixPublisherNode, nodeConfiguration);
    }

    public void doStop() {

        nodeMainExecutor.shutdown();
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandDataStruct;
    }

    @Override
    public boolean execCommand(DataBlock command) throws CommandException {

        DataRecord commandData = commandDataStruct.copy();

        commandData.setData(command);

        try {

            Vector location = (Vector) commandData.getField("location");

            getLogger().debug("location: " + location);

            double latitude = location.getCoordinate("lat").getData().getDoubleValue();

            getLogger().debug("latitude: " + latitude);

            double longitude = location.getCoordinate("lon").getData().getDoubleValue() % 180.0;

            getLogger().debug("longitude: " + longitude);

            NavSatFix message = navSatFixPublisherNode.getNewMessageBuffer();

            message.setLatitude(latitude);

            message.setLongitude(longitude);

            navSatFixPublisherNode.publishMessage(message);

        } catch (Exception e) {

            throw new CommandException("Failed to handle command", e);
        }

        return true;
    }
}
