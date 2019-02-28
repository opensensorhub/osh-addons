/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Botts Innovative Research Inc.. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.foscam;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.JarModuleProvider;

/**
 * <p>
 * Implementation of sensor interface for Foscam Cameras using IP
 * protocol. This particular class provides a description of the Foscam
 * IP video camera module.
 * </p>
 *
 * <p>
 * Copyright (c) 2016
 * </p>
 * 
 * @author Lee Butler <labutler10@gmail.com>
 * @since September 2016
 */


public class FoscamModuleDescriptor extends JarModuleProvider implements IModuleProvider
{

	@Override
	public String getModuleName()
	{
		return "Foscam IP Video Camera";
	}

	@Override
	public String getModuleDescription()
	{
		return "Supports access to video and tasking of Pan-Tilt-Zoom gimbal for Foscam FI9821P video camera using IP protocol";
	}

	@Override
	public String getProviderName()
	{
		return "Botts Innovative Research Inc.";
	}

	@Override
	public Class<? extends IModule<?>> getModuleClass()
	{
		return FoscamDriver.class;	
	}

	@Override
	public Class<? extends ModuleConfig> getModuleConfigClass()
	{
	       return FoscamConfig.class;
	}

}
