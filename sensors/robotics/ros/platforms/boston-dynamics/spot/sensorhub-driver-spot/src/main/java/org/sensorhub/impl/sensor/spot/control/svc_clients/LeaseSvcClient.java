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
import org.sensorhub.impl.sensor.spot.control.SpotLeaseControl;
import std_srvs.TriggerRequest;
import std_srvs.TriggerResponse;

/**
 * ROS Service client for manage lease claim commands.
 *
 * @author Nick Garay
 * @since Sept. 25, 2023
 */
public class LeaseSvcClient extends RosServiceClient<TriggerRequest, TriggerResponse> implements ServiceResponseListener<TriggerResponse> {

    /**
     * The control owning this service client
     */
    private final SpotLeaseControl control;

    /**
     * Constructor
     *
     * @param parentControl the parent control for this service client
     * @param nodeName      name of the node
     * @param serviceName   name of the service
     * @param serviceType   the type of the service being invoked
     */
    public LeaseSvcClient(SpotLeaseControl parentControl, String nodeName, String serviceName, String serviceType) {
        super(nodeName, serviceName, serviceType);
        setServiceResponseListener(this);
        this.control = parentControl;
    }

    @Override
    public void onSuccess(TriggerResponse response) {

        ICommandStatus status;

        if (response.getSuccess()) {

            status = CommandStatus.completed(control.getCommandId());

        } else {

            status = CommandStatus.failed(control.getCommandId(), response.getMessage());
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
