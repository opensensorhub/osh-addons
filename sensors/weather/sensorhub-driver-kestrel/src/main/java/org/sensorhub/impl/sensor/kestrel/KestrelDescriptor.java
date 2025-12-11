/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2025 the Initial Developer. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.kestrel;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.JarModuleProvider;


/**
 * <p>
 * Descriptor of Android sensors driver module for automatic discovery
 * by the ModuleRegistry
 * </p>
 *
 * @author Kalyn Stricklin
 * @since Dec 1, 2025
 */
public class KestrelDescriptor extends JarModuleProvider implements IModuleProvider {

    @Override
    public String getModuleName() {
        return "Kestrel Driver";
    }


    @Override
    public String getModuleDescription() {
        return "Driver supporting Kestrel Weather Meter";
    }


    @Override
    public String getModuleVersion() {
        return "0.1";
    }


    @Override
    public String getProviderName() {
        return "Bott's Innovative Research";
    }


    @Override
    public Class<? extends IModule<?>> getModuleClass() {
        return Kestrel.class;
    }


    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass() {
        return KestrelConfig.class;
    }

}
