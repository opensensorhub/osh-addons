#### MQTT Service API

Common MQTT API that should be used by OSH drivers or services wishing to receive or transmit data
via the embedded MQTT server. Using the API allows switching the underlying MQTT server implementation
w/o any change to other modules.

This API is meant to be implemented by specific MQTT implementations (e.g. Moquette, HiveMQ, etc.)

