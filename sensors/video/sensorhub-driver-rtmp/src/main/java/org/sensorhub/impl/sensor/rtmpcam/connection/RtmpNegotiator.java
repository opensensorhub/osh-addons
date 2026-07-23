package org.sensorhub.impl.sensor.rtmpcam.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Owns the complete RTMP protocol for one publisher connection.
 *
 * Phase 1  doHandshake()  — C0/C1/C2 ↔ S0/S1/S2
 * Phase 2  negotiate()    — AMF0 command loop → RtmpConnectionContext
 * Phase 3  buildFlvStream() — chunk → FLV pump on a virtual thread → InputStream
 *
 * All three phases share chunk-size and per-stream reassembly state.
 */
public class RtmpNegotiator {

    // Protocol constants
    private static final int RTMP_VERSION       = 3;
    private static final int HANDSHAKE_SIZE     = 1536;
    private static final int DEFAULT_CHUNK_SIZE = 128;
    private static final int SERVER_CHUNK_SIZE  = 4096;
    private static final int WINDOW_ACK_SIZE    = 2_500_000;

    // Inbound message type IDs
    private static final int MSG_SET_CHUNK_SIZE     = 1;
    private static final int MSG_ABORT              = 2;
    private static final int MSG_ACK                = 3;
    private static final int MSG_USER_CONTROL       = 4;
    private static final int MSG_WINDOW_ACK_SIZE    = 5;
    private static final int MSG_SET_PEER_BANDWIDTH = 6;
    private static final int MSG_AUDIO              = 8;
    private static final int MSG_VIDEO              = 9;
    private static final int MSG_DATA_AMF3          = 15;
    private static final int MSG_COMMAND_AMF3       = 17;
    private static final int MSG_DATA_AMF0          = 18;
    private static final int MSG_COMMAND_AMF0       = 20;

    // User control event types
    private static final int EVENT_PING_REQUEST  = 6;

    // Chunk stream IDs used for server-sent messages (all < 64 → 1-byte basic header)
    private static final int CS_PROTOCOL = 2; // protocol control
    private static final int CS_COMMAND  = 3; // AMF0 command responses
    private static final int CS_STREAM   = 5; // per-stream status (onStatus)

    private static final byte[] FLV_HEADER = {
            'F', 'L', 'V',
            0x01,              // version
            0x05,              // flags: audio | video
            0x00, 0x00, 0x00, 0x09,  // data offset = 9
            0x00, 0x00, 0x00, 0x00   // PreviousTagSize0 = 0
    };
    private static final Logger logger = LoggerFactory.getLogger(RtmpNegotiator.class);

    // Instance state
    private final DataInputStream  in;
    private final DataOutputStream out;
    private final int              port;

    private int  readChunkSize = DEFAULT_CHUNK_SIZE;
    private int  windowAckSize = WINDOW_ACK_SIZE;
    private long bytesReceived = 0;
    private long bytesAcked    = 0;

    private final Map<Integer, ChunkStream> chunkStreams = new HashMap<>();

    public RtmpNegotiator(DataInputStream in, DataOutputStream out, int port) {
        this.in   = in;
        this.out  = out;
        this.port = port;
    }

    /**
     * Performs the C0/C1/C2 ↔ S0/S1/S2 handshake (simple mode).
     * Modern publishers (OBS, FFmpeg, etc.) accept the simple echo handshake.
     */
    public void doHandshake() throws IOException {
        // C0: version byte
        int c0 = in.readUnsignedByte();
        if (c0 != RTMP_VERSION) throw new IOException("Unsupported RTMP version: " + c0);

        // C1: [4 time] [4 zeros] [1528 random]
        byte[] c1 = new byte[HANDSHAKE_SIZE];
        in.readFully(c1);

        // S0
        out.writeByte(RTMP_VERSION);

        // S1: [4 time=0] [4 zeros] [1528 random]
        byte[] s1 = new byte[HANDSHAKE_SIZE];
        ThreadLocalRandom.current().nextBytes(s1);
        Arrays.fill(s1, 0, 8, (byte) 0); // zero time and reserved fields

        // S2: echo of C1 — [4 C1-time] [4 server-time] [1528 C1-random]
        byte[] s2 = Arrays.copyOf(c1, HANDSHAKE_SIZE);
        long   now = System.currentTimeMillis();
        s2[4] = (byte)(now >> 24);
        s2[5] = (byte)(now >> 16);
        s2[6] = (byte)(now >>  8);
        s2[7] = (byte) now;

        out.write(s1);
        out.write(s2);
        out.flush();

        // C2: echo of S1 — read and discard (no validation for simple mode)
        in.readFully(new byte[HANDSHAKE_SIZE]);
    }

    /**
     * Sends server control messages then loops over incoming RTMP chunks,
     * processing AMF0 commands until a {@code publish} command is confirmed.
     *
     * Handles: connect, releaseStream, FCPublish, createStream, publish.
     * Ignores: getStreamLength, FCSubscribe, and any unknown commands.
     */
    public RtmpConnectionContext negotiate() throws IOException {
        // Server control sent immediately — expected before any AMF0 exchange
        sendWindowAckSize(WINDOW_ACK_SIZE);
        sendSetPeerBandwidth(WINDOW_ACK_SIZE);
        sendSetChunkSize(SERVER_CHUNK_SIZE);

        String username  = null;
        String password  = null;
        String path      = null;
        String streamKey = null;

        negotiateLoop:
        while (streamKey == null) {
            RtmpMessage msg = readMessage();

            switch (msg.type()) {
                case MSG_SET_CHUNK_SIZE     -> readChunkSize = parseUInt32(msg.data());
                case MSG_WINDOW_ACK_SIZE    -> windowAckSize = parseUInt32(msg.data());
                case MSG_ACK                -> { /* client ack — no action needed */ }
                case MSG_ABORT              -> chunkStreams.remove(parseUInt32(msg.data()));
                case MSG_USER_CONTROL       -> handleUserControl(msg.data());
                case MSG_SET_PEER_BANDWIDTH -> { /* ignore */ }
                case MSG_DATA_AMF0          -> { /* @setDataFrame before publish — ignore */ }
                case MSG_DATA_AMF3          -> { /* ignore */ }

                case MSG_COMMAND_AMF0 -> {
                    DataInputStream amf     = wrapBytes(msg.data());
                    Object          nameObj = readAmf0Value(amf);
                    if (!(nameObj instanceof String cmdName)) continue;

                    switch (cmdName) {
                        case "connect" -> {
                            double txId = asDouble(readAmf0Value(amf));

                            @SuppressWarnings("unchecked")
                            Map<String, Object> info =
                                    (Map<String, Object>) readAmf0Value(amf);

                            path = getString(info, "app");

                            // Credentials: prefer tcUrl embed, then explicit fields
                            String tcUrl = getString(info, "tcUrl");
                            if (tcUrl != null) {
                                String[] creds = extractCredentials(tcUrl);
                                username = creds[0];
                                password = creds[1];
                            }
                            if (username == null)
                                username = coalesce(getString(info, "user"),
                                        getString(info, "username"));
                            if (password == null)
                                password = coalesce(getString(info, "pass"),
                                        getString(info, "password"));

                            sendConnectResult(txId);
                        }

                        case "releaseStream", "getStreamLength", "FCSubscribe", "_checkbw" -> { /* no-op */ }

                        case "FCPublish" -> {
                            double txId = asDouble(readAmf0Value(amf)); // txId
                            readAmf0Value(amf);                          // null
                            String name = readOptionalString(amf);
                            sendFCPublishResult(txId, name);
                        }

                        case "createStream" -> {
                            double txId = asDouble(readAmf0Value(amf));
                            sendCreateStreamResult(txId, 1); // always stream ID 1
                        }

                        case "publish" -> {
                            readAmf0Value(amf); // txId (0 for publish commands)
                            readAmf0Value(amf); // null command object
                            streamKey = (String) readAmf0Value(amf); // stream name
                            // publish type ("live", "record", "append") — ignored
                            sendPublishStart(1, streamKey);
                            break negotiateLoop;
                        }

                        default ->
                                System.out.printf("[RTMP:%d] Unknown negotiate command: %s%n",
                                        port, cmdName);
                    }
                }

                case MSG_COMMAND_AMF3 -> {
                    // AMF3 command: skip leading 0x00 compatibility byte, then read name
                    byte[] d = msg.data();
                    if (d.length > 1) {
                        Object nameObj = readAmf0Value(wrapBytes(d, 1));
                        System.out.printf("[RTMP:%d] AMF3 command ignored: %s%n", port, nameObj);
                    }
                }
            }
        }

        return new RtmpConnectionContext(
                port,
                username  != null ? username  : "",
                password  != null ? password  : "",
                path      != null ? path      : "",
                streamKey);
    }

    /**
     * Starts a virtual thread that reads RTMP chunks and writes FLV-framed
     * bytes into a pipe.  The returned {@link InputStream} is consumed by
     * FFmpeg via custom AVIO.
     *
     * The background thread exits cleanly when the publisher disconnects
     * (IOException from the socket) or sends deleteStream / FCUnpublish.
     */
    public InputStream buildFlvStream() throws IOException {
        PipedOutputStream pipeOut = new PipedOutputStream();
        PipedInputStream  pipeIn  = new PipedInputStream(pipeOut, 1 << 20);

        Thread pumpThread = new Thread(() -> {
            try (pipeOut) {
                pipeOut.write(FLV_HEADER);
                pumpFlv(pipeOut);
            } catch (IOException ignored) {
                // Normal exit: publisher dropped or pipe closed
            }
        }, "rtmp-flv-pump-" + port);
        pumpThread.setDaemon(true);
        pumpThread.start();

        return pipeIn;
    }

    private void pumpFlv(OutputStream flvOut) throws IOException {
        while (true) {
            RtmpMessage msg = readMessage();
            switch (msg.type()) {
                case MSG_AUDIO, MSG_VIDEO -> writeFlvMediaTag(flvOut, msg);
                case MSG_DATA_AMF0        -> writeFlvScriptTag(flvOut, msg);
                case MSG_SET_CHUNK_SIZE   -> readChunkSize = parseUInt32(msg.data());
                case MSG_WINDOW_ACK_SIZE  -> windowAckSize = parseUInt32(msg.data());
                case MSG_ACK              -> { /* ignore */ }
                case MSG_ABORT            -> chunkStreams.remove(parseUInt32(msg.data()));
                case MSG_USER_CONTROL     -> handleUserControl(msg.data());

                case MSG_COMMAND_AMF0 -> {
                    Object nameObj = readAmf0Value(wrapBytes(msg.data()));
                    if (nameObj instanceof String cmdName) {
                        switch (cmdName) {
                            case "deleteStream",
                                 "closeStream",
                                 "FCUnpublish" -> { return; }
                            default -> { /* ignore mid-stream commands */ }
                        }
                    }
                }
            }
        }
    }

    /**
     * Writes a media tag encapsulated in the FLV format to the specified output stream.
     */
    private void writeFlvMediaTag(OutputStream out, RtmpMessage msg) throws IOException {
        writeFlvTag(out, msg.type(), msg.data(), (int) msg.timestamp());
    }

    /**
     * Data messages carry "@setDataFrame" + "onMetaData" + ECMA array.
     * FLV script tags expect "onMetaData" + ECMA array — strip the prefix.
     */
    private void writeFlvScriptTag(OutputStream out, RtmpMessage msg) throws IOException {
        DataInputStream src   = wrapBytes(msg.data());
        Object          first = readAmf0Value(src);

        byte[] payload = "@setDataFrame".equals(first)
                ? src.readAllBytes()   // remaining bytes start with "onMetaData"
                : msg.data();          // already starts with event name

        writeFlvTag(out, 0x12 /* script */, payload, (int) msg.timestamp());
    }

    /**
     * Writes one complete FLV tag:
     * [1 type][3 dataSize][3 ts_low][1 ts_high][3 streamId=0][N data][4 prevTagSize]
     */
    private static void writeFlvTag(OutputStream out, int tagType,
                                    byte[] payload, int ts) throws IOException {
        int dataSize = payload.length;
        int tagTotal = 11 + dataSize;

        out.write(tagType);
        writeUInt24(out, dataSize);
        writeUInt24(out, ts & 0x00FFFFFF);   // lower 24 bits
        out.write((ts >> 24) & 0xFF);         // TimestampExtended (upper 8 bits)
        writeUInt24(out, 0);                  // StreamID always 0 in FLV
        out.write(payload);
        writeUInt32(out, tagTotal);           // PreviousTagSize
    }

    /**
     * Reads and reassembles RTMP chunks until a complete RTMP message is ready.
     *
     * Chunk wire format (per Adobe spec):
     *   [basic header 1-3B] [message header 0/3/7/11B] [ext timestamp 0/4B] [payload ≤ chunkSize]
     *
     * fmt=0: full 11-byte header → new message with absolute timestamp
     * fmt=1: 7-byte header      → new message, inherits stream ID, delta timestamp
     * fmt=2: 3-byte header      → continues with same type+length, delta timestamp
     * fmt=3: no header          → continuation chunk, same everything
     */
    private RtmpMessage readMessage() throws IOException {
        while (true) {

            // ── Basic header ───────────────────────────────────────────────
            int byte0 = readByte();
            int fmt   = (byte0 >> 6) & 0x3;
            int csId  = byte0 & 0x3F;

            // Extended chunk stream IDs
            if      (csId == 0) csId = readByte() + 64;
            else if (csId == 1) csId = readByte() * 256 + readByte() + 64;

            ChunkStream cs       = chunkStreams.computeIfAbsent(csId, k -> new ChunkStream());
            boolean     prevDone = cs.payload == null || cs.bytesRead >= cs.messageLength;

            // ── Message header ─────────────────────────────────────────────
            switch (fmt) {
                case 0 -> {
                    // New message — absolute timestamp, full header
                    long ts = readUInt24();
                    cs.messageLength    = (int) readUInt24();
                    cs.messageType      = readByte();
                    cs.messageStreamId  = readLittleEndianInt();
                    cs.hasExtTimestamp  = (ts >= 0xFFFFFF);
                    cs.timestamp        = cs.hasExtTimestamp ? readUInt32() : ts;
                    cs.timestampDelta   = 0;
                    cs.payload          = new byte[cs.messageLength];
                    cs.bytesRead        = 0;
                }
                case 1 -> {
                    // New message — inherits stream ID, delta timestamp
                    long delta = readUInt24();
                    cs.messageLength    = (int) readUInt24();
                    cs.messageType      = readByte();
                    cs.hasExtTimestamp  = (delta >= 0xFFFFFF);
                    cs.timestampDelta   = cs.hasExtTimestamp ? readUInt32() : delta;
                    cs.timestamp       += cs.timestampDelta;
                    cs.payload          = new byte[cs.messageLength];
                    cs.bytesRead        = 0;
                }
                case 2 -> {
                    // Delta timestamp only — inherits type and length
                    long delta = readUInt24();
                    cs.hasExtTimestamp = (delta >= 0xFFFFFF);
                    cs.timestampDelta  = cs.hasExtTimestamp ? readUInt32() : delta;
                    cs.timestamp      += cs.timestampDelta;
                    if (prevDone) {
                        cs.payload   = new byte[cs.messageLength];
                        cs.bytesRead = 0;
                    }
                }
                case 3 -> {
                    // Spec: re-read 4-byte ext timestamp if the last fmt 0/1/2 had one
                    if (cs.hasExtTimestamp) readUInt32();
                    if (prevDone) {
                        cs.timestamp += cs.timestampDelta;
                        cs.payload    = new byte[cs.messageLength];
                        cs.bytesRead  = 0;
                    }
                }
            }

            if (cs.payload == null) cs.payload = new byte[cs.messageLength]; // safety

            // ── Payload bytes for this chunk ───────────────────────────────
            int remaining = cs.messageLength - cs.bytesRead;
            int toRead    = Math.min(readChunkSize, remaining);
            in.readFully(cs.payload, cs.bytesRead, toRead);
            cs.bytesRead  += toRead;
            bytesReceived += toRead;
            maybeAck();

            if (cs.bytesRead >= cs.messageLength) {
                byte[] data = cs.payload.clone();
                cs.payload   = null; // ready for next message on this chunk stream
                cs.bytesRead = 0;
                return new RtmpMessage(cs.messageType, cs.messageStreamId, cs.timestamp, data);
            }
            // Message spans more chunks — keep looping
        }
    }


    private void sendWindowAckSize(int size) throws IOException {
        sendMessage(CS_PROTOCOL, MSG_WINDOW_ACK_SIZE, 0, 0, encodeUInt32(size));
    }

    private void sendSetPeerBandwidth(int bandwidth) throws IOException {
        byte[] b = Arrays.copyOf(encodeUInt32(bandwidth), 5);
        b[4] = 0x02; // limit type: dynamic
        sendMessage(CS_PROTOCOL, MSG_SET_PEER_BANDWIDTH, 0, 0, b);
    }

    private void sendSetChunkSize(int size) throws IOException {
        sendMessage(CS_PROTOCOL, MSG_SET_CHUNK_SIZE, 0, 0, encodeUInt32(size));
    }

    private void sendStreamBegin(int streamId) throws IOException {
        byte[] payload = new byte[6];
        payload[0] = 0x00; payload[1] = 0x00; // event type: StreamBegin
        payload[2] = (byte)(streamId >> 24); payload[3] = (byte)(streamId >> 16);
        payload[4] = (byte)(streamId >>  8); payload[5] = (byte) streamId;
        sendMessage(CS_PROTOCOL, MSG_USER_CONTROL, 0, 0, payload);
    }

    private void sendAck(long seq) throws IOException {
        sendMessage(CS_PROTOCOL, MSG_ACK, 0, 0, encodeUInt32((int) seq));
    }

    private void sendPingResponse(long token) throws IOException {
        byte[] payload = new byte[6];
        payload[0] = 0x00; payload[1] = 0x07; // event type: PingResponse
        payload[2] = (byte)(token >> 24); payload[3] = (byte)(token >> 16);
        payload[4] = (byte)(token >>  8); payload[5] = (byte) token;
        sendMessage(CS_PROTOCOL, MSG_USER_CONTROL, 0, 0, payload);
    }



    private void sendConnectResult(double txId) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(256);
        DataOutputStream      d   = new DataOutputStream(buf);

        writeAmf0Str(d, "_result");
        writeAmf0Num(d, txId);

        writeAmf0ObjStart(d);
        writeAmf0Field(d, "fmsVer",       "FMS/3,0,1,123");
        writeAmf0Field(d, "capabilities", 31.0);
        writeAmf0Field(d, "mode",          1.0);
        writeAmf0ObjEnd(d);

        writeAmf0ObjStart(d);
        writeAmf0Field(d, "level",          "status");
        writeAmf0Field(d, "code",           "NetConnection.Connect.Success");
        writeAmf0Field(d, "description",    "Connection succeeded.");
        writeAmf0Field(d, "objectEncoding", 0.0);
        writeAmf0ObjEnd(d);

        sendMessage(CS_COMMAND, MSG_COMMAND_AMF0, 0, 0, buf.toByteArray());

        // onBWDone — expected by OBS and other publishers after _result
        buf.reset();
        writeAmf0Str(d, "onBWDone");
        writeAmf0Num(d, 0);
        writeAmf0Null(d);
        sendMessage(CS_COMMAND, MSG_COMMAND_AMF0, 0, 0, buf.toByteArray());
    }

    private void sendFCPublishResult(double txId, String streamKey) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        DataOutputStream      d   = new DataOutputStream(buf);

        writeAmf0Str(d, "onFCPublish");
        writeAmf0Num(d, txId);
        writeAmf0Null(d);
        writeAmf0ObjStart(d);
        writeAmf0Field(d, "code",        "NetStream.Publish.Start");
        writeAmf0Field(d, "description", streamKey + " is now published.");
        writeAmf0ObjEnd(d);

        sendMessage(CS_COMMAND, MSG_COMMAND_AMF0, 0, 0, buf.toByteArray());
    }

    private void sendCreateStreamResult(double txId, int streamId) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        DataOutputStream      d   = new DataOutputStream(buf);

        writeAmf0Str(d, "_result");
        writeAmf0Num(d, txId);
        writeAmf0Null(d);
        writeAmf0Num(d, streamId);

        sendMessage(CS_COMMAND, MSG_COMMAND_AMF0, 0, 0, buf.toByteArray());
    }

    private void sendPublishStart(int streamId, String streamKey) throws IOException {
        sendStreamBegin(streamId);

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        DataOutputStream      d   = new DataOutputStream(buf);

        writeAmf0Str(d, "onStatus");
        writeAmf0Num(d, 0);
        writeAmf0Null(d);
        writeAmf0ObjStart(d);
        writeAmf0Field(d, "level",       "status");
        writeAmf0Field(d, "code",        "NetStream.Publish.Start");
        writeAmf0Field(d, "description", streamKey + " is now published.");
        writeAmf0Field(d, "details",     streamKey);
        writeAmf0ObjEnd(d);

        sendMessage(CS_STREAM, MSG_COMMAND_AMF0, streamId, 0, buf.toByteArray());
    }

    /**
     * Serialises one RTMP message.  Uses fmt=0 for the first chunk (full
     * header) and fmt=3 (no header) for any continuation chunks.
     * Assumes csId < 64 (1-byte basic header) for all server-sent chunk streams.
     */
    private void sendMessage(int csId, int msgType, int msgStreamId,
                             long timestamp, byte[] payload) throws IOException {
        boolean extTs = (timestamp >= 0xFFFFFF);

        // Basic header (fmt=00 | csId)
        out.writeByte(csId & 0x3F);

        // 11-byte message header
        writeUInt24(out, extTs ? 0xFFFFFF : (int) timestamp);
        writeUInt24(out, payload.length);
        out.writeByte(msgType);
        // Stream ID is little-endian in the RTMP spec
        out.writeByte( msgStreamId        & 0xFF);
        out.writeByte((msgStreamId >>  8) & 0xFF);
        out.writeByte((msgStreamId >> 16) & 0xFF);
        out.writeByte((msgStreamId >> 24) & 0xFF);

        if (extTs) out.writeInt((int) timestamp);

        // Payload — split into SERVER_CHUNK_SIZE-byte chunks
        int offset = 0;
        while (offset < payload.length) {
            if (offset > 0) out.writeByte(0xC0 | (csId & 0x3F)); // fmt=11 continuation
            int n = Math.min(SERVER_CHUNK_SIZE, payload.length - offset);
            out.write(payload, offset, n);
            offset += n;
        }
        out.flush();
    }

    /**
     * Handles user control messages by analyzing the provided byte array.
     * If the data represents a ping request and contains a valid token,
     * a ping response is sent back.
     *
     * @param data the byte array containing the user control message data
     * @throws IOException if an error occurs during the processing of the message
     */
    private void handleUserControl(byte[] data) throws IOException {
        if (data.length < 2) return;
        int eventType = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
        if (eventType == EVENT_PING_REQUEST && data.length >= 6) {
            long token = ((long)(data[2] & 0xFF) << 24)
                    | ((long)(data[3] & 0xFF) << 16)
                    | ((long)(data[4] & 0xFF) <<  8)
                    |  (long)(data[5] & 0xFF);
            sendPingResponse(token);
        }
    }

    /**
     * Conditionally sends an acknowledgment message based on the number of bytes
     * received and the configured acknowledgment window size.
     * <p>
     * When the difference between the total bytes received and the bytes
     * already acknowledged reaches or exceeds the window acknowledgment size,
     * this method sends an acknowledgment for the total number of bytes received
     * so far.
     */
    private void maybeAck() throws IOException {
        if (windowAckSize > 0 && (bytesReceived - bytesAcked) >= windowAckSize) {
            bytesAcked = bytesReceived;
            sendAck(bytesReceived);
        }
    }

    /**
     * Reads and decodes an AMF0 (Action Message Format 0) value from the provided input stream.
     * The AMF0 format is a binary serialization format used in Adobe Flash and other related technologies.
     * The method interprets a type byte to identify the kind of data being read and processes it accordingly.
     *
     * @param src the input stream from which the AMF0 value is read.
     *            It must be properly positioned to read the type and value bytes.
     * @return the deserialized object corresponding to the AMF0 type.
     *         May return {@code null} for certain AMF0 types such as null, undefined, object end, or date (skipped).
     * @throws IOException if an I/O error occurs while reading from the input stream or if an unknown type is encountered.
     */
    private Object readAmf0Value(DataInputStream src) throws IOException {
        int type = src.readUnsignedByte();
        return switch (type) {
            case 0  -> src.readDouble();
            case 1  -> src.readUnsignedByte() != 0;
            case 2  -> readAmf0Utf8(src);
            case 3  -> readAmf0Object(src);
            case 5, 6 -> null;                             // Null, Undefined
            case 8  -> readAmf0EcmaArray(src);
            case 9  -> null;                               // ObjectEnd (context-terminator)
            case 10 -> readAmf0StrictArray(src);
            case 11 -> { src.skipBytes(10); yield null; }  // Date — skip
            case 12 -> readAmf0LongString(src);
            default -> throw new IOException("Unknown AMF0 type: 0x" + Integer.toHexString(type));
        };
    }

    private String readAmf0Utf8(DataInputStream src) throws IOException {
        byte[] b = new byte[src.readUnsignedShort()];
        src.readFully(b);
        return new String(b, StandardCharsets.UTF_8);
    }

    private String readAmf0LongString(DataInputStream src) throws IOException {
        byte[] b = new byte[src.readInt()];
        src.readFully(b);
        return new String(b, StandardCharsets.UTF_8);
    }

    private Map<String, Object> readAmf0Object(DataInputStream src) throws IOException {
        Map<String, Object> map = new LinkedHashMap<>();
        while (true) {
            int keyLen = src.readUnsignedShort();
            if (keyLen == 0) { src.readUnsignedByte(); break; } // consume 0x09 end marker
            byte[] kb = new byte[keyLen];
            src.readFully(kb);
            map.put(new String(kb, StandardCharsets.UTF_8), readAmf0Value(src));
        }
        return map;
    }

    private Map<String, Object> readAmf0EcmaArray(DataInputStream src) throws IOException {
        src.readInt(); // array count — informational only
        return readAmf0Object(src); // same layout as Object after the count
    }

    private List<Object> readAmf0StrictArray(DataInputStream src) throws IOException {
        int count = src.readInt();
        List<Object> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) list.add(readAmf0Value(src));
        return list;
    }

    /** Reads one AMF0 value if bytes remain in the stream; otherwise returns "". */
    private String readOptionalString(DataInputStream src) throws IOException {
        if (src.available() <= 0) return "";
        Object v = readAmf0Value(src);
        return v instanceof String s ? s : "";
    }

    private static void writeAmf0Str(DataOutputStream d, String s) throws IOException {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        d.writeByte(2); d.writeShort(b.length); d.write(b);
    }

    private static void writeAmf0Num(DataOutputStream d, double n) throws IOException {
        d.writeByte(0); d.writeDouble(n);
    }

    private static void writeAmf0Null(DataOutputStream d) throws IOException {
        d.writeByte(5);
    }

    private static void writeAmf0Bool(DataOutputStream d, boolean b) throws IOException {
        d.writeByte(1); d.writeByte(b ? 1 : 0);
    }

    private static void writeAmf0ObjStart(DataOutputStream d) throws IOException {
        d.writeByte(3);
    }

    private static void writeAmf0ObjEnd(DataOutputStream d) throws IOException {
        d.writeShort(0); d.writeByte(9); // empty key + 0x09 end marker
    }

    /** Writes one key-value property inside an AMF0 object. */
    private static void writeAmf0Field(DataOutputStream d, String key, Object value)
            throws IOException {
        byte[] kb = key.getBytes(StandardCharsets.UTF_8);
        d.writeShort(kb.length);
        d.write(kb);

        if (value == null) {
            writeAmf0Null(d);
        } else if (value instanceof String s) {
            writeAmf0Str(d, s);
        } else if (value instanceof Double n) {
            writeAmf0Num(d, n);
        } else if (value instanceof Boolean b) {
            writeAmf0Bool(d, b);
        } else {
            writeAmf0Null(d);
            logger.warn("Unsupported AMF0 value type: {}", value.getClass().getName());
        }
    }

    /** Reads one byte and increments the ACK counter. */
    private int readByte() throws IOException {
        bytesReceived++;
        return in.readUnsignedByte();
    }

    private long readUInt24() throws IOException {
        long a = readByte(), b = readByte(), c = readByte();
        return (a << 16) | (b << 8) | c;
    }

    private long readUInt32() throws IOException {
        long a = readByte(), b = readByte(), c = readByte(), d = readByte();
        return (a << 24) | (b << 16) | (c << 8) | d;
    }

    /** RTMP message stream IDs are written little-endian. */
    private int readLittleEndianInt() throws IOException {
        int a = readByte(), b = readByte(), c = readByte(), d = readByte();
        return a | (b << 8) | (c << 16) | (d << 24);
    }

    private static void writeUInt24(OutputStream out, int v) throws IOException {
        out.write((v >> 16) & 0xFF);
        out.write((v >>  8) & 0xFF);
        out.write( v        & 0xFF);
    }

    private static void writeUInt32(OutputStream out, int v) throws IOException {
        out.write((v >> 24) & 0xFF);
        out.write((v >> 16) & 0xFF);
        out.write((v >>  8) & 0xFF);
        out.write( v        & 0xFF);
    }

    private static byte[] encodeUInt32(int v) {
        return new byte[]{ (byte)(v >> 24), (byte)(v >> 16), (byte)(v >> 8), (byte) v };
    }

    private static DataInputStream wrapBytes(byte[] data) {
        return new DataInputStream(new ByteArrayInputStream(data));
    }

    /** Wraps {@code data[offset..]} — used to skip the AMF3 compatibility byte. */
    private static DataInputStream wrapBytes(byte[] data, int offset) {
        return new DataInputStream(
                new ByteArrayInputStream(data, offset, data.length - offset));
    }

    private static int parseUInt32(byte[] b) {
        return ((b[0] & 0xFF) << 24) | ((b[1] & 0xFF) << 16)
                | ((b[2] & 0xFF) <<  8) |  (b[3] & 0xFF);
    }

    private static String getString(Map<String, Object> m, String key) {
        Object v = m.get(key); return v instanceof String s ? s : null;
    }

    private static double asDouble(Object v) {
        return v instanceof Number n ? n.doubleValue() : 0;
    }

    @SafeVarargs
    private static <T> T coalesce(T... values) {
        for (T v : values) if (v != null) return v;
        return null;
    }

    /**
     * Extracts [username, password] from an RTMP URL.
     * "rtmp://user:pass@host:1935/app" → ["user", "pass"]
     * "rtmp://host:1935/app"           → [null, null]
     */
    private static String[] extractCredentials(String tcUrl) {
        try {
            // URI doesn't understand rtmp:// — swap scheme for parsing only
            URI    uri  = new URI(tcUrl.replaceFirst("^rtmps?://", "http://"));
            String info = uri.getUserInfo();
            if (info == null) return new String[]{null, null};
            int c = info.indexOf(':');
            return c < 0
                    ? new String[]{info, null}
                    : new String[]{info.substring(0, c), info.substring(c + 1)};
        } catch (Exception e) {
            return new String[]{null, null};
        }
    }

    /** Per-chunk-stream reassembly state, persisted across chunk reads. */
    private static final class ChunkStream {
        int     messageType;
        int     messageStreamId;
        int     messageLength;
        long    timestamp;
        long    timestampDelta;
        boolean hasExtTimestamp; // true → re-read ext ts on fmt=3 continuation
        byte[]  payload;
        int     bytesRead;
    }

    /** A fully reassembled RTMP message. */
    record RtmpMessage(int type, int streamId, long timestamp, byte[] data) {}
}