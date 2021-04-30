/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.misb.stanag4609.tags;

import com.google.gson.JsonObject;

/**
 * Interface specifying contract for performing a function similar to toString
 * but formatting data as key, value pairs in JSON format.
 *
 * @author Nicolas Garay
 * @since Feb. 3, 2020
 */
public interface JsonPrinter {

    /**
     * Creates a JSON string representation of the data.
     *
     * @return a JSON formatted string representation of the the data values.
     */
    String toJsonString();

    /**
     * Creates a JSON Object representation of the data.
     *
     * @return a JSON object containing the data.
     */
    JsonObject getAsJsonObject();
}
