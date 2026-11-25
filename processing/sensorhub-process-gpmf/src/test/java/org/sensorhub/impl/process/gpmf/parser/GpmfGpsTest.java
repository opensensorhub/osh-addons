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
import java.time.Instant;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for pulling GPS fixes out of a decoded GPMF payload, covering both the {@code GPS5} layout used
 * through the HERO10 and the self-contained {@code GPS9} layout used by the HERO11 and newer.
 */
public class GpmfGpsTest {
    @Test
    public void readsGps5WithSiblingFixQualityAndTime() {
        // A GPS container holds the coordinates alongside the fix quality, precision, and UTC time
        GpmfPayload payload = GpmfPayload.parse(stream(
                entry("STNM", 'c', 1, 3, ascii("GPS")),
                entry("GPSF", 'L', 4, 1, ints(3)),
                entry("GPSU", 'U', 16, 1, ascii("230607120000.000")),
                entry("GPSP", 'S', 2, 1, shorts(150)),
                entry("SCAL", 'l', 4, 5, ints(10000000, 10000000, 1000, 1000, 1000)),
                entry("GPS5", 'l', 20, 2, ints(
                        345678900, -864321000, 250500, 3400, 3600,
                        345679000, -864322000, 251000, 3500, 3700))));

        List<GpmfGpsFix> fixes = GpmfGps.extract(payload);

        assertEquals(2, fixes.size());

        GpmfGpsFix first = fixes.get(0);
        assertEquals(34.56789, first.latitude(), 1e-9);
        assertEquals(-86.4321, first.longitude(), 1e-9);
        assertEquals(250.5, first.altitude(), 1e-9);
        assertEquals(3.4, first.speed2D(), 1e-9);
        assertEquals(3.6, first.speed3D(), 1e-9);
        assertEquals(3, first.fixType());

        // GPSP declares no scale divisor, so its stored value is precision times 100
        assertEquals(1.5, first.dop(), 1e-9);

        assertEquals(Instant.parse("2023-06-07T12:00:00Z").getEpochSecond(), first.utcTime(), 1e-6);

        // The siblings describe the whole run, so every fix of the payload reports them
        GpmfGpsFix second = fixes.get(1);
        assertEquals(34.5679, second.latitude(), 1e-9);
        assertEquals(3, second.fixType());
        assertEquals(first.utcTime(), second.utcTime(), 0);
    }

    @Test
    public void readsGps5WithoutSiblingStreams() {
        GpmfPayload payload = GpmfPayload.parse(stream(
                entry("SCAL", 'l', 4, 5, ints(10000000, 10000000, 1000, 1000, 1000)),
                entry("GPS5", 'l', 20, 1, ints(345678900, -864321000, 250500, 3400, 3600))));

        GpmfGpsFix fix = GpmfGps.extract(payload).get(0);

        assertEquals(34.56789, fix.latitude(), 1e-9);
        assertEquals(GpmfGpsFix.FIX_UNKNOWN, fix.fixType());
        assertTrue(Double.isNaN(fix.dop()));
        assertTrue(Double.isNaN(fix.utcTime()));
    }

    @Test
    public void doesNotApplyPositionScaleDivisorsToSiblingStreams() {
        // The container's five divisors describe GPS5, not the single-value fix quality and precision
        // recorded beside it, so those must come through as stored.
        GpmfPayload payload = GpmfPayload.parse(stream(
                entry("GPSF", 'L', 4, 1, ints(3)),
                entry("GPSP", 'S', 2, 1, shorts(150)),
                entry("SCAL", 'l', 4, 5, ints(10000000, 10000000, 1000, 1000, 1000)),
                entry("GPS5", 'l', 20, 1, ints(345678900, -864321000, 250500, 3400, 3600))));

        GpmfStream fixQuality = payload.findStreamInContainer("GPSF", 0);
        assertNotNull(fixQuality);
        assertFalse(fixQuality.isScaled());
        assertEquals(3, fixQuality.getValues()[0], 0);

        // The position stream is still scaled, since its divisor count matches its sample width
        GpmfStream position = payload.findStreamInContainer("GPS5", 0);
        assertNotNull(position);
        assertTrue(position.isScaled());
        assertEquals(34.56789, position.getValues()[0], 1e-9);
    }

    @Test
    public void honoursScaleDivisorDeclaredForPrecisionAlone() {
        // A container whose single divisor fits every stream in it does scale them
        GpmfPayload payload = GpmfPayload.parse(stream(
                entry("GPSP", 'S', 2, 1, shorts(150)),
                entry("SCAL", 'S', 2, 1, shorts(100))));

        GpmfStream precision = payload.findStreamInContainer("GPSP", 0);
        assertNotNull(precision);
        assertTrue(precision.isScaled());
        assertEquals(1.5, precision.getValues()[0], 1e-9);
    }

    @Test
    public void readsGps9WithPerSampleTimeAndFixQuality() {
        // Fields are lat, lon, alt, 2D speed, 3D speed, days since 2000, seconds of day, DOP, fix
        ByteArrayOutputStream samples = new ByteArrayOutputStream();
        write(samples, ints(345678900, -864321000, 250500, 3400, 3600, 8558, 43200));
        write(samples, shorts(150, 3));
        write(samples, ints(345679000, -864322000, 251000, 3500, 3700, 8558, 43201));
        write(samples, shorts(120, 2));

        GpmfPayload payload = GpmfPayload.parse(stream(
                entry("TYPE", 'c', 1, 9, ascii("lllllllSS")),
                entry("SCAL", 'l', 4, 9, ints(10000000, 10000000, 1000, 1000, 1000, 1, 1, 100, 1)),
                entry("GPS9", '?', 32, 2, samples.toByteArray())));

        List<GpmfGpsFix> fixes = GpmfGps.extract(payload);

        assertEquals(2, fixes.size());

        GpmfGpsFix first = fixes.get(0);
        assertEquals(34.56789, first.latitude(), 1e-9);
        assertEquals(-86.4321, first.longitude(), 1e-9);
        assertEquals(250.5, first.altitude(), 1e-9);
        assertEquals(3, first.fixType());
        assertEquals(1.5, first.dop(), 1e-9);

        // 8558 days after 2000-01-01 is 2023-06-07; 43200 seconds into the day is noon
        assertEquals(Instant.parse("2023-06-07T12:00:00Z").getEpochSecond(), first.utcTime(), 1e-6);

        // Unlike GPS5, each GPS9 sample carries its own time and fix quality
        GpmfGpsFix second = fixes.get(1);
        assertEquals(2, second.fixType());
        assertEquals(first.utcTime() + 1, second.utcTime(), 1e-6);
    }

    @Test
    public void prefersGps9WhenBothArePresent() {
        ByteArrayOutputStream gps9Sample = new ByteArrayOutputStream();
        write(gps9Sample, ints(100000000, 200000000, 1000, 0, 0, 8558, 0));
        write(gps9Sample, shorts(100, 3));

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        write(body, stream(
                entry("SCAL", 'l', 4, 5, ints(10000000, 10000000, 1000, 1000, 1000)),
                entry("GPS5", 'l', 20, 1, ints(345678900, -864321000, 250500, 3400, 3600))));
        write(body, stream(
                entry("TYPE", 'c', 1, 9, ascii("lllllllSS")),
                entry("SCAL", 'l', 4, 9, ints(10000000, 10000000, 1000, 1000, 1000, 1, 1, 100, 1)),
                entry("GPS9", '?', 32, 1, gps9Sample.toByteArray())));

        byte[] devc = entry("DEVC", (char) 0, 1, body.size(), body.toByteArray());

        List<GpmfGpsFix> fixes = GpmfGps.extract(GpmfPayload.parse(devc));

        assertEquals(1, fixes.size());
        assertEquals(10.0, fixes.get(0).latitude(), 1e-9);
    }

    @Test
    public void correlatesSiblingsWithinTheirOwnContainer() {
        // Two GPS containers in one payload must not borrow each other's fix quality
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        write(body, stream(
                entry("GPSF", 'L', 4, 1, ints(3)),
                entry("SCAL", 'l', 4, 5, ints(10000000, 10000000, 1000, 1000, 1000)),
                entry("GPS5", 'l', 20, 1, ints(345678900, -864321000, 250500, 0, 0))));
        write(body, stream(
                entry("GPSF", 'L', 4, 1, ints(0)),
                entry("SCAL", 'l', 4, 5, ints(10000000, 10000000, 1000, 1000, 1000)),
                entry("GPS5", 'l', 20, 1, ints(100000000, 200000000, 1000, 0, 0))));

        byte[] devc = entry("DEVC", (char) 0, 1, body.size(), body.toByteArray());

        List<GpmfGpsFix> fixes = GpmfGps.extract(GpmfPayload.parse(devc));

        assertEquals(2, fixes.size());
        assertEquals(3, fixes.get(0).fixType());
        assertEquals(0, fixes.get(1).fixType());
    }

    @Test
    public void reportsWhetherTheReceiverHadALock() {
        // A GoPro keeps reporting coordinates with no lock, flagged by a fix type of zero
        GpmfPayload payload = GpmfPayload.parse(stream(
                entry("GPSF", 'L', 4, 1, ints(0)),
                entry("SCAL", 'l', 4, 5, ints(10000000, 10000000, 1000, 1000, 1000)),
                entry("GPS5", 'l', 20, 1, ints(193094173, -435768238, 2339570, 0, 0))));

        GpmfGpsFix unlocked = GpmfGps.extract(payload).get(0);
        assertEquals(GpmfGpsFix.FIX_NONE, unlocked.fixType());
        assertFalse(unlocked.hasLock());

        // A fix quality the camera did not report is taken at face value
        GpmfPayload unreported = GpmfPayload.parse(stream(
                entry("SCAL", 'l', 4, 5, ints(10000000, 10000000, 1000, 1000, 1000)),
                entry("GPS5", 'l', 20, 1, ints(371305659, -764645106, 1588, 1495, 1430))));

        GpmfGpsFix unknown = GpmfGps.extract(unreported).get(0);
        assertEquals(GpmfGpsFix.FIX_UNKNOWN, unknown.fixType());
        assertTrue(unknown.hasLock());
    }

    @Test
    public void readsDeclaredAltitudeSystem() {
        // The MAX 2 declares its altitude reference in the GPS container
        ByteArrayOutputStream sample = new ByteArrayOutputStream();
        write(sample, ints(371305659, -764645106, 1588, 1495, 143, 9656, 43200));
        write(sample, shorts(422, 2));

        GpmfPayload payload = GpmfPayload.parse(stream(
                entry("TYPE", 'c', 1, 9, ascii("lllllllSS")),
                entry("SCAL", 'l', 4, 9, ints(10000000, 10000000, 1000, 1000, 100, 1, 1000, 100, 1)),
                entry("GPSA", 'F', 4, 1, ascii("MSLV")),
                entry("GPS9", '?', 32, 1, sample.toByteArray())));

        GpmfGpsFix fix = GpmfGps.extract(payload).get(0);

        assertEquals("MSLV", fix.altitudeSystem());
        assertEquals(37.1305659, fix.latitude(), 1e-9);
        assertEquals(-76.4645106, fix.longitude(), 1e-9);
        assertEquals(1.588, fix.altitude(), 1e-9);
        assertEquals(1.495, fix.speed2D(), 1e-9);
        assertEquals(4.22, fix.dop(), 1e-9);
        assertEquals(2, fix.fixType());
        assertTrue(fix.hasLock());
    }

    @Test
    public void reportsNoAltitudeSystemWhenCameraDeclaresNone() {
        GpmfPayload payload = GpmfPayload.parse(stream(
                entry("SCAL", 'l', 4, 5, ints(10000000, 10000000, 1000, 1000, 1000)),
                entry("GPS5", 'l', 20, 1, ints(371305659, -764645106, 1588, 0, 0))));

        assertNull(GpmfGps.extract(payload).get(0).altitudeSystem());
    }

    @Test
    public void returnsNoFixesWhenPayloadHasNoPositionStreams() {
        GpmfPayload payload = GpmfPayload.parse(stream(
                entry("SCAL", 's', 2, 1, shorts(418)),
                entry("ACCL", 's', 6, 1, shorts(418, 836, 209))));

        assertTrue(GpmfGps.extract(payload).isEmpty());
    }

    @Test
    public void ignoresPositionStreamWithUnexpectedElementCount() {
        GpmfPayload payload = GpmfPayload.parse(stream(
                entry("GPS5", 'l', 12, 1, ints(1, 2, 3))));

        assertTrue(GpmfGps.extract(payload).isEmpty());
    }

    @Test
    public void readsGpsUtcTimestamps() {
        assertEquals(Instant.parse("2023-06-07T12:00:00Z").getEpochSecond(),
                GpmfGps.parseGpsUtc("230607120000.000"), 1e-6);

        assertEquals(Instant.parse("2023-06-07T12:00:00Z").getEpochSecond() + 0.25,
                GpmfGps.parseGpsUtc("230607120000.250"), 1e-6);

        // The fractional part is optional
        assertEquals(Instant.parse("2024-12-31T23:59:59Z").getEpochSecond(),
                GpmfGps.parseGpsUtc("241231235959"), 1e-6);

        assertTrue(Double.isNaN(GpmfGps.parseGpsUtc("")));
        assertTrue(Double.isNaN(GpmfGps.parseGpsUtc("2306071200")));
        assertTrue(Double.isNaN(GpmfGps.parseGpsUtc("not a time!!")));
        assertTrue(Double.isNaN(GpmfGps.parseGpsUtc("239907120000.000")));
    }

    // ---------------------------------------------------------------------
    // Helpers for assembling GPMF payloads
    // ---------------------------------------------------------------------

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

    private static byte[] stream(byte[]... entries) {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        for (byte[] entry : entries) {
            write(body, entry);
        }
        return entry("STRM", (char) 0, 1, body.size(), body.toByteArray());
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
}