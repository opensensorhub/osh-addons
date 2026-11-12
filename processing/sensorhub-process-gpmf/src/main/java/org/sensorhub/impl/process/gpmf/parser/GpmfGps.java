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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Pulls GPS fixes out of a decoded GPMF payload.
 * <p>
 * GoPro cameras record their position in one of two forms, and this reads both:
 * <ul>
 *   <li>{@code GPS9}, on the HERO11 and newer, packs coordinates, speeds, the fix time, precision, and
 *       fix quality into every sample, so each sample stands on its own.</li>
 *   <li>{@code GPS5}, on earlier models, carries only coordinates and speeds. Fix quality
 *       ({@code GPSF}), precision ({@code GPSP}), and the fix time ({@code GPSU}) come from sibling
 *       streams of the same container, and describe the run of samples as a whole.</li>
 * </ul>
 * The field order within each is fixed by the GPMF specification.
 *
 * @see <a href="https://github.com/gopro/gpmf-parser/blob/main/docs/README.md">GPMF specification</a>
 */
public final class GpmfGps {
    private static final Logger logger = LoggerFactory.getLogger(GpmfGps.class);

    /** Coordinates, speeds, fix time, precision, and fix quality per sample. HERO11 and newer. */
    public static final String KEY_GPS9 = "GPS9";
    /** Coordinates and speeds per sample. HERO5 through HERO10. */
    public static final String KEY_GPS5 = "GPS5";
    /** Fix quality accompanying a {@code GPS5} stream. */
    private static final String KEY_FIX = "GPSF";
    /** Dilution of precision accompanying a {@code GPS5} stream. */
    private static final String KEY_PRECISION = "GPSP";
    /** UTC time accompanying a {@code GPS5} stream, as a {@code yymmddhhmmss.sss} string. */
    private static final String KEY_UTC = "GPSU";
    /** Altitude system the camera measured from, {@code MSLV} or {@code GEOD}. */
    private static final String KEY_ALTITUDE_SYSTEM = "GPSA";

    private static final int GPS5_ELEMENTS = 5;
    private static final int GPS9_ELEMENTS = 9;

    /**
     * {@code GPS9} counts days from the start of 2000 rather than the epoch.
     */
    private static final long GPS9_EPOCH_SECONDS =
            LocalDateTime.of(2000, 1, 1, 0, 0).toEpochSecond(ZoneOffset.UTC);

    /**
     * A {@code GPSP} stream that declares no scale divisor stores precision multiplied by this.
     */
    private static final double GPSP_IMPLICIT_SCALE = 100d;

    private GpmfGps() {
    }

    /**
     * Reads every GPS fix a payload carries.
     *
     * @param payload The decoded payload.
     * @return The fixes in payload order, empty if the payload carried no position streams.
     */
    public static List<GpmfGpsFix> extract(GpmfPayload payload) {
        List<GpmfGpsFix> fixes = new ArrayList<>();

        // Prefer GPS9, whose samples are self-contained, when a camera records both
        for (GpmfStream stream : payload.getStreams(KEY_GPS9)) {
            extractGps9(stream, payload, fixes);
        }

        if (fixes.isEmpty()) {
            for (GpmfStream stream : payload.getStreams(KEY_GPS5)) {
                extractGps5(stream, payload, fixes);
            }
        }

        return fixes;
    }

    /**
     * Reads the samples of a {@code GPS9} stream, whose fields are latitude, longitude, altitude, 2D
     * speed, 3D speed, days since 2000, seconds of the day, precision, and fix quality.
     */
    private static void extractGps9(GpmfStream stream, GpmfPayload payload, List<GpmfGpsFix> fixes) {
        if (stream.getElementCount() != GPS9_ELEMENTS) {
            logger.warn("Ignoring GPS9 stream with {} elements per sample, expected {}",
                    stream.getElementCount(), GPS9_ELEMENTS);
            return;
        }

        String altitudeSystem = altitudeSystem(payload, stream.getContainerIndex());
        double[] values = stream.getValues();

        for (int sample = 0; sample < stream.getSampleCount(); sample++) {
            int offset = sample * GPS9_ELEMENTS;

            double days = values[offset + 5];
            double secondsOfDay = values[offset + 6];

            fixes.add(new GpmfGpsFix(
                    values[offset],
                    values[offset + 1],
                    values[offset + 2],
                    values[offset + 3],
                    values[offset + 4],
                    (int) values[offset + 8],
                    values[offset + 7],
                    GPS9_EPOCH_SECONDS + days * 86400d + secondsOfDay,
                    altitudeSystem));
        }
    }

    /**
     * Reads the altitude system a GPS container declared, which is how newer cameras record whether their
     * altitude is measured from mean sea level or from the ellipsoid.
     *
     * @return The four-character system name, or null if the container declared none.
     */
    private static String altitudeSystem(GpmfPayload payload, int containerIndex) {
        GpmfStream stream = payload.findStreamInContainer(KEY_ALTITUDE_SYSTEM, containerIndex);
        return stream != null ? stream.getText() : null;
    }

    /**
     * Reads the samples of a {@code GPS5} stream, whose fields are latitude, longitude, altitude, 2D
     * speed, and 3D speed, and applies the fix quality, precision, and time from its sibling streams.
     * <p>
     * Those siblings describe the whole run of samples rather than individual ones, so every fix from one
     * payload reports the same fix quality, precision, and time.
     */
    private static void extractGps5(GpmfStream stream, GpmfPayload payload, List<GpmfGpsFix> fixes) {
        if (stream.getElementCount() != GPS5_ELEMENTS) {
            logger.warn("Ignoring GPS5 stream with {} elements per sample, expected {}",
                    stream.getElementCount(), GPS5_ELEMENTS);
            return;
        }

        int container = stream.getContainerIndex();

        int fixType = GpmfGpsFix.FIX_UNKNOWN;
        GpmfStream fixStream = payload.findStreamInContainer(KEY_FIX, container);
        if (fixStream != null && fixStream.getValues().length > 0) {
            fixType = (int) fixStream.getValues()[0];
        }

        double dop = Double.NaN;
        GpmfStream precisionStream = payload.findStreamInContainer(KEY_PRECISION, container);
        if (precisionStream != null && precisionStream.getValues().length > 0) {
            dop = precisionStream.getValues()[0];
            // Cameras that declare no divisor for GPSP store precision multiplied by 100
            if (!precisionStream.isScaled()) {
                dop /= GPSP_IMPLICIT_SCALE;
            }
        }

        double utcTime = Double.NaN;
        GpmfStream utcStream = payload.findStreamInContainer(KEY_UTC, container);
        if (utcStream != null && utcStream.getText() != null) {
            utcTime = parseGpsUtc(utcStream.getText());
        }

        String altitudeSystem = altitudeSystem(payload, container);
        double[] values = stream.getValues();

        for (int sample = 0; sample < stream.getSampleCount(); sample++) {
            int offset = sample * GPS5_ELEMENTS;

            fixes.add(new GpmfGpsFix(
                    values[offset],
                    values[offset + 1],
                    values[offset + 2],
                    values[offset + 3],
                    values[offset + 4],
                    fixType,
                    dop,
                    utcTime,
                    altitudeSystem));
        }
    }

    /**
     * Reads a {@code GPSU} timestamp, which is a {@code yymmddhhmmss.sss} string in UTC.
     *
     * @param text The timestamp as the camera wrote it.
     * @return Seconds since the epoch, or NaN if the text is not a timestamp.
     */
    static double parseGpsUtc(String text) {
        String trimmed = text.trim();

        // yymmddhhmmss is the shortest form that carries a whole time; the fractional part is optional
        if (trimmed.length() < 12) {
            logger.debug("Ignoring GPSU timestamp '{}': too short", text);
            return Double.NaN;
        }

        try {
            int year = 2000 + Integer.parseInt(trimmed.substring(0, 2));
            int month = Integer.parseInt(trimmed.substring(2, 4));
            int day = Integer.parseInt(trimmed.substring(4, 6));
            int hour = Integer.parseInt(trimmed.substring(6, 8));
            int minute = Integer.parseInt(trimmed.substring(8, 10));
            int second = Integer.parseInt(trimmed.substring(10, 12));

            double fraction = 0;
            if (trimmed.length() > 13 && trimmed.charAt(12) == '.') {
                fraction = Double.parseDouble("0" + trimmed.substring(12));
            }

            return LocalDateTime.of(year, month, day, hour, minute, second).toEpochSecond(ZoneOffset.UTC) + fraction;
        } catch (NumberFormatException | DateTimeException e) {
            logger.debug("Ignoring unreadable GPSU timestamp '{}'", text);
            return Double.NaN;
        }
    }
}