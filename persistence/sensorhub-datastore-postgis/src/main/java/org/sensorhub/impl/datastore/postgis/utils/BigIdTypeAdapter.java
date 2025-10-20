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

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.sensorhub.api.common.BigId;

import java.io.IOException;

public class BigIdTypeAdapter extends TypeAdapter<BigId> {
    @Override
    public void write(JsonWriter out, BigId value) throws IOException {
        System.out.println(out);
        out.beginObject();
        out.name("scope").value(value.getScope());
        out.name("id").value(value.getIdAsLong());
        out.endObject();
    }

    @Override
    public BigId read(JsonReader in) throws IOException {
        long id = 0L;
        int scope = 0;
        while(in.hasNext()) {
            switch (in.nextName()) {
                case "scope": in.nextInt();break;
                case "id": in.nextLong();break;
            }
        }
        return BigId.fromLong(scope,id);
    }
}
