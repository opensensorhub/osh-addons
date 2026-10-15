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

package org.sensorhub.impl.service.consys.flatbuffers;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.service.IServiceModule;
import org.sensorhub.impl.module.AbstractModule;


/**
 * <p>
 * Add-on service to the Connected Systems API that enables bidirectional
 * FlatBuffers ({@code application/swe+flatbuffers}) encoding of observations and
 * commands via {@link FlatBuffersFormat}.
 * </p>
 *
 * <p>
 * Unlike {@code ConSysApiProtoService}, this module owns no per-node schema
 * cache: FlexBuffers payloads are self-describing and the SWE record structure
 * used to interpret them comes from the datastream/control-stream registry on
 * both encode and decode. The module exists for parity with the proto add-on and
 * so the format can carry future runtime options; the {@link FlatBuffersFormat}
 * itself is instantiated reflectively by {@code ConSysApiService.doStart()} from
 * the {@code customFormats} config and does not depend on this module running.
 * </p>
 *
 * @author Ian Patterson
 * @since 2026
 */
public class ConSysApiFlatBuffersService extends AbstractModule<ConSysApiFlatBuffersServiceConfig>
    implements IServiceModule<ConSysApiFlatBuffersServiceConfig>
{
    @Override
    protected void doStart() throws SensorHubException
    {
        getLogger().info("Connected Systems API FlatBuffers (swe+flatbuffers) support enabled");
    }


    @Override
    protected void doStop() throws SensorHubException
    {
    }
}
