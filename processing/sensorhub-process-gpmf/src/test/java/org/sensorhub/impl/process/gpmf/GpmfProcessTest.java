/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research, LLC. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.process.gpmf;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.junit.Test;
import org.vast.data.DataBlockByte;
import org.vast.process.DataQueue;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests that the process turns a GPMF packet into the telemetry and position records the outputs declare.
 */
public class GpmfProcessTest {
    /** Field indices within a telemetry record, which the values array follows. */
    private static final int STREAM_KEY = 3;
    private static final int VALUE_COUNT = 9;
    private static final int FIRST_VALUE = 10;

    /** Field indices within a position record. */
    private static final int LATITUDE = 2;
    private static final int LONGITUDE = 3;
    private static final int ALTITUDE = 4;
    private static final int FIX_TYPE = 7;

    @Test
    public void publishesTelemetryAndPositionsFromOnePacket() throws Exception {
        var harness = new Harness();
        harness.run(gpsPacket(3));

        // One telemetry record per sensor stream in the packet, the coordinates among them
        DataBlock coordinates = harness.telemetryFor("GPS5");
        assertNotNull("no telemetry record published for the GPS5 stream", coordinates);
        assertEquals(10, coordinates.getIntValue(VALUE_COUNT));
        assertEquals(34.56789, coordinates.getDoubleValue(FIRST_VALUE), 1e-9);
        assertEquals(-86.4321, coordinates.getDoubleValue(FIRST_VALUE + 1), 1e-9);

        // One position record per fix, with the camera's readings published unchanged
        assertEquals(2, harness.positions.size());

        DataBlock first = harness.positions.get(0);
        assertEquals(34.56789, first.getDoubleValue(LATITUDE), 1e-9);
        assertEquals(-86.4321, first.getDoubleValue(LONGITUDE), 1e-9);
        assertEquals(250.5, first.getDoubleValue(ALTITUDE), 1e-9);
        assertEquals(3, first.getIntValue(FIX_TYPE));

        assertEquals(34.5679, harness.positions.get(1).getDoubleValue(LATITUDE), 1e-9);
    }

    @Test
    public void dropsPositionsWithNoLockButKeepsTelemetry() throws Exception {
        var harness = new Harness();
        harness.run(gpsPacket(0));

        assertTrue("fixes with no lock should not be published by default", harness.positions.isEmpty());
        assertNotNull("telemetry should report the GPS streams as recorded", harness.telemetryFor("GPS5"));
    }

    @Test
    public void publishesPositionsWithNoLockWhenNotRequiringAFix() throws Exception {
        var harness = new Harness();
        harness.process.getParameterList().getComponent("requireGpsFix").getData().setBooleanValue(false);
        harness.run(gpsPacket(0));

        assertEquals(2, harness.positions.size());
        assertEquals(34.56789, harness.positions.get(0).getDoubleValue(LATITUDE), 1e-9);
    }

    @Test
    public void publishesNothingForAMalformedPacket() throws Exception {
        var harness = new Harness();
        harness.run(new byte[]{0x00, 0x01, 0x02, 0x03});

        assertTrue(harness.telemetry.isEmpty());
        assertTrue(harness.positions.isEmpty());
    }

    @Test
    public void publishesNothingForAnEmptyPacket() throws Exception {
        var harness = new Harness();
        harness.run(new byte[0]);

        assertTrue(harness.telemetry.isEmpty());
        assertTrue(harness.positions.isEmpty());
    }

    /**
     * An initialized process with a capture queue on each output, standing in for the connections a
     * process chain would make.
     */
    private static class Harness {
        final GpmfProcess process = new GpmfProcess();
        final List<DataBlock> telemetry = new ArrayList<>();
        final List<DataBlock> positions = new ArrayList<>();

        Harness() throws Exception {
            process.init();
            capture(process.getOutputList().getComponent(GpmfProcess.TELEMETRY_OUTPUT_NAME), telemetry);
            capture(process.getOutputList().getComponent(GpmfProcess.GPS_OUTPUT_NAME), positions);
        }

        void run(byte[] packet) throws Exception {
            DataBlockByte block = new DataBlockByte();
            block.setUnderlyingObject(packet);
            process.getPacketInput().setData(block);

            process.execute();
        }

        /**
         * @return The first telemetry record published for the given GPMF key, or null if there is none.
         */
        DataBlock telemetryFor(String streamKey) {
            for (DataBlock record : telemetry) {
                if (streamKey.equals(record.getStringValue(STREAM_KEY))) {
                    return record;
                }
            }
            return null;
        }

        private void capture(DataComponent output, List<DataBlock> records) throws Exception {
            process.connect(output, new DataQueue() {
                @Override
                public synchronized void publishData() {
                    records.add(sourceComponent.getData());
                }
            });
        }
    }

    /**
     * Builds a GPS stream carrying two fixes, as a HERO10 and earlier record them.
     *
     * @param fixQuality The {@code GPSF} value: 0 for no lock, 3 for a 3D fix.
     */
    private static byte[] gpsPacket(int fixQuality) {
        return stream(
                entry("STNM", 'c', 1, 3, ascii("GPS")),
                entry("GPSF", 'L', 4, 1, ints(fixQuality)),
                entry("SCAL", 'l', 4, 5, ints(10000000, 10000000, 1000, 1000, 1000)),
                entry("GPS5", 'l', 20, 2, ints(
                        345678900, -864321000, 250500, 3400, 3600,
                        345679000, -864322000, 251000, 3500, 3700)));
    }

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
            body.write(entry, 0, entry.length);
        }
        return entry("STRM", (char) 0, 1, body.size(), body.toByteArray());
    }

    private static byte[] ascii(String text) {
        return text.getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] ints(int... values) {
        ByteBuffer buffer = ByteBuffer.allocate(values.length * 4).order(ByteOrder.BIG_ENDIAN);
        for (int value : values) {
            buffer.putInt(value);
        }
        return buffer.array();
    }
}