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
import java.util.Base64;
import java.util.Collection;
import org.sensorhub.api.common.IdEncoders;
import org.sensorhub.api.data.DataStreamInfo;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.datastore.obs.DataStreamKey;
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
 * Datastream schema binding for {@code application/swe+proto}. Serves the
 * per-datastream observation descriptor at the {@code /datastreams/{id}/schema}
 * endpoint (requested via {@code ?obsFormat=application/swe+proto}) as the JSON
 * envelope the OSHConnect swe+proto contract expects:
 * </p>
 * <pre>
 *   {
 *     "obsFormat": "application/swe+proto",
 *     "messageType": "&lt;fully-qualified observation message name&gt;",
 *     "fileDescriptorSet": "&lt;base64 FileDescriptorSet&gt;"
 *   }
 * </pre>
 *
 * <p>
 * The {@code fileDescriptorSet} carries the observation schema plus its
 * {@code swe_options.proto} import (see {@link ProtoSchemaWriter#toFileDescriptorSet});
 * receivers seed the google well-known types themselves. This is the "schema
 * once" half of the wire model — observations on the obs endpoint are bare
 * messages decoded against this descriptor.
 * </p>
 *
 * @see ProtoSchemaWriter
 * @author Ian Patterson
 * @since 2026
 */
public class DataStreamSchemaBindingProto extends ResourceBindingJson<DataStreamKey, IDataStreamInfo>
{
    final GeneratedSchemaCache schemas;
    final DataStreamSchemaCache ingestCache;


    public DataStreamSchemaBindingProto(RequestContext ctx, IdEncoders idEncoders, boolean forReading, GeneratedSchemaCache schemas) throws IOException
    {
        this(ctx, idEncoders, forReading, schemas, null);
    }


    /**
     * @param ingestCache the per-node descriptor cache (owned by
     *        {@code ConSysApiProtoService}) used to resolve a client-supplied
     *        {@code FileDescriptorSet} on schema ingest; may be null when the
     *        proto service is not running, in which case ingestion is refused.
     */
    public DataStreamSchemaBindingProto(RequestContext ctx, IdEncoders idEncoders, boolean forReading, GeneratedSchemaCache schemas, DataStreamSchemaCache ingestCache) throws IOException
    {
        super(ctx, idEncoders, forReading);
        this.schemas = schemas;
        this.ingestCache = ingestCache;
    }


    @Override
    public void serialize(DataStreamKey key, IDataStreamInfo dsInfo, boolean showLinks, JsonWriter writer) throws IOException
    {
        GeneratedSchemaCache.Entry entry;
        try
        {
            entry = schemas.get(key.getInternalID(), dsInfo.getRecordStructure());
        }
        catch (com.google.protobuf.Descriptors.DescriptorValidationException e)
        {
            throw new IOException("Failed to build swe+proto schema for datastream: " + e.getMessage(), e);
        }

        writer.beginObject();
        writer.name("obsFormat").value(ProtoFormat.MIME_TYPE);
        writer.name("messageType").value(entry.schema.messageTypeName);
        writer.name("fileDescriptorSet").value(Base64.getEncoder().encodeToString(entry.fileDescriptorSet));
        writer.endObject();
        writer.flush();
    }


    /**
     * Ingest a client-supplied swe+proto schema. Reads the envelope this
     * binding's {@link #serialize} emits — {@code messageType} +
     * {@code fileDescriptorSet} (base64) — resolves the descriptor through
     * {@link DataStreamSchemaCache#resolveFromSet} (which reads the
     * {@code swe_options} annotations), and reconstructs the SWE record
     * structure via {@link ProtoSchemaReader}. The caller may already have
     * consumed the leading {@code obsFormat} property (embedded-schema path),
     * so {@code obsFormat} is skipped if seen again.
     */
    @Override
    public IDataStreamInfo deserialize(JsonReader reader) throws IOException
    {
        if (ingestCache == null)
            throw ServiceErrors.unsupportedOperation(
                "swe+proto schema ingestion is unavailable (the ConSysApiProtoService module is not running)");

        String messageType = null;
        byte[] fdsBytes = null;
        try
        {
            // read BEGIN_OBJECT only if not already consumed by the caller
            // (the embedded-schema path reads obsFormat then hands us the reader)
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
                    reader.skipValue();   // obsFormat or unknown
            }
            reader.endObject();
        }
        catch (IllegalStateException | IllegalArgumentException e)
        {
            throw new ResourceParseException("Invalid swe+proto schema document: " + e.getMessage());
        }

        if (messageType == null || fdsBytes == null)
            throw new ResourceParseException("swe+proto schema requires 'messageType' and 'fileDescriptorSet'");

        DataComponent recordStruct;
        try
        {
            var desc = ingestCache.resolveFromSet(fdsBytes, messageType);
            recordStruct = new ProtoSchemaReader().readRecord(desc);
            recordStruct.setName(SWECommonUtils.NO_NAME);
        }
        catch (Exception e)
        {
            throw new ResourceParseException("Cannot ingest swe+proto schema: " + e.getMessage());
        }

        // proto is the obs wire format; the stored SWE encoding is a neutral
        // default (other formats negotiate independently), mirroring the
        // SWE-common schema binding's TextEncoding fallback.
        return new DataStreamInfo.Builder()
            .withName(SWECommonUtils.NO_NAME)   // set later from outputName
            .withSystem(FeatureId.NULL_FEATURE) // system id set later
            .withRecordDescription(recordStruct)
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
