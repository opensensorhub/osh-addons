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

import java.io.IOException;
import org.sensorhub.api.command.ICommandData;
import org.sensorhub.api.command.ICommandStreamInfo;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.IdEncoders;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.datastore.command.CommandStreamKey;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.impl.service.consys.flatbuffers.codec.FlexArrays;
import org.sensorhub.impl.service.consys.flatbuffers.codec.FlexTypes;
import org.sensorhub.impl.service.consys.flatbuffers.commands.CommandBindingFlatBuffers;
import org.sensorhub.impl.service.consys.flatbuffers.controlstreams.CommandStreamSchemaBindingFlatBuffers;
import org.sensorhub.impl.service.consys.flatbuffers.datastreams.DataStreamSchemaBindingFlatBuffers;
import org.sensorhub.impl.service.consys.flatbuffers.observations.ObsBindingFlatBuffers;
import org.sensorhub.impl.service.consys.obs.CustomObsFormat;
import org.sensorhub.impl.service.consys.resource.RequestContext;
import org.sensorhub.impl.service.consys.resource.ResourceBinding;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.RangeComponent;
import net.opengis.swe.v20.Time;
import net.opengis.swe.v20.Vector;


/**
 * <p>
 * MIME-format registration for {@code application/swe+flatbuffers}. Implements
 * the ConSys {@code CustomObsFormat} SPI so the Connected Systems API routes
 * FlatBuffers observation/command requests into this module's resource bindings.
 * Observations and commands are carried as self-describing FlexBuffers payloads;
 * the datastream/control-stream shape is described out of band as a JSON logical
 * schema at the {@code /schema} endpoint.
 * </p>
 *
 * <p>
 * <b>Registration.</b> Like {@code ProtoFormat}, instances are created at startup
 * by {@code ConSysApiService.doStart()} from
 * {@code ConSysApiServiceConfig.customFormats} (a {@code mimeType} +
 * {@code className} pair). No runtime state is needed, so unlike proto this class
 * holds no schema cache and does not depend on {@code ConSysApiFlatBuffersService}
 * running — the module only needs to be on the classpath.
 * </p>
 *
 * @see ObsBindingFlatBuffers
 * @author Ian Patterson
 * @since 2026
 */
public final class FlatBuffersFormat implements CustomObsFormat
{
    /** MIME type for FlatBuffers (FlexBuffers) encoded SWE Common payloads. */
    public static final String MIME_TYPE = "application/swe+flatbuffers";

    /** Subtopic token for transports that encode formats as URL/topic suffixes
     *  (parallel to {@code swe-proto}, {@code swe-json}). */
    public static final String FORMAT_TOKEN = "swe-flatbuffers";


    @Override
    public boolean isCompatible(IDataStreamInfo dsInfo)
    {
        return canEncode(dsInfo.getRecordStructure());
    }


    /** True if the FlexBuffers codec can round-trip this structure: every scalar
     *  maps to a FlexBuffers type and no array element hides a DataChoice (a
     *  DataBlockList the flat-index walk can't address). */
    static boolean canEncode(DataComponent struct)
    {
        if (hasArrayWithChoiceElement(struct))
            return false;
        try
        {
            validate(struct);
            return true;
        }
        catch (Exception e)
        {
            return false;   // Geometry, unmapped scalar type, …
        }
    }


    private static void validate(DataComponent c)
    {
        if (c instanceof DataArray)
        {
            validate(((DataArray) c).getElementType());
            return;
        }
        if (c instanceof DataChoice || c instanceof DataRecord || c instanceof Vector)
        {
            for (int i = 0; i < c.getComponentCount(); i++)
                validate(c.getComponent(i));
            return;
        }
        if (c instanceof Time && ((Time) c).isIsoTime())
            return;
        // scalar or range — throws if the SWE type has no FlexBuffers mapping
        FlexTypes.of(c);
    }


    static boolean hasArrayWithChoiceElement(DataComponent c)
    {
        if (c instanceof DataArray)
        {
            if (FlexArrays.elementHasChoice(((DataArray) c).getElementType()))
                return true;
            return hasArrayWithChoiceElement(((DataArray) c).getElementType());
        }
        for (int i = 0; i < c.getComponentCount(); i++)
            if (hasArrayWithChoiceElement(c.getComponent(i)))
                return true;
        return false;
    }


    @Override
    public boolean isAutoSelectable(IDataStreamInfo dsInfo)
    {
        // Opt-in only: unqualified browser requests keep the default SWE JSON view.
        return false;
    }


    @Override
    public ResourceBinding<BigId, IObsData> getObsBinding(RequestContext ctx, IdEncoders idEncoders, IDataStreamInfo dsInfo) throws IOException
    {
        return new ObsBindingFlatBuffers(ctx, idEncoders, dsInfo);
    }


    @Override
    public ResourceBinding<DataStreamKey, IDataStreamInfo> getSchemaBinding(RequestContext ctx, IdEncoders idEncoders, IDataStreamInfo dsInfo) throws IOException
    {
        // /datastreams/{id}/schema?obsFormat=application/swe+flatbuffers → JSON logical schema
        return new DataStreamSchemaBindingFlatBuffers(ctx, idEncoders, false);
    }


    @Override
    public ResourceBinding<CommandStreamKey, ICommandStreamInfo> getCommandSchemaBinding(RequestContext ctx, IdEncoders idEncoders, ICommandStreamInfo csInfo) throws IOException
    {
        // /controlstreams/{id}/schema?commandFormat=application/swe+flatbuffers → JSON schema
        return new CommandStreamSchemaBindingFlatBuffers(ctx, idEncoders, false);
    }


    @Override
    public ResourceBinding<BigId, ICommandData> getCommandBinding(RequestContext ctx, IdEncoders idEncoders, ICommandStreamInfo csInfo, boolean forReading) throws IOException
    {
        return new CommandBindingFlatBuffers(ctx, idEncoders, forReading, csInfo);
    }
}
