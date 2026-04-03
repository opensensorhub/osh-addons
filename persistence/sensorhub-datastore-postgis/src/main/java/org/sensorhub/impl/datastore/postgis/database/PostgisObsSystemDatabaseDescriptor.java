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

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.JarModuleProvider;


/**
 * <p>
 * Descriptor class for {@link PostgisObsSystemDatabaseDescriptor} module.
 * </p>
 *
 * @author Mathieu Dhainaut
 * @date Jul 25, 2023
 */
public class PostgisObsSystemDatabaseDescriptor extends JarModuleProvider
{
    @Override
    public String getModuleName()
    {
        return "PostGis Historical Obs Database";
    }
    

    @Override
    public String getModuleDescription()
    {
        return "Historical observation database backed by a PostGis database";
    }
    

    @Override
    public Class<? extends IModule<?>> getModuleClass()
    {
        return PostgisObsSystemDatabase.class;
    }
    

    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass()
    {
        return PostgisObsSystemDatabaseConfig.class;
    }
}
