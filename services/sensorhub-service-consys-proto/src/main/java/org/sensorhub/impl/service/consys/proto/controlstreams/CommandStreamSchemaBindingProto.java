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

package org.sensorhub.impl.service.consys.proto.controlstreams;

import org.sensorhub.impl.service.consys.proto.ProtoFormat;
import org.sensorhub.impl.service.consys.proto.datastreams.DataStreamSchemaBindingProto;
import org.sensorhub.impl.service.consys.proto.schema.DataStreamSchemaCache;
import org.sensorhub.impl.service.consys.proto.schema.GeneratedSchemaCache;
import org.sensorhub.impl.service.consys.proto.schema.ProtoSchemaReader;
import org.sensorhub.impl.service.consys.proto.schema.ProtoSchemaWriter;
import java.io.IOException;
import java.util.Base64;
import java.util.Collection;
import org.sensorhub.api.command.CommandStreamInfo;
import org.sensorhub.api.command.ICommandStreamInfo;
import org.sensorhub.api.common.IdEncoders;
import org.sensorhub.api.datastore.command.CommandStreamKey;
import org.sensorhub.api.feature.FeatureId;
import org.sensorhub.impl.service.consys.ResourceParseException;
import org.sensorhub.impl.service.consys.SWECommonUtils;
import org.sensorhub.impl.service.consys.ServiceErrors;
import org.sensorhub.impl.service.consys.resource.RequestContext;
import org.sensorhub.impl.service.consys.resource.ResourceBindingJson;
import org.sensorhub.impl.service.consys.resource.ResourceLink;
import org.vast.data.TextEncodingImpl;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.opengis.swe.v20.DataComponent;


/**
 * <p>
 * Control-stream schema binding for {@code application/swe+proto}. Serves the
 * per-controlstream command descriptor at
 * {@code /controlstreams/{id}/schema?commandFormat=application/swe+proto} as a
 * JSON envelope mirroring the datastream schema contract:
 * </p>
 * <pre>
 *   {
 *     "commandFormat": "application/swe+proto",
 *     "messageType": "&lt;fully-qualified command message name&gt;",
 *     "fileDescriptorSet": "&lt;base64 FileDescriptorSet&gt;"
 *   }
 * </pre>
 *
 * <p>
 * The message described carries the command envelope at fields 1–5
 * ({@link ProtoSchemaWriter#COMMAND_ENVELOPE_FIELD_NAMES}) and the SWE
 * parameter components at 6+; the {@code fileDescriptorSet} packaging rules
 * are identical to observations ({@code swe_options.proto} travels, google
 * WKTs do not — see {@link ProtoSchemaWriter#toFileDescriptorSet}).
 * </p>
 *
 * @see DataStreamSchemaBindingProto
 * @see ProtoSchemaWriter
 * @author Ian Patterson
 * @since 2026
 */
public class CommandStreamSchemaBindingProto extends ResourceBindingJson<CommandStreamKey, ICommandStreamInfo>
{
    final GeneratedSchemaCache schemas;
    final DataStreamSchemaCache ingestCache;


    public CommandStreamSchemaBindingProto(RequestContext ctx, IdEncoders idEncoders, boolean forReading, GeneratedSchemaCache schemas) throws IOException
    {
        this(ctx, idEncoders, forReading, schemas, null);
    }


    /**
     * @param ingestCache per-node descriptor cache used to resolve a
     *        client-supplied {@code FileDescriptorSet} on schema ingest; null
     *        when the proto service is not running (ingestion is then refused).
     */
    public CommandStreamSchemaBindingProto(RequestContext ctx, IdEncoders idEncoders, boolean forReading, GeneratedSchemaCache schemas, DataStreamSchemaCache ingestCache) throws IOException
    {
        super(ctx, idEncoders, forReading);
        this.schemas = schemas;
        this.ingestCache = ingestCache;
    }


    @Override
    public void serialize(CommandStreamKey key, ICommandStreamInfo csInfo, boolean showLinks, JsonWriter writer) throws IOException
    {
        GeneratedSchemaCache.Entry entry;
        try
        {
            entry = schemas.get(key.getInternalID(), csInfo.getRecordStructure());
        }
        catch (com.google.protobuf.Descriptors.DescriptorValidationException e)
        {
            throw new IOException("Failed to build swe+proto schema for control stream: " + e.getMessage(), e);
        }

        writer.beginObject();
        writer.name("commandFormat").value(ProtoFormat.MIME_TYPE);
        writer.name("messageType").value(entry.schema.messageTypeName);
        writer.name("fileDescriptorSet").value(Base64.getEncoder().encodeToString(entry.fileDescriptorSet));
        writer.endObject();
        writer.flush();
    }


    /**
     * Ingest a client-supplied swe+proto command schema — the control-stream
     * counterpart of {@link DataStreamSchemaBindingProto#deserialize}. Reads
     * {@code messageType} + {@code fileDescriptorSet}, resolves the descriptor,
     * and reconstructs the SWE parameter structure via {@link ProtoSchemaReader}.
     */
    @Override
    public ICommandStreamInfo deserialize(JsonReader reader) throws IOException
    {
        if (ingestCache == null)
            throw ServiceErrors.unsupportedOperation(
                "swe+proto command schema ingestion is unavailable (the ConSysApiProtoService module is not running)");

        String messageType = null;
        byte[] fdsBytes = null;
        try
        {
            if (reader.peek() == JsonToken.BEGIN_OBJECT)
                reader.beginObject();

            while (reader.hasNext())
            {
                var prop = reader.nextName();
                if ("messageType".equals(prop))
                    messageType = reader.nextString();
                else if ("fileDescriptorSet".equals(prop))
                    fdsBytes = Base64.getDecoder().decode(reader.nextString());
                else
                    reader.skipValue();   // commandFormat or unknown
            }
            reader.endObject();
        }
        catch (IllegalStateException | IllegalArgumentException e)
        {
            throw new ResourceParseException("Invalid swe+proto command schema document: " + e.getMessage());
        }

        if (messageType == null || fdsBytes == null)
            throw new ResourceParseException("swe+proto command schema requires 'messageType' and 'fileDescriptorSet'");

        DataComponent paramStruct;
        try
        {
            var desc = ingestCache.resolveFromSet(fdsBytes, messageType);
            paramStruct = new ProtoSchemaReader().readRecord(desc);
            paramStruct.setName(SWECommonUtils.NO_NAME);
        }
        catch (Exception e)
        {
            throw new ResourceParseException("Cannot ingest swe+proto command schema: " + e.getMessage());
        }

        return new CommandStreamInfo.Builder()
            .withName(SWECommonUtils.NO_NAME)   // set later from controlInput name
            .withSystem(FeatureId.NULL_FEATURE) // system id set later
            .withRecordDescription(paramStruct)
            .withRecordEncoding(new TextEncodingImpl())
            .build();
    }


    @Override
    public void startCollection() throws IOException
    {
        startJsonCollection(writer);
    }


    @Override
    public void endCollection(Collection<ResourceLink> links) throws IOException
    {
        endJsonCollection(writer, links);
    }
}
