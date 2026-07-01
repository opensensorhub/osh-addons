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

package org.sensorhub.impl.service.consys.flatbuffers.observations;

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
import com.google.flatbuffers.FlexBuffers;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;


/**
 * <p>
 * Observation binding for {@code application/swe+flatbuffers}. Encodes each
 * {@link IObsData} as a self-describing FlexBuffers map (via {@link FlexEncoder})
 * framed with a length prefix ({@link FlexFraming}) so a stream/collection is
 * individually parseable. No per-datastream schema travels with the bytes; the
 * receiver interprets them against the datastream's record structure — the same
 * "schema once, observations bare" model as the proto binding, and the datastream
 * schema is separately available as a JSON logical schema at the
 * {@code /schema} endpoint.
 * </p>
 *
 * @see FlexEncoder
 * @see FlexDecoder
 * @author Ian Patterson
 * @since 2026
 */
public class ObsBindingFlatBuffers extends ResourceBinding<BigId, IObsData>
{
    final ObsHandlerContextData contextData;
    final DataComponent recordStruct;
    final ScalarIndexer timeIndexer;
    InputStream is;
    OutputStream os;


    public ObsBindingFlatBuffers(RequestContext ctx, IdEncoders idEncoders, IDataStreamInfo dsInfo) throws IOException
    {
        super(ctx, idEncoders);
        this.contextData = (ObsHandlerContextData) ctx.getData();
        // private copy: FlexDecoder mutates DataChoice selection state, and the
        // dsInfo record structure is shared across requests
        this.recordStruct = dsInfo.getRecordStructure().copy();
        this.timeIndexer = SWEHelper.getTimeStampIndexer(dsInfo.getRecordStructure());
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
    public void serialize(BigId key, IObsData obs, boolean showLinks) throws IOException
    {
        var out = writer();
        // v1 envelope: phenomenon/result time only; id/datastream_id/foi_id are
        // left empty (addressed by URL/topic) until id-encoding is wired.
        var env = new FlexEncoder.Envelope(null, null, null,
            obs.getPhenomenonTime(), obs.getResultTime());
        FlexFraming.writeFrame(out, FlexEncoder.encode(recordStruct, obs.getResult(), env));
        out.flush();
    }


    @Override
    public IObsData deserialize() throws IOException
    {
        if (is == null)
            is = ctx.getInputStream();

        var payload = FlexFraming.readFrame(is);
        if (payload == null)
            return null;   // end of stream

        DataBlock result;
        Instant phenomenonTime;
        Instant resultTime;
        try
        {
            var map = FlexBuffers.getRoot(ByteBuffer.wrap(payload)).asMap();
            result = FlexDecoder.decodeResult(recordStruct, map.get("result"));
            phenomenonTime = FlexDecoder.getInstant(map, "phenomenon_time");
            resultTime = FlexDecoder.getInstant(map, "result_time");
        }
        catch (RuntimeException e)
        {
            throw new ResourceParseException("swe+flatbuffers observation does not match the datastream schema: " + e.getMessage());
        }

        // phenomenon time: trust the envelope when set, else the record's own
        // time stamp, else server time
        if (phenomenonTime == null && timeIndexer != null)
        {
            double t = timeIndexer.getDoubleValue(result);
            if (!Double.isNaN(t))
                phenomenonTime = Instant.ofEpochMilli((long) (t * 1000.));
        }
        if (phenomenonTime == null)
            phenomenonTime = Instant.now();

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
