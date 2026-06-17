/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

Author: Ian Patterson <ian.patterson@georobotix.us>

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.consys.proto;

import java.io.IOException;
import org.sensorhub.api.command.ICommandData;
import org.sensorhub.api.command.ICommandStreamInfo;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.IdEncoders;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.datastore.command.CommandStreamKey;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.impl.service.consys.obs.CustomObsFormat;
import org.sensorhub.impl.service.consys.resource.RequestContext;
import org.sensorhub.impl.service.consys.resource.ResourceBinding;


/**
 * <p>
 * MIME-format registration for {@code application/swe+proto}. Implements the
 * ConSys {@code CustomObsFormat} SPI so the Connected Systems API content-
 * negotiation table routes protobuf observation requests into this module's
 * resource bindings.
 * </p>
 *
 * <p>
 * <b>Registration model.</b> {@code CustomObsFormat} implementations are not
 * dynamically registered at runtime; {@code ConSysApiService.doStart()}
 * instantiates them at startup (via the no-arg constructor) from
 * {@code ConSysApiServiceConfig.customFormats} — a list of {@code mimeType} +
 * {@code className} pairs. To activate this format in a deployment, add a
 * {@code CustomFormatConfig} pointing at this class's fully qualified name to
 * the ConSys service config; the module simply needs to be on the classpath.
 * </p>
 *
 * <p>
 * <b>Routing.</b> {@code ObsHandler.getCustomFormatBinding} routes explicit
 * requests ({@code ?f=application/swe+proto}) to {@link #getObsBinding} by mime
 * type. {@link #isCompatible} returns {@code true} so the format is advertised
 * in the datastream resource's {@code formats} list, while
 * {@link #isAutoSelectable} returns {@code false} so it is never AUTO-selected —
 * unqualified observation requests keep the default SWE JSON encoding and proto
 * is strictly opt-in. The patched {@code DataStreamSchemaHandler} routes
 * {@code /schema?obsFormat=application/swe+proto} to {@link #getSchemaBinding}.
 * </p>
 *
 * @see ObsBindingProto
 * @see <a href="../../../../../../../docs/architecture.md">docs/architecture.md</a>
 * @author Ian Patterson
 * @since 2026
 */
public final class ProtoFormat implements CustomObsFormat
{
    /** MIME type for protobuf-encoded SWE Common payloads. */
    public static final String MIME_TYPE = "application/swe+proto";

    /** Subtopic token for transports that encode formats as URL/topic suffixes
     *  (parallel to {@code swe-binary}, {@code swe-json}, {@code swe-csv}
     *  used by sensorhub-service-consys-mqtt). */
    public static final String FORMAT_TOKEN = "swe-proto";

    // ProtoFormat is instantiated once per ConSysApiService start and kept in
    // the customFormats map, so per-stream schema artifacts memoized here live
    // for the service lifetime (entries self-invalidate via a structural
    // fingerprint when a stream's record structure changes)
    final GeneratedSchemaCache obsSchemas = new GeneratedSchemaCache(struct ->
        new ProtoSchemaWriter().write(struct,
            "datastreams/obs.proto", ObsBindingProto.OBS_PACKAGE, ObsBindingProto.OBS_MESSAGE));
    final GeneratedSchemaCache cmdSchemas = new GeneratedSchemaCache(struct ->
        new ProtoSchemaWriter().writeCommand(struct,
            "controlstreams/cmd.proto", CommandBindingProto.CMD_PACKAGE, CommandBindingProto.CMD_MESSAGE));


    @Override
    public boolean isCompatible(IDataStreamInfo dsInfo)
    {
        // Every record structure ProtoSchemaWriter can translate is encodable.
        // This drives the "formats" list advertised on the datastream resource,
        // so clients negotiating by that list can discover swe+proto.
        // TODO return false for structures the schema writer rejects (DataChoice)
        // so unsupported datastreams don't advertise a format that 500s.
        return true;
    }


    @Override
    public boolean isAutoSelectable(IDataStreamInfo dsInfo)
    {
        // Never AUTO-select proto for unqualified browser requests — the
        // default SWE JSON view stays; proto is opt-in per request
        // (?f=application/swe+proto or the swe-proto MQTT/WS topic token).
        return false;
    }


    @Override
    public ResourceBinding<BigId, IObsData> getObsBinding(RequestContext ctx, IdEncoders idEncoders, IDataStreamInfo dsInfo) throws IOException
    {
        return new ObsBindingProto(ctx, idEncoders, dsInfo, obsSchemas);
    }


    @Override
    public ResourceBinding<DataStreamKey, IDataStreamInfo> getSchemaBinding(RequestContext ctx, IdEncoders idEncoders, IDataStreamInfo dsInfo) throws IOException
    {
        // Serves the per-datastream FileDescriptorSet (as the swe+proto JSON
        // schema envelope) at /datastreams/{id}/schema?obsFormat=application/swe+proto.
        // The schema is built from the datastream's record structure passed to
        // serialize(), so dsInfo here is not required. Requires the patched
        // DataStreamSchemaHandler that routes custom obs formats to this method.
        // ingestCache (looked up lazily — the proto service may start after this
        // format) backs the deserialize/ingest path; null ⇒ ingestion refused.
        return new DataStreamSchemaBindingProto(ctx, idEncoders, false, obsSchemas, ConSysApiProtoService.getCache());
    }


    @Override
    public ResourceBinding<CommandStreamKey, ICommandStreamInfo> getCommandSchemaBinding(RequestContext ctx, IdEncoders idEncoders, ICommandStreamInfo csInfo) throws IOException
    {
        // /controlstreams/{id}/schema?commandFormat=application/swe+proto
        return new CommandStreamSchemaBindingProto(ctx, idEncoders, false, cmdSchemas, ConSysApiProtoService.getCache());
    }


    @Override
    public ResourceBinding<BigId, ICommandData> getCommandBinding(RequestContext ctx, IdEncoders idEncoders, ICommandStreamInfo csInfo, boolean forReading) throws IOException
    {
        // GET/stream commands as proto AND ingest proto commands on POST/publish
        return new CommandBindingProto(ctx, idEncoders, forReading, csInfo, cmdSchemas);
    }
}