## MQTT Support based on HiveMQ

This module exposes common MQTT resources that can be used by other components (such as SensorThings or SensorWeb API). These resources are:

- Common MQTT API (for creating topics and connecting them to internal OSH queues)
- MQTT server
- MQTT client

The default MQTT server and client implementations provided by this module are based on HiveMQ.


### Wildcard subscriptions

Since version 0.2.0, the bundled HiveMQ config (`config.xml`) allows wildcard topic filters (`+`, `#`).

- At start, the MQTT Server copies the bundled config to its config folder (`configFolder`) if there is no config there.
- The MQTT Server does not change a config that is in that folder.
- A node from an earlier version has a config that does not allow wildcards. Earlier bundled configs did not allow them. The MQTT Server then logs a warning at start.
- To allow wildcards on such a node, set `<wildcard-subscriptions><enabled>true</enabled>` in the config, then start the MQTT Server again.

A wildcard filter can match the topics of resources that the user cannot read. So the server checks each message before it sends it to a client:

- A message on a topic that the client subscribed to exactly is sent. The handler checked the subscription.
- Any other message matched a wildcard filter. The handler of its topic decides if it is sent (`IMqttHandler.canDeliver`).
- A handler accepts wildcard filters only if `IMqttHandler.supportsWildcards` returns true. Otherwise the server rejects them. The default is false.

The check of a wildcard match runs outside the HiveMQ threads. Each message that matches a wildcard filter of a client adds one check for that client.


### Login

When OSH access control is on, an MQTT client logs in with the name and the password of an OSH user.

- A user must have a password to log in with a password. The server refuses a CONNECT that has a user name and an empty password or no password.
- The server also refuses a user that has no password in the user registry, for example a user that an OAuth login added.
- A CONNECT without a user name gets the `anonymous` user when `requireAuth` is off.
