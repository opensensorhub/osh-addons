/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.utils;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.opengis.swe.v20.*;
import org.sensorhub.api.command.*;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.data.DataStreamInfo;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.feature.FeatureId;
import org.sensorhub.impl.datastore.DataStoreFiltersTypeAdapterFactory;
import org.sensorhub.impl.datastore.postgis.RuntimeTypeAdapterFactory;
import org.sensorhub.impl.service.consys.SWECommonUtils;
import org.vast.cdm.common.DataStreamWriter;
import org.vast.ogc.gml.GeoJsonBindings;
import org.vast.ogc.gml.IFeature;
import org.vast.sensorML.SMLJsonBindings;
import org.vast.swe.AbstractDataWriter;
import org.vast.swe.BinaryDataWriter;
import org.vast.swe.SWEJsonBindings;
import org.vast.swe.fast.JsonDataParserGson;
import org.vast.swe.fast.JsonDataWriterGson;
import org.vast.util.DateTimeFormat;
import org.vast.util.TimeExtent;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class   SerializerUtils {

    static final GsonBuilder builder = new GsonBuilder()
            .setLenient()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .serializeSpecialFloatingPointValues()
            .registerTypeAdapter(BigId.class, new BigIdTypeAdapter())
            .setFieldNamingStrategy(new DataStoreFiltersTypeAdapterFactory.FieldNamingStrategy())
            .registerTypeAdapterFactory(new RuntimeTypeAdapterFactory<>(Object.class, "objClass"))
            .setExclusionStrategies(new DataStreamExclusionStrategy());

//    public static final Gson Gson = builder.create();

    private static final  ThreadLocal<GeoJsonBindings> geoJsonBindings = ThreadLocal.withInitial(() -> new GeoJsonBindings(true));
    private static final  ThreadLocal<SWEJsonBindings> sweJsonBindings = ThreadLocal.withInitial(() -> new SWEJsonBindings(false));

    private static final  ThreadLocal<SMLJsonBindings> smlJsonBindings  = ThreadLocal.withInitial(() -> new SMLJsonBindings(false));

    private SerializerUtils() {
    }

    public static String writeDataBlockToJson(DataComponent dataComponent, DataEncoding dataEncoding, DataBlock dataBlock) {
        DataStreamWriter dataWriter;
        if(dataEncoding instanceof BinaryEncoding) {
            dataWriter = new BinaryDataWriter();
        } else {
            dataWriter = new JsonDataWriterGson();
        }
        // set datastream schema
        dataWriter.setDataComponents(dataComponent);
        dataWriter.setDataEncoding(dataEncoding);
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            dataWriter.setOutput(bos);
            dataWriter.write(dataBlock);
            dataWriter.flush();
            return bos.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static DataBlock readDataBlockFromJson(DataComponent dataComponent, String json) {
        JsonDataParserGson jsonDataParserGson = new JsonDataParserGson();
        // set datastream schema
        jsonDataParserGson.setDataComponents(dataComponent);

        try (ByteArrayInputStream bis = new ByteArrayInputStream(json.getBytes())) {
            jsonDataParserGson.setInput(bis);
            return jsonDataParserGson.parseNextBlock();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String writeIFeatureToJson(IFeature feature) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            try (var osw = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                try (JsonWriter jsonWriter = new JsonWriter(osw)) {
                    jsonWriter.setLenient(true);
                    jsonWriter.setSerializeNulls(false);
                    jsonWriter.setIndent("  ");
                    // start writing
                    geoJsonBindings.get().writeFeature(jsonWriter, feature);
                    jsonWriter.flush();
                    return os.toString();
                }
            }
        }
    }

    public static IFeature readIFeatureFromJson(String json) throws IOException {
        try (var bis = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))) {
            try (var osr = new InputStreamReader(bis, StandardCharsets.UTF_8)) {
                try (JsonReader jsonReader = new JsonReader(osr)) {
                    jsonReader.setLenient(true);
                    return geoJsonBindings.get().readFeature(jsonReader);
                }
            }
        }
    }

    public static IDataStreamInfo readIDataStreamInfoFromJson(String json) throws IOException {
        StringReader stringReader = new StringReader(json);
        JsonReader jsonReader = new JsonReader(stringReader);

        DataStreamInfo.DataStreamInfoBuilder<DataStreamInfo.Builder, DataStreamInfo> dataStreamInfoBuilder =
                new DataStreamInfo.Builder().withName(SWECommonUtils.NO_NAME) // name will be set later
                        .withSystem(FeatureId.NULL_FEATURE); // System ID will be set later
        try {
            // read BEGIN_OBJECT only if not already read by caller
            // this happens when reading embedded schema and auto-detecting obs format
            if (jsonReader.peek() == JsonToken.BEGIN_OBJECT)
                jsonReader.beginObject();

            DataComponent resultStruct = null;
            String name = SWECommonUtils.NO_NAME;
            while (jsonReader.hasNext()) {
                var prop = jsonReader.nextName();

                if ("recordSchema".equals(prop)) {
                    resultStruct = sweJsonBindings.get().readDataComponent(jsonReader);
                } else if ("recordEncoding".equals(prop)) {
                    DataEncoding dataEncoding = sweJsonBindings.get().readEncoding(jsonReader);
                    dataStreamInfoBuilder = dataStreamInfoBuilder.withRecordEncoding(dataEncoding);
                } else if ("system@id".equals(prop)) {
                    dataStreamInfoBuilder = dataStreamInfoBuilder.withSystem(readSystemID(jsonReader));
                } else if ("outputName".equals(prop)) {
                    name = jsonReader.nextString();
                    dataStreamInfoBuilder = dataStreamInfoBuilder.withName(name);
                }  else if ("description".equals(prop)) {
                    dataStreamInfoBuilder = dataStreamInfoBuilder.withDescription(jsonReader.nextString());
                } else if ("validTime".equals(prop)) {
                    dataStreamInfoBuilder = dataStreamInfoBuilder.withValidTime(readTimeExtent(jsonReader));
                } else {
                    jsonReader.skipValue();
                }
            }
            jsonReader.endObject();
            if (resultStruct != null) {
                resultStruct.setName(name);
                dataStreamInfoBuilder = dataStreamInfoBuilder.withRecordDescription(resultStruct);
            }
        } catch (IOException | IllegalStateException e) {
            throw new IOException(e.getMessage());
        }

        return dataStreamInfoBuilder.build();
    }

    public static String writeIDataStreamInfoToJson(IDataStreamInfo dsInfo) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            try (var osw = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                try (JsonWriter jsonWriter = new JsonWriter(osw)) {
                    jsonWriter.beginObject();
                    jsonWriter.setLenient(true);
                    jsonWriter.setSerializeNulls(false);
                    jsonWriter.setIndent("  ");
                    // start writing
                    jsonWriter.name("name").value(dsInfo.getName());
                    jsonWriter.name("outputName").value(dsInfo.getOutputName());

                    if (dsInfo.getSystemID() != null) {
                        SerializerUtils.writeSystemID(jsonWriter, "system@id", dsInfo.getSystemID());
                    }
                    if(dsInfo.getValidTime() != null) {
                        SerializerUtils.writeTimeExtent(jsonWriter,"validTime",dsInfo.getValidTime());
                    }
                    if (!Strings.isNullOrEmpty(dsInfo.getDescription())) {
                        jsonWriter.name("description").value(dsInfo.getDescription());
                    }

                    if (dsInfo.getRecordEncoding() != null) {
                        jsonWriter.name("recordEncoding");
                        sweJsonBindings.get().writeAbstractEncoding(jsonWriter, dsInfo.getRecordEncoding());
                    }

                    jsonWriter.name("recordSchema");
                    sweJsonBindings.get().writeDataComponent(jsonWriter, dsInfo.getRecordStructure(), false);
                    jsonWriter.endObject();
                    jsonWriter.flush();
                    return os.toString();
                } catch (Exception e) {
                    throw new IOException("Error writing SWE Common record structure", e);
                }
            }
        }
    }

    public static ICommandStreamInfo readICommandStreamInfoFromJson(String json) throws IOException {
        StringReader stringReader = new StringReader(json);
        JsonReader jsonReader = new JsonReader(stringReader);

        CommandStreamInfo.CommandStreamInfoBuilder<CommandStreamInfo.Builder, CommandStreamInfo> commandStreamInfoBuilder =
                new CommandStreamInfo.Builder().withName(SWECommonUtils.NO_NAME) // name will be set later
                        .withSystem(FeatureId.NULL_FEATURE); // System ID will be set later
        try {
            // read BEGIN_OBJECT only if not already read by caller
            // this happens when reading embedded schema and auto-detecting obs format
            if (jsonReader.peek() == JsonToken.BEGIN_OBJECT)
                jsonReader.beginObject();

            DataComponent resultStruct = null;
            String name = SWECommonUtils.NO_NAME;
            while (jsonReader.hasNext()) {
                var prop = jsonReader.nextName();

                if ("recordSchema".equals(prop)) {
                    resultStruct = sweJsonBindings.get().readDataComponent(jsonReader);
                } else if ("recordEncoding".equals(prop)) {
                    DataEncoding dataEncoding = sweJsonBindings.get().readEncoding(jsonReader);
                    commandStreamInfoBuilder = commandStreamInfoBuilder.withRecordEncoding(dataEncoding);
                } else if ("system@id".equals(prop)) {
                    commandStreamInfoBuilder = commandStreamInfoBuilder.withSystem(readSystemID(jsonReader));
                } else if ("name".equals(prop)) {
                    name = jsonReader.nextString();
                    commandStreamInfoBuilder = commandStreamInfoBuilder.withName(name);
                } else if ("description".equals(prop)) {
                    commandStreamInfoBuilder = commandStreamInfoBuilder.withDescription(jsonReader.nextString());
                } else if ("validTime".equals(prop)) {
                    commandStreamInfoBuilder = commandStreamInfoBuilder.withValidTime(readTimeExtent(jsonReader));
                } else {
                    jsonReader.skipValue();
                }
            }
            jsonReader.endObject();
            if (resultStruct != null) {
                resultStruct.setName(name);
                commandStreamInfoBuilder = commandStreamInfoBuilder.withRecordDescription(resultStruct);
            }
        } catch (IOException | IllegalStateException e) {
            throw new IOException(e.getMessage());
        }

        return commandStreamInfoBuilder.build();
    }

    public static String writeICommandStreamInfoToJson(ICommandStreamInfo commandStreamInfo) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            try (var osw = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                try (JsonWriter jsonWriter = new JsonWriter(osw)) {
                    jsonWriter.beginObject();
                    jsonWriter.setLenient(true);
                    jsonWriter.setSerializeNulls(false);
                    jsonWriter.setIndent("  ");
                    // start writing
                    jsonWriter.name("name").value(commandStreamInfo.getName());

                    if (commandStreamInfo.getSystemID() != null) {
                        SerializerUtils.writeSystemID(jsonWriter, "system@id", commandStreamInfo.getSystemID());
                    }
                    if(commandStreamInfo.getValidTime() != null) {
                        SerializerUtils.writeTimeExtent(jsonWriter,"validTime",commandStreamInfo.getValidTime());
                    }
                    if (!Strings.isNullOrEmpty(commandStreamInfo.getDescription())) {
                        jsonWriter.name("description").value(commandStreamInfo.getDescription());
                    }

                    if (commandStreamInfo.getRecordEncoding() != null) {
                        jsonWriter.name("recordEncoding");
                        sweJsonBindings.get().writeAbstractEncoding(jsonWriter, commandStreamInfo.getRecordEncoding());
                    }

                    jsonWriter.name("recordSchema");
                    sweJsonBindings.get().writeDataComponent(jsonWriter, commandStreamInfo.getRecordStructure(), false);
                    jsonWriter.endObject();
                    jsonWriter.flush();
                    return os.toString();
                } catch (Exception e) {
                    throw new IOException("Error writing SWE Common record structure", e);
                }
            }
        }
    }

    // CommandStatus
    public static ICommandStatus readICommandStatusFromJson(String json) throws IOException {
        StringReader stringReader = new StringReader(json);
        JsonReader jsonReader = new JsonReader(stringReader);

        CommandStatus.CommandStatusBuilder<CommandStatus.Builder, CommandStatus> commandStatusBuilder =
                new CommandStatus.Builder();

        try {
            // read BEGIN_OBJECT only if not already read by caller
            // this happens when reading embedded schema and auto-detecting obs format
            if (jsonReader.peek() == JsonToken.BEGIN_OBJECT)
                jsonReader.beginObject();

            while (jsonReader.hasNext()) {
                var prop = jsonReader.nextName();

                if ("message".equals(prop)) {
                    commandStatusBuilder = commandStatusBuilder.withMessage(jsonReader.nextString());
                } else if ("command@id".equals(prop)) {
                    commandStatusBuilder = commandStatusBuilder.withCommand(readBigId(jsonReader));
                } else if ("statusCode".equals(prop)) {
                    commandStatusBuilder = commandStatusBuilder.withStatusCode(ICommandStatus.CommandStatusCode.valueOf(jsonReader.nextString()));
                } else if ("progress".equals(prop)) {
                    commandStatusBuilder = commandStatusBuilder.withProgress(jsonReader.nextInt());
                } else if ("executionTime".equals(prop)) {
                    commandStatusBuilder = commandStatusBuilder.withExecutionTime(readTimeExtent(jsonReader));
                } else if ("reportTime".equals(prop)) {
                    commandStatusBuilder = commandStatusBuilder.withReportTime(PostgisUtils.readInstantFromString(jsonReader.nextString()));
                } else if ("commandResult".equals(prop)) {
                    commandStatusBuilder = commandStatusBuilder.withResult(readICommandResult(jsonReader));
                } else {
                    jsonReader.skipValue();
                }
            }
            jsonReader.endObject();
        } catch (IOException | IllegalStateException e) {
            throw new IOException(e.getMessage());
        }

        return commandStatusBuilder.build();
    }

    public static String writeICommandStatusToJson(ICommandStatus commandStatus) throws IOException{
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            try (var osw = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                try (JsonWriter jsonWriter = new JsonWriter(osw)) {
                    jsonWriter.beginObject();
                    jsonWriter.setLenient(true);
                    jsonWriter.setSerializeNulls(false);
                    jsonWriter.setIndent("  ");
                    // start writing
                    if(!Strings.isNullOrEmpty(commandStatus.getMessage())) {
                        jsonWriter.name("message").value(commandStatus.getMessage());
                    }
                    if(commandStatus.getCommandID() != null) {
                        writeBigId(jsonWriter, "command@id", commandStatus.getCommandID());
                    }
                    if(commandStatus.getStatusCode() != null) {
                        jsonWriter.name("statusCode").value(commandStatus.getStatusCode().name());
                    }

                    jsonWriter.name("progress").value(commandStatus.getProgress());

                    if(commandStatus.getExecutionTime() != null) {
                        writeTimeExtent(jsonWriter, "executionTime", commandStatus.getExecutionTime());
                    }

                    if(commandStatus.getReportTime() != null) {
                        jsonWriter.name("reportTime").value(PostgisUtils.writeInstantToString(commandStatus.getReportTime()));
                    }

                    if(commandStatus.getResult() != null) {
                        writeICommandResult(jsonWriter, "commandResult",commandStatus.getResult());
                    }

                    jsonWriter.endObject();
                    jsonWriter.flush();
                    return os.toString();
                }
            }
        } catch (Exception e) {
            throw new IOException("Error writing ICommandStatus structure", e);
        }
    }

    protected static void writeICommandResult(JsonWriter jsonWriter, String nodeName, ICommandResult commandResult) throws IOException {
        jsonWriter.beginObject();
        jsonWriter.name(nodeName);
        writeBigId(jsonWriter, "datastream@id",commandResult.getDataStreamID());

        jsonWriter.name("observationRefs");
        jsonWriter.beginArray();
        for(BigId obsId: commandResult.getObservationRefs()) {
            writeBigId(jsonWriter, null, obsId);
        }
        jsonWriter.endArray();
        jsonWriter.endObject();
    }

    protected static ICommandResult readICommandResult(JsonReader jsonReader) throws IOException {
        BigId dataStreamId = null;
        List<BigId> obsRef = null;

        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            switch (jsonReader.nextName()) {
                case "datastream@id":
                    dataStreamId  = readBigId(jsonReader);
                    break;
                case "observationRefs":
                    obsRef = new ArrayList<>();
                    jsonReader.beginArray();
                    while (jsonReader.hasNext()) {
                        obsRef.add(readBigId(jsonReader));
                    }
                    jsonReader.endArray();
                    break;
            }
        }
        jsonReader.endObject();
        return CommandResult.withObsInDatastream(dataStreamId, obsRef);
    }
    //
    protected static void writeBigId(JsonWriter jsonWriter, String nodeName, BigId bigId) throws IOException {
        if(nodeName != null) {
            jsonWriter.name(nodeName);
        }
        jsonWriter.beginObject();
        jsonWriter.name("scope").value(bigId.getScope());
        jsonWriter.name("id").value(bigId.getIdAsLong());
        jsonWriter.endObject();
    }

    protected static void writeSystemID(JsonWriter jsonWriter, String nodeName, FeatureId systemId) throws IOException {
        jsonWriter.name(nodeName);
        jsonWriter.beginObject();
        jsonWriter.name("uniqueID").value(systemId.getUniqueID());
        writeBigId(jsonWriter, "internalID", systemId.getInternalID());
        jsonWriter.endObject();
    }

    protected static BigId readBigId(JsonReader jsonReader) throws IOException {
        long id = 0L;
        int scope = 0;
        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            switch (jsonReader.nextName()) {
                case "scope":
                    scope = jsonReader.nextInt();
                    break;
                case "id":
                    id = jsonReader.nextLong();
                    break;
            }
        }
        jsonReader.endObject();
        return BigId.fromLong(scope, id);
    }

    protected static FeatureId readSystemID(JsonReader jsonReader) throws IOException {
        BigId internalID = null;
        String uniqueID = null;
        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            switch (jsonReader.nextName()) {
                case "uniqueID":
                    uniqueID = jsonReader.nextString();
                    break;
                case "internalID":
                    internalID = readBigId(jsonReader);
                    break;
            }
        }

        jsonReader.endObject();
        return new FeatureId(internalID, uniqueID);
    }

    public static String writeTimeExtent(TimeExtent timeExtent) throws IOException {

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            try (var osw = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                try (JsonWriter jsonWriter = new JsonWriter(osw)) {
                    jsonWriter.beginObject();
                    jsonWriter.setLenient(true);
                    jsonWriter.setSerializeNulls(false);
                    jsonWriter.setIndent("  ");

                    if(timeExtent.beginsNow()) {
                        jsonWriter.name("begin").value("now");
                        jsonWriter.name("end").value(PostgisUtils.writeInstantToString(timeExtent.end()));
                    } else if(timeExtent.endsNow()) {
                        jsonWriter.name("begin").value(PostgisUtils.writeInstantToString(timeExtent.begin()));
                        jsonWriter.name("end").value("now");
                    } else  {
                        jsonWriter.name("begin").value(PostgisUtils.writeInstantToString(timeExtent.begin()));
                        jsonWriter.name("end").value(PostgisUtils.writeInstantToString(timeExtent.end()));
                    }

                    jsonWriter.endObject();
                    jsonWriter.flush();
                    return os.toString();
                }
            }
        }
    }

    public static void writeTimeExtent(JsonWriter jsonWriter, String nodeName, TimeExtent timeExtent) throws IOException {
        jsonWriter.name(nodeName);
        jsonWriter.beginObject();
        if(timeExtent.beginsNow()) {
            jsonWriter.name("begin").value("now");
            jsonWriter.name("end").value(PostgisUtils.writeInstantToString(timeExtent.end()));
        } else if(timeExtent.endsNow()) {
            jsonWriter.name("begin").value(PostgisUtils.writeInstantToString(timeExtent.begin()));
            jsonWriter.name("end").value("now");
        } else  {
            jsonWriter.name("begin").value(PostgisUtils.writeInstantToString(timeExtent.begin()));
            jsonWriter.name("end").value(PostgisUtils.writeInstantToString(timeExtent.end()));
        }

        jsonWriter.endObject();

    }

    public static TimeExtent readTimeExtent(JsonReader jsonReader) throws IOException {
        String begin = null;
        String end = null;

        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            switch (jsonReader.nextName()) {
                case "begin": {
                    begin = jsonReader.nextString();
                    break;
                }

                case "end":
                    end = jsonReader.nextString();
                    break;
            }
        }
        jsonReader.endObject();

        // check special values
        Instant beginInstant = null;
        Instant endInstant = null;

        // beginsNow
        if(begin.equalsIgnoreCase("now")) {
            endInstant = PostgisUtils.readInstantFromString(end);
            return TimeExtent.beginNow(endInstant);
        } else if(end.equalsIgnoreCase("now")) {
            beginInstant = PostgisUtils.readInstantFromString(begin);
            return TimeExtent.endNow(beginInstant);
        } else {
            return TimeExtent.period(PostgisUtils.readInstantFromString(begin), PostgisUtils.readInstantFromString(end));
        }
    }
}
