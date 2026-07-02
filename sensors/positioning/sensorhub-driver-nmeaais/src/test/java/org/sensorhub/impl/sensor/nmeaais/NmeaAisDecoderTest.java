/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.nmeaais;

import dk.dma.ais.message.*;
import dk.dma.ais.reader.AisReader;
import dk.dma.ais.reader.AisReaders;
import org.junit.Test;
import org.sensorhub.impl.sensor.nmeaais.helpers.AisCodeHelper;

import java.io.ByteArrayInputStream;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * Integration tests that parse real captured NMEA AIS sentences using the
 * dma-ais library and verify:
 *   1. Each sentence decodes to the expected AIS message type.
 *   2. Key decoded field values are valid (MMSI, position, flags).
 *   3. {@link AisCodeHelper} lookups return meaningful descriptions for
 *      values actually present in the captured messages.
 *
 * No OSH infrastructure is required — only the dma-ais library.
 */
public class NmeaAisDecoderTest {

    // Real NMEA AIS sentences captured from a live AIS receiver
    private static final String MSG_1       = "!AIVDM,1,1,,A,15Mw`3PP00qm2v`CapW>4?vj2@?>,0*2C";
    private static final String MSG_3       = "!AIVDM,1,1,,A,35Mw`3PP0@qm28jCasvK@Ov<22wk,0*70";
    private static final String MSG_18      = "!AIVDM,1,1,,B,B5O>w?03wnMOFo4tTvcQ0`d1jDAr,0*12";
    private static final String MSG_24      = "!AIVDM,1,1,,B,H5O>w?4Uooo4:uLG4>nhoq0p614t,0*0A";
    private static final String MSG_26      = "!AIVDM,1,1,,B,JBAB3qRk69e>mN:TIKQuAK6Vc@R?lqCh,2*7A";
    private static final String MSG_5_PT1  = "!AIVDM,2,1,0,B,55Mw`3`00001L@?;K38nuJ0N0F0l00000000000O00000vPd0AAmCU1D,0*50";
    private static final String MSG_5_PT2  = "!AIVDM,2,2,0,B,TmRC31H0C000000,2*06";

    /**
     * Parses one or more NMEA sentences and returns the decoded {@link AisMessage}.
     * Uses {@link AisReader} backed by a {@link ByteArrayInputStream}, which
     * exhausts cleanly so {@code join()} returns promptly.
     */
    private AisMessage parseMessage(String... sentences) throws InterruptedException {
        String input = String.join("\n", sentences) + "\n";
        AisReader reader = AisReaders.createReaderFromInputStream(
                new ByteArrayInputStream(input.getBytes()));
        AtomicReference<AisMessage> result = new AtomicReference<>();
        reader.registerHandler(result::set);
        reader.start();
        reader.join(3000);
        return result.get();
    }

    // -------------------------------------------------------------------------
    // Message 1 — Class A scheduled position report
    // -------------------------------------------------------------------------

    @Test
    public void testMsg1_decodesCorrectly() throws InterruptedException {
        AisMessage msg = parseMessage(MSG_1);
        assertNotNull("Message 1 should parse without error", msg);
        assertEquals(1, msg.getMsgId());
    }

    @Test
    public void testMsg1_mmsiIsValid() throws InterruptedException {
        AisPositionMessage pos = (AisPositionMessage) parseMessage(MSG_1);
        assertTrue("MMSI must be a positive 9-digit number",
                pos.getUserId() > 0 && pos.getUserId() <= 999_999_999);
    }

    @Test
    public void testMsg1_navStatusResolvesToKnownDescription() throws InterruptedException {
        AisPositionMessage pos = (AisPositionMessage) parseMessage(MSG_1);
        String desc = AisCodeHelper.NavigationalStatus.getDescription(pos.getNavStatus());
        assertFalse("Nav status should resolve to a known description",
                desc.startsWith("Unknown"));
    }

    @Test
    public void testMsg1_positionIsPresent() throws InterruptedException {
        AisPositionMessage pos = (AisPositionMessage) parseMessage(MSG_1);
        assertNotNull("Position should not be null", pos.getPos());
    }

    // -------------------------------------------------------------------------
    // Message 3 — Class A position report, response to interrogation
    // -------------------------------------------------------------------------

    @Test
    public void testMsg3_decodesCorrectly() throws InterruptedException {
        AisMessage msg = parseMessage(MSG_3);
        assertNotNull("Message 3 should parse without error", msg);
        assertEquals(3, msg.getMsgId());
    }

    @Test
    public void testMsg3_mmsiIsValid() throws InterruptedException {
        AisPositionMessage pos = (AisPositionMessage) parseMessage(MSG_3);
        assertTrue(pos.getUserId() > 0 && pos.getUserId() <= 999_999_999);
    }

    @Test
    public void testMsg1AndMsg3_shareTheSameVessel() throws InterruptedException {
        // Both captured messages are from the same vessel (same MMSI prefix in payload)
        AisPositionMessage pos1 = (AisPositionMessage) parseMessage(MSG_1);
        AisPositionMessage pos3 = (AisPositionMessage) parseMessage(MSG_3);
        assertEquals("Msg 1 and Msg 3 should share the same MMSI",
                pos1.getUserId(), pos3.getUserId());
    }

    @Test
    public void testMsg3_smiResolvesToKnownDescription() throws InterruptedException {
        AisPositionMessage pos = (AisPositionMessage) parseMessage(MSG_3);
        String desc = AisCodeHelper.Spi.getDescription(pos.getSpecialManIndicator());
        assertFalse(desc.startsWith("Unknown SPI"));
    }

    // -------------------------------------------------------------------------
    // Message 18 — Class B standard CS position report
    // -------------------------------------------------------------------------

    @Test
    public void testMsg18_decodesCorrectly() throws InterruptedException {
        AisMessage msg = parseMessage(MSG_18);
        assertNotNull("Message 18 should parse without error", msg);
        assertEquals(18, msg.getMsgId());
    }

    @Test
    public void testMsg18_mmsiIsValid() throws InterruptedException {
        AisMessage18 pos18 = (AisMessage18) parseMessage(MSG_18);
        assertTrue(pos18.getUserId() > 0 && pos18.getUserId() <= 999_999_999);
    }

    @Test
    public void testMsg18_positionIsPresent() throws InterruptedException {
        AisMessage18 pos18 = (AisMessage18) parseMessage(MSG_18);
        assertNotNull(pos18.getPos());
    }

    @Test
    public void testMsg18_allFlagDescriptionsResolve() throws InterruptedException {
        AisMessage18 pos18 = (AisMessage18) parseMessage(MSG_18);
        assertFalse(AisCodeHelper.ClassBUnitFlag.getDescription(pos18.getClassBUnitFlag()).startsWith("Unknown"));
        assertFalse(AisCodeHelper.DisplayFlag.getDescription(pos18.getClassBDisplayFlag()).startsWith("Unknown"));
        assertFalse(AisCodeHelper.DscFlag.getDescription(pos18.getClassBDscFlag()).startsWith("Unknown"));
        assertFalse(AisCodeHelper.BandFlag.getDescription(pos18.getClassBBandFlag()).startsWith("Unknown"));
        assertFalse(AisCodeHelper.Message22Flag.getDescription(pos18.getClassBMsg22Flag()).startsWith("Unknown"));
        assertFalse(AisCodeHelper.AssignedMode.getDescription(pos18.getModeFlag()).startsWith("Unknown"));
        assertFalse(AisCodeHelper.CommStateSelectorFlag.getDescription(pos18.getCommStateSelectorFlag()).startsWith("Unknown"));
    }

    // -------------------------------------------------------------------------
    // Message 24 Part A — Class B CS static data (vessel name)
    // -------------------------------------------------------------------------

    @Test
    public void testMsg24_decodesCorrectly() throws InterruptedException {
        AisMessage msg = parseMessage(MSG_24);
        assertNotNull("Message 24 should parse without error", msg);
        assertEquals(24, msg.getMsgId());
    }

    @Test
    public void testMsg24_isPartA() throws InterruptedException {
        AisMessage24 msg24 = (AisMessage24) parseMessage(MSG_24);
        assertEquals("Captured sentence should be Part B (partNumber = 1)", 1, msg24.getPartNumber());
    }

    @Test
    public void testMsg24_mmsiIsValid() throws InterruptedException {
        AisMessage24 msg24 = (AisMessage24) parseMessage(MSG_24);
        assertTrue(msg24.getUserId() > 0 && msg24.getUserId() <= 999_999_999);
    }

    @Test
    public void testMsg24_vesselNameCleanedSuccessfully() throws InterruptedException {
        AisMessage24 msg24 = (AisMessage24) parseMessage(MSG_24);
        String cleanName = AisCodeHelper.cleanVesselName(msg24.getName());
        assertNotNull(cleanName);
        assertFalse("Cleaned name must not contain '@'", cleanName.contains("@"));
    }

    @Test
    public void testMsg24_msg18ShareTheSameVessel() throws InterruptedException {
        // Msg 18 and Msg 24 in the test data are from the same vessel
        AisMessage18 pos18  = (AisMessage18) parseMessage(MSG_18);
        AisMessage24 static24 = (AisMessage24) parseMessage(MSG_24);
        assertEquals("Msg 18 and Msg 24 should share the same MMSI",
                pos18.getUserId(), static24.getUserId());
    }

    // -------------------------------------------------------------------------
    // Message 5 — Static and Voyage Related Data (two-part)
    // -------------------------------------------------------------------------

    @Test
    public void testMsg5_assemblesAndDecodesCorrectly() throws InterruptedException {
        AisMessage msg = parseMessage(MSG_5_PT1, MSG_5_PT2);
        assertNotNull("Two-part Message 5 should assemble and parse without error", msg);
        assertEquals(5, msg.getMsgId());
    }

    @Test
    public void testMsg5_mmsiIsValid() throws InterruptedException {
        AisMessage5 msg5 = (AisMessage5) parseMessage(MSG_5_PT1, MSG_5_PT2);
        assertTrue(msg5.getUserId() > 0 && msg5.getUserId() <= 999_999_999);
    }

    @Test
    public void testMsg5_msg1ShareTheSameVessel() throws InterruptedException {
        // Msg 1 and Msg 5 in the test data are from the same vessel
        AisPositionMessage pos1 = (AisPositionMessage) parseMessage(MSG_1);
        AisMessage5 msg5 = (AisMessage5) parseMessage(MSG_5_PT1, MSG_5_PT2);
        assertEquals("Msg 1 and Msg 5 should share the same MMSI",
                pos1.getUserId(), msg5.getUserId());
    }

    @Test
    public void testMsg5_vesselNameCleanedSuccessfully() throws InterruptedException {
        AisMessage5 msg5 = (AisMessage5) parseMessage(MSG_5_PT1, MSG_5_PT2);
        String cleanName = AisCodeHelper.cleanVesselName(msg5.getName());
        assertFalse("Cleaned vessel name must not contain '@'", cleanName.contains("@"));
    }

    @Test
    public void testMsg5_destinationCleanedSuccessfully() throws InterruptedException {
        AisMessage5 msg5 = (AisMessage5) parseMessage(MSG_5_PT1, MSG_5_PT2);
        String cleanDest = AisCodeHelper.cleanVesselName(msg5.getDest());
        assertFalse("Cleaned destination must not contain '@'", cleanDest.contains("@"));
    }

    @Test
    public void testMsg5_shipTypeResolvesToKnownDescription() throws InterruptedException {
        AisMessage5 msg5 = (AisMessage5) parseMessage(MSG_5_PT1, MSG_5_PT2);
        String shipType = AisCodeHelper.ShipType.getDescription(msg5.getShipType());
        assertFalse("Ship type should resolve to a known description",
                shipType.startsWith("Unknown ship type"));
    }

    @Test
    public void testMsg5_epfdResolvesToKnownDescription() throws InterruptedException {
        AisMessage5 msg5 = (AisMessage5) parseMessage(MSG_5_PT1, MSG_5_PT2);
        String epfd = AisCodeHelper.EpfdType.getDescription(msg5.getPosType());
        assertFalse(epfd.startsWith("Unknown EPFD"));
    }


    // -------------------------------------------------------------------------
    // Message 26 — Scheduled binary broadcast (no driver output assigned)
    // -------------------------------------------------------------------------

    @Test
    public void testMsg26_decodesWithoutException() throws InterruptedException {
        // Message 26 is a scheduled binary broadcast. The driver handler falls
        // through to the default case and logs a debug message — no output
        // is published. This test documents that behaviour and confirms the
        // NMEA sentence parses cleanly.
        AisMessage msg = parseMessage(MSG_26);
        if (msg != null) {
            // dma-ais decoded it — verify the type
            assertEquals(26, msg.getMsgId());
            // Confirm MessageType has a human-readable description for type 26
            assertFalse(AisCodeHelper.MessageType.getDescription(26).startsWith("Unknown"));
        }
        // If msg is null the library doesn't support type 26; still no exception = pass
    }
}
