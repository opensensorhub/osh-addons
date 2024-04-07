/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 - 2024 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.spot.control.svc_clients;

import org.ros.exception.RemoteException;
import org.ros.node.service.ServiceResponseListener;
import org.sensorhub.api.command.CommandStatus;
import org.sensorhub.api.command.CommandStatusEvent;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.impl.ros.nodes.service.RosServiceClient;
import org.sensorhub.impl.sensor.spot.control.SpotPoseControl;
import spot_msgs.PosedStand;
import spot_msgs.PosedStandRequest;
import spot_msgs.PosedStandResponse;

/**
 * ROS Service client for manage pose commands.
 *
 * @author Nick Garay
 * @since Sept. 25, 2023
 */
public class PoseSvcClient extends RosServiceClient<PosedStandRequest, PosedStandResponse> implements ServiceResponseListener<PosedStandResponse> {

    /**
     * The control owning this service client
     */
    private final SpotPoseControl control;

    /**
     * Constructor
     *
     * @param parentControl the parent control for this service client
     * @param nodeName      name of the node
     * @param serviceName   name of the service
     */
    public PoseSvcClient(SpotPoseControl parentControl, String nodeName, String serviceName) {
        super(nodeName, serviceName, PosedStand._TYPE);
        setServiceResponseListener(this);
        this.control = parentControl;
    }

    @Override
    public void onSuccess(PosedStandResponse posedStandResponse) {

        ICommandStatus status;

        if (posedStandResponse.getSuccess()) {

            status = CommandStatus.completed(control.getCommandId());

        } else {

            status = CommandStatus.failed(control.getCommandId(), posedStandResponse.getMessage());
        }

        control.getEventHandler().publish(new CommandStatusEvent(control, 200L, status));
    }

    @Override
    public void onFailure(RemoteException e) {
        ICommandStatus status;
        status = CommandStatus.failed(control.getCommandId(), e.getMessage());
        control.getEventHandler().publish(new CommandStatusEvent(control, 200L, status));
    }
}
