/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2025 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.utils.aero.impl;

import static org.junit.Assert.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.Test;
import org.vast.data.DataBlockProxy;


public class TestWaypointRecord
{

    @Test
    public void testGetSetMethods()
    {
        var schema = WaypointRecordExt.getSchema("wpt");
        var wpt = DataBlockProxy.generate(schema, WaypointRecordExt.class);
        wpt.wrap(schema.createDataBlock());
        
        var code = "QUAKY";
        var lat = 36.458942;
        var lon = -91.451236;
        var alt = 32000;
        var time = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        var gs = 526;
        var tas = 429;
        var mach = 0.79;
        var fob = 64500;
        
        wpt.setCode(code);
        wpt.setLatitude(lat);
        wpt.setLongitude(lon);
        wpt.setBaroAltitude(alt);
        wpt.setTime(time);
        wpt.setGroundSpeed(gs);
        wpt.setTrueAirSpeed(tas);
        wpt.setMach(mach);
        wpt.setFuelOnBoard(fob);
        
        System.out.println(wpt);

        assertEquals(code, wpt.getCode());
        assertEquals(lat, wpt.getLatitude(), 1e-18);
        assertEquals(lon, wpt.getLongitude(), 1e-18);
        assertEquals(alt, wpt.getBaroAltitude(), 1e-5);
        assertEquals(time, wpt.getTime());
        assertEquals(gs, wpt.getGroundSpeed(), 1e-5);
        assertEquals(tas, wpt.getTrueAirSpeed(), 1e-5);
        assertEquals(mach, wpt.getMach(), 1e-5);
        assertEquals(fob, wpt.getFuelOnBoard(), 1e-5);
    }

}
