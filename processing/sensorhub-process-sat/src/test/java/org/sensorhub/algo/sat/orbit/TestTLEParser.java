/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2022 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.algo.sat.orbit;

import static org.junit.Assert.*;
import org.junit.Test;


public class TestTLEParser
{

    @Test
    public void testReadLatestFromMultiSatFile() throws Exception
    {
        var tleUrl = getClass().getResource("planet_all.tle");
        var p = new TLEParser(tleUrl);
        
        var satID = "41975";
        var tle = p.getClosestTLE(satID, 0);
        
        assertEquals(tle.satID, Integer.parseInt(satID));
        assertEquals(0.0013862, tle.bstar, 1e-8);
        assertEquals(Math.toRadians(97.2691), tle.inclination, 1e-8);
        assertEquals(Math.toRadians(315.1754), tle.rightAscension, 1e-8);
        assertEquals(0.0004952, tle.eccentricity, 1e-8);
        assertEquals(Math.toRadians(276.8678), tle.argOfPerigee, 1e-8);
        assertEquals(Math.toRadians(83.2005), tle.meanAnomaly, 1e-8);
        assertEquals(Math.toRadians(15.39711643*360.)/24/3600., tle.meanMotion, 1e-8);
        assertEquals(30925, tle.revNumber);
    }
    
    
    @Test
    public void testReadLatestFromSingleSatFile() throws Exception
    {
        var tleUrl = getClass().getResource("planet_41977.tle");
        var p = new TLEParser(tleUrl);
        
        var satID = "41977";
        var tle = p.getClosestTLE(satID, 0);
        
        assertEquals(tle.satID, Integer.parseInt(satID));
        assertEquals(0.0014502, tle.bstar, 1e-8);
        assertEquals(Math.toRadians(97.2703), tle.inclination, 1e-8);
        assertEquals(Math.toRadians(315.7658), tle.rightAscension, 1e-8);
        assertEquals(0.0005336, tle.eccentricity, 1e-8);
        assertEquals(Math.toRadians(298.4435), tle.argOfPerigee, 1e-8);
        assertEquals(Math.toRadians(61.6275), tle.meanAnomaly, 1e-8);
        assertEquals(Math.toRadians(15.42308280*360.)/24/3600., tle.meanMotion, 1e-8);
        assertEquals(30959, tle.revNumber);
    }
    
    
    @Test
    public void testReadHistoricalFromSingleSatFile() throws Exception
    {
        var tleUrl = getClass().getResource("planet_41977.tle");
        var p = new TLEParser(tleUrl);
        
        var satID = "41977";
        var tle = p.getClosestTLE(satID, 0);
        
        assertEquals(tle.satID, Integer.parseInt(satID));
        assertEquals(0.0014502, tle.bstar, 1e-8);
        assertEquals(Math.toRadians(97.2703), tle.inclination, 1e-8);
        assertEquals(Math.toRadians(315.7658), tle.rightAscension, 1e-8);
        assertEquals(0.0005336, tle.eccentricity, 1e-8);
        assertEquals(Math.toRadians(298.4435), tle.argOfPerigee, 1e-8);
        assertEquals(Math.toRadians(61.6275), tle.meanAnomaly, 1e-8);
        assertEquals(Math.toRadians(15.42308280*360.)/24/3600., tle.meanMotion, 1e-8);
        assertEquals(30959, tle.revNumber);
    }

}
