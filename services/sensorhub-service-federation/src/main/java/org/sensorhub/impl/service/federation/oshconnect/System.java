package org.sensorhub.impl.service.federation.oshconnect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

/**
 * Port of oshconnect.streamableresource.System for the surface the broker uses:
 * datastream/control-stream discovery, mirroring inserts, and resource access.
 */
public class System
{
    private String label;
    private String urn;
    private Node parentNode;
    private String resourceId;
    private SystemResource underlyingResource;

    private final List<Datastream> datastreams = new ArrayList<>();
    private final List<ControlStream> controlChannels = new ArrayList<>();

    private System()
    {
    }

    /**
     * Build a {@link System} from a parsed {@link SystemResource}, handling both
     * the GeoJSON form ({@code properties.name}/{@code properties.uid}) and the
     * SML form ({@code label}/{@code uniqueId}).
     */
    public static System fromResource(SystemResource systemResource, Node parentNode)
    {
        System sys = new System();
        if (systemResource.hasProperties())
        {
            JsonObject props = systemResource.getProperties();
            sys.label = props.has("name") && !props.get("name").isJsonNull() ? props.get("name").getAsString() : null;
            sys.urn = props.has("uid") && !props.get("uid").isJsonNull() ? props.get("uid").getAsString() : null;
        }
        else
        {
            sys.label = systemResource.getLabel();
            sys.urn = systemResource.getUid();
        }
        sys.resourceId = systemResource.getSystemId();
        sys.parentNode = parentNode;
        sys.underlyingResource = systemResource;
        return sys;
    }

    public String getUrn()
    {
        return urn;
    }

    public SystemResource getSystemResource()
    {
        return underlyingResource;
    }

    public void setParentNode(Node node)
    {
        this.parentNode = node;
    }

    public List<Datastream> getDatastreams()
    {
        return datastreams;
    }

    public List<ControlStream> getControlChannels()
    {
        return controlChannels;
    }

    /**
     * GET {@code /systems/{id}/datastreams}, then for each fetch the SWE+JSON
     * record schema ({@code /datastreams/{id}/schema?obsFormat=application/swe+json})
     * and cache it on the resource. A single schema failure is downgraded so it
     * doesn't poison the whole call.
     */
    public List<Datastream> discoverDatastreams()
    {
        APIHelper api = parentNode.getApiHelper();
        ApiResponse res = api.getResource(APIResourceTypes.SYSTEM, resourceId, APIResourceTypes.DATASTREAM, null);
        List<Datastream> result = new ArrayList<>();

        for (JsonElement ds : res.json().getAsJsonObject().getAsJsonArray("items"))
        {
            DatastreamResource datastreamObjs = new DatastreamResource(ds.getAsJsonObject());
            Datastream newDs = new Datastream(parentNode, datastreamObjs);

            // Only treat a datastream as opaque binary passthrough (e.g. video) when
            // it advertises swe+binary AND cannot be represented as SWE text: a genuine
            // opaque/video stream has no swe+json form, whereas scalar datastreams (e.g.
            // a PTZ pan/tilt/zoom output) advertise swe+json/csv/xml alongside swe+binary
            // and must stay on the JSON path.
            boolean binary = datastreamObjs.advertisesFormat("application/swe+binary")
                    && !datastreamObjs.advertisesFormat("application/swe+json");
            String schemaFormat = binary ? "application/swe+binary" : "application/swe+json";

            try
            {
                ApiResponse schemaResp = api.getResource(APIResourceTypes.DATASTREAM, datastreamObjs.getDsId(),
                        APIResourceTypes.SCHEMA, Map.of("obsFormat", schemaFormat));
                schemaResp.raiseForStatus();
                datastreamObjs.setRecordSchema(schemaResp.json());
            }
            catch (Exception e)
            {
                org.sensorhub.impl.service.federation.BrokerLogging.log.warn(
                        "Failed to fetch {} schema for datastream {}: {}",
                        schemaFormat, datastreamObjs.getDsId(), e.toString());
            }

            if (binary)
                newDs.setEncodingMode(EncodingMode.BINARY);

            result.add(newDs);
            datastreams.add(newDs);
        }
        return result;
    }

    /**
     * GET {@code /systems/{id}/controlstreams}, then for each fetch the command
     * schema ({@code /controlstreams/{id}/schema?f=json}) and cache it.
     */
    public List<ControlStream> discoverControlstreams()
    {
        APIHelper api = parentNode.getApiHelper();
        ApiResponse res = api.getResource(APIResourceTypes.SYSTEM, resourceId, APIResourceTypes.CONTROL_CHANNEL, null);
        List<ControlStream> result = new ArrayList<>();

        for (JsonElement csJson : res.json().getAsJsonObject().getAsJsonArray("items"))
        {
            ControlStreamResource controlstreamObjs = new ControlStreamResource(csJson.getAsJsonObject());
            ControlStream newCs = new ControlStream(parentNode, controlstreamObjs);
            try
            {
                ApiResponse schemaResp = api.getResource(APIResourceTypes.CONTROL_CHANNEL, controlstreamObjs.getCsId(),
                        APIResourceTypes.SCHEMA, Map.of("f", "json"));
                schemaResp.raiseForStatus();
                controlstreamObjs.setCommandSchema(schemaResp.json());
            }
            catch (Exception e)
            {
                org.sensorhub.impl.service.federation.BrokerLogging.log.warn(
                        "Failed to fetch command schema for control stream {}: {}",
                        controlstreamObjs.getCsId(), e.toString());
            }
            result.add(newCs);
            controlChannels.add(newCs);
        }
        return result;
    }

    public Datastream addInsertDatastream(DatastreamResource datastreamSchema)
    {
        APIHelper api = parentNode.getApiHelper();
        ApiResponse res = api.createResource(APIResourceTypes.DATASTREAM, datastreamSchema.toWireJson(),
                resourceId, Map.of("Content-Type", ContentTypes.JSON.value()));

        if (res.ok())
        {
            String location = res.header("Location");
            String datastreamId = location.substring(location.lastIndexOf('/') + 1);
            datastreamSchema.setDsId(datastreamId);
        }
        else
        {
            throw new RuntimeException("Failed to create datastream " + datastreamSchema.getName()
                    + ": HTTP " + res.statusCode() + " — " + res.text());
        }

        Datastream newDs = new Datastream(parentNode, datastreamSchema);
        newDs.setParentResourceId(underlyingResource.getSystemId());
        datastreams.add(newDs);
        return newDs;
    }

    public ControlStream addInsertControlstream(ControlStreamResource controlstreamResource)
    {
        APIHelper api = parentNode.getApiHelper();
        ApiResponse res = api.createResource(APIResourceTypes.CONTROL_CHANNEL, controlstreamResource.toWireJson(),
                resourceId, Map.of("Content-Type", ContentTypes.JSON.value()));

        if (res.ok())
        {
            String location = res.header("Location");
            String csId = location.substring(location.lastIndexOf('/') + 1);
            controlstreamResource.setCsId(csId);
        }
        else
        {
            throw new RuntimeException("Failed to create control stream " + controlstreamResource.getName()
                    + ": HTTP " + res.statusCode() + " — " + res.text());
        }

        ControlStream newCs = new ControlStream(parentNode, controlstreamResource);
        newCs.setParentResourceId(underlyingResource.getSystemId());
        controlChannels.add(newCs);
        return newCs;
    }

    /**
     * POST this system to the server (Content-Type {@code application/sml+json}),
     * stripping server-assigned {@code id}/{@code links}, and capture the new
     * resource ID from the {@code Location} header.
     */
    public void insertSelf()
    {
        Map<String, JsonElement> clear = Map.of("id", JsonNull.INSTANCE, "links", JsonNull.INSTANCE);
        SystemResource bodyResource = underlyingResource.modelCopy(clear);
        ApiResponse res = parentNode.getApiHelper().createResource(APIResourceTypes.SYSTEM,
                bodyResource.toWireJson(), null, Map.of("Content-Type", "application/sml+json"));

        if (res.ok())
        {
            String location = res.header("Location");
            String sysId = location.substring(location.lastIndexOf('/') + 1);
            this.resourceId = sysId;
            if (underlyingResource != null)
                underlyingResource.setSystemId(sysId);
        }
    }
}
