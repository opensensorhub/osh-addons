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

/**
 * One sensor stream decoded out of a GPMF payload, with its scale divisors already applied.
 * <p>
 * A stream is a run of samples of one quantity, for example the accelerometer readings recorded over the
 * payload's time interval. Samples carry {@link GpmfStream#getElementCount()} values each, so a
 * three-axis accelerometer with 200 samples reports an element count of 3 and 600 values.
 * <p>
 * Streams whose samples are text rather than numbers, such as the GPS UTC timestamp, report zero values
 * and carry their content in {@link GpmfStream#getText()} instead.
 */
public class GpmfStream {
    private final String deviceName;
    private final String key;
    private final String name;
    private final String units;
    private final int elementCount;
    private final int sampleCount;
    private final double[] values;
    private final String text;
    private final double timestamp;
    private final int containerIndex;
    private final boolean scaled;

    /**
     * @param deviceName     Name of the device that recorded this stream, from the payload's {@code DVNM}.
     * @param key            Four-character key of the stream, for example {@code ACCL}.
     * @param name           Human-readable stream name, from {@code STNM}, or null if absent.
     * @param units          Units of the values, from {@code SIUN} or {@code UNIT}, or null if absent.
     * @param elementCount   Number of values in one sample.
     * @param sampleCount    Number of samples in this payload.
     * @param values         The values, sample-major, with scale divisors applied.
     * @param text           The content of a text stream, or null for a numeric stream.
     * @param timestamp      Offset in seconds from the start of the recording, from {@code STMP}, or NaN if absent.
     * @param containerIndex Ordinal of the {@code STRM} container this stream came from.
     * @param scaled         Whether the container declared a {@code SCAL} divisor that has been applied.
     */
    GpmfStream(String deviceName, String key, String name, String units, int elementCount, int sampleCount,
               double[] values, String text, double timestamp, int containerIndex, boolean scaled) {
        this.deviceName = deviceName;
        this.key = key;
        this.name = name;
        this.units = units;
        this.elementCount = elementCount;
        this.sampleCount = sampleCount;
        this.values = values;
        this.text = text;
        this.timestamp = timestamp;
        this.containerIndex = containerIndex;
        this.scaled = scaled;
    }

    /**
     * @return Name of the device that recorded this stream, for example {@code Hero11 Black}, or null if absent.
     */
    public String getDeviceName() {
        return deviceName;
    }

    /**
     * @return Four-character key of the stream, for example {@code ACCL}, {@code GYRO}, or {@code GPS5}.
     */
    public String getKey() {
        return key;
    }

    /**
     * @return Human-readable stream name as the camera reported it, for example
     * {@code Accelerometer (z,x,y)}, or null if the payload carried none.
     */
    public String getName() {
        return name;
    }

    /**
     * @return Units of the values, for example {@code m/s2}, or null if the payload carried none.
     */
    public String getUnits() {
        return units;
    }

    /**
     * @return Number of values in one sample.
     */
    public int getElementCount() {
        return elementCount;
    }

    /**
     * @return Number of samples in this payload.
     */
    public int getSampleCount() {
        return sampleCount;
    }

    /**
     * @return The values, sample-major, with scale divisors applied. Not copied; do not modify.
     */
    public double[] getValues() {
        return values;
    }

    /**
     * @return The content of a text stream, or null for a numeric stream.
     */
    public String getText() {
        return text;
    }

    /**
     * @return True if this stream carries text rather than numbers.
     */
    public boolean isText() {
        return text != null;
    }

    /**
     * @return Offset in seconds from the start of the recording, or NaN if the payload carried none.
     */
    public double getTimestamp() {
        return timestamp;
    }

    /**
     * Identifies the {@code STRM} container this stream was decoded from, so that streams recorded
     * together can be correlated. A GPS container, for example, reports its coordinates, fix quality, and
     * UTC time as separate streams that share a container index.
     *
     * @return Ordinal of the container within the payload.
     */
    public int getContainerIndex() {
        return containerIndex;
    }

    /**
     * Reports whether the camera declared a scale divisor for this stream. A stream that declared none
     * carries its values exactly as stored, which for a few keys means they are pre-multiplied by a
     * constant the caller is expected to know.
     *
     * @return True if a {@code SCAL} divisor was applied to the values.
     */
    public boolean isScaled() {
        return scaled;
    }

    @Override
    public String toString() {
        return isText()
                ? key + " = \"" + text + "\""
                : key + " [" + sampleCount + " x " + elementCount + (units != null ? " " + units : "") + "]";
    }
}