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

import com.georobotix.swecommon.SweOptions;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Timestamp;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.service.IServiceModule;
import org.sensorhub.impl.module.AbstractModule;


/**
 * <p>
 * Add-on service to the Connected Systems API that enables bidirectional
 * Protobuf ({@code application/swe+proto}) encoding for resources and their
 * payloads: systems, datastreams, control streams, observations, commands,
 * command status, and command result.
 * </p>
 *
 * <p>
 * The module owns the per-node {@link DataStreamSchemaCache} singleton.
 * {@link ProtoFormat} and (future) {@code ProtoCommandFormat}, both
 * instantiated reflectively by {@code ConSysApiService.doStart()}, look the
 * cache up via {@link #getCache()}. The cache is null until {@link #doStart}
 * has run; format instances should defer cache use until first request.
 * </p>
 *
 * @author Ian Patterson
 * @since 2026
 */
public class ConSysApiProtoService extends AbstractModule<ConSysApiProtoServiceConfig>
    implements IServiceModule<ConSysApiProtoServiceConfig>
{
    private static volatile DataStreamSchemaCache cache;


    public static DataStreamSchemaCache getCache()
    {
        return cache;
    }


    @Override
    protected void doStart() throws SensorHubException
    {
        var c = new DataStreamSchemaCache();

        // Extension registry so swe_options FieldOptions (definition, uomCode,
        // referenceFrame, …) on ingested descriptors parse as typed extensions
        // rather than unknown fields — see DataStreamSchemaCache.setExtensionRegistry.
        var extReg = ExtensionRegistry.newInstance();
        SweOptions.registerAllExtensions(extReg);
        c.setExtensionRegistry(extReg);

        // Bootstrap the dependency descriptors that client/generated observation
        // FileDescriptorProtos import. The bootstrap key is FileDescriptor.getName(),
        // which must match the import string the ProtoSchemaWriter emits:
        //   google/protobuf/timestamp.proto   (Time fields; WKT, always available)
        //   swecommon/swe_options.proto       (SWE FieldOptions annotations)
        // registerBootstrapTree pulls transitive deps too (swe_options -> descriptor.proto).
        c.registerBootstrapTree(Timestamp.getDescriptor().getFile());
        c.registerBootstrapTree(SweOptions.getDescriptor());

        cache = c;
    }


    @Override
    protected void doStop() throws SensorHubException
    {
        if (cache != null)
            cache.clear();
        cache = null;
    }
}
