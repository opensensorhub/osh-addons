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

import org.ros.concurrent.CancellableLoop;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Implementation of a ROS Publisher.  Nodes of this type publish a message to subcribers within
 * the ROS ecosystem.
 *
 * @param <T> The type message being published
 * @author Nicolas Garay
 * @since Mar 20, 2023
 */
public class RosPublisherNode<T> extends AbstractNodeMain {

    /**
     * The topic name
     */
    private final String topic;

    /**
     * The node name
     */
    private final String nodeName;

    /**
     * String representation of the message type
     */
    private final String messageType;

    /**
     * The ROS publisher
     */
    protected Publisher<T> publisher;

    /**
     * Message queue of messages to publish
     */
    private final Queue<T> messagesQueue = new ConcurrentLinkedQueue<>();

    /**
     * Constructor
     *
     * @param nodeName    name of the node
     * @param topic       name of the topic to publish
     * @param messageType the string representation of the message type being published
     */
    public RosPublisherNode(final String nodeName, final String topic, final String messageType) {

        this.topic = topic;
        this.nodeName = nodeName;
        this.messageType = messageType;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(this.nodeName);
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {

        super.onStart(connectedNode);

        publisher = connectedNode.newPublisher(this.topic, this.messageType);

        connectedNode.executeCancellableLoop(new CancellableLoop() {
            @Override
            protected void loop() throws InterruptedException {

                T currentMessage;

                synchronized (messagesQueue) {

                    while (messagesQueue.isEmpty()) {

                        messagesQueue.wait();
                    }

                    currentMessage = messagesQueue.remove();
                }

                publisher.publish(currentMessage);
            }
        });
    }

    /**
     * Creates a message buffer for the type messages
     *
     * @return a message buffer of the correct type
     */
    public T getNewMessageBuffer() {

        return publisher.newMessage();
    }

    /**
     * Publishes the message on the registered topic
     *
     * @param message the message to publish
     * @return the id of the goal
     */
    public void publishMessage(T message) {

        synchronized (messagesQueue) {

            messagesQueue.add(message);

            messagesQueue.notifyAll();
        }
    }
}
