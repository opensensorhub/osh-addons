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
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Collection;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.ICommandData;
import org.sensorhub.api.command.ICommandStreamInfo;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.IdEncoders;
import org.sensorhub.impl.service.consys.ResourceParseException;
import org.sensorhub.impl.service.consys.resource.RequestContext;
import org.sensorhub.impl.service.consys.resource.ResourceBinding;
import org.sensorhub.impl.service.consys.resource.ResourceLink;
import org.sensorhub.impl.service.consys.task.CommandHandler.CommandHandlerContextData;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;


/**
 * <p>
 * Command binding for {@code application/swe+proto} — both directions. The
 * per-controlstream message carries the fixed command envelope at fields 1–5
 * ({@code id}, {@code controlstream_id}, {@code foi_id} strings;
 * {@code issue_time} Timestamp; {@code sender} string — see
 * {@link ProtoSchemaWriter#COMMAND_ENVELOPE_FIELD_NAMES}) and the control
 * stream's SWE parameter components at 6+. Messages are length-delimited on
 * the wire, matching the observation binding.
 * </p>
 *
 * <p>
 * <b>Outbound</b> (GET/stream): {@link ProtoObsEncoder#encodeCommand} fills
 * the envelope from command metadata — ids are encoded with the service's
 * {@link IdEncoders} so receivers can correlate to REST resources.
 * </p>
 *
 * <p>
 * <b>Inbound</b> (POST/publish): {@link #deserialize} parses one delimited
 * message per command and decodes the parameters via
 * {@link ProtoRecordDecoder}. Wire envelope handling on ingest: {@code id} is
 * ignored (server-assigned), {@code controlstream_id} is ignored (the stream
 * is addressed by URL/topic), {@code sender} is ignored in favor of the
 * authenticated user (not spoofable), {@code issue_time} is honored when set,
 * otherwise server time is used — mirroring the SWE-common binding.
 * </p>
 *
 * @see ProtoSchemaWriter
 * @see ProtoObsEncoder
 * @see ProtoRecordDecoder
 * @author Ian Patterson
 * @since 2026
 */
public class CommandBindingProto extends ResourceBinding<BigId, ICommandData>
{
    static final String CMD_PACKAGE = "org.sensorhub.consys.proto.cmd";
    static final String CMD_MESSAGE = "Command";

    final CommandHandlerContextData contextData;
    final DataComponent paramStruct;
    final Descriptor descriptor;
    final String userID;
    InputStream is;
    OutputStream os;


    public CommandBindingProto(RequestContext ctx, IdEncoders idEncoders, boolean forReading, ICommandStreamInfo csInfo, GeneratedSchemaCache schemas) throws IOException
    {
        super(ctx, idEncoders);
        this.contextData = (CommandHandlerContextData) ctx.getData();
        // private copy: ProtoRecordDecoder mutates DataChoice selection state,
        // and csInfo's record structure is shared across requests
        this.paramStruct = csInfo.getRecordStructure().copy();
        try
        {
            // memoized per control stream (ctx.getParentID() = its internal ID);
            // fingerprinted on the ORIGINAL structure so the cache key content
            // matches what other bindings see
            this.descriptor = schemas.get(ctx.getParentID(), csInfo.getRecordStructure()).descriptor;
        }
        catch (DescriptorValidationException e)
        {
            throw new IOException("Failed to build swe+proto schema for control stream: " + e.getMessage(), e);
        }

        if (forReading)
        {
            this.is = ctx.getInputStream();
            var user = ctx.getSecurityHandler().getCurrentUser();
            this.userID = user != null ? user.getId() : "api";
        }
        else
        {
            this.userID = null;
        }
    }


    /**
     * Lazily set the response content type and grab the output stream (same
     * pattern as {@link ObsBindingProto}).
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
    public ICommandData deserialize() throws IOException
    {
        DynamicMessage msg;
        try
        {
            // parser returns null on clean EOF (end of the delimited stream)
            msg = DynamicMessage.getDefaultInstance(descriptor).getParserForType().parseDelimitedFrom(is);
        }
        catch (InvalidProtocolBufferException e)
        {
            throw new ResourceParseException("Invalid swe+proto command message: " + e.getMessage());
        }
        if (msg == null)
            return null;   // end of stream

        DataBlock params;
        try
        {
            params = ProtoRecordDecoder.decodeRecord(paramStruct, msg);
        }
        catch (RuntimeException e)
        {
            throw new ResourceParseException("swe+proto command does not match the control stream schema: " + e.getMessage());
        }

        var issueTime = ProtoRecordDecoder.getInstant(msg, "issue_time");
        return new CommandData.Builder()
            .withCommandStream(contextData.streamID)
            .withSender(userID)
            .withIssueTime(issueTime != null ? issueTime : Instant.now())
            .withParams(params)
            .build();
    }


    @Override
    public void serialize(BigId key, ICommandData cmd, boolean showLinks) throws IOException
    {
        var out = writer();
        var env = new ProtoObsEncoder.CommandEnvelope(
            key != null ? idEncoders.getCommandIdEncoder().encodeID(key) : null,
            cmd.getCommandStreamID() != null ? idEncoders.getCommandStreamIdEncoder().encodeID(cmd.getCommandStreamID()) : null,
            cmd.hasFoi() ? idEncoders.getFoiIdEncoder().encodeID(cmd.getFoiID()) : null,
            cmd.getIssueTime(),
            cmd.getSenderID());
        var msg = ProtoObsEncoder.encodeCommand(paramStruct, descriptor, cmd.getParams(), env);
        msg.writeDelimitedTo(out);
        out.flush();
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
