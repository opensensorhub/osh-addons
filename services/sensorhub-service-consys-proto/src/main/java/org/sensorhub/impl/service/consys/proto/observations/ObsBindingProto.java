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

package org.sensorhub.impl.service.consys.proto.observations;

import org.sensorhub.impl.service.consys.proto.ProtoFormat;
import org.sensorhub.impl.service.consys.proto.codec.ProtoObsEncoder;
import org.sensorhub.impl.service.consys.proto.codec.ProtoRecordDecoder;
import org.sensorhub.impl.service.consys.proto.schema.GeneratedSchemaCache;
import org.sensorhub.impl.service.consys.proto.schema.ProtoSchemaWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Collection;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.IdEncoders;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.data.ObsData;
import org.sensorhub.impl.service.consys.ResourceParseException;
import org.sensorhub.impl.service.consys.obs.ObsHandler.ObsHandlerContextData;
import org.sensorhub.impl.service.consys.resource.RequestContext;
import org.sensorhub.impl.service.consys.resource.ResourceBinding;
import org.sensorhub.impl.service.consys.resource.ResourceLink;
import org.vast.swe.SWEHelper;
import org.vast.swe.ScalarIndexer;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;


/**
 * <p>
 * Observation binding for {@code application/swe+proto}. Encodes each
 * {@link IObsData} as a protobuf message whose schema is generated on the fly
 * from the datastream's record structure by {@link ProtoSchemaWriter}, with the
 * values filled in by {@link ProtoObsEncoder}. Messages are written
 * length-delimited so a stream/collection of observations is individually
 * parseable.
 * </p>
 *
 * <p>
 * The per-datastream {@link Descriptor} is built once when the binding is
 * created (one binding instance per request). Receivers need that same
 * descriptor to decode — it is NOT sent inline with the observation bytes (the
 * "schema once, observations bare" model). Exposing it (embedding the
 * {@code FileDescriptorProto} in the {@code DataStream} resource, or wiring
 * {@code getSchemaBinding}) is a separate follow-up; until then the bytes are
 * only decodable by a client that built the schema the same way.
 * </p>
 *
 * <p>
 * <b>Inbound</b> (POST/publish): {@link #deserialize} parses one delimited
 * message per observation and decodes the result record via
 * {@link ProtoRecordDecoder}, against the same on-the-fly descriptor used for
 * encoding (the "schema once, observations bare" model — sender and receiver
 * generate the identical descriptor from the datastream's record structure).
 * Wire envelope handling on ingest: {@code id} is ignored (server-assigned),
 * {@code datastream_id} is ignored (the stream is addressed by URL/topic),
 * {@code foi_id} is taken from the request context (the {@code foi} query
 * param), {@code phenomenon_time} is honored when set — otherwise the record's
 * own time stamp, otherwise server time — and {@code result_time} is honored
 * when set. {@code DataArray} and {@code DataChoice} record structures are
 * rejected (by the schema writer, encoder, and decoder) until a later revision.
 * </p>
 *
 * @see ProtoSchemaWriter
 * @see ProtoObsEncoder
 * @see ProtoRecordDecoder
 * @author Ian Patterson
 * @since 2026
 */
public class ObsBindingProto extends ResourceBinding<BigId, IObsData>
{
    public static final String OBS_PACKAGE = "org.sensorhub.consys.proto.obs";
    public static final String OBS_MESSAGE = "Observation";

    final ObsHandlerContextData contextData;
    final DataComponent recordStruct;
    final Descriptor descriptor;
    final ScalarIndexer timeIndexer;
    InputStream is;
    OutputStream os;


    public ObsBindingProto(RequestContext ctx, IdEncoders idEncoders, IDataStreamInfo dsInfo, GeneratedSchemaCache schemas) throws IOException
    {
        super(ctx, idEncoders);
        this.contextData = (ObsHandlerContextData) ctx.getData();
        // private copy: ProtoRecordDecoder mutates DataChoice selection state,
        // and the dsInfo record structure is shared across requests
        this.recordStruct = dsInfo.getRecordStructure().copy();
        this.timeIndexer = SWEHelper.getTimeStampIndexer(dsInfo.getRecordStructure());
        try
        {
            // rebuilt per request (schema fingerprint memoization is parked —
            // see GeneratedSchemaCache). ctx.getParentID() is the datastream's
            // internal ID (always set here: custom obs bindings are only used
            // for single-datastream requests).
            this.descriptor = schemas.get(ctx.getParentID(), dsInfo.getRecordStructure()).descriptor;
        }
        catch (DescriptorValidationException e)
        {
            throw new IOException("Failed to build swe+proto schema for datastream: " + e.getMessage(), e);
        }
    }


    /**
     * Lazily set the response content type and grab the output stream. Called
     * from both {@link #startCollection} (list/stream GET) and {@link #serialize}
     * (single-item GET, where startCollection is not invoked), so the content
     * type is always set before the first byte is written.
     */
    OutputStream writer() throws IOException
    {
        if (os == null)
        {
            ctx.setResponseContentType(ProtoFormat.MIME_TYPE);
            os = ctx.getOutputStream();
        }
        return os;
    }


    @Override
    public void serialize(BigId key, IObsData obs, boolean showLinks) throws IOException
    {
        var out = writer();
        // v1 envelope: phenomenon/result time only; id/datastream_id/foi_id are
        // left empty (valid proto3 defaults) until id-encoding is wired.
        var env = new ProtoObsEncoder.Envelope(null, null, null,
            obs.getPhenomenonTime(), obs.getResultTime());
        var msg = ProtoObsEncoder.encode(recordStruct, descriptor, obs.getResult(), env);
        msg.writeDelimitedTo(out);
        out.flush();
    }


    @Override
    public IObsData deserialize() throws IOException
    {
        if (is == null)
            is = ctx.getInputStream();

        DynamicMessage msg;
        try
        {
            // parser returns null on clean EOF (end of the delimited stream)
            msg = DynamicMessage.getDefaultInstance(descriptor).getParserForType().parseDelimitedFrom(is);
        }
        catch (InvalidProtocolBufferException e)
        {
            throw new ResourceParseException("Invalid swe+proto observation message: " + e.getMessage());
        }
        if (msg == null)
            return null;   // end of stream

        DataBlock result;
        try
        {
            result = ProtoRecordDecoder.decodeRecord(recordStruct, msg);
        }
        catch (RuntimeException e)
        {
            throw new ResourceParseException("swe+proto observation does not match the datastream schema: " + e.getMessage());
        }

        // phenomenon time: trust the envelope when set, else the record's own
        // time stamp (what the obs store indexes), else server time
        var phenomenonTime = ProtoRecordDecoder.getInstant(msg, "phenomenon_time");
        if (phenomenonTime == null && timeIndexer != null)
        {
            double t = timeIndexer.getDoubleValue(result);
            if (!Double.isNaN(t))
                phenomenonTime = Instant.ofEpochMilli((long) (t * 1000.));
        }
        if (phenomenonTime == null)
            phenomenonTime = Instant.now();

        var resultTime = ProtoRecordDecoder.getInstant(msg, "result_time");

        return new ObsData.Builder()
            .withDataStream(contextData.dsID)
            .withFoi(contextData.foiId)
            .withPhenomenonTime(phenomenonTime)
            .withResultTime(resultTime != null ? resultTime : phenomenonTime)
            .withResult(result)
            .build();
    }


    @Override
    public void startCollection() throws IOException
    {
        writer();
    }


    @Override
    public void endCollection(Collection<ResourceLink> links) throws IOException
    {
        writer().flush();
    }
}
