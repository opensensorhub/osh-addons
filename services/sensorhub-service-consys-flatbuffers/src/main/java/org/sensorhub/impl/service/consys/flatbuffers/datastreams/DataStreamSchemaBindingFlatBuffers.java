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

package org.sensorhub.impl.service.consys.flatbuffers.datastreams;

import java.io.IOException;
import org.sensorhub.api.common.IdEncoders;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.impl.service.consys.flatbuffers.codec.FlexJsonSchema;
import org.sensorhub.impl.service.consys.obs.DataStreamSchemaBindingLogicalJsonSchema;
import org.sensorhub.impl.service.consys.resource.RequestContext;
import com.google.gson.stream.JsonWriter;


/**
 * <p>
 * Datastream schema binding for {@code application/swe+flatbuffers}. Serves a JSON
 * Schema that <b>faithfully mirrors the FlexBuffers wire structure</b> the codec
 * emits (nested records → objects, arrays → arrays, choices → {@code {case,value}},
 * ranges → 2-tuples, plus the envelope + {@code result} slot) — see
 * {@link FlexJsonSchema}. Served at
 * {@code /datastreams/{id}/schema?obsFormat=application/swe+flatbuffers}.
 * </p>
 *
 * <p>
 * Extends {@link DataStreamSchemaBindingLogicalJsonSchema} to inherit the JSON
 * response plumbing (content type, collection wrapping), but overrides
 * {@link #serialize} to produce the FlexBuffers-structured schema rather than the
 * flat, encoding-independent leaf view. The generic
 * {@code ?obsFormat=logical} endpoint still returns that flat view.
 * </p>
 *
 * @author Ian Patterson
 * @since 2026
 */
public class DataStreamSchemaBindingFlatBuffers extends DataStreamSchemaBindingLogicalJsonSchema
{
    public DataStreamSchemaBindingFlatBuffers(RequestContext ctx, IdEncoders idEncoders, boolean forReading) throws IOException
    {
        super(ctx, idEncoders, forReading);
    }


    @Override
    public void serialize(DataStreamKey key, IDataStreamInfo dsInfo, boolean showLinks, JsonWriter writer) throws IOException
    {
        FlexJsonSchema.writePayloadSchema(writer, dsInfo.getName(), dsInfo.getDescription(), dsInfo.getRecordStructure());
    }
}
