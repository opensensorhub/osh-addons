/*
 * The contents of this file are subject to the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one
 * at http://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the License.
 *
 * Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 *
 */

package org.sensorhub.impl.ros.nodes.service;

import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceServer;

/**
 * An abstract class for the creation of a ROS Server nodes to particate in a ROS ecosystem.
 *
 * @param <RequestType>  The type of request being supported
 * @param <ResponseType> The type of the responses to be sent
 * @author Nicolas Garay
 * @since Mar 20, 2023
 */
public abstract class RosServerNode<RequestType, ResponseType> extends AbstractNodeMain {

    /**
     * Name of the node
     */
    private final String nodeName;

    /**
     * Name of the service
     */
    private final String serviceName;

    /**
     * Type of the service being created
     */
    private final String serviceType;

    /**
     * Constructor
     *
     * @param nodeName    Name of the node
     * @param serviceName Name of the service
     * @param serviceType Type of the service being created
     */
    public RosServerNode(final String nodeName, final String serviceName, final String serviceType) {

        this.nodeName = nodeName;
        this.serviceName = serviceName;
        this.serviceType = serviceType;
    }

    @Override
    public GraphName getDefaultNodeName() {

        return GraphName.of(nodeName);
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {

        final ServiceServer<RequestType, ResponseType> serviceServer = connectedNode.newServiceServer(this.serviceName, serviceType, this::onMessageCallback);
    }

    /**
     * Provides a mechanism for derived classes to handle the request and response
     *
     * @param request  The request received
     * @param response The response to send
     */
    protected abstract void onMessageCallback(RequestType request, ResponseType response);
}
