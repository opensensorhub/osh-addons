/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 - 2024 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.spot.config;

import org.sensorhub.api.config.DisplayInfo;

/**
 * Configuration settings for the Boston Dynamics SPOT driver status information exposed via the OpenSensorHub Admin panel.
 * <p>
 * Configuration settings take the form of
 * <code>
 * DisplayInfo(desc="Description of configuration field to show in UI")
 * public Type configOption;
 * </code>
 * <p>
 * Containing an annotation describing the setting and if applicable its range of values
 * as well as a public access variable of the given Type
 *
 * @author Nick Garay
 * @since Sept. 25, 2023
 */
public class SpotStatusConfig {

    @DisplayInfo.Required
    @DisplayInfo(label = "Metrics", desc = "General metrics for the system like distance walked")
    public MetricsOutput metricsOutput = new MetricsOutput();

    @DisplayInfo.Required
    @DisplayInfo(label = "Lease Status", desc = "A list of what leases are held on the system")
    public LeaseStatusOutput leaseStatusOutput = new LeaseStatusOutput();

    @DisplayInfo.Required
    @DisplayInfo(label = "Odometry", desc = "The estimated odometry of the platform")
    public OdometryStatusOutput odometryStatusOutput = new OdometryStatusOutput();

    @DisplayInfo.Required
    @DisplayInfo(label = "Feet Position", desc = "The status and position of each foot")
    public FeetPositionStatusOutput feetPositionStatusOutput = new FeetPositionStatusOutput();

    @DisplayInfo.Required
    @DisplayInfo(label = "eStop Status", desc = "The status of the eStop system")
    public EStopStatusOutput eStopStatusOutput = new EStopStatusOutput();

    @DisplayInfo.Required
    @DisplayInfo(label = "WiFi Status", desc = "Status of the wifi system")
    public WiFiStatusOutput wiFiStatusOutput = new WiFiStatusOutput();

    @DisplayInfo.Required
    @DisplayInfo(label = "Power State", desc = "General power information")
    public PowerStateStatusOutput powerStateStatusOutput = new PowerStateStatusOutput();

    @DisplayInfo.Required
    @DisplayInfo(label = "Battery Status", desc = "Information for the battery and all cells in the system")
    public BatteryStatusOutput batteryStatusOutput = new BatteryStatusOutput();

    @DisplayInfo.Required
    @DisplayInfo(label = "Behavior Faults", desc = "A listing of behavior faults in the system")
    public BehaviorFaultsStatusOutput behaviorFaultsStatusOutput = new BehaviorFaultsStatusOutput();

    @DisplayInfo.Required
    @DisplayInfo(label = "System Faults", desc = "A listing of system faults in the system")
    public SystemFaultsStatusOutput systemFaultsStatusOutput = new SystemFaultsStatusOutput();

    @DisplayInfo.Required
    @DisplayInfo(label = "Feedback", desc = "Feedback from the Spot robot")
    public FeedbackStatusOutput feedbackStatusOutput = new FeedbackStatusOutput();

    @DisplayInfo.Required
    @DisplayInfo(label = "Mobility Parameters", desc = "Describes the current state of the mobility parameters defining the motion behaviour of the robot")
    public MobilityParamsStatusOutput mobilityParamsStatusOutput = new MobilityParamsStatusOutput();

    public static class MetricsOutput {

        @DisplayInfo.Required
        @DisplayInfo(label = "Metrics Topic", desc = "General metrics for the system like distance walked")
        public String metricsTopic = "/spot/status/metrics";

        @DisplayInfo.Required
        @DisplayInfo(label = "Enable", desc = "Enables the output, allowing publication of metrics")
        public boolean enabled = false;
    }

    public static class LeaseStatusOutput {

        @DisplayInfo.Required
        @DisplayInfo(label = "Lease Status Topic", desc = "A list of what leases are held on the system")
        public String leaseStatusTopic = "/spot/status/leases";

        @DisplayInfo.Required
        @DisplayInfo(label = "Enable", desc = "Enables the output, allowing publication of lease information")
        public boolean enabled = false;
    }

    public static class OdometryStatusOutput {

        @DisplayInfo.Required
        @DisplayInfo(label = "Odometry Topic", desc = "The estimated odometry of the platform")
        public String odometryTopic = "/spot/odometry/twist";

        @DisplayInfo.Required
        @DisplayInfo(label = "Enable", desc = "Enables the output, allowing publication of odometry information")
        public boolean enabled = false;
    }

    public static class FeetPositionStatusOutput {

        @DisplayInfo.Required
        @DisplayInfo(label = "Feet Position Topic", desc = "The status and position of each foot")
        public String feetPositionTopic = "/spot/status/feet";

        @DisplayInfo.Required
        @DisplayInfo(label = "Enable", desc = "Enables the output, allowing publication of feet position information")
        public boolean enabled = false;
    }

    public static class EStopStatusOutput {

        @DisplayInfo.Required
        @DisplayInfo(label = "Emergency Stop Status Topic", desc = "The status of the eStop system")
        public String eStopStatusTopic = "/spot/status/estop";

        @DisplayInfo.Required
        @DisplayInfo(label = "Enable", desc = "Enables the output, allowing publication of Emergency Stop information")
        public boolean enabled = false;
    }

    public static class WiFiStatusOutput {

        @DisplayInfo.Required
        @DisplayInfo(label = "WiFi Status Topic", desc = "Status of the wifi system")
        public String wifiStatusTopic = "/spot/status/wifi";

        @DisplayInfo.Required
        @DisplayInfo(label = "Enable", desc = "Enables the output, allowing publication of WiFi status information")
        public boolean enabled = false;
    }

    public static class PowerStateStatusOutput {

        @DisplayInfo.Required
        @DisplayInfo(label = "Power State Topic", desc = "General power information")
        public String powerStateTopic = "/spot/status/power_state";

        @DisplayInfo.Required
        @DisplayInfo(label = "Enable", desc = "Enables the output, allowing publication of power state information")
        public boolean enabled = false;
    }

    public static class BatteryStatusOutput {

        @DisplayInfo.Required
        @DisplayInfo(label = "Battery Status Topic", desc = "Information for the battery and all cells in the system")
        public String batteryStatusTopic = "/spot/status/battery_states";

        @DisplayInfo.Required
        @DisplayInfo(label = "Enable", desc = "Enables the output, allowing publication of battery status information")
        public boolean enabled = false;
    }

    public static class BehaviorFaultsStatusOutput {

        @DisplayInfo.Required
        @DisplayInfo(label = "Behavior Faults Topic", desc = "A listing of behavior faults in the system")
        public String behaviorFaultsTopic = "/spot/status/behavior_faults";

        @DisplayInfo.Required
        @DisplayInfo(label = "Enable", desc = "Enables the output, allowing publication of behavior information")
        public boolean enabled = false;
    }

    public static class SystemFaultsStatusOutput {

        @DisplayInfo.Required
        @DisplayInfo(label = "System Faults Topic", desc = "A listing of system faults in the system")
        public String systemFaultsTopic = "/spot/status/system_faults";

        @DisplayInfo.Required
        @DisplayInfo(label = "Enable", desc = "Enables the output, allowing publication of system faults information")
        public boolean enabled = false;
    }

    public static class FeedbackStatusOutput {

        @DisplayInfo.Required
        @DisplayInfo(label = "Feedback Topic", desc = "Feedback from the Spot robot")
        public String feedbackTopic = "/spot/status/feedback";

        @DisplayInfo.Required
        @DisplayInfo(label = "Enable", desc = "Enables the output, allowing publication of feedback information")
        public boolean enabled = false;
    }

    public static class MobilityParamsStatusOutput {

        @DisplayInfo.Required
        @DisplayInfo(label = "Mobility Params Topic", desc = "Describes the current state of the mobility parameters defining the motion behaviour of the robot")
        public String mobilityParamsTopic = "/spot/status/mobility_params";

        @DisplayInfo.Required
        @DisplayInfo(label = "Enable", desc = "Enables the output, allowing publication of mobility parameters information")
        public boolean enabled = false;
    }
}
