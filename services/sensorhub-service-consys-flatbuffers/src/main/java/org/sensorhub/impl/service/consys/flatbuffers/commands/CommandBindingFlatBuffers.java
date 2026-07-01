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

package org.sensorhub.impl.service.consys.flatbuffers.commands;

import org.sensorhub.impl.service.consys.flatbuffers.FlatBuffersFormat;
import org.sensorhub.impl.service.consys.flatbuffers.codec.FlexDecoder;
import org.sensorhub.impl.service.consys.flatbuffers.codec.FlexEncoder;
import org.sensorhub.impl.service.consys.flatbuffers.codec.FlexFraming;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
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
import com.google.flatbuffers.FlexBuffers;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;


/**
 * <p>
 * Command binding for {@code application/swe+flatbuffers} — both directions. The
 * per-controlstream payload carries the fixed command envelope
 * ({@code id}, {@code controlstream_id}, {@code foi_id}, {@code issue_time},
 * {@code sender}) plus a {@code result} slot with the control stream's SWE
 * parameter record, encoded as FlexBuffers and length-framed like the
 * observation binding.
 * </p>
 *
 * <p>
 * Inbound envelope handling mirrors the proto/SWE-common bindings: {@code id} and
 * {@code controlstream_id} are ignored (server-assigned / addressed by URL),
 * {@code sender} is replaced by the authenticated user, and {@code issue_time} is
 * honored when set, otherwise server time.
 * </p>
 *
 * @see FlexEncoder
 * @see FlexDecoder
 * @author Ian Patterson
 * @since 2026
 */
public class CommandBindingFlatBuffers extends ResourceBinding<BigId, ICommandData>
{
    final CommandHandlerContextData contextData;
    final DataComponent paramStruct;
    final String userID;
    InputStream is;
    OutputStream os;


    public CommandBindingFlatBuffers(RequestContext ctx, IdEncoders idEncoders, boolean forReading, ICommandStreamInfo csInfo) throws IOException
    {
        super(ctx, idEncoders);
        this.contextData = (CommandHandlerContextData) ctx.getData();
        // private copy: FlexDecoder mutates DataChoice selection state
        this.paramStruct = csInfo.getRecordStructure().copy();

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


    OutputStream writer() throws IOException
    {
        if (os == null)
        {
            ctx.setResponseContentType(FlatBuffersFormat.MIME_TYPE);
            os = ctx.getOutputStream();
        }
        return os;
    }


    @Override
    public ICommandData deserialize() throws IOException
    {
        var payload = FlexFraming.readFrame(is);
        if (payload == null)
            return null;   // end of stream

        DataBlock params;
        Instant issueTime;
        try
        {
            var map = FlexBuffers.getRoot(ByteBuffer.wrap(payload)).asMap();
            params = FlexDecoder.decodeResult(paramStruct, map.get("result"));
            issueTime = FlexDecoder.getInstant(map, "issue_time");
        }
        catch (RuntimeException e)
        {
            throw new ResourceParseException("swe+flatbuffers command does not match the control stream schema: " + e.getMessage());
        }

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
        var env = new FlexEncoder.CommandEnvelope(
            key != null ? idEncoders.getCommandIdEncoder().encodeID(key) : null,
            cmd.getCommandStreamID() != null ? idEncoders.getCommandStreamIdEncoder().encodeID(cmd.getCommandStreamID()) : null,
            cmd.hasFoi() ? idEncoders.getFoiIdEncoder().encodeID(cmd.getFoiID()) : null,
            cmd.getIssueTime(),
            cmd.getSenderID());
        FlexFraming.writeFrame(out, FlexEncoder.encodeCommand(paramStruct, cmd.getParams(), env));
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
