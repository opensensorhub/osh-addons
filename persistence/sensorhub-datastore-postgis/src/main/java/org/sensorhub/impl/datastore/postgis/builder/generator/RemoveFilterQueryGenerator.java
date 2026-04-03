/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.builder.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RemoveFilterQueryGenerator extends FilterQueryGenerator {
    protected List<String> using;

    public String toQuery() {
        StringBuilder sb = new StringBuilder();
        sb.append("DELETE FROM ").append(this.tableName);

        if (this.using != null && !this.using.isEmpty()) {
            // add Inner join and its corresponding conditions
            sb.append(" USING ");
            sb.append(this.using.stream().collect(Collectors.joining(",")));
        }

        // check WHERE clause
        if (this.addConditions != null && !this.addConditions.isEmpty()) {
            sb.append(" WHERE ");
            sb.append(this.addConditions.stream().collect(Collectors.joining(" AND ")));
        }
        if (this.orConditions != null && !this.orConditions.isEmpty()) {
            if (addConditions == null || addConditions.isEmpty()) {
                sb.append(" WHERE ");
            }
            sb.append(this.orConditions.stream().collect(Collectors.joining(" OR ")));
        }
        return sb.toString();
    }

    private void checkUsing() {
        if (this.using == null) {
            this.using = new ArrayList<>();
        }
    }

    public void addUsing(String using) {
        this.checkUsing();
        this.using.add(using);
    }
}
