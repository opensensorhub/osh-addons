/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research, LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package com.georobotix.impl.service.mcp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.jetty.server.Request;
import org.sensorhub.api.command.ICommandData;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.api.command.ICommandStreamInfo;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.IdEncoders;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.command.CommandStreamKey;
import org.sensorhub.api.datastore.feature.FeatureKey;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.system.ISystemWithDesc;
import org.sensorhub.impl.common.IdEncodersBase32;
import org.sensorhub.impl.service.consys.obs.DataStreamBindingJson;
import org.sensorhub.impl.service.consys.obs.ObsBindingOmJson;
import org.sensorhub.impl.service.consys.obs.ObsHandler;
import org.sensorhub.impl.service.consys.resource.RequestContext;
import org.sensorhub.impl.service.consys.resource.ResourceFormat;
import org.sensorhub.impl.service.consys.system.SystemBindingGeoJson;
import org.sensorhub.impl.service.consys.task.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * Small adapter for using the same ConSys JSON bindings/parsers that back the Connected Systems API service.
 */
public class ConSysJsonHelper {

    private final IObsSystemDatabase db;
    private final IdEncoders idEncoders;
    private final Gson gson;

    public ConSysJsonHelper(IObsSystemDatabase db) {
        this(db, new IdEncodersBase32());
    }

    public ConSysJsonHelper(IObsSystemDatabase db, IdEncoders idEncoders) {
        this.db = db;
        this.idEncoders = idEncoders;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .serializeNulls()
                .create();
    }

    public String writeSystem(ISystemWithDesc system, FeatureKey key, boolean showLinks) {
        return write(buffer -> {
            var ctx = new RequestContext(buffer);
            var binding = new SystemBindingGeoJson(ctx, idEncoders, db, false);
            binding.serialize(key, system, showLinks);
        }, "system");
    }

    public String writeDataStream(IDataStreamInfo dataStream, DataStreamKey key, boolean showLinks) {
        return write(buffer -> {
            var ctx = new RequestContext(buffer);
            var binding = new DataStreamBindingJson(ctx, idEncoders, db, false, Collections.emptyMap());
            binding.serialize(key, dataStream, showLinks);
        }, "datastream");
    }

    public String writeObservation(IObsData observation, BigId key, boolean showLinks) {
        return write(buffer -> {
            var ctx = new RequestContext(buffer);
            var binding = new ObsBindingOmJson(ctx, idEncoders, false, db.getObservationStore());
            binding.serialize(key, observation, showLinks);
        }, "observation");
    }

    public String writeCommandStream(ICommandStreamInfo commandStream, CommandStreamKey key, boolean showLinks) {
        return write(buffer -> {
            var ctx = new RequestContext(buffer);
            var binding = new CommandStreamBindingJson(ctx, idEncoders, db, false);
            binding.serialize(key, commandStream, showLinks);
        }, "command stream");
    }

    public String writeCommand(ICommandData command, BigId key, boolean showLinks) {
        return write(buffer -> {
            var ctx = new RequestContext(buffer);
            var contextData = new CommandHandler.CommandHandlerContextData();
            contextData.csInfo = db.getCommandStreamStore().get(new CommandStreamKey(command.getCommandStreamID()));
            ctx.setData(contextData);
            var binding = new CommandBindingJson(ctx, idEncoders, false, db.getCommandStore());
            binding.serialize(key, command, showLinks);
        }, "command");
    }

    public String writeCommandStatus(ICommandStatus commandStatus, BigId key, boolean showLinks) {
        return write(buffer -> {
           var ctx = new RequestContext(buffer);
           var binding = new CommandStatusBindingJson(ctx, idEncoders, false, db.getCommandStatusStore());
            binding.serialize(key, commandStatus, showLinks);
        }, "command status");
    }

    public IObsData readObservation(IDataStreamInfo dataStream, BigId dataStreamId, Instant phenomenonTime,
                                   BigId foiId, Map<String, Object> resultData) {
        var json = gson.toJson(Map.of(
                "phenomenonTime", phenomenonTime.toString(),
                "result", resultData
        ));

        try {
            var ctx = new RequestContext(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
            var contextData = new ObsHandler.ObsHandlerContextData();
            contextData.dsID = dataStreamId;
            contextData.dsInfo = dataStream;
            contextData.foiId = foiId != null ? foiId : BigId.NONE;
            ctx.setData(contextData);
            ctx.setFormat(ResourceFormat.OM_JSON);
            var binding = new ObsBindingOmJson(ctx, idEncoders, true, db.getObservationStore());
            return binding.deserialize();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to parse observation JSON: " + e.getMessage(), e);
        }
    }

    public Object parseJson(String json) {
        return gson.fromJson(json, Object.class);
    }

    public Object parseJsonOrRaw(String json) {
        try {
            return parseJson(json);
        } catch (Exception e) {
            return Map.of("raw", json);
        }
    }

    public String toJson(Object value) {
        return gson.toJson(value);
    }

    private String write(WriterAction action, String resourceType) {
        try {
            var buffer = new ByteArrayOutputStream();
            action.write(buffer);
            return buffer.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to serialize " + resourceType + ": " + e.getMessage(), e);
        }
    }

    @FunctionalInterface
    private interface WriterAction {
        void write(ByteArrayOutputStream buffer) throws IOException;
    }
}
