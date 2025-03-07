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


public class TestAircraftStateRecord
{

    @Test
    public void testGetSetMethods()
    {
        var schema = AircraftStateRecord.getSchema("acState");
        var state = DataBlockProxy.generate(schema, AircraftStateRecord.class);
        state.wrap(schema.createDataBlock());
        
        var time = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        var tailNum = "NX5600";
        var lat = 36.458942;
        var lon = -91.451236;
        var gnssAlt = 35489;
        var baroAlt = 35600;
        var track = 56.9;
        var heading = 55.4;
        var gs = 526;
        var altRate = 1230;
        var tas = 429;
        var mach = 0.79;
        var sat = -48;
        var zfw = 312000;
        var fob = 64500;
        
        state.setTime(time);
        state.setTailNumber(tailNum);
        state.setLatitude(lat);
        state.setLongitude(lon);
        state.setGnssAltitude(gnssAlt);
        state.setBaroAltitude(baroAlt);
        state.setTrueTrack(track);
        state.setTrueHeading(heading);
        state.setGroundSpeed(gs);
        state.setVerticalRate(altRate);
        state.setTrueAirSpeed(tas);
        state.setMach(mach);
        state.setStaticAirTemperature(sat);
        state.setZeroFuelWeight(zfw);
        state.setFuelOnBoard(fob);
        
        System.out.println(state);
        
        assertEquals(time, state.getTime());
        assertEquals(tailNum, state.getTailNumber());
        assertEquals(lat, state.getLatitude(), 1e-18);
        assertEquals(lon, state.getLongitude(), 1e-18);
        assertEquals(gnssAlt, state.getGnssAltitude(), 1e-5);
        assertEquals(baroAlt, state.getBaroAltitude(), 1e-5);
        assertEquals(Double.NaN, state.getBaroAltSetting(), 1e-5);
        assertEquals(track, state.getTrueTrack(), 1e-5);
        assertEquals(Double.NaN, state.getMagneticTrack(), 1e-5);
        assertEquals(heading, state.getTrueHeading(), 1e-5);
        assertEquals(Double.NaN, state.getMagneticHeading(), 1e-5);
        assertEquals(gs, state.getGroundSpeed(), 1e-5);
        assertEquals(altRate, state.getVerticalRate(), 1e-5);
        assertEquals(tas, state.getTrueAirSpeed(), 1e-5);
        assertEquals(Double.NaN, state.getCalibratedAirSpeed(), 1e-5);
        assertEquals(mach, state.getMach(), 1e-5);
        assertEquals(sat, state.getStaticAirTemperature(), 1e-5);
        assertEquals(Double.NaN, state.getStaticAirPressure(), 1e-5);
        assertEquals(zfw, state.getZeroFuelWeight(), 1e-5);
        assertEquals(fob, state.getFuelOnBoard(), 1e-5);
    }

}
