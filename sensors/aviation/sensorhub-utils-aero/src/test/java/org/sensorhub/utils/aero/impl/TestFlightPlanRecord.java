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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.junit.Test;
import org.vast.data.DataBlockProxy;


public class TestFlightPlanRecord
{

    @Test
    public void testGetSetMethods()
    {
        var schema = FlightPlanRecord.getSchema("fp");
        var fp = DataBlockProxy.generate(schema, FlightPlanRecord.class);
        fp.wrap(schema.createDataBlock());
        
        var time = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        var source = "SWIM";
        var flightNum = "DAL156";
        var fltDate = LocalDate.parse("2024-11-05");
        var origApt = "KDEN";
        var destApt = "KATL";
        var altApts = "KMCI,KBHM";
        var departTime = time.plus(2, ChronoUnit.HOURS);
        var tailNum = "NX5600";
        var acType = "B738";
        var cruiseAlt = 38000;
        var cruiseSpeed = 450;
        var cruiseMach = 0.78;
        var ci = 95;
        var sar = 1.012;
        var route = "KDEN QUAKY1 GCN DCT WINEN DCT NEERO DCT PDT CHINS5 KATL";
        
        fp.setIssueTime(time);
        fp.setSource(source);
        fp.setFlightNumber(flightNum);
        fp.setFlightDate(fltDate);
        fp.setOriginAirport(origApt);
        fp.setDestinationAirport(destApt);
        fp.setAlternateAirports(altApts);
        fp.setDepartureTime(departTime);
        fp.setTailNumber(tailNum);
        fp.setAircraftType(acType);
        fp.setCruiseAltitude(cruiseAlt);
        fp.setCruiseSpeed(cruiseSpeed);
        fp.setCruiseMach(cruiseMach);
        fp.setCostIndex(ci);
        fp.setFuelFactor(sar);
        fp.setCodedRoute(route);
        
        for (int i = 1; i <= 10; i++)
        {
            var wpt = fp.addWaypoint();
            wpt.setCode(String.format("WP%03d", i));
            wpt.setLatitude(30 + i);
            wpt.setLongitude(-120 + 2*i);
            wpt.setBaroAltitude(10000 + i*10);
        }
                
        System.out.println(fp);

        assertEquals(time, fp.getIssueTime());
        assertEquals(source, fp.getSource());
        assertEquals(flightNum, fp.getFlightNumber());
        assertEquals(fltDate, fp.getFlightDate());
        assertEquals(origApt, fp.getOriginAirport());
        assertEquals(destApt, fp.getDestinationAirport());
        assertEquals(altApts, fp.getAlternateAirports());
        assertEquals(departTime, fp.getDepartureTime());
        assertEquals(tailNum, fp.getTailNumber());
        assertEquals(acType, fp.getAircraftType());
        assertEquals(cruiseAlt, fp.getCruiseAltitude(), 1.0);
        assertEquals(cruiseSpeed, fp.getCruiseSpeed(), 1.0);
        assertEquals(cruiseMach, fp.getCruiseMach(), 1e-3);
        assertEquals(ci, fp.getCostIndex(), 1e-5);
        assertEquals(sar, fp.getFuelFactor(), 1e-5);
        assertEquals(route, fp.getCodedRoute());
        
        int i = 1;
        for (var wpt: fp.getWaypoints())
        {
            assertEquals(String.format("WP%03d", i), wpt.getCode());
            assertEquals(30 + i, wpt.getLatitude(), 1e-6);
            assertEquals(-120 + 2*i, wpt.getLongitude(), 1e-6);
            assertEquals(10000 + i*10, wpt.getBaroAltitude(), 1.0);
            i++;
        }
    }

}
