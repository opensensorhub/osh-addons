package org.sensorhub.impl.service.federation.oshconnect;

/**
 * Observation wire encoding a datastream uses. Chosen at discovery from the
 * datastream's advertised formats and used to route the observation relay:
 * JSON goes through the parse/rebuild path, BINARY through opaque passthrough.
 */
public enum EncodingMode
{
    JSON,
    BINARY
}
