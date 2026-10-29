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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The sensor streams carried by one GPMF payload.
 * <p>
 * A payload describes one or more devices ({@code DEVC}), each holding a set of sensor streams
 * ({@code STRM}). Every stream pairs its samples with the metadata needed to interpret them -- a
 * human-readable name, units, and the scale divisors that turn the stored integers back into real
 * values -- and this class resolves all of that into a flat list of {@link GpmfStream}.
 * <p>
 * Which streams a payload carries depends on the camera model, its firmware, and the capture settings,
 * so nothing here is keyed off a fixed table of stream names: every stream the payload declares is
 * reported, described by its own metadata.
 *
 * @see <a href="https://github.com/gopro/gpmf-parser/blob/main/docs/README.md">GPMF specification</a>
 */
public class GpmfPayload {
    private static final Logger logger = LoggerFactory.getLogger(GpmfPayload.class);

    private static final String KEY_DEVICE = "DEVC";
    private static final String KEY_STREAM = "STRM";
    private static final String KEY_DEVICE_NAME = "DVNM";
    private static final String KEY_STREAM_NAME = "STNM";
    private static final String KEY_SI_UNITS = "SIUN";
    private static final String KEY_DISPLAY_UNITS = "UNIT";
    private static final String KEY_SCALE = "SCAL";
    private static final String KEY_TYPE_DEF = "TYPE";
    private static final String KEY_TIMESTAMP = "STMP";

    /**
     * Keys that describe a stream rather than carry its samples. Everything else inside a {@code STRM}
     * is treated as sample data.
     */
    private static final Set<String> MODIFIER_KEYS = Set.of(
            KEY_STREAM_NAME, KEY_SI_UNITS, KEY_DISPLAY_UNITS, KEY_SCALE, KEY_TYPE_DEF, KEY_TIMESTAMP,
            "TSMP", "TIMO", "ORIN", "ORIO", "MTRX", "EMPT", "RMRK", "VERS", "STPS",
            KEY_DEVICE_NAME, "DVID", "TICK", "TOCK");

    /**
     * Microseconds per second, for converting a {@code STMP} timestamp.
     */
    private static final double MICROS_PER_SECOND = 1_000_000d;

    /**
     * Largest sample size a GPMF entry can declare, since the header stores it in one byte. A {@code TYPE}
     * definition describing more fields than this cannot match any entry.
     */
    private static final int MAX_STRUCT_SIZE = 255;

    /**
     * Streams already reported as undecodable, so that a layout repeated in every payload is only
     * logged the first time it is seen.
     */
    private static final Set<String> reportedStreams = ConcurrentHashMap.newKeySet();

    private final List<GpmfStream> streams;

    private GpmfPayload(List<GpmfStream> streams) {
        this.streams = streams;
    }

    /**
     * Decodes the sensor streams of a GPMF payload.
     *
     * @param data The payload bytes, as carried by one packet of a GPMF stream.
     * @return The streams the payload carries, which may be empty.
     * @throws GpmfParseException If the payload is malformed or truncated.
     */
    public static GpmfPayload parse(byte[] data) {
        return of(GpmfParser.parse(data));
    }

    /**
     * Decodes the sensor streams from an already-parsed GPMF tree.
     *
     * @param elements The top-level entries of a payload.
     * @return The streams the entries describe, which may be empty.
     */
    public static GpmfPayload of(List<GpmfElement> elements) {
        List<GpmfStream> streams = new ArrayList<>();
        int[] containerIndex = {0};

        for (GpmfElement element : elements) {
            collectStreams(element, null, streams, containerIndex);
        }

        return new GpmfPayload(streams);
    }

    /**
     * Finds the streams carrying the given key.
     *
     * @param key The four-character key to look for, for example {@code GPS5}.
     * @return The matching streams, in payload order.
     */
    public List<GpmfStream> getStreams(String key) {
        List<GpmfStream> matches = new ArrayList<>();

        for (GpmfStream stream : streams) {
            if (stream.getKey().equals(key)) {
                matches.add(stream);
            }
        }

        return matches;
    }

    /**
     * Finds the first stream carrying the given key within one {@code STRM} container, so that streams
     * recorded together can be correlated.
     *
     * @param key            The four-character key to look for, for example {@code GPSF}.
     * @param containerIndex The container to search, as reported by {@link GpmfStream#getContainerIndex()}.
     * @return The matching stream, or null if the container has none.
     */
    public GpmfStream findStreamInContainer(String key, int containerIndex) {
        for (GpmfStream stream : streams) {
            if (stream.getContainerIndex() == containerIndex && stream.getKey().equals(key)) {
                return stream;
            }
        }

        return null;
    }

    /**
     * @return The sensor streams of this payload, in payload order.
     */
    public List<GpmfStream> getStreams() {
        return Collections.unmodifiableList(streams);
    }

    /**
     * @return True if the payload carried no sensor streams.
     */
    public boolean isEmpty() {
        return streams.isEmpty();
    }

    /**
     * Walks a payload entry looking for sensor streams, descending through device and other containers.
     *
     * @param element    The entry to walk.
     * @param deviceName Name of the enclosing device, or null if none has been seen yet.
     * @param streams    Collects the streams found.
     */
    private static void collectStreams(GpmfElement element, String deviceName, List<GpmfStream> streams, int[] containerIndex) {
        if (KEY_STREAM.equals(element.getKey())) {
            collectStreamSamples(element, deviceName, streams, containerIndex[0]++);
            return;
        }

        if (!element.isNested()) {
            return;
        }

        // A device container names the device its streams belong to
        if (KEY_DEVICE.equals(element.getKey())) {
            GpmfElement name = element.findChild(KEY_DEVICE_NAME);
            if (name != null) {
                deviceName = name.asString();
            }
        }

        for (GpmfElement child : element.getChildren()) {
            collectStreams(child, deviceName, streams, containerIndex);
        }
    }

    /**
     * Builds a {@link GpmfStream} for each sample-carrying entry of a {@code STRM} container, applying
     * the container's shared name, units, and scale divisors.
     * <p>
     * A stream that cannot be decoded is logged and skipped rather than failing the whole payload, so one
     * unfamiliar stream does not cost the caller the rest of the telemetry.
     */
    private static void collectStreamSamples(GpmfElement stream, String deviceName, List<GpmfStream> streams, int containerIndex) {
        String name = childText(stream, KEY_STREAM_NAME);
        String units = childUnits(stream, KEY_SI_UNITS);
        if (units == null) {
            units = childUnits(stream, KEY_DISPLAY_UNITS);
        }

        GpmfElement scaleElement = stream.findChild(KEY_SCALE);
        double[] scale = scaleElement != null ? scaleElement.asDoubles() : new double[0];

        String typeDefinition = childText(stream, KEY_TYPE_DEF);

        GpmfElement timestampElement = stream.findChild(KEY_TIMESTAMP);
        double timestamp = Double.NaN;
        if (timestampElement != null) {
            double[] micros = timestampElement.asDoubles();
            if (micros.length > 0) {
                timestamp = micros[0] / MICROS_PER_SECOND;
            }
        }

        for (GpmfElement element : stream.getChildren()) {
            if (MODIFIER_KEYS.contains(element.getKey()) || element.isNested()) {
                continue;
            }

            try {
                GpmfStream decoded = decodeStream(element, deviceName, name, units, scale, typeDefinition,
                        timestamp, containerIndex);
                if (decoded != null) {
                    streams.add(decoded);
                }
            } catch (RuntimeException e) {
                logger.warn("Skipping GPMF stream {}: {}", element.getKey(), e.getMessage());
            }
        }
    }

    /**
     * Decodes one sample-carrying entry into a stream.
     *
     * @return The decoded stream, or null if its type is one this implementation cannot decode.
     */
    private static GpmfStream decodeStream(GpmfElement element, String deviceName, String name, String units,
                                           double[] scale, String typeDefinition, double timestamp, int containerIndex) {
        GpmfType type = element.getType();

        if (type == null) {
            // Cameras repeat their layout every payload, so report an undecodable stream only once
            if (firstReport(element.getKey() + ":" + element.getTypeCode())) {
                logger.info("Ignoring GPMF stream {}: type code '{}' is not one this driver decodes. " +
                                "Further payloads of this stream will not be reported.",
                        element.getKey(), element.getTypeCode());
            }
            return null;
        }

        // Text-valued streams, such as the GPS UTC timestamp, carry no numbers to scale
        if (type == GpmfType.CHAR || type == GpmfType.UTC_DATE || type == GpmfType.FOURCC || type == GpmfType.UUID) {
            return new GpmfStream(deviceName, element.getKey(), name, units,
                    element.getStructSize(), element.getRepeat(), new double[0], element.asString(), timestamp,
                    containerIndex, false);
        }

        if (type == GpmfType.COMPLEX) {
            return decodeComplexStream(element, deviceName, name, units, scale, typeDefinition, timestamp, containerIndex);
        }

        if (!type.isNumeric()) {
            if (firstReport(element.getKey() + ":nonnumeric")) {
                logger.debug("Ignoring GPMF stream {} of non-numeric type '{}'", element.getKey(), type.getCode());
            }
            return null;
        }

        int elementCount = element.getElementCount();
        if (elementCount == 0) {
            if (firstReport(element.getKey() + ":width")) {
                logger.debug("Ignoring GPMF stream {} with a {} byte sample of {} byte values",
                        element.getKey(), element.getStructSize(), type.getSize());
            }
            return null;
        }

        double[] applicableScale = scaleFor(scale, elementCount, element.getKey());

        double[] values = element.asDoubles();
        applyScale(values, elementCount, applicableScale);

        return new GpmfStream(deviceName, element.getKey(), name, units, elementCount,
                values.length / elementCount, values, null, timestamp, containerIndex,
                applicableScale.length > 0);
    }

    /**
     * Decides whether a container's scale divisors describe the given stream.
     * <p>
     * A container declares one {@code SCAL} for the stream it is named after, but it can hold other
     * streams too: a GPS container reports its fix quality and precision alongside its coordinates, and
     * those are stored as they are. A {@code SCAL} only describes a sample of as many values as it has
     * divisors, or of any width when it carries a single divisor for all of them, so a divisor count that
     * does not fit the stream belongs to one of its siblings.
     *
     * @param scale        The divisors the container declared.
     * @param elementCount Number of values in one sample of this stream.
     * @param key          Four-character key of the stream, for logging.
     * @return The divisors to apply, empty if the container's do not describe this stream.
     */
    private static double[] scaleFor(double[] scale, int elementCount, String key) {
        if (scale.length == 0 || scale.length == 1 || scale.length == elementCount) {
            return scale;
        }

        logger.debug("Not scaling GPMF stream {}: its container declares {} divisors for a {} value sample",
                key, scale.length, elementCount);

        return new double[0];
    }

    /**
     * Decodes a stream whose samples are heterogeneous structures, using the field layout from the
     * {@code TYPE} entry of its container. This is how newer cameras store combined streams such as
     * {@code GPS9}.
     *
     * @return The decoded stream, or null if the layout is missing or holds fields this cannot read.
     */
    private static GpmfStream decodeComplexStream(GpmfElement element, String deviceName, String name, String units,
                                                  double[] scale, String typeDefinition, double timestamp,
                                                  int containerIndex) {
        if (typeDefinition == null) {
            if (firstReport(element.getKey() + ":notype")) {
                logger.debug("Ignoring complex GPMF stream {}: its container declares no TYPE", element.getKey());
            }
            return null;
        }

        List<GpmfType> fieldTypes = expandTypeDefinition(typeDefinition);

        int structSize = 0;
        for (GpmfType fieldType : fieldTypes) {
            if (!fieldType.isNumeric()) {
                if (firstReport(element.getKey() + ":" + typeDefinition)) {
                    logger.debug("Ignoring complex GPMF stream {}: TYPE '{}' holds non-numeric field '{}'",
                            element.getKey(), typeDefinition, fieldType.getCode());
                }
                return null;
            }
            structSize += fieldType.getSize();
        }

        if (fieldTypes.isEmpty() || structSize != element.getStructSize()) {
            if (firstReport(element.getKey() + ":" + typeDefinition + ":" + element.getStructSize())) {
                logger.debug("Ignoring complex GPMF stream {}: TYPE '{}' describes a {} byte sample but the entry declares {}",
                        element.getKey(), typeDefinition, structSize, element.getStructSize());
            }
            return null;
        }

        int elementCount = fieldTypes.size();
        double[] applicableScale = scaleFor(scale, elementCount, element.getKey());
        int sampleCount = Math.min(element.getRepeat(), element.getPayload().length / structSize);
        double[] values = new double[sampleCount * elementCount];

        ByteBuffer buffer = ByteBuffer.wrap(element.getPayload()).order(ByteOrder.BIG_ENDIAN);
        for (int sample = 0; sample < sampleCount; sample++) {
            for (int field = 0; field < elementCount; field++) {
                values[sample * elementCount + field] = fieldTypes.get(field).read(buffer);
            }
        }

        applyScale(values, elementCount, applicableScale);

        return new GpmfStream(deviceName, element.getKey(), name, units, elementCount, sampleCount,
                values, null, timestamp, containerIndex, applicableScale.length > 0);
    }

    /**
     * Reads a {@code TYPE} definition into the sequence of field types it describes.
     * <p>
     * A definition is a run of type codes, optionally with a bracketed repeat count, so {@code "lll"} and
     * {@code "l[3]"} both describe three 32-bit integers.
     *
     * @param typeDefinition The definition string.
     * @return The field types in order, or an empty list if the definition cannot be read.
     */
    static List<GpmfType> expandTypeDefinition(String typeDefinition) {
        List<GpmfType> fieldTypes = new ArrayList<>();

        for (int i = 0; i < typeDefinition.length(); i++) {
            GpmfType type = GpmfType.fromCode(typeDefinition.charAt(i));

            if (type == null) {
                logger.debug("Unknown field type '{}' in TYPE definition '{}'", typeDefinition.charAt(i), typeDefinition);
                return Collections.emptyList();
            }

            int repeat = 1;

            // An optional bracketed count repeats the field it follows
            if (i + 1 < typeDefinition.length() && typeDefinition.charAt(i + 1) == '[') {
                int close = typeDefinition.indexOf(']', i + 2);
                if (close < 0) {
                    logger.debug("Unterminated repeat count in TYPE definition '{}'", typeDefinition);
                    return Collections.emptyList();
                }

                try {
                    repeat = Integer.parseInt(typeDefinition.substring(i + 2, close).trim());
                } catch (NumberFormatException e) {
                    logger.debug("Unreadable repeat count in TYPE definition '{}'", typeDefinition);
                    return Collections.emptyList();
                }

                // A sample is at most MAX_STRUCT_SIZE bytes, so a definition claiming more fields than
                // that cannot describe one. Rejecting it here rather than at the sample size check below
                // keeps a corrupt count from being expanded into a list first.
                if (repeat < 0 || repeat > MAX_STRUCT_SIZE) {
                    logger.debug("Ignoring repeat count of {} in TYPE definition '{}'", repeat, typeDefinition);
                    return Collections.emptyList();
                }

                i = close;
            }

            for (int r = 0; r < repeat; r++) {
                fieldTypes.add(type);
            }
        }

        return fieldTypes;
    }

    /**
     * Divides sample values by the stream's scale divisors, in place.
     * <p>
     * A stream declares either one divisor for every value or one per element of a sample. A divisor of
     * zero is skipped, since it carries no usable scale.
     *
     * @param values       The values to scale, sample-major.
     * @param elementCount Number of values in one sample.
     * @param scale        The divisors that describe this stream, as resolved by
     *                     {@link GpmfPayload#scaleFor(double[], int, String)}, so either one divisor for
     *                     every value or one per element. Empty if the stream is unscaled.
     */
    private static void applyScale(double[] values, int elementCount, double[] scale) {
        if (scale.length == 0) {
            return;
        }

        for (int i = 0; i < values.length; i++) {
            double divisor = scale.length == 1 ? scale[0] : scale[i % elementCount];

            if (divisor != 0) {
                values[i] /= divisor;
            }
        }
    }

    /**
     * Reads a child entry of a container as text.
     *
     * @return The text, or null if the container has no such child.
     */
    private static String childText(GpmfElement container, String key) {
        GpmfElement child = container.findChild(key);
        if (child == null) {
            return null;
        }

        String text = child.asString();
        return text.isEmpty() ? null : text;
    }

    /**
     * Reads a units entry, which stores one field per element of a sample rather than a single string.
     * A GPS stream reports {@code deg}, {@code deg}, {@code m}, {@code m/s} and so on, which is returned
     * here as a comma-separated list in element order.
     *
     * @return The units, or null if the container has no such child.
     */
    private static String childUnits(GpmfElement container, String key) {
        GpmfElement child = container.findChild(key);
        if (child == null) {
            return null;
        }

        String units = String.join(",", child.asStrings());
        return units.isBlank() ? null : units;
    }

    /**
     * Reports whether a diagnostic about the given stream has already been logged.
     * <p>
     * Cameras repeat their layout in every payload, so a stream this implementation cannot decode would
     * otherwise log once a second for as long as the driver runs. The set is bounded by the number of
     * distinct undecodable streams a camera produces, which is a handful.
     *
     * @param dedupeKey Identifies the diagnostic, normally the stream key and its type.
     * @return True the first time a given key is seen, false afterwards.
     */
    private static boolean firstReport(String dedupeKey) {
        return reportedStreams.add(dedupeKey);
    }
}