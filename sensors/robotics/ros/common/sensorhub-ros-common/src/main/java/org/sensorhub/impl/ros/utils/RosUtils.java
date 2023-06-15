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

package org.sensorhub.impl.ros.utils;

import org.ros.node.NodeConfiguration;

import java.net.URI;

/**
 * Utility functions to simplify ROS operations
 *
 * @author Nicolas Garay
 * @since Mar 20, 2023
 */
public class RosUtils {

    /**
     * Constructor. Hidden.
     */
    private RosUtils() {
    }

    /**
     * Creates a configuration for a ROS Node
     *
     * @param hostIp       the host IP address
     * @param name         the name of the node being created
     * @param rosMasterUri the URI of the ROS Master
     * @return a new configuration for a ROS Node
     */
    public static final NodeConfiguration getNodeConfiguration(final String hostIp, final String name, final URI rosMasterUri) {

        final NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(hostIp);
        nodeConfiguration.setNodeName(name);
        nodeConfiguration.setMasterUri(rosMasterUri);

        return nodeConfiguration;
    }
}
