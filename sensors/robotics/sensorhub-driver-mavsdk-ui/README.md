# MAVLink Communication Support Custom UI

Custom UI components for MAVLink Driver

## Custom Panel Configuration

Ensure to enable the UI by adding the custom UI config to the ```customPanels``` section of the ```AdminUiConfig```
block.

        {
          "objClass": "org.sensorhub.ui.AdminUIConfig",
          "widgetSet": "org.sensorhub.ui.SensorHubWidgetSet",
          "bundleRepoUrls": [
            "https://cloud.georobotix.io/addons/index.xml"
          ],
          "customPanels": [
            {
              "objClass": "org.sensorhub.ui.CustomUIConfig",
              "configClass": "org.sensorhub.impl.sensor.mavsdk.UnmannedConfig",
              "uiClass": "org.sensorhub.impl.sensor.mavsdk.ui.MavLinkUI"
            }
          ],
          "customForms": [],
          "enableLandingPage": false,
          "id": "7219eb9f-b591-4c2c-9ad9-4b63a29a1c4a",
          "autoStart": true,
          "moduleClass": "org.sensorhub.ui.AdminUIModule",
          "name": "Admin UI"
        }
