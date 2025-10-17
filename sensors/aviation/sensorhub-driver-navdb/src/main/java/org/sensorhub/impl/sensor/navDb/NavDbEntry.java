/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2018 Delta Air Lines, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.navDb;


/**
 * <p>
 * Base class for all Nav DB entries
 * </p>
 *
 * @author Alex Robin
 * @since Nov 11, 2021
 */
public class NavDbEntry implements Comparable<NavDbEntry>
{
    public enum Type
    {
        AIRPORT, NAVAID, WAYPOINT, AIRWAY, SID, STAR, UNKNOWN
    };

    public enum Subtype
    {
        NAVAID_NDB, NAVAID_VOR, NAVAID_DME, NAVAID_TACAN, NAVAID_ILS, NAVAID_MLS, UNDEFINED
    }

    public String region; // 3 chars region id
    public String icao_code; // 2
    public Type type;
    public Subtype subtype = Subtype.UNDEFINED;
    public String id; // ID is the same as airport ICAO for airports
    public String airport = ""; // ICAO code of associated airport
    

    public NavDbEntry(Type type, String id)
    {
        this.type = type;
        this.id = id;
    }


    @Override
    public int compareTo(NavDbEntry o)
    {
        int comp = id.compareTo(o.id);
        
        if (comp == 0)
            comp = region.compareTo(o.region);
        
        if (comp == 0)
            comp = airport.compareTo(o.airport);

        if (comp == 0)
            comp = icao_code.compareTo(o.icao_code);
        
        return comp;
    }
}
