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

package org.sensorhub.impl.service.consys.flatbuffers.controlstreams;

import java.io.IOException;
import org.sensorhub.api.common.IdEncoders;
import org.sensorhub.impl.service.consys.resource.RequestContext;
import org.sensorhub.impl.service.consys.task.CommandStreamSchemaBindingJson;


/**
 * <p>
 * Control-stream (command) schema binding for {@code application/swe+flatbuffers}.
 * Like the datastream schema binding, it describes the command parameter shape
 * with JSON rather than a compiled binary schema — reusing OSH's existing
 * {@link CommandStreamSchemaBindingJson}. Served at
 * {@code /controlstreams/{id}/schema?commandFormat=application/swe+flatbuffers}.
 * </p>
 *
 * @author Ian Patterson
 * @since 2026
 */
public class CommandStreamSchemaBindingFlatBuffers extends CommandStreamSchemaBindingJson
{
    public CommandStreamSchemaBindingFlatBuffers(RequestContext ctx, IdEncoders idEncoders, boolean forReading) throws IOException
    {
        super(ctx, idEncoders, forReading);
    }
}
