/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2025 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.utils.aero;

import java.time.LocalDate;


/**
 * <p>
 * Read-only interface for flight identification information
 * </p>
 *
 * @author Alex Robin
 * @since Jan 27, 2025
 */
public interface IFlightIdentification
{
    /**
     * @return Full flight identifier
     */
    String getFlightID();
    
    
    /**
     * @return ICAO flight number (3 letters airline code + 1-4 numbers, e.g. DAL123)
     */
    String getFlightNumber();
    
    
    /**
     * @return Origin airport code (ICAO)
     */
    String getOriginAirport();
    
    
    /**
     * @return Destination airport code (ICAO)
     */
    String getDestinationAirport();
    
    
    /**
     * @return Original flight departure date (local time at departure airport)
     */
    LocalDate getFlightDate();
    
}
