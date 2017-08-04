/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.comm.trek1000;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;

/**
 * <p>
 * Implementation of DecaWave's Trek1000 sensor. This particular class stores 
 * descriptor attributes.
 * </p>
 * 
 * @author Joshua Wolfe <developer.wolfe@gmail.com>
 * @since July 20, 2017
 */
public class Trek1000Descriptor implements IModuleProvider
{
	@Override
	public String getModuleName()
	{
		return "DecaWave TREK1000 Serial Sensor";
	}

	@Override
	public String getModuleDescription()
	{
		return "Serial communication provider for DevaWave\'s TREK1000 sensor using OSH\'s RXTX library";
	}

	@Override
	public String getModuleVersion()
	{
		return "0.1";
	}

	@Override
	public String getProviderName()
	{
		return "Botts Innovative Research";
	}

	@Override
	public Class<? extends IModule<?>> getModuleClass()
	{
		return Trek1000Sensor.class;
	}

	@Override
	public Class<? extends ModuleConfig> getModuleConfigClass()
	{
		return Trek1000Config.class;
	}
}
