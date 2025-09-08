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

import org.sensorhub.api.database.DatabaseConfig;


/**
 * <p>
 * Base config class for PostGis based database modules
 * </p>
 *
 * @author Mathieu Dhainaut
 * @date Jul 25, 2023
 */
public abstract class PostgisDatabaseConfig extends DatabaseConfig
{
    public PostgisDatabaseConfig()
    {
        this.moduleClass = PostgisObsSystemDatabase.class.getCanonicalName();
    }

}