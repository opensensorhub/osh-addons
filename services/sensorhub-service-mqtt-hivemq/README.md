## MQTT Support based on HiveMQ

This module exposes common MQTT resources that can be used by other components (such as SensorThings or SensorWeb API). These resources are:

- Common MQTT API (for creating topics and connecting them to internal OSH queues)
- MQTT server
- MQTT client

The default MQTT server and client implementations provided by this module are based on HiveMQ.


### Login

When OSH access control is on, an MQTT client logs in with the name and the password of an OSH user.

- A user must have a password to log in with a password. The server refuses a CONNECT that has a user name and an empty password or no password.
- The server also refuses a user that has no password in the user registry, for example a user that an OAuth login added.
- A CONNECT without a user name gets the `anonymous` user when `requireAuth` is off.
