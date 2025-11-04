/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.database;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.database.DatabaseConfig;
import org.sensorhub.impl.datastore.postgis.IdProviderType;

import javax.validation.constraints.Min;


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
    @DisplayInfo(label = "Database URL")
    public String url;

    @DisplayInfo(label="Database Name")
    public String dbName;

    @DisplayInfo(desc="Username")
    public String login;

    @DisplayInfo(desc="Password")
    @DisplayInfo.FieldType(DisplayInfo.FieldType.Type.PASSWORD)
    public String password;

    @DisplayInfo(label = "ID Generator", desc = "Method used to generate new resource IDs")
    public IdProviderType idProviderType = IdProviderType.SEQUENTIAL;

    @Min(value = 0)
    @DisplayInfo(desc = "Max delay between auto-commit execution, in seconds. 0 to disable time-based auto-commit")
    public int autoCommitPeriod = 10;

    @DisplayInfo(desc="Use Batch")
    public boolean useBatch = false;

    public PostgisDatabaseConfig()
    {
        this.moduleClass = PostgisObsSystemDatabase.class.getCanonicalName();
    }

}