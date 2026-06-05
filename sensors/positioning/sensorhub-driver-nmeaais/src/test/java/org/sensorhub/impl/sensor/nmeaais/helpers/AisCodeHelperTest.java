/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.nmeaais.helpers;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link AisCodeHelper} enum lookups and utility methods.
 * No OSH infrastructure required — all tests are self-contained.
 */
public class AisCodeHelperTest {

    // -------------------------------------------------------------------------
    // MessageType
    // -------------------------------------------------------------------------

    @Test
    public void testMessageType_knownCodes() {
        assertTrue(AisCodeHelper.MessageType.getDescription(1).contains("Class A"));
        assertTrue(AisCodeHelper.MessageType.getDescription(18).contains("Class B"));
        assertTrue(AisCodeHelper.MessageType.getDescription(5).contains("static"));
        assertNotNull(AisCodeHelper.MessageType.fromCode(24));
    }

    @Test
    public void testMessageType_unknownCode() {
        assertTrue(AisCodeHelper.MessageType.getDescription(99).startsWith("Unknown message type"));
    }

    // -------------------------------------------------------------------------
    // NavigationalStatus
    // -------------------------------------------------------------------------

    @Test
    public void testNavigationalStatus_knownCodes() {
        assertEquals("Under way using engine",  AisCodeHelper.NavigationalStatus.getDescription(0));
        assertEquals("At anchor",               AisCodeHelper.NavigationalStatus.getDescription(1));
        assertEquals("Moored",                  AisCodeHelper.NavigationalStatus.getDescription(5));
        assertEquals("Undefined / default",     AisCodeHelper.NavigationalStatus.getDescription(15));
    }

    @Test
    public void testNavigationalStatus_unknownCode() {
        assertTrue(AisCodeHelper.NavigationalStatus.getDescription(99).startsWith("Unknown"));
    }

    // -------------------------------------------------------------------------
    // ShipType — including reserved-range fallbacks
    // -------------------------------------------------------------------------

    @Test
    public void testShipType_knownCodes() {
        assertEquals("Fishing vessel",                      AisCodeHelper.ShipType.getDescription(30));
        assertEquals("Cargo ship, all ships of this type",  AisCodeHelper.ShipType.getDescription(70));
        assertEquals("Tanker, all ships of this type",      AisCodeHelper.ShipType.getDescription(80));
        assertEquals("Pilot vessel",                        AisCodeHelper.ShipType.getDescription(50));
    }

    @Test
    public void testShipType_reservedRangeFallbacks() {
        assertEquals("Reserved for future use",
                AisCodeHelper.ShipType.getDescription(8));
        assertEquals("Reserved for future use",
                AisCodeHelper.ShipType.getDescription(10));
        assertEquals("Wing in Ground (WIG), reserved for future use",
                AisCodeHelper.ShipType.getDescription(25));
        assertEquals("High Speed Craft (HSC), reserved for future use",
                AisCodeHelper.ShipType.getDescription(47));
        assertEquals("Passenger ship, reserved for future use",
                AisCodeHelper.ShipType.getDescription(68));
        assertEquals("Tanker, reserved for future use",
                AisCodeHelper.ShipType.getDescription(87));
        assertEquals("Other type, reserved for future use",
                AisCodeHelper.ShipType.getDescription(95));
        assertEquals("Reserved for regional use",
                AisCodeHelper.ShipType.getDescription(100));
        assertEquals("Reserved for future use",
                AisCodeHelper.ShipType.getDescription(200));
    }

    @Test
    public void testShipType_unknownCode() {
        assertTrue(AisCodeHelper.ShipType.getDescription(256).startsWith("Unknown ship type"));
    }

    // -------------------------------------------------------------------------
    // EpfdType
    // -------------------------------------------------------------------------

    @Test
    public void testEpfdType_knownCodes() {
        assertEquals("Undefined",       AisCodeHelper.EpfdType.getDescription(0));
        assertEquals("GPS",             AisCodeHelper.EpfdType.getDescription(1));
        assertEquals("GLONASS",         AisCodeHelper.EpfdType.getDescription(2));
        assertEquals("Galileo",         AisCodeHelper.EpfdType.getDescription(8));
        assertEquals("Internal GNSS",   AisCodeHelper.EpfdType.getDescription(15));
    }

    @Test
    public void testEpfdType_unknownCode() {
        assertTrue(AisCodeHelper.EpfdType.getDescription(99).startsWith("Unknown EPFD"));
    }

    // -------------------------------------------------------------------------
    // PosAcc
    // -------------------------------------------------------------------------

    @Test
    public void testPosAcc() {
        assertEquals("Low (> 10 m) or Default", AisCodeHelper.PosAcc.getDescription(0));
        assertEquals("High (<= 10 m)",           AisCodeHelper.PosAcc.getDescription(1));
        assertTrue(AisCodeHelper.PosAcc.getDescription(9).startsWith("Unknown Position Accuracy"));
    }

    // -------------------------------------------------------------------------
    // RaimFlag
    // -------------------------------------------------------------------------

    @Test
    public void testRaimFlag() {
        assertEquals("RAIM not in use (default)", AisCodeHelper.RaimFlag.getDescription(0));
        assertEquals("RAIM in use",               AisCodeHelper.RaimFlag.getDescription(1));
        assertTrue(AisCodeHelper.RaimFlag.getDescription(9).startsWith("Unknown RAIM"));
    }

    // -------------------------------------------------------------------------
    // AssignedMode
    // -------------------------------------------------------------------------

    @Test
    public void testAssignedMode() {
        assertEquals("Station operating in autonomous and continuous mode (default)",
                AisCodeHelper.AssignedMode.getDescription(0));
        assertEquals("Station operating in assigned mode",
                AisCodeHelper.AssignedMode.getDescription(1));
        assertTrue(AisCodeHelper.AssignedMode.getDescription(9).startsWith("Unknown assigned mode"));
    }

    // -------------------------------------------------------------------------
    // ClassBUnitFlag
    // -------------------------------------------------------------------------

    @Test
    public void testClassBUnitFlag() {
        assertEquals("Class B SOTDMA unit", AisCodeHelper.ClassBUnitFlag.getDescription(0));
        assertEquals("Class B 'CS' unit",   AisCodeHelper.ClassBUnitFlag.getDescription(1));
        assertTrue(AisCodeHelper.ClassBUnitFlag.getDescription(9).startsWith("Unknown"));
    }

    // -------------------------------------------------------------------------
    // New Class B binary flag enums
    // -------------------------------------------------------------------------

    @Test
    public void testDisplayFlag() {
        assertEquals("No display available (default)",
                AisCodeHelper.DisplayFlag.getDescription(0));
        assertEquals("Equipped with display for Message 12 and 14",
                AisCodeHelper.DisplayFlag.getDescription(1));
        assertTrue(AisCodeHelper.DisplayFlag.getDescription(9).startsWith("Unknown"));
    }

    @Test
    public void testDscFlag() {
        assertEquals("Not equipped with DSC function (default)",
                AisCodeHelper.DscFlag.getDescription(0));
        assertEquals("Equipped with DSC function",
                AisCodeHelper.DscFlag.getDescription(1));
        assertTrue(AisCodeHelper.DscFlag.getDescription(9).startsWith("Unknown"));
    }

    @Test
    public void testBandFlag() {
        assertEquals("Capable of operating over the upper 525 kHz band of the marine band (default)",
                AisCodeHelper.BandFlag.getDescription(0));
        assertEquals("Capable of operating over the whole marine band",
                AisCodeHelper.BandFlag.getDescription(1));
        assertTrue(AisCodeHelper.BandFlag.getDescription(9).startsWith("Unknown"));
    }

    @Test
    public void testMessage22Flag() {
        assertEquals("No frequency management via Message 22 (default)",
                AisCodeHelper.Message22Flag.getDescription(0));
        assertEquals("Frequency management via Message 22",
                AisCodeHelper.Message22Flag.getDescription(1));
        assertTrue(AisCodeHelper.Message22Flag.getDescription(9).startsWith("Unknown"));
    }

    @Test
    public void testCommStateSelectorFlag() {
        assertEquals("SOTDMA communication state follows (default)",
                AisCodeHelper.CommStateSelectorFlag.getDescription(0));
        assertEquals("ITDMA communication state follows (always 1 for Class B CS)",
                AisCodeHelper.CommStateSelectorFlag.getDescription(1));
        assertTrue(AisCodeHelper.CommStateSelectorFlag.getDescription(9).startsWith("Unknown"));
    }

    @Test
    public void testDte() {
        assertEquals("Data terminal equipment available (default)",
                AisCodeHelper.Dte.getDescription(0));
        assertEquals("Data terminal equipment not available",
                AisCodeHelper.Dte.getDescription(1));
        assertTrue(AisCodeHelper.Dte.getDescription(9).startsWith("Unknown DTE"));
    }

    // -------------------------------------------------------------------------
    // Spi
    // -------------------------------------------------------------------------

    @Test
    public void testSpi() {
        // Note: "Deafult" is the spelling preserved in the source enum
        assertTrue(AisCodeHelper.Spi.getDescription(0).contains("Not Available"));
        assertEquals("Not Engaged in Special Maneuver", AisCodeHelper.Spi.getDescription(1));
        assertEquals("Engaged in Special Maneuver",     AisCodeHelper.Spi.getDescription(2));
        assertTrue(AisCodeHelper.Spi.getDescription(9).startsWith("Unknown SPI"));
    }

    // -------------------------------------------------------------------------
    // AtoNType
    // -------------------------------------------------------------------------

    @Test
    public void testAtoNType_knownCodes() {
        assertEquals("Default, Type of AtoN not specified",
                AisCodeHelper.AtoNType.getDescription(0));
        assertEquals("RACON",
                AisCodeHelper.AtoNType.getDescription(2));
        assertEquals("Light Vessel/LANBY/Rigs",
                AisCodeHelper.AtoNType.getDescription(31));
    }

    @Test
    public void testAtoNType_unknownCode() {
        assertTrue(AisCodeHelper.AtoNType.getDescription(32).startsWith("Unknown Type"));
    }

    // -------------------------------------------------------------------------
    // OffPositionIndicator
    // -------------------------------------------------------------------------

    @Test
    public void testOffPositionIndicator() {
        assertEquals("On position",
                AisCodeHelper.OffPositionIndicator.getDescription(0));
        assertEquals("Off position (floating AtoN only)",
                AisCodeHelper.OffPositionIndicator.getDescription(1));
        assertTrue(AisCodeHelper.OffPositionIndicator.getDescription(9).startsWith("Unknown off-position"));
    }

    // -------------------------------------------------------------------------
    // VirtualAtoN
    // -------------------------------------------------------------------------

    @Test
    public void testVirtualAtoN() {
        assertEquals("Default = real AtoN at indicated position",
                AisCodeHelper.VirtualAtoN.getDescription(0));
        assertEquals("Virtual AtoN, does not physically exist",
                AisCodeHelper.VirtualAtoN.getDescription(1));
        assertTrue(AisCodeHelper.VirtualAtoN.getDescription(9).startsWith("Unknown virtual"));
    }

    // -------------------------------------------------------------------------
    // getRotData
    // -------------------------------------------------------------------------

    @Test
    public void testGetRotData_notAvailable() {
        AisCodeHelper.RotRecord r = AisCodeHelper.getRotData(-128);
        assertNull("ROT should be null for code -128", r.rot());
        assertEquals("Not Available", r.roti());
    }

    @Test
    public void testGetRotData_maxRight() {
        AisCodeHelper.RotRecord r = AisCodeHelper.getRotData(127);
        assertEquals(10.0, r.rot(), 0.001);
        assertTrue(r.roti().contains("No TI available"));
    }

    @Test
    public void testGetRotData_maxLeft() {
        AisCodeHelper.RotRecord r = AisCodeHelper.getRotData(-127);
        assertEquals(-10.0, r.rot(), 0.001);
        assertTrue(r.roti().contains("No TI available"));
    }

    @Test
    public void testGetRotData_computed_positive() {
        // sign * (code/4.733)^2 — for code=10: 1 * (10/4.733)^2 ≈ 4.465
        AisCodeHelper.RotRecord r = AisCodeHelper.getRotData(10);
        assertNotNull(r.rot());
        assertTrue("Positive code should give positive ROT", r.rot() > 0);
        assertEquals("TI available", r.roti());
    }

    @Test
    public void testGetRotData_computed_negative() {
        // sign is -1, so result should be negative
        AisCodeHelper.RotRecord r = AisCodeHelper.getRotData(-10);
        assertNotNull(r.rot());
        assertTrue("Negative code should give negative ROT", r.rot() < 0);
        assertEquals("TI available", r.roti());
    }

    // -------------------------------------------------------------------------
    // cleanVesselName
    // -------------------------------------------------------------------------

    @Test
    public void testCleanVesselName_stripsAtPadding() {
        assertEquals("VESSEL NAME", AisCodeHelper.cleanVesselName("VESSEL NAME@@@@@@@@@@"));
    }

    @Test
    public void testCleanVesselName_allPadding() {
        assertEquals("", AisCodeHelper.cleanVesselName("@@@@@@@@@@@@@@@@@@@@"));
    }

    @Test
    public void testCleanVesselName_trimsWhitespace() {
        assertEquals("VESSEL NAME", AisCodeHelper.cleanVesselName("  VESSEL NAME  "));
    }

    @Test
    public void testCleanVesselName_mixedPaddingAndSpaces() {
        assertEquals("MY SHIP", AisCodeHelper.cleanVesselName("MY SHIP@@@   "));
    }

    @Test
    public void testCleanVesselName_noChange() {
        assertEquals("VESSEL", AisCodeHelper.cleanVesselName("VESSEL"));
    }

    @Test
    public void testCleanVesselName_null() {
        assertEquals("", AisCodeHelper.cleanVesselName(null));
    }

    @Test
    public void testCleanVesselName_emptyString() {
        assertEquals("", AisCodeHelper.cleanVesselName(""));
    }
}
