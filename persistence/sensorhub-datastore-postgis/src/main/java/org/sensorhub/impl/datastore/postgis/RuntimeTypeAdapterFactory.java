/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis;

import com.google.gson.*;
import com.google.gson.internal.Streams;
import com.google.gson.internal.bind.JsonTreeReader;
import com.google.gson.internal.bind.JsonTreeWriter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public final class RuntimeTypeAdapterFactory<T> implements TypeAdapterFactory {
    private static final Logger log = LoggerFactory.getLogger(RuntimeTypeAdapterFactory.class);

    private final Class<?> baseType;
    private final String typeFieldName;


    public RuntimeTypeAdapterFactory(Class<?> baseType, String typeFieldName) {
        if (typeFieldName == null || baseType == null)
            throw new NullPointerException();

        this.baseType = baseType;
        this.typeFieldName = typeFieldName;
    }


    @Override
    public <R> TypeAdapter<R> create(final Gson gson, final TypeToken<R> type) {
        if (baseType != Object.class && !type.getRawType().isInstance(baseType))
            return null;

        return new TypeAdapter<R>() {
            @Override
            public R read(JsonReader in) throws IOException {
                JsonElement jsonElement = Streams.parse(in);
                TypeAdapter<R> delegate = gson.getDelegateAdapter(RuntimeTypeAdapterFactory.this, type);

                if (jsonElement.isJsonObject()) {
                    JsonElement typeField = jsonElement.getAsJsonObject().remove(typeFieldName);

                    if (typeField != null) {
                        String type = typeField.getAsString();

                        try {
                            @SuppressWarnings("unchecked")
                            Class<R> runtimeClass = (Class<R>) Class.forName(type);
                            delegate = gson.getDelegateAdapter(RuntimeTypeAdapterFactory.this, TypeToken.get(runtimeClass));
                        } catch (ClassNotFoundException e) {
                            throw new IllegalStateException("Runtime class specified in JSON is invalid: " + type, e);
                        }
                    }
                }

                JsonReader jsonReader = new JsonTreeReader(jsonElement);
                jsonReader.setLenient(true);
                return delegate.read(jsonReader);
            }

            @Override
            public void write(JsonWriter out, R value) throws IOException {
                @SuppressWarnings("unchecked")
                Class<R> runtimeClass = (Class<R>) value.getClass();
                String typeName = runtimeClass.getName();
                TypeAdapter<R> delegate = gson.getDelegateAdapter(RuntimeTypeAdapterFactory.this, TypeToken.get(runtimeClass));

                //JsonElement jsonElt = delegate.toJsonTree(value); // JsonTreeWriter is not lenient in this case
                JsonTreeWriter jsonWriter = new JsonTreeWriter();
                jsonWriter.setLenient(true);
                jsonWriter.setSerializeNulls(false);
                delegate.write(jsonWriter, value);
                JsonElement jsonElt = jsonWriter.get();

                if (jsonElt.isJsonObject()) {
                    JsonObject jsonObject = jsonElt.getAsJsonObject();
                    JsonObject clone = new JsonObject();

                    // insert class name as first attribute
                    clone.add(typeFieldName, new JsonPrimitive(typeName));
                    for (Map.Entry<String, JsonElement> e : jsonObject.entrySet())
                        clone.add(e.getKey(), e.getValue());

                    jsonElt = clone;
                }

                Streams.write(jsonElt, out);
            }
        }.nullSafe();
    }
}
