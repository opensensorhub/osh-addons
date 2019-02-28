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

//{
//	   "pitr":"1506541790",
//	   "type":"position",
//	   "ident":"DAL1877",
//	   "aircrafttype":"B739",
//	   "alt":"3550",
//	   "clock":"1506541784",
//	   "facility_hash":"483af88b36b961950eff691dda2824c2cb021a36",
//	   "facility_name":"FlightAware ADS-B",
//	   "id":"DAL1877-1506317145-airline-0309",
//	   "gs":"184",
//	   "heading":"271",
//	   "hexid":"AAE5F7",
//	   "lat":"33.64935",
//	   "lon":"-84.24522",
//	   "reg":"N801DZ",
//	   "updateType":"A",
//	   "altChange":"C",
//	   "air_ground":"A"
//	}
@Deprecated //Position is basically FlightObject with an oshFlightId added, so this isn't needed
public class Position
{
	public String type;
    public String ident;
    public String air_ground;
    public String alt;
    public String clock;
    public String id;
    public String gs;
    public String heading;
    public String lat;
    public String lon;
    public String reg;
    public String squawk;
    public String updateType;
}
