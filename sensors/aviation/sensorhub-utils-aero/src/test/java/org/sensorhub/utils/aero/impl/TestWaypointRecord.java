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
import static org.sensorhub.utils.aero.IWaypoint.*;
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
    
    
    @Test
    public void testAddTags()
    {
        var wpt = WaypointRecord.create();
        
        wpt.addTag(ETOPS_ENTRY_TAG);
        assertEquals(ETOPS_ENTRY_TAG, wpt.getInfo());
        assertTrue(wpt.hasTag(ETOPS_ENTRY_TAG));
        
        // add new tag
        wpt.addTag(ROUTE_CHANGE_TAG);
        assertEquals(ETOPS_ENTRY_TAG+","+ROUTE_CHANGE_TAG, wpt.getInfo());
        assertTrue(wpt.hasTag(ETOPS_ENTRY_TAG));
        assertTrue(wpt.hasTag(ROUTE_CHANGE_TAG));
        
        // add an existing tag (should not do anything)
        wpt.addTag(ROUTE_CHANGE_TAG);
        assertEquals(ETOPS_ENTRY_TAG+","+ROUTE_CHANGE_TAG, wpt.getInfo());
        assertTrue(wpt.hasTag(ETOPS_ENTRY_TAG));
        assertTrue(wpt.hasTag(ROUTE_CHANGE_TAG));
        
        // add another existing tag (should not do anything)
        wpt.addTag(ETOPS_ENTRY_TAG);
        assertEquals(ETOPS_ENTRY_TAG+","+ROUTE_CHANGE_TAG, wpt.getInfo());
        assertTrue(wpt.hasTag(ROUTE_CHANGE_TAG));
        assertTrue(wpt.hasTag(ETOPS_ENTRY_TAG));
        
        // add new tag
        wpt.addTag(REQUIRED_TAG);
        assertEquals(ETOPS_ENTRY_TAG+","+ROUTE_CHANGE_TAG+","+REQUIRED_TAG, wpt.getInfo());
        assertTrue(wpt.hasTag(ETOPS_ENTRY_TAG));
        assertTrue(wpt.hasTag(REQUIRED_TAG));
        assertTrue(wpt.hasTag(ROUTE_CHANGE_TAG));
    }
    
    
    @Test
    public void testRemoveTags()
    {
        // remove only tag
        var wpt = WaypointRecord.create();
        wpt.addTag(ALTERNATE_TAG);
        assertTrue(wpt.hasTag(ALTERNATE_TAG));
        assertEquals(ALTERNATE_TAG, wpt.getInfo());
        wpt.removeTag(ALTERNATE_TAG);
        assertEquals(null, wpt.getInfo());
        
        // remove first tag of 2
        wpt = WaypointRecord.create();
        wpt.addTag(ALTERNATE_TAG);
        wpt.addTag(ROUTE_CHANGE_TAG);
        assertTrue(wpt.hasTag(ROUTE_CHANGE_TAG));
        assertTrue(wpt.hasTag(ALTERNATE_TAG));
        wpt.removeTag(ALTERNATE_TAG);
        assertTrue(wpt.hasTag(ROUTE_CHANGE_TAG));
        assertFalse(wpt.hasTag(ALTERNATE_TAG));
        assertEquals(ROUTE_CHANGE_TAG, wpt.getInfo());
        
        // remove last tag of 2
        wpt = WaypointRecord.create();
        wpt.addTag(ALTERNATE_TAG);
        wpt.addTag(ROUTE_CHANGE_TAG);
        assertTrue(wpt.hasTag(ROUTE_CHANGE_TAG));
        assertTrue(wpt.hasTag(ALTERNATE_TAG));
        wpt.removeTag(ROUTE_CHANGE_TAG);
        assertFalse(wpt.hasTag(ROUTE_CHANGE_TAG));
        assertTrue(wpt.hasTag(ALTERNATE_TAG));
        assertEquals(ALTERNATE_TAG, wpt.getInfo());
        
        // remove middle tag
        wpt = WaypointRecord.create();
        wpt.addTag(ALTERNATE_TAG);
        wpt.addTag(ROUTE_CHANGE_TAG);
        wpt.addTag(REQUIRED_TAG);
        assertTrue(wpt.hasTag(ROUTE_CHANGE_TAG));
        assertTrue(wpt.hasTag(ALTERNATE_TAG));
        assertTrue(wpt.hasTag(REQUIRED_TAG));
        wpt.removeTag(ROUTE_CHANGE_TAG);
        assertFalse(wpt.hasTag(ROUTE_CHANGE_TAG));
        assertTrue(wpt.hasTag(REQUIRED_TAG));
        assertTrue(wpt.hasTag(ALTERNATE_TAG));
        assertEquals(ALTERNATE_TAG+","+REQUIRED_TAG, wpt.getInfo());
    }

}
