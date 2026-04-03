# Universal Controller Driver

By: Alex Almanza (https://www.github.com/earocorn)

Sensor adapter for HID Compliant gamepads, Wii Remotes w/ or w/o nunchuk extension.

## Repositories

**universal controller driver dev** - The osh-node-dev-template I was working from
https://github.com/earocorn/osh-node-dev-template/tree/controller-dev

**controllerlibrary** - A java library I created from JInput and MoteJ that encompasses connectivity to Wii and HID devices.
https://github.com/earocorn/controllerlibrary/tree/librarydev

## Configuration

Configuring the sensor requires:
Select ```Sensors``` from the left hand accordion control and right click for context sensitive menu in accordion control
- **Module Name:** A name for the instance of the driver
- **Serial Number:** The platforms serial number, or a unique identifier
- **Auto Start:** Check the box to start this module when OSH node is launched

## Post-Build Setup
- Be sure to include the `armlib` and `jinputlibs` folders in your node's working directory and include the following argument in your `launch.sh` file
- `-Djava.library.path="./jinputlibs"` or `-Djava.library.path="./armlib"` (for Raspberry Pi)
- **NOTE:** If BlueCove is not recognized for WiiMotes please try using other versions of `bluecove` and `bluecove-gpl` that are in the `altlibs` folder.
  - Also, to ensure you are able to find bluetooth gamepads and WiiMotes, please install `bluez`, `bluez-utils`, and `blueman`

## Supported Platforms

The Universal Controller Driver has the most controller support on Linux machines or 32-bit Raspberry Pi operating systems/

## Launching
1. Launch with the proper command line arguments specifying the java library path of `jinputlibs` or `armlib` for RaspberryPi.

2. Add the Universal Controller Driver module to Sensors.

3. Configuration options:
    - **Controller Types** - Types of controllers to search for. Options are GAMEPAD and WIIMOTE.

    - **Primary Controller Index** - Index of the primary controller from the list of connected controllers in the "gamepads" array.
   
    - **Primary Control Stream Index** - Index of the primary control stream.

    - **Number of Control Streams** - Number of control streams for use with SensorML process chain.

    - **Polling Rate** - Polling rate in ms for the outputs of connected controllers.

    - **Controller Search Time** - Time in seconds to search for controllers. Needed mainly for WiiMotes which need to be connected over bluetooth. The driver will search for WiiMotes during the INITIALIZING stage for the provided amount of search time.

    - **Controller Layer Config** - Option to add hotkeys or controller layering to the driver. In order to use, specify the component name on the controller given by the controller outputs. The common name for most controllers' Home button is "Mode" but others are more intuitive such as "A", "B", "X", "Select", "Start". If multiple components are added, they must be simulatenously pressed in order to trigger the desired hotkey event. Hotkey events include:
        - *CYCLES_PRIMARY_CONTROLLER* - Upon button press, the primary controller will cycle through each other controller. This button will cycle the primary controller whether or not the controller is the primary controller.
        - *OVERRIDES_PRIMARY_CONTROLLER* - Upon button press, the current controller becomes the primary controller.
        - *PASS_PRIMARY_TO_NEXT* - Upon button press, if the current controller is the primary controller, the primary controller will become the next controller in the array.
        - *CYCLES_PRIMARY_CONTROL_STREAM* - Upon button press, the primary control stream will be cycled.
          A controller must be specified to which the hotkey will be applied. This specification is through the controller index. By default it is 0, the first controller in the array.

4. Once starting the node, the driver will search for WiiMotes during the INITIALIZING stage for the specified amount of Search Time. **IMPORTANT** In order to connect during this discovery period, press 1 and 2 on WiiMote(s) or the red "Sync" button near the batteries. After the node is running you may add or disconnect the Nunchuk extension to connected WiiMotes.

**Suggested setup**
Add controller hotkeys to each controller to either cycle primary controller or override primary controller. The process chain works by using controller outputs of only the primary controller. Be sure to add these hotkeys before connecting the WiiMote.

## Common Issues and Fixes

 - `BlueCove native library version mismatch` or `BlueCove library bluecove not available`
   - To solve this, replace the `bluecove-2.1.1-SNAPSHOT.jar` and `bluecove-gpl-2.1.1-SNAPSHOT.jar` with older version from the `altlibs` folder
 - Driver is unable to find HID gamepads with the GAMEPAD setting
   - Ensure that the following argument is included in your `launch.sh` file:
   - `-Djava.library.path="./jinputlibs"` or `-Djava.library.path="./armlib"` (for Raspberry Pi)