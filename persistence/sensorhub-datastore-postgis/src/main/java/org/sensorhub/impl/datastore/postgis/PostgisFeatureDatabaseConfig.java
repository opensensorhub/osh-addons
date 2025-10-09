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


/**
 * <p>
 * Config class for {@link PostgisObsSystemDatabase} module
 * </p>
 *
 * @author Mathieu Dhainaut
 * @date Sept 13, 2025
 */
public class PostgisFeatureDatabaseConfig extends PostgisDatabaseConfig {
    public PostgisFeatureDatabaseConfig()
    {
        this.moduleClass = PostgisFeatureDatabase.class.getCanonicalName();
    }
}
