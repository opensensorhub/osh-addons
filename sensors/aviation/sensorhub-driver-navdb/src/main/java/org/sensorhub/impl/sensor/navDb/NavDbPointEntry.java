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
 * Entry class used to store info for point features:<br/>
 * airports, navaid, waypoint
 * </p>
 *
 * @author Alex Robin
 * @since Nov 11, 2021
 */
public class NavDbPointEntry extends NavDbEntry
{
    public String name;
    public Double lat;
    public Double lon;


    public NavDbPointEntry(Type type, String id, double lat, double lon) throws NumberFormatException
    {
        super(type, id.trim());
        this.lat = lat;
        this.lon = lon;
    }


    @Override
    public String toString()
    {
        return type + "," + region + "," + airport + "," + id + "," + name + "," + lat + "," + lon;
    }


    @Override
    public int compareTo(NavDbEntry o)
    {
        int comp = super.compareTo(o);
        
        if (comp == 0)
            comp = Double.compare(lat, ((NavDbPointEntry)o).lat);
        
        if (comp == 0)
            comp = Double.compare(lon, ((NavDbPointEntry)o).lon);
        
        return comp;
    }
}
