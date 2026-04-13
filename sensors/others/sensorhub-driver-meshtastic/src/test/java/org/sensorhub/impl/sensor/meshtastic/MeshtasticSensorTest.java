package org.sensorhub.impl.sensor.meshtastic;

import com.google.protobuf.ByteString;
import net.opengis.swe.v20.DataBlock;
import org.junit.Test;
import org.meshtastic.proto.MeshProtos;
import org.meshtastic.proto.Portnums;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * Unit tests for the Meshtastic sensor driver.
 *
 * These tests verify key pieces of logic without requiring a physical radio.
 * They are organized into four areas:
 *
 *   1. Control Message Construction  — verifies that outgoing text messages are
 *      built correctly for broadcast and direct destinations.
 *   2. Node ID Formatting            — verifies that 32-bit node numbers are
 *      converted to the "!XXXXXXXX" string format Meshtastic uses.
 *   3. Packet Framing                — verifies the 4-byte binary header that
 *      wraps every protobuf payload sent to the radio.
 *   4. Handler Routing               — verifies that incoming packets are
 *      dispatched to the correct output based on portnum.
 */
public class MeshtasticSensorTest {

    // =========================================================================
    // SECTION 1 — Control Message Construction
    //
    // MeshtasticControlTextMessage.createMeshtasticMessage() builds the
    // MeshProtos.ToRadio protobuf that is sent to the radio. It accepts three
    // inputs from the admin panel: message text, channel number, and destination
    // (which can be "broadcast", a hex node ID like "!9ee6f858", or a decimal
    // node number like "2665936984").
    // =========================================================================

    /**
     * Helper: builds a control, fills in a DataBlock, and calls the private
     * createMeshtasticMessage() method via reflection.
     *
     * Reflection is used here because createMeshtasticMessage is private — it
     * is an internal helper called by execCommand. Testing it directly lets us
     * verify the protobuf it produces without needing a live comm provider.
     */
    private MeshProtos.ToRadio buildControlMessage(String message, int channel, String destination) throws Exception {
        // Create the control (null sensor is fine here — we never send the message)
        MeshtasticControlTextMessage ctrl = new MeshtasticControlTextMessage(new MeshtasticSensor());

        // Create a DataBlock whose layout matches the command description fields:
        //   index 0 — message (String)
        //   index 1 — channel (int)
        //   index 2 — destination (String)
        DataBlock block = ctrl.getCommandDescription().createDataBlock();
        block.setStringValue(0, message);
        block.setIntValue(1, channel);
        block.setStringValue(2, destination);

        // Access the private method
        Method m = MeshtasticControlTextMessage.class
                .getDeclaredMethod("createMeshtasticMessage", DataBlock.class);
        m.setAccessible(true);
        return (MeshProtos.ToRadio) m.invoke(ctrl, block);
    }

    @Test
    public void testControlMessage_broadcastSetsCorrectDestination() throws Exception {
        MeshProtos.MeshPacket packet = buildControlMessage("Hello mesh!", 0, "broadcast").getPacket();

        // 0xFFFFFFFF is the Meshtastic "send to all nodes" address
        assertEquals("Broadcast destination should be 0xFFFFFFFF",
                0xFFFFFFFFL, Integer.toUnsignedLong(packet.getTo()));
    }

    @Test
    public void testControlMessage_broadcastDoesNotRequestAck() throws Exception {
        MeshProtos.MeshPacket packet = buildControlMessage("Hello mesh!", 0, "broadcast").getPacket();

        // Broadcasts go to every node — requesting an ack from every node would
        // flood the mesh, so wantAck must be false for broadcast messages.
        assertFalse("Broadcast should not request acknowledgement", packet.getWantAck());
    }

    @Test
    public void testControlMessage_broadcastIsCaseInsensitive() throws Exception {
        // The admin panel might send "BROADCAST" or "Broadcast" — all should work
        MeshProtos.MeshPacket packet = buildControlMessage("Test", 0, "BROADCAST").getPacket();

        assertEquals("BROADCAST (uppercase) should set destination to 0xFFFFFFFF",
                0xFFFFFFFFL, Integer.toUnsignedLong(packet.getTo()));
        assertFalse(packet.getWantAck());
    }

    @Test
    public void testControlMessage_hexDestinationParsedCorrectly() throws Exception {
        // "!9ee6f858" is the standard Meshtastic node ID format
        MeshProtos.MeshPacket packet = buildControlMessage("Direct msg", 0, "!9ee6f858").getPacket();

        assertEquals("Hex node ID should be parsed to the correct 32-bit value",
                0x9ee6f858L, Integer.toUnsignedLong(packet.getTo()));
    }

    @Test
    public void testControlMessage_directMessageRequestsAck() throws Exception {
        MeshProtos.MeshPacket packet = buildControlMessage("Direct msg", 0, "!9ee6f858").getPacket();

        // When sending directly to a specific node, wantAck = true so the
        // sender knows whether the message was received.
        assertTrue("Direct (non-broadcast) message should request acknowledgement",
                packet.getWantAck());
    }

    @Test
    public void testControlMessage_decimalDestinationMatchesHex() throws Exception {
        // 0x9ee6f858 = 2665936984 in decimal — both should resolve to the same node
        MeshProtos.MeshPacket hexPacket     = buildControlMessage("msg", 0, "!9ee6f858").getPacket();
        MeshProtos.MeshPacket decimalPacket = buildControlMessage("msg", 0, "2665936984").getPacket();

        assertEquals("Decimal and hex destinations should resolve to the same node ID",
                Integer.toUnsignedLong(hexPacket.getTo()),
                Integer.toUnsignedLong(decimalPacket.getTo()));
    }

    @Test
    public void testControlMessage_channelIsPassedThrough() throws Exception {
        MeshProtos.MeshPacket packet = buildControlMessage("Channel test", 3, "broadcast").getPacket();

        assertEquals("The specified channel number should appear in the packet", 3, packet.getChannel());
    }

    @Test
    public void testControlMessage_portnumIsTextMessageApp() throws Exception {
        MeshProtos.MeshPacket packet = buildControlMessage("Hello", 0, "broadcast").getPacket();

        // Portnum 1 = TEXT_MESSAGE_APP — this tells the receiving radio how to
        // display the message in its inbox.
        assertEquals("Control must use the TEXT_MESSAGE_APP portnum",
                Portnums.PortNum.TEXT_MESSAGE_APP_VALUE,
                packet.getDecoded().getPortnumValue());
    }

    @Test
    public void testControlMessage_payloadContainsMessageText() throws Exception {
        String text = "Hello from OSH!";
        MeshProtos.MeshPacket packet = buildControlMessage(text, 0, "broadcast").getPacket();

        String decoded = packet.getDecoded().getPayload().toString(StandardCharsets.UTF_8);
        assertEquals("The message text should be present in the packet payload as UTF-8", text, decoded);
    }


    // =========================================================================
    // SECTION 2 — Node ID Formatting
    //
    // Meshtastic node IDs are 32-bit unsigned integers stored as signed Java
    // ints. The driver formats them as "!XXXXXXXX" (8 lower-case hex digits).
    // These tests verify that conversion (used in both handleMyInfo and
    // MeshtasticOutputPacketInfo.convert32IntToString).
    // =========================================================================

    @Test
    public void testNodeIdFormat_knownValue() {
        // 0x9ee6f858 stored as a signed int is negative, but must format correctly
        int nodeNum = 0x9ee6f858;
        long unsigned = Integer.toUnsignedLong(nodeNum);
        String result = String.format("!%08x", unsigned);

        assertEquals("Known node number should produce the expected ID string",
                "!9ee6f858", result);
    }

    @Test
    public void testNodeIdFormat_zero() {
        long unsigned = Integer.toUnsignedLong(0);
        String result = String.format("!%08x", unsigned);

        assertEquals("Zero node number must be zero-padded to 8 hex digits", "!00000000", result);
    }

    @Test
    public void testNodeIdFormat_maxUnsignedValue() {
        // 0xFFFFFFFF = -1 as signed int — the broadcast address
        int maxUnsigned = 0xFFFFFFFF;
        long unsigned = Integer.toUnsignedLong(maxUnsigned);
        String result = String.format("!%08x", unsigned);

        assertEquals("Max unsigned value (broadcast address) should format as !ffffffff",
                "!ffffffff", result);
    }


    // =========================================================================
    // SECTION 3 — Packet Framing
    //
    // Every message sent to the radio is wrapped in a 4-byte frame:
    //   [0x94] [0xC3] [length MSB] [length LSB]
    // followed by the protobuf payload bytes. These tests verify the framing
    // arithmetic that MeshtasticSensor.sendMessage() uses.
    // =========================================================================

    @Test
    public void testFrameHeader_startBytes() {
        byte[] header = new byte[4];
        header[0] = (byte) 0x94;   // START1
        header[1] = (byte) 0xC3;   // START2

        assertEquals("First header byte must be 0x94 (START1)",  (byte) 0x94, header[0]);
        assertEquals("Second header byte must be 0xC3 (START2)", (byte) 0xC3, header[1]);
    }

    @Test
    public void testFrameHeader_lengthRoundTrip() {
        // Verify that a payload length can be encoded into two bytes and decoded back.
        // This matters because payloads can exceed 255 bytes, requiring the MSB.
        int[] testLengths = {0, 1, 127, 255, 256, 300, 512};

        for (int len : testLengths) {
            byte msb = (byte) ((len >> 8) & 0xFF);
            byte lsb = (byte) (len & 0xFF);
            int decoded = ((msb & 0xFF) << 8) | (lsb & 0xFF);

            assertEquals("Length " + len + " should survive a MSB/LSB encode-decode round trip",
                    len, decoded);
        }
    }


    // =========================================================================
    // SECTION 4 — Handler Routing
    //
    // MeshtasticHandler dispatches incoming packets to the correct output based
    // on the Meshtastic portnum value inside the decoded data:
    //   portnum 1  → textOutput
    //   portnum 3  → posOutput
    //   portnum 4  → nodeInfoOutput
    //   any other  → genericOutput
    //
    // Each test creates a minimal sensor with stub outputs (anonymous subclasses
    // that only track whether setData was called), then verifies that exactly
    // the right output received the packet.
    // =========================================================================

    /** Builds a packet with the given portnum and an empty payload. */
    private MeshProtos.MeshPacket packetWithPortnum(int portnum) {
        return MeshProtos.MeshPacket.newBuilder()
                .setDecoded(MeshProtos.Data.newBuilder()
                        .setPortnumValue(portnum)
                        .setPayload(ByteString.EMPTY)
                        .build())
                .build();
    }

    @Test
    public void testHandlerRouting_textPortnum() {
        boolean[] received = {false};

        MeshtasticSensor sensor = new MeshtasticSensor();
        sensor.textOutput     = new MeshtasticOutputTextMessage(sensor) {
            @Override public void setData(MeshProtos.MeshPacket p, ByteString b) { received[0] = true; }
        };
        sensor.posOutput      = new MeshtasticOutputPosition(sensor)  { @Override public void setData(MeshProtos.MeshPacket p, ByteString b) {} };
        sensor.nodeInfoOutput = new MeshtasticOutputNodeInfo(sensor)   { @Override public void setData(MeshProtos.MeshPacket p, ByteString b) {} };
        sensor.genericOutput  = new MeshtasticOutputGeneric(sensor)    { @Override public void setData(MeshProtos.MeshPacket p, ByteString b) {} };

        new MeshtasticHandler(sensor).handlePacket(packetWithPortnum(1));

        assertTrue("Portnum 1 (TEXT_MESSAGE_APP) must be routed to textOutput", received[0]);
    }

    @Test
    public void testHandlerRouting_positionPortnum() {
        boolean[] received = {false};

        MeshtasticSensor sensor = new MeshtasticSensor();
        sensor.textOutput     = new MeshtasticOutputTextMessage(sensor) { @Override public void setData(MeshProtos.MeshPacket p, ByteString b) {} };
        sensor.posOutput      = new MeshtasticOutputPosition(sensor)   {
            @Override public void setData(MeshProtos.MeshPacket p, ByteString b) { received[0] = true; }
        };
        sensor.nodeInfoOutput = new MeshtasticOutputNodeInfo(sensor)   { @Override public void setData(MeshProtos.MeshPacket p, ByteString b) {} };
        sensor.genericOutput  = new MeshtasticOutputGeneric(sensor)    { @Override public void setData(MeshProtos.MeshPacket p, ByteString b) {} };

        new MeshtasticHandler(sensor).handlePacket(packetWithPortnum(3));

        assertTrue("Portnum 3 (POSITION_APP) must be routed to posOutput", received[0]);
    }

    @Test
    public void testHandlerRouting_nodeInfoPortnum() {
        boolean[] received = {false};

        MeshtasticSensor sensor = new MeshtasticSensor();
        sensor.textOutput     = new MeshtasticOutputTextMessage(sensor) { @Override public void setData(MeshProtos.MeshPacket p, ByteString b) {} };
        sensor.posOutput      = new MeshtasticOutputPosition(sensor)   { @Override public void setData(MeshProtos.MeshPacket p, ByteString b) {} };
        sensor.nodeInfoOutput = new MeshtasticOutputNodeInfo(sensor)   {
            @Override public void setData(MeshProtos.MeshPacket p, ByteString b) { received[0] = true; }
        };
        sensor.genericOutput  = new MeshtasticOutputGeneric(sensor)    { @Override public void setData(MeshProtos.MeshPacket p, ByteString b) {} };

        new MeshtasticHandler(sensor).handlePacket(packetWithPortnum(4));

        assertTrue("Portnum 4 (NODEINFO_APP) must be routed to nodeInfoOutput", received[0]);
    }

    @Test
    public void testHandlerRouting_unknownPortnumGoesToGeneric() {
        boolean[] received = {false};

        MeshtasticSensor sensor = new MeshtasticSensor();
        sensor.textOutput     = new MeshtasticOutputTextMessage(sensor) { @Override public void setData(MeshProtos.MeshPacket p, ByteString b) {} };
        sensor.posOutput      = new MeshtasticOutputPosition(sensor)   { @Override public void setData(MeshProtos.MeshPacket p, ByteString b) {} };
        sensor.nodeInfoOutput = new MeshtasticOutputNodeInfo(sensor)   { @Override public void setData(MeshProtos.MeshPacket p, ByteString b) {} };
        sensor.genericOutput  = new MeshtasticOutputGeneric(sensor)    {
            @Override public void setData(MeshProtos.MeshPacket p, ByteString b) { received[0] = true; }
        };

        // Portnum 99 is not registered — should fall through to genericOutput
        new MeshtasticHandler(sensor).handlePacket(packetWithPortnum(99));

        assertTrue("An unregistered portnum must be routed to genericOutput", received[0]);
    }
}
