/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the License.
 *
 * The Initial Developer is Terravolv, Inc. Portions created by the Initial
 * Developer are Copyright (C) 2015 the Initial Developer. All Rights Reserved.
 *
 * Date: 2015-04-07T00:00Z
 */

/**
 * @param {number[]} ecef - Earth Centered Earth Fixed coordinates in meters
 * @returns {number[]} lla - LLA
 * @public
 */
 
function ecef2lla(ecef) {
  
	var x = ecef[0];
	var y = ecef[1];
	var z = ecef[2];
	var lla = [0, 0, 0, 0];
	
	var A = 6378137.0;
	var B = 6356752.3;
	var E_ECC   = 0.0818191;
	
	var P = Math.sqrt(x*x + y*y);
	var THETA = Math.atan( z*A/(P*B) );
	var sint3 = Math.sin(THETA)*Math.sin(THETA)*Math.sin(THETA);
	var cost3 = Math.cos(THETA)*Math.cos(THETA)*Math.cos(THETA);

  // For some reason if I didn't use parseFloat just in this
  // one case for numlat I got weird results and consequently
  // Lat endup being NaN! [Mani]	
	var numlat = parseFloat(z) + parseFloat(((A*A-B*B)/B)*sint3);
	var denlat = P - E_ECC*E_ECC*A*cost3;
	var Lat = Math.atan( numlat/denlat );
	var Lon = Math.atan2( y, x );
	
	var Ntemp = 1 - E_ECC*E_ECC*Math.sin(Lat)*Math.sin(Lat);

	var N = 0;
	if (Ntemp < 0.0)
		N = A;
	else
		N = A / Math.sqrt( Ntemp );
	
	var Altitude = P / Math.cos(Lat) - N;

	lla[0] = Lat*180.0/Math.PI;
	lla[1] = Lon*180.0/Math.PI;
	lla[2] = Altitude;
  lla[3] = ecef[3];  // Time
	
	return lla;
}