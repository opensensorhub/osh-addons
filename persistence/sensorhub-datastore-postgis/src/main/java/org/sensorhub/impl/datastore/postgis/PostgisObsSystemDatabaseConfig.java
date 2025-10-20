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

import org.sensorhub.api.config.DisplayInfo;

import javax.validation.constraints.Min;


/**
 * <p>
 * Config class for {@link PostgisObsSystemDatabase} module
 * </p>
 *
 * @author Mathieu Dhainaut
 * @date Jul 25, 2023
 */
public class PostgisObsSystemDatabaseConfig extends PostgisDatabaseConfig
{
    @DisplayInfo(desc="URL database")
    public String url;

    @DisplayInfo(label="Dababase name", desc="Dabatase name")
    public String dbName;

    @DisplayInfo(desc="Login")
    public String login;

    @DisplayInfo(desc="Password")
    public String password;

    @DisplayInfo(label = "ID Generator", desc = "Method used to generate new resource IDs")
    public IdProviderType idProviderType = IdProviderType.SEQUENTIAL;

    @Min(value = 0)
    @DisplayInfo(desc = "Max delay between auto-commit execution, in seconds. 0 to disable time-based auto-commit")
    public int autoCommitPeriod = 10;

    public PostgisObsSystemDatabaseConfig()
    {
        this.moduleClass = PostgisObsSystemDatabase.class.getCanonicalName();
    }
}
