package org.sensorhub.impl.service.federation.events;

/**
 * Port of events.DefaultEventTypes.
 */
public enum DefaultEventTypes
{
    ADD_COMMANDER("add_commander"),
    REMOVE_COMMANDER("remove_commander"),
    ADD_REMOTE_NODE("add_remote_node"),
    REMOVE_REMOTE_NODE("remove_remote_node"),
    ADD_SYSTEM("add_system"),
    REMOVE_SYSTEM("remove_system"),
    ADD_DATASTREAM("add_datastream"),
    REMOVE_DATASTREAM("remove_datastream"),
    ADD_CONTROLSTREAM("add_controlstream"),
    REMOVE_CONTROLSTREAM("remove_controlstream"),
    NEW_OBSERVATION("new_observation"),
    NEW_COMMAND("new_command"),
    NEW_COMMAND_STATUS("new_command_status");

    private final String value;

    DefaultEventTypes(String value)
    {
        this.value = value;
    }

    public String value()
    {
        return value;
    }
}
