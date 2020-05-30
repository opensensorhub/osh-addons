/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2019 Delta Air Lines, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.flightAware;

import org.sensorhub.api.config.DisplayInfo;


/**
 * Configuration class for flight number range filter
 * @author Alex Robin
 * @since Nov 26, 2019
 */
public class FlightNumberRangeFilterConfig extends FlightObjectFilterConfig
{
        
    @DisplayInfo(desc="Path of CSV file containing the ranges of accepted flight numbers.\n"
            + "Each row must have the format: {airline ICAO code},{begin number},{end number}")
    public String dataFile;
    
    
    public IFlightObjectFilter getFilter()
    {
        return new FlightNumberRangeFilter(this);
    }
}
