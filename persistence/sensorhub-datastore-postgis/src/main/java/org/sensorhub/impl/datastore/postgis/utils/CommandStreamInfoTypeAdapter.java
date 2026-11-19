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

//import com.google.gson.TypeAdapter;
//import com.google.gson.stream.JsonReader;
//import com.google.gson.stream.JsonToken;
//import com.google.gson.stream.JsonWriter;
//import net.opengis.swe.v20.DataComponent;
//import net.opengis.swe.v20.DataEncoding;
//import org.sensorhub.api.command.CommandStreamInfo;
//import org.sensorhub.api.system.SystemId;
//import org.sensorhub.impl.service.consys.SWECommonUtils;
//import org.vast.swe.SWEJsonBindings;
//import org.vast.util.TimeExtent;
//
//import java.io.IOException;

/*
public class CommandStreamInfoTypeAdapter extends TypeAdapter<CommandStreamInfo> {
    SWEJsonBindings sweJsonBindings = new SWEJsonBindings(false);

    @Override
    public void write(JsonWriter writer, CommandStreamInfo dsInfo) throws IOException {
        writer.beginObject();
        writer.name("name").value(dsInfo.getName());
        if(dsInfo.getSystemID() != null) {
            PostgisUtils.Gson.toJson(dsInfo.getSystemID(), SystemId.class, writer.name("systemID"));
        }
        if(dsInfo.getValidTime() != null) {
            PostgisUtils.Gson.toJson(dsInfo.getValidTime(), TimeExtent.class, writer.name("validTime"));
        }
        if(dsInfo.getDescription() != null) {
            writer.name("description").value(dsInfo.getDescription());
        }

        // result structure & encoding
        try {
            writer.name("recordSchema");
            sweJsonBindings.writeDataComponent(writer, dsInfo.getRecordStructure(), false);
        } catch (Exception e) {
            throw new IOException("Error writing SWE Common record structure", e);
        }
        PostgisUtils.Gson.toJson(dsInfo.getRecordEncoding(), DataEncoding.class, writer.name("recordEncoding"));

        writer.endObject();
        writer.flush();
    }

    @Override
    public CommandStreamInfo read(JsonReader reader) throws IOException {

        CommandStreamInfo.CommandStreamInfoBuilder<CommandStreamInfo.Builder, CommandStreamInfo> commandStreamInfoBuilder =
                new CommandStreamInfo.Builder().withName(SWECommonUtils.NO_NAME) // name will be set later
                .withSystem(SystemId.NO_SYSTEM_ID); // System ID will be set later
        try {
            // read BEGIN_OBJECT only if not already read by caller
            // this happens when reading embedded schema and auto-detecting obs format
            if (reader.peek() == JsonToken.BEGIN_OBJECT)
                reader.beginObject();

            DataComponent resultStruct = null;
            String name = SWECommonUtils.NO_NAME;
            while (reader.hasNext()) {
                var prop = reader.nextName();

                if ("recordSchema".equals(prop)) {
                    resultStruct = sweJsonBindings.readDataComponent(reader);
                } else if ("recordEncoding".equals(prop)) {
                    commandStreamInfoBuilder = commandStreamInfoBuilder.withRecordEncoding(PostgisUtils.Gson.fromJson(reader,DataEncoding.class));
                } else if ("systemID".equals(prop)) {
                    commandStreamInfoBuilder = commandStreamInfoBuilder.withSystem(PostgisUtils.Gson.fromJson(reader,SystemId.class));
                } else if ("name".equals(prop)) {
                    name = reader.nextString();
                    commandStreamInfoBuilder = commandStreamInfoBuilder.withName(name);
                } else if ("description".equals(prop)) {
                    commandStreamInfoBuilder = commandStreamInfoBuilder.withDescription(reader.nextString());
                } else if ("validTime".equals(prop)) {
                    commandStreamInfoBuilder = commandStreamInfoBuilder.withValidTime(PostgisUtils.Gson.fromJson(reader,TimeExtent.class));
                }
                else {
                    reader.skipValue();
                }
            }
            reader.endObject();
            if(resultStruct != null) {
                resultStruct.setName(name);
                commandStreamInfoBuilder = commandStreamInfoBuilder.withRecordDescription(resultStruct);
            }
        } catch (IOException | IllegalStateException e) {
            throw new IOException(e.getMessage());
        }

        return commandStreamInfoBuilder.build();
    }
}
*/