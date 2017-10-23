/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2017 Botts Innovative Research, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.flightAware;

import org.sensorhub.impl.sensor.lawBox.LatLonAlt;

public class LawBox
{
	LatLonAlt brTopLla;
	LatLonAlt brBottomLla; 
	LatLonAlt blTopLla;
	LatLonAlt blBottomLla; 
	LatLonAlt frTopLla;
	LatLonAlt frBottomLla;
	LatLonAlt flTopLla;
	LatLonAlt flBottomLla; 

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("brTop:" + brTopLla + "\n");
		b.append("blTop:" + blTopLla + "\n");
		b.append("flTop:" + flTopLla + "\n");
		b.append("frTop:" + frTopLla + "\n");
		b.append("brBottom:" + brBottomLla + "\n");
		b.append("blBottom:" + blBottomLla + "\n");
		b.append("flBottom:" + flBottomLla + "\n");
		b.append("frBottom:" + frBottomLla);
		
		return b.toString();
	}
	
}
