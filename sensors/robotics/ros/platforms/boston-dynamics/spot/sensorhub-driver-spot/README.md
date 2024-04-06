# Boston Dynamics SPOT

Sensor adapter for Boston Dynamics SPOT.

"Spot is a quadruped (four-legged) robot capable of mobility on a variety of terrains. Spot uses multiple sensors and
three motors in each leg to navigate indoor and outdoor environments, maintain balance, and attain postures.

Spot can be operated manually by remote control, or automatically using its perception and guidance systems to follow
predefined routes. It can be equipped with a variety of sensors and other payloads to tailor Spot for specific
applications such as thermal sensing, site inspections, and more.
"

Details: https://support.bostondynamics.com/s/article/About-the-Spot-robot

Specs: https://support.bostondynamics.com/s/article/Robot-specifications

## Prerquisites:

Usage of this driver for integration with Spot requires Clearpath Robotics ROS package.  You can find out more by 
reading this [article](https://clearpathrobotics.com/blog/2020/09/clearpath-robotics-releases-ros-package-for-boston-dynamics-spot-robot/).

The Clearpath Robotics ROS package can be found [here](https://github.com/heuristicus/spot_ros).

## Building

The following will have to be added to the project's settings.gradle for the necessary OSH drivers to be recognized for builds:

    include 'sensorhub-spot-messages'
    project(':sensorhub-spot-messages').projectDir = "$gradle.oshAddonsDir/sensors/robotics/ros/platforms/boston-dynamics/spot/sensorhub-spot-messages" as File

    include 'sensorhub-driver-spot'
    project(':sensorhub-driver-spot').projectDir = "$gradle.oshAddonsDir/sensors/robotics/ros/platforms/boston-dynamics/spot/sensorhub-driver-spot" as File

The following will have to be added to project's build.gradle to package the driver as part of the deployment zip file:

    implementation project(':sensorhub-driver-spot')


## Configuration

Configuring the sensor requires:
Select ```Sensors``` from the left hand accordion control and right click for context-sensitive menu in accordion
control

- **Module Name:** A name for the instance of the driver
- **Description:** Optional short description of the system
- **SensorML URL:** Optional, if available, the path to a SensorML document for the system
- **Serial Number:** The platforms serial number, or a unique identifier
- **Auto Start:** Check the box to start this module when OSH node is launched

The driver has a number of configurable options across several tabs aiming to logically
group the configurable options. The following describe each tab and their configurable properties.

#### ROS

These parameters establish the location of the ROS Master and how the local system connects to it.

- **Master URI:** The URI of the ROS Master
    - **Default Value:** http://127.0.0.1:11311
- **Local Host Address:** The IP Address of the machine hosting the ROS node(s) participating in the ROS Masters ROS
  Graph
    - **Default Value:** 127.0.0.1
- **Local ROS Master:** This option spools up a local ROS Master Node, it should be used only for testing and left unchecked
  when connecting to a physical Spot hosting Clearpath Robotics Spot package as Spot will host the ROS Master.
  - **Default Value:** Unchecked

#### Lease Controls

The collection of services through which establishing control of the platform is performed and the specific
services necessary to manage control of the platform. A description is given for each along with the default
value for the topic to establish communications

- **Lease Claim Service:** A body lease gives the holder the ability to command the spot to make actions in the world
    - **Default Value:** /spot/claim
- **Lease Release Service:** Release the lease
    - **Default Value:** /spot/release
- **Gentle eStop Service:** Stops all motion of the robot and commands it to sit. This stop does not have to be released
    - **Default Value:** /spot/estop/gentle
- **Hard eStop Service:** The hard emergency stop will kill power to the motors and must be released before you can send
  any commands to the robot. Requires call to eStop Release to allow further control
    - **Default Value:** /spot/estop/hard
- **eStop Release Service:** Allows further control after a hard e-stop
    - **Default Value:** /spot/estop/release
- **Cancel Command Service:** This service stops any command that is currently being executed
    - **Default Value:** /spot/stop
- **Locked Cancel Command Service:** Stops the current command and also disallows any further motion of the robot
    - **Default Value:** /spot/locked_stop
- **Unlock Cancel Command Service:** Allows motion after a Locked Cancel Command
    - **Default Value:** /spot/allow_motion
- **Motor Power Service:** Enables motor power. Needs to be enabled once you have a Lease on the body
    - **Default Value:** /spot/power_on
- **Motor Power Service:** Disables motor power. Needs to be disabled power to motors and shut down
    - **Default Value:** /spot/power_off
- **Stand Service:** Stand SPOT up from resting/powered off mode
    - **Default Value:** /spot/stand

#### Body Pose Controls

Service and publishers necessary to send commands for the platform to assume a pose. A description is given for each
along
with the default value for the topic to establish communications

- **Body Pose Service:** The static body pose changes the body position only when the robot is stationary
    - **Default Value:** /spot/posed_stand
- **In Motion or Idle Body Pose Topic:** Move the robot by specifying a pose either while robot is stationary or in
  motion
    - **Default Value:** /spot/in_motion_or_idle_body_pose
- **Go to Pose Topic:** Move the robot by specifying a pose
    - **Default Value:** /spot/motion_or_idle_body_pose

#### Motion Controls

Service and publishers necessary to send commands for the platform to execute commanded movements
A description is given for each along with the default value for the topic to establish communications

- **Motion Command Topic:** Command to control motion
    - **Default Value:** /spot/cmd_vel
- **Velocity Limit Service:** Set a velocity limit in m/s for the motion to poses
    - **Default Value:** /spot/velocity_limit
- **Command Rate:** Will publish command velocity message at a specific rate in Hz
    - **Default Value:** 10

#### Status Outputs

List of topics for subscription to receive status messages from the platform. Each status can be enabled
or disabled. If enabling or disabling a particular status will require a restart of the sensor module.
A description is given for each along with the default value for the topic to establish communications

##### Metrics

- **Metrics Topic:** General metrics for the system like distance walked
    - **Default Value:** /spot/status/metrics

##### Lease Status

- **Lease Status Topic:** A list of what leases are held on the system
    - **Default Value:** /spot/status/leases

##### Odometry

- **Odometry Topic:** The estimated odometry of the platform
    - **Default Value:** /spot/odometry/twist

##### Feet Position

- **Feet Position Topic:** The status and position of each foot
    - **Default Value:** /spot/status/feet

##### eStop Status

- **Emergency Stop Status Topic:** The status of the eStop system
    - **Default Value:** /spot/status/estop

##### WiFi Status

- **WiFi Status Topic:** Status of the wifi system
    - **Default Value:** /spot/status/wifi

##### Power State

- **Power State Topic:** General power information
    - **Default Value:** /spot/status/power_state

##### Battery Status

- **Battery Status Topic:** Information for the battery and all cells in the system
    - **Default Value:** /spot/status/battery_states

##### Behavior Faults

- **Behavior Faults Topic:** A listing of behavior faults in the system
    - **Default Value:** /spot/status/behavior_faults

##### System Faults

- **System Faults Topic:** A listing of system faults in the system
    - **Default Value:** /spot/status/system_faults

##### Feedback

- **Feedback Topic:** Feedback from the Spot robot
    - **Default Value:** /spot/status/feedback

##### Mobility Parameters

- **Mobility Params Topic:** Describes the current state of the mobility parameters defining the motion behaviour of the
  robot
    - **Default Value:** /spot/status/mobility_params

#### Camera Outputs

These configuration parameters enable or disable the outputs of observations from the associated RGB cameras mounted on
the platform.
They also specify the topics to subscribe to in order to receive the respective sensors observations.
A description is given for each along with the default value for the topic to establish communications

##### Image Sensor Resolution

Specify the resolution of the sensor frame

- **Width:** Sensor feed frame Width
    - **Default Value:** 640
- **Height:** Sensor feed frame Height
    - **Default Value:** 480

##### Front Left Camera

Camera sensor feed

- **Frame Topic:** Frame sensor feed
    - **Default Value:** /spot/camera/frontleft/image

##### Front Right Camera

Camera sensor feed

- **Frame Topic:** Frame sensor feed
    - **Default Value:** /spot/camera/frontright/image

##### Left Camera

Camera sensor feed

- **Frame Topic:** Frame sensor feed
    - **Default Value:** /spot/camera/left/image

##### Right Camera

Camera sensor feed

- **Frame Topic:** Frame sensor feed
    - **Default Value:** /spot/camera/right/image

##### Back Camera

Camera sensor feed

- **Frame Topic:** Frame sensor feed
    - **Default Value:** /spot/camera/back/image

#### Depth Camera Outputs

These configuration parameters enable or disable the outputs of observations from the associated depth cameras mounted
on the platform.
They also specify the topics to subscribe to in order to receive the respective sensors observations.
A description is given for each along with the default value for the topic to establish communications

##### Sensor Resolution

Specify the resolution of the sensor frame

- **Width:** Sensor feed frame Width
  - **Default Value:** 424
- **Height:** Sensor feed frame Height
  - **Default Value:** 240

##### Front Left Camera

Camera sensor feed

- **Frame Topic:** Frame sensor feed
    - **Default Value:** /spot/depth/frontleft/image

##### Front Right Camera

Camera sensor feed

- **Frame Topic:** Frame sensor feed
    - **Default Value:** /spot/depth/frontright/image

##### Left Camera

Camera sensor feed

- **Frame Topic:** Frame sensor feed
    - **Default Value:** /spot/depth/left/image

##### Right Camera

Camera sensor feed

- **Frame Topic:** Frame sensor feed
    - **Default Value:** /spot/depth/right/image

##### Back Camera

Camera sensor feed

- **Frame Topic:** Frame sensor feed
    - **Default Value:** /spot/depth/back/image

Storage:
Select ```Databases``` from the left hand accordion control and right click for context-sensitive menu in accordion
control
Use a ```System Driver Database``` providing the sensor module as the

- **System UIDs:** Select the identifier(s) for the sensor modules create in configuring sensor step,
  use the + button to select from a list of know sensor modules
- **Auto Start:** Check the box to start this module when OSH node is launched

And then configure the

- **Database Config** using a ```H2 Historical Obs Database``` instance providing the
    - **Storage Path** as the location where the OSH records are to be stored.

Other configuration options are available and may be filled out as necessary including
the ```Automatic Purge Policy``` to maintain a records for a maximum amount of time.