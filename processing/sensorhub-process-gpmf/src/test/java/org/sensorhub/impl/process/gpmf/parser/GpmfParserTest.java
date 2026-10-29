/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research, LLC. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.process.gpmf.parser;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for the GPMF key-length-value parser and the telemetry extraction built on it.
 * <p>
 * Payloads are assembled here rather than read from a recording, so that each structural feature of the
 * format -- nesting, 32-bit padding, scale divisors, complex sample layouts -- can be exercised on its own.
 */
public class GpmfParserTest {
    @Test
    public void parsesSimpleEntry() {
        // ACCL, three int16 values per sample, two samples
        byte[] payload = entry("ACCL", 's', 6, 2, shorts(1, 2, 3, 4, 5, 6));

        List<GpmfElement> elements = GpmfParser.parse(payload);

        assertEquals(1, elements.size());
        GpmfElement accl = elements.get(0);
        assertEquals("ACCL", accl.getKey());
        assertEquals(GpmfType.INT16, accl.getType());
        assertEquals(6, accl.getStructSize());
        assertEquals(2, accl.getRepeat());
        assertEquals(3, accl.getElementCount());
        assertArrayEquals(new double[]{1, 2, 3, 4, 5, 6}, accl.asDoubles(), 0);
    }

    @Test
    public void padsPayloadToFourByteBoundary() {
        // A five-byte payload is followed by three padding bytes before the next entry starts
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, entry("RMRK", 'c', 1, 5, "hello".getBytes(StandardCharsets.US_ASCII)));
        write(out, entry("TSMP", 'L', 4, 1, ints(42)));

        List<GpmfElement> elements = GpmfParser.parse(out.toByteArray());

        assertEquals(2, elements.size());
        assertEquals("hello", elements.get(0).asString());
        assertEquals("TSMP", elements.get(1).getKey());
        assertArrayEquals(new double[]{42}, elements.get(1).asDoubles(), 0);
    }

    @Test
    public void parsesNestedContainers() {
        byte[] inner = entry("GYRO", 's', 6, 1, shorts(10, 20, 30));
        byte[] stream = entry("STRM", (char) 0, 1, inner.length, inner);
        byte[] device = entry("DEVC", (char) 0, 1, stream.length, stream);

        List<GpmfElement> elements = GpmfParser.parse(device);

        assertEquals(1, elements.size());
        GpmfElement devc = elements.get(0);
        assertTrue(devc.isNested());
        assertEquals("DEVC", devc.getKey());

        GpmfElement strm = devc.findChild("STRM");
        assertNotNull(strm);

        GpmfElement gyro = strm.findChild("GYRO");
        assertNotNull(gyro);
        assertArrayEquals(new double[]{10, 20, 30}, gyro.asDoubles(), 0);
    }

    @Test
    public void stopsAtTrailingPadding() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, entry("TSMP", 'L', 4, 1, ints(7)));
        write(out, new byte[8]);

        List<GpmfElement> elements = GpmfParser.parse(out.toByteArray());

        assertEquals(1, elements.size());
        assertEquals("TSMP", elements.get(0).getKey());
    }

    @Test
    public void readsEmptyPayloadAsNoEntries() {
        assertTrue(GpmfParser.parse(new byte[0]).isEmpty());
        assertTrue(GpmfParser.parse(null).isEmpty());
    }

    @Test(expected = GpmfParseException.class)
    public void rejectsPayloadRunningPastEndOfContainer() {
        // Declares 40 bytes of payload but only supplies 6
        byte[] header = entry("ACCL", 's', 6, 1, shorts(1, 2, 3));
        header[7] = 7; // bump the repeat count so the declared length overruns
        GpmfParser.parse(header);
    }

    @Test(expected = GpmfParseException.class)
    public void rejectsNonPrintableKey() {
        byte[] payload = entry("ACCL", 's', 2, 1, shorts(1));
        payload[1] = 0x01;
        GpmfParser.parse(payload);
    }

    @Test
    public void readsUnsignedAndFixedPointTypes() {
        // A uint32 that would be negative if read as signed
        GpmfElement unsigned = GpmfParser.parse(entry("TICK", 'L', 4, 1, ints(0xFFFFFFFF))).get(0);
        assertEquals(4294967295d, unsigned.asDoubles()[0], 0);

        // Q15.16: 0x00018000 is 1.5
        GpmfElement fixed = GpmfParser.parse(entry("WBAL", 'q', 4, 1, ints(0x00018000))).get(0);
        assertEquals(1.5d, fixed.asDoubles()[0], 1e-9);
    }

    @Test
    public void appliesSingleScaleDivisorToEveryValue() {
        GpmfPayload payload = GpmfPayload.parse(stream(
                entry("STNM", 'c', 1, 13, "Accelerometer".getBytes(StandardCharsets.US_ASCII)),
                entry("SIUN", 'c', 1, 4, "m/s2".getBytes(StandardCharsets.US_ASCII)),
                entry("SCAL", 's', 2, 1, shorts(418)),
                entry("ACCL", 's', 6, 1, shorts(418, 836, 209))));

        assertEquals(1, payload.getStreams().size());
        GpmfStream accl = payload.getStreams().get(0);

        assertEquals("ACCL", accl.getKey());
        assertEquals("Accelerometer", accl.getName());
        assertEquals("m/s2", accl.getUnits());
        assertEquals(3, accl.getElementCount());
        assertEquals(1, accl.getSampleCount());
        assertArrayEquals(new double[]{1, 2, 0.5}, accl.getValues(), 1e-9);
    }

    @Test
    public void appliesPerElementScaleDivisors() {
        // GPS5 stores lat/lon scaled by 1e7 and altitude and speeds by 1000
        GpmfPayload payload = GpmfPayload.parse(stream(
                entry("STNM", 'c', 1, 3, "GPS".getBytes(StandardCharsets.US_ASCII)),
                entry("SCAL", 'l', 4, 5, ints(10000000, 10000000, 1000, 1000, 1000)),
                entry("GPS5", 'l', 20, 1, ints(345678900, -864321000, 250500, 3400, 3600))));

        GpmfStream gps = payload.getStreams().get(0);

        assertEquals("GPS5", gps.getKey());
        assertEquals(5, gps.getElementCount());
        assertArrayEquals(new double[]{34.56789, -86.4321, 250.5, 3.4, 3.6}, gps.getValues(), 1e-9);
    }

    @Test
    public void scalesEverySampleOfAMultiSampleStream() {
        GpmfPayload payload = GpmfPayload.parse(stream(
                entry("SCAL", 's', 2, 2, shorts(2, 4)),
                entry("MAGN", 's', 4, 3, shorts(2, 4, 6, 8, 10, 12))));

        GpmfStream magn = payload.getStreams().get(0);

        assertEquals(2, magn.getElementCount());
        assertEquals(3, magn.getSampleCount());
        assertArrayEquals(new double[]{1, 1, 3, 2, 5, 3}, magn.getValues(), 1e-9);
    }

    @Test
    public void readsDeviceNameAndStreamTimestamp() {
        byte[] strm = stream(
                entry("STMP", 'J', 8, 1, longs(2_500_000L)),
                entry("ACCL", 's', 2, 1, shorts(5)));

        ByteArrayOutputStream deviceBody = new ByteArrayOutputStream();
        write(deviceBody, entry("DVNM", 'c', 1, 12, "Hero11 Black".getBytes(StandardCharsets.US_ASCII)));
        write(deviceBody, strm);

        byte[] devc = entry("DEVC", (char) 0, 1, deviceBody.size(), deviceBody.toByteArray());

        GpmfStream accl = GpmfPayload.parse(devc).getStreams().get(0);

        assertEquals("Hero11 Black", accl.getDeviceName());
        assertEquals(2.5d, accl.getTimestamp(), 1e-9);
    }

    @Test
    public void readsTextValuedStream() {
        GpmfPayload payload = GpmfPayload.parse(stream(
                entry("GPSU", 'U', 16, 1, "230607120000.000".getBytes(StandardCharsets.US_ASCII))));

        GpmfStream gpsu = payload.getStreams().get(0);

        assertTrue(gpsu.isText());
        assertEquals("230607120000.000", gpsu.getText());
        assertEquals(0, gpsu.getValues().length);
    }

    @Test
    public void readsComplexStreamUsingTypeDefinition() {
        // A GPS9-style record: seven int32 fields then two uint16 fields
        ByteArrayOutputStream sample = new ByteArrayOutputStream();
        write(sample, ints(345678900, -864321000, 250500, 3400, 3600, 8000, 12));
        write(sample, shorts(150, 3));

        GpmfPayload payload = GpmfPayload.parse(stream(
                entry("TYPE", 'c', 1, 9, "lllllllSS".getBytes(StandardCharsets.US_ASCII)),
                entry("SCAL", 'l', 4, 9, ints(10000000, 10000000, 1000, 1000, 1000, 1, 1, 100, 1)),
                entry("GPS9", '?', 32, 1, sample.toByteArray())));

        assertEquals(1, payload.getStreams().size());
        GpmfStream gps9 = payload.getStreams().get(0);

        assertEquals("GPS9", gps9.getKey());
        assertEquals(9, gps9.getElementCount());
        assertEquals(1, gps9.getSampleCount());
        assertArrayEquals(new double[]{34.56789, -86.4321, 250.5, 3.4, 3.6, 8000, 12, 1.5, 3},
                gps9.getValues(), 1e-9);
    }

    @Test
    public void skipsComplexStreamWithoutTypeDefinition() {
        GpmfPayload payload = GpmfPayload.parse(stream(
                entry("GPS9", '?', 8, 1, ints(1, 2))));

        assertTrue(payload.isEmpty());
    }

    @Test
    public void skipsUnrecognizedStreamWithoutLosingTheRest() {
        GpmfPayload payload = GpmfPayload.parse(stream(
                entry("WXYZ", 'z', 4, 1, ints(1)),
                entry("ACCL", 's', 2, 1, shorts(9))));

        assertEquals(1, payload.getStreams().size());
        assertEquals("ACCL", payload.getStreams().get(0).getKey());
    }

    @Test
    public void reportsEveryStreamOfEveryDevice() {
        byte[] firstDevice = device("Hero11 Black", stream(entry("ACCL", 's', 2, 1, shorts(1))));
        byte[] secondDevice = device("Karma", stream(entry("GYRO", 's', 2, 1, shorts(2))));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, firstDevice);
        write(out, secondDevice);

        List<GpmfStream> streams = GpmfPayload.parse(out.toByteArray()).getStreams();

        assertEquals(2, streams.size());
        assertEquals("ACCL", streams.get(0).getKey());
        assertEquals("Hero11 Black", streams.get(0).getDeviceName());
        assertEquals("GYRO", streams.get(1).getKey());
        assertEquals("Karma", streams.get(1).getDeviceName());
    }

    @Test
    public void ignoresZeroScaleDivisor() {
        GpmfPayload payload = GpmfPayload.parse(stream(
                entry("SCAL", 's', 2, 1, shorts(0)),
                entry("TMPC", 's', 2, 1, shorts(25))));

        assertArrayEquals(new double[]{25}, payload.getStreams().get(0).getValues(), 0);
    }

    @Test
    public void readsUnitsAsOneFieldPerElement() {
        // A GPS container packs nine three-byte unit fields, one per element of a sample
        String units = "deg" + "deg" + "m  " + "m/s" + "m/s" + "   " + "s  " + "   " + "   ";

        GpmfPayload payload = GpmfPayload.parse(stream(
                entry("UNIT", 'c', 3, 9, ascii(units)),
                entry("SCAL", 'l', 4, 5, ints(10000000, 10000000, 1000, 1000, 1000)),
                entry("GPS5", 'l', 20, 1, ints(345678900, -864321000, 250500, 3400, 3600))));

        // Without splitting, these run together as "degdegm  m/sm/s   s"
        assertEquals("deg,deg,m,m/s,m/s,,s,,", payload.getStreams().get(0).getUnits());
    }

    @Test
    public void readsSingleQuantityUnitsAsOneString() {
        // A single-quantity stream states its unit as a run of characters, not as one-byte fields
        GpmfPayload payload = GpmfPayload.parse(stream(
                entry("SIUN", 'c', 5, 1, ascii("rad/s")),
                entry("SCAL", 's', 2, 1, shorts(939)),
                entry("GYRO", 's', 6, 1, shorts(939, 1878, 0))));

        assertEquals("rad/s", payload.getStreams().get(0).getUnits());
    }

    @Test
    public void readsLatin1UnitStrings() {
        // GPMF unit strings are Latin-1, which is what carries the micro sign and the superscript two
        byte[] microTesla = {(byte) 0xB5, 'T'};
        GpmfElement element = GpmfParser.parse(entry("SIUN", 'c', 1, 2, microTesla)).get(0);
        assertEquals("µT", element.asString());

        byte[] metresPerSecondSquared = {'m', '/', 's', (byte) 0xB2};
        element = GpmfParser.parse(entry("SIUN", 'c', 1, 4, metresPerSecondSquared)).get(0);
        assertEquals("m/s²", element.asString());
    }

    @Test
    public void skipsStreamWithUndocumentedTypeCode() {
        // The MAX 2 records its disparity matrix as an opaque '#' type that has no documented layout
        GpmfPayload payload = GpmfPayload.parse(stream(
                entry("STNM", 'c', 1, 15, ascii("DisparityMatrix")),
                entry("DISP", '#', 1, 8, new byte[8]),
                entry("DISP", '#', 1, 8, new byte[8])));

        assertTrue(payload.isEmpty());
    }

    @Test
    public void expandsBracketedRepeatInTypeDefinition() {
        assertEquals(List.of(GpmfType.INT32, GpmfType.INT32, GpmfType.INT32),
                GpmfPayload.expandTypeDefinition("l[3]"));
        assertEquals(List.of(GpmfType.INT32, GpmfType.UINT16, GpmfType.UINT16),
                GpmfPayload.expandTypeDefinition("lS[2]"));
        assertTrue(GpmfPayload.expandTypeDefinition("l[3").isEmpty());
        assertTrue(GpmfPayload.expandTypeDefinition("l[x]").isEmpty());
    }

    @Test
    public void rejectsRepeatCountNoSampleCouldHold() {
        // A sample size is stored in one byte, so a count past 255 describes no entry that can exist and
        // is rejected before it is expanded rather than after
        assertTrue(GpmfPayload.expandTypeDefinition("l[256]").isEmpty());
        assertTrue(GpmfPayload.expandTypeDefinition("l[2000000000]").isEmpty());
        assertEquals(255, GpmfPayload.expandTypeDefinition("b[255]").size());
    }

    // ---------------------------------------------------------------------
    // Helpers for assembling GPMF payloads
    // ---------------------------------------------------------------------

    /**
     * Builds one GPMF entry, padded out to a 32-bit boundary.
     */
    private static byte[] entry(String key, char type, int structSize, int repeat, byte[] payload) {
        int padded = (payload.length + 3) / 4 * 4;
        ByteBuffer buffer = ByteBuffer.allocate(8 + padded).order(ByteOrder.BIG_ENDIAN);

        buffer.put(key.getBytes(StandardCharsets.US_ASCII));
        buffer.put((byte) type);
        buffer.put((byte) structSize);
        buffer.putShort((short) repeat);
        buffer.put(payload);

        return buffer.array();
    }

    /**
     * Wraps entries in a STRM container.
     */
    private static byte[] stream(byte[]... entries) {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        for (byte[] entry : entries) {
            write(body, entry);
        }
        return entry("STRM", (char) 0, 1, body.size(), body.toByteArray());
    }

    /**
     * Wraps a stream in a named DEVC container.
     */
    private static byte[] device(String name, byte[] stream) {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        write(body, entry("DVNM", 'c', 1, name.length(), name.getBytes(StandardCharsets.US_ASCII)));
        write(body, stream);
        return entry("DEVC", (char) 0, 1, body.size(), body.toByteArray());
    }

    private static void write(ByteArrayOutputStream out, byte[] bytes) {
        out.write(bytes, 0, bytes.length);
    }

    private static byte[] ascii(String text) {
        return text.getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] shorts(int... values) {
        ByteBuffer buffer = ByteBuffer.allocate(values.length * 2).order(ByteOrder.BIG_ENDIAN);
        for (int value : values) {
            buffer.putShort((short) value);
        }
        return buffer.array();
    }

    private static byte[] ints(int... values) {
        ByteBuffer buffer = ByteBuffer.allocate(values.length * 4).order(ByteOrder.BIG_ENDIAN);
        for (int value : values) {
            buffer.putInt(value);
        }
        return buffer.array();
    }

    private static byte[] longs(long... values) {
        ByteBuffer buffer = ByteBuffer.allocate(values.length * 8).order(ByteOrder.BIG_ENDIAN);
        for (long value : values) {
            buffer.putLong(value);
        }
        return buffer.array();
    }
}