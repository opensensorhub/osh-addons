package org.sensorhub.impl.service.federation.oshconnect;

/**
 * Port of oshconnect.csapi4py.constants.ContentTypes.
 */
public enum ContentTypes
{
    JSON("application/json"),
    XML("application/xml"),
    SWE_XML("application/swe+xml"),
    SWE_JSON("application/swe+json"),
    SWE_CSV("application/swe+csv"),
    SWE_BINARY("application/swe+binary"),
    SWE_TEXT("application/swe+text"),
    GEO_JSON("application/geo+json"),
    SML_JSON("application/sml+json"),
    OM_JSON("application/om+json");

    private final String value;

    ContentTypes(String value)
    {
        this.value = value;
    }

    public String value()
    {
        return value;
    }
}
