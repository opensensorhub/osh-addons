/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2019 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sta;

import org.sensorhub.api.procedure.ProcedureFilter;
import de.fraunhofer.iosb.ilt.frostserver.query.Query;

/**
 * <p>
 * TODO DataStoreQueryBuilder type description
 * </p>
 *
 * @author Alex Robin
 * @date Sep 7, 2019
 */
public class DataStoreQueryBuilder
{
    // common properties
    static final String ID_PROPERTY = "id";
    static final String NAME_PROPERTY = "name";
    static final String DESC_PROPERTY = "description";

    // observations
    static final String PHENOMENON_TIME_PROPERTY = "phenomenonTime";
    static final String RESULT_TIME_PROPERTY = "resultTime";
    static final String VALID_TIME_PROPERTY = "validTime";
    
    // 1. identify requested resource type from URL path
    //    -> select data store
    
    // 2. create OSH filter from $filter and $top argument
    
    // 3. get result stream from datastore + apply $skip
    // optim: cache Streams and use them if same query filter and next $top = previous $top+$skip
    
    // 4. post process $select
    
    // 5. post process $expand (issue other queries from step 1 and then aggregate results)
    
    public ProcedureFilter buildSensorFilter(Query q)
    {
        return null;
    }
    
    
    
    
}
