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

package org.sensorhub.impl.sensor.axis;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;

/**
 * <p>
 * Implementation of sensor interface for generic Axis Cameras using IP
 * protocol. This particular class provides a description of the Axis
 * IP video camera module.
 * </p>
 *
 * <p>
 * Copyright (c) 2014
 * </p>
 * 
 * @author Mike Botts <mike.botts@botts-inc.com>
 * @since October 30, 2014
 */


public class AxisCameraModuleDescriptor implements IModuleProvider
{

	@Override
	public String getModuleName()
	{
		return "Axis IP Video Camera";
	}

	@Override
	public String getModuleDescription()
	{
		return "Supports access to video and tasking of Pan-Tilt-Zoom gimbal for any Axis video camera using IP protocol";
	}

	@Override
	public String getModuleVersion()
	{
		return "0.1";
	}

	@Override
	public String getProviderName()
	{
		return "Botts Innovative Research Inc.";
	}

	@Override
	public Class<? extends IModule<?>> getModuleClass()
	{
		return AxisCameraDriver.class;	
	}

	@Override
	public Class<? extends ModuleConfig> getModuleConfigClass()
	{
	       return AxisCameraConfig.class;
	}

}
