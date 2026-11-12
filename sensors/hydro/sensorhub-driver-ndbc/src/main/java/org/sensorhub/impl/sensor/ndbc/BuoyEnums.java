/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
 
package org.sensorhub.impl.sensor.ndbc;

/**
 * enum class for observable parameters
 * 
 * @author Tony Cook
 */

@Deprecated // This is a legacy enum from original driver. Marking deprecated but may bring it back in future driver development 
public class BuoyEnums {

	public enum ObsParam
	{
	    AIR_PRESSURE_AT_SEA_LEVEL,
	    AIR_TEMPERATURE,
	    CURRENTS,
	    SEA_FLOOR_DEPTH_BELOW_SEA_SURFACE,
	    SEA_WATER_ELECTRICAL_CONDUCTIVITY,
	    SEA_WATER_SALINITY,
	    SEA_WATER_TEMPERATURE,
	    WAVES,
	    WINDS
	}
}
