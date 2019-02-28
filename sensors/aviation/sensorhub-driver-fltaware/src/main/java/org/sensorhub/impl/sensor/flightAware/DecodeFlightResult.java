/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2018 Delta Air Lines, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.flightAware;

public class DecodeFlightResult extends FlightAwareResult
{
	Result DecodeFlightRouteResult = new Result();  // Gson class name doesn't matter, just the variable name.  And Case matters!!!

	class Result {
		int next_offset;
		Data [] data = new Data[] {};
		
		
		class Data {
			String name;
			String type;
			double latitude;
			double longitude;
		}
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		for(Result.Data d: DecodeFlightRouteResult.data) {
			b.append(d.name + "," + d.type + "," + d.latitude + "," + d.longitude + "\n");
		}
		return b.toString();
	}
}
