package org.sensorhub.impl.service.federation.oshconnect;

/**
 * Port of oshconnect.csapi4py.constants.APIResourceTypes.
 *
 * The enum value mirrors the Python string value; {@link #term()} returns the
 * matching URL/topic endpoint segment (resource_type_to_endpoint in
 * default_api_helpers.py / APITerms in constants.py).
 */
public enum APIResourceTypes
{
    ROOT(""),
    COLLECTION("Collection"),
    COMMAND("Command"),
    COMPONENT("Component"),
    CONTROL_CHANNEL("ControlChannel"),
    DATASTREAM("Datastream"),
    DEPLOYMENT("Deployment"),
    OBSERVATION("Observation"),
    PROCEDURE("Procedure"),
    PROPERTY("Property"),
    SAMPLING_FEATURE("SamplingFeature"),
    SYSTEM("System"),
    SYSTEM_EVENT("SystemEvent"),
    SYSTEM_HISTORY("SystemHistory"),
    STATUS("Status"),
    SCHEMA("Schema");

    private final String value;

    APIResourceTypes(String value)
    {
        this.value = value;
    }

    public String value()
    {
        return value;
    }

    /**
     * Mirror of resource_type_to_endpoint() for the resource types the broker uses.
     */
    public String term()
    {
        switch (this)
        {
            case SYSTEM: return "systems";
            case COLLECTION: return "collections";
            case CONTROL_CHANNEL: return "controlstreams";
            case COMMAND: return "commands";
            case DATASTREAM: return "datastreams";
            case OBSERVATION: return "observations";
            case SYSTEM_EVENT: return "systemEvents";
            case SAMPLING_FEATURE: return "samplingFeatures";
            case PROCEDURE: return "procedures";
            case PROPERTY: return "properties";
            case SYSTEM_HISTORY: return "history";
            case DEPLOYMENT: return "deployments";
            case STATUS: return "status";
            case SCHEMA: return "schema";
            default: throw new IllegalArgumentException("Invalid resource type");
        }
    }

    /**
     * Mirror of determine_parent_type() for the resource types the broker uses.
     */
    public APIResourceTypes parentType()
    {
        switch (this)
        {
            case SYSTEM: return SYSTEM;
            case CONTROL_CHANNEL: return SYSTEM;
            case COMMAND: return CONTROL_CHANNEL;
            case DATASTREAM: return SYSTEM;
            case OBSERVATION: return DATASTREAM;
            case SYSTEM_EVENT: return SYSTEM;
            case SAMPLING_FEATURE: return SYSTEM;
            default: return null;
        }
    }
}
