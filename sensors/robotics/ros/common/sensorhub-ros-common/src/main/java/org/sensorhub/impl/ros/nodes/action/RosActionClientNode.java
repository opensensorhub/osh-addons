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

package org.sensorhub.impl.ros.nodes.action;

import actionlib_msgs.GoalID;
import com.github.ekumen.rosjava_actionlib.ActionClient;
import com.github.ekumen.rosjava_actionlib.ActionClientListener;
import org.ros.internal.message.Message;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;

/**
 * Implementation of a ROS Action Client.  Action Clients are used to set goals and receive feedback
 * on progress towards the assigned goal.  The goal is also cancellable or overrideable.  In case the
 * goal cannot be met the action service will notify client that is the case.
 *
 * @param <Goal>     The type of the goal
 * @param <Feedback> The type of the feedback expected to be received
 * @param <Result>   The type of the result
 * @author Nicolas Garay
 * @since Mar 20, 2023
 */
public class RosActionClientNode<Goal extends Message, Feedback extends Message, Result extends Message> extends AbstractNodeMain {

    /**
     * Name of the action
     */
    private final String actionName;

    /**
     * Name of the node
     */
    private final String nodeName;

    /**
     * String representation of the goal type
     */
    private final String goalType;

    /**
     * String representation of the feedback type
     */
    private final String feedbackType;

    /**
     * String representation of the result type
     */
    private final String resultType;

    /**
     * A listener for feedback and results
     */
    private final ActionClientListener<Feedback, Result> actionClientListener;

    /**
     * Instance of the action client to create specifying Goal, Feedback, and Result message classes
     */
    private ActionClient<Goal, Feedback, Result> actionClient;

    /**
     * Constructor
     *
     * @param nodeName             Name of the node
     * @param actionName           Name of the action
     * @param goalType             String representation of the goal type message
     * @param feedbackType         String representation of the feedback type message
     * @param resultType           String representation of the result type message
     * @param actionClientListener listener for feedback and results
     */
    public RosActionClientNode(final String nodeName, final String actionName,
                               final String goalType, final String feedbackType,
                               final String resultType,
                               ActionClientListener<Feedback, Result> actionClientListener) {

        this.actionName = actionName;
        this.nodeName = nodeName;
        this.goalType = goalType;
        this.feedbackType = feedbackType;
        this.resultType = resultType;
        this.actionClientListener = actionClientListener;
    }

    @Override
    public GraphName getDefaultNodeName() {

        return GraphName.of(this.nodeName);
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        super.onStart(connectedNode);

        actionClient = new ActionClient<>(connectedNode, actionName, goalType, feedbackType, resultType);

        actionClient.attachListener(actionClientListener);

        actionClient.waitForActionServerToStart();
    }

    /**
     * Creates a message buffer for the goal type messages
     *
     * @return a message buffer of the correct type
     */
    public Goal getNewMessageBuffer() {

        return actionClient.newGoalMessage();
    }

    /**
     * Publishes the goal to the goal server
     *
     * @param goal the goal message to publish
     * @return the id of the goal
     */
    public GoalID publishGoal(Goal goal) {

        actionClient.sendGoal(goal);

        return actionClient.getGoalId(goal);
    }

    /**
     * Cancels a goal by id, notifies the action server to terminate the goal
     *
     * @param goalId the id of the goal to cancel
     */
    public void cancelGoal(GoalID goalId) {

        actionClient.sendCancel(goalId);
    }
}
