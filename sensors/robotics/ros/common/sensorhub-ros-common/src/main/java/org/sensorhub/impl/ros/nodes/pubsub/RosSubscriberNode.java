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

package org.sensorhub.impl.ros.nodes.pubsub;

import org.sensorhub.impl.ros.output.BaseRosOutput;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;

/**
 * Implementation of a ROS Subscriber.  Nodes of this type subscribe to topics to receive messages from
 * the ROS ecosystem.
 *
 * @author Nicolas Garay
 * @since Mar 20, 2023
 */
public class RosSubscriberNode extends AbstractNodeMain {

    /**
     * The topic being subscribed to
     */
    private final String topic;

    /**
     * Name of the node
     */
    private final String nodeName;

    /**
     * String representation of the message type
     */
    private final String messageType;

    /**
     * The OSH output class that will handle messages received
     */
    private final BaseRosOutput<?> output;

    /**
     * Constructor
     *
     * @param nodeName        Name of the node
     * @param topic           The topic being subscribed to
     * @param messageType     String representation of the message type
     * @param rosSensorOutput The OSH output class that will handle messages received
     */
    public RosSubscriberNode(final String nodeName, final String topic, final String messageType, final BaseRosOutput<?> rosSensorOutput) {

        this.topic = topic;
        this.nodeName = nodeName;
        this.messageType = messageType;
        this.output = rosSensorOutput;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(this.nodeName);
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {

        super.onStart(connectedNode);

        final Subscriber<?> subscriber = connectedNode.newSubscriber(this.topic, this.messageType);

        subscriber.addMessageListener(output::onNewMessage);
    }
}
