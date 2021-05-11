/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.misb.stanag4609.klv;

import org.sensorhub.misb.stanag4609.tags.Tag;
import org.sensorhub.misb.stanag4609.tags.TagSet;
import java.math.BigInteger;
import java.time.Instant;
import java.util.HashMap;

/**
 * Encompasses a UAS Data Link Set allowing for extraction of elements from the set for further decoding.
 *
 * @author Nick Garay
 * @since Mar. 18, 2020
 */
public abstract class AbstractDataSet {

    protected String designator;

    protected int length;

    protected byte[] payload;

    protected int embeddedDataLength;

    protected int position;

    protected TagSet tagSet;

    protected AbstractDataSet() {
    }

    /**
     * Grants access to he set's length
     *
     * @return the length of the entire KLV set
     */
    public int getLength() {

        return length;
    }

    /**
     * Grants access to the length of all the data less the header information for this set
     *
     * @return length of all the data less the header information for this set
     */
    public int getEmbeddedDataLength() {

        return embeddedDataLength;
    }

    /**
     * Grants access to the set's data
     *
     * @return a copy of the raw data contained in the set as a byte array
     */
    public byte[] getPayload() {

        return payload.clone();
    }

    /**
     * Retrieves the next {@link Element} of encoded data from the set by reading the set and decoding the
     * KLV sequences contained within.  The set will contain 1 or more data elements, each element may include other
     * elements.  Embedded elements are returned as part of the element retrieved and the {@link Element} class is
     * responsible for further decoding.
     *
     * @return an Element representing the encoded data as contained in the set
     */
    protected Element getNextElement(){

        byte tagId = payload[position++];

        int length = decodeLength();

        byte[] value = new byte[length];

        for (int idx = 0; idx < length; ++idx) {

            value[idx] = payload[position++];
        }

        return new Element(tagSet, tagId, value);
    }

    /**
     * Reports whether the set contains more data elements.
     *
     * @return true if there are more data elements, false otherwise.
     */
    protected boolean hasMoreElements() {

        return position < length;
    }

    /**
     * Reads the encoded length of the set and decodes it.
     *
     * @return the number of bytes representing the total length of the set
     */
    protected int decodeLength() {

        int length = 0;

        boolean usingLongBasicEncodingRules = ((payload[position] & 0xFF) >> 7) == 1;

        if (usingLongBasicEncodingRules) {

            int numBytesForLength = payload[position++] & 0x7F;

            for (int count = 0; count < numBytesForLength; ++count) {

                length = (length << 8) | (payload[position++] & 0xFF);
            }

        } else {

            length = (payload[position++] & 0x7F);
        }

        return length;
    }

    /**
     * Converts the value passed in as an integer to a double according to the given constraints
     *
     * @param value         The raw value to convert
     * @param localSetRange The maximum value that can be represented as double
     * @param fieldMaxRange The maximum value of the field
     * @param offset        An offset adjustment
     * @return a double value decoded from the integer value passed in the the formula given by the constraints
     */
    protected double convertToDouble(int value, double localSetRange, double fieldMaxRange, double offset) {

        return (localSetRange / fieldMaxRange) * value + offset;
    }

    /**
     * Converts the value passed in as a short integer to a double according to the given constraints
     *
     * @param value         The raw value to convert
     * @param localSetRange The maximum value that can be represented as double
     * @param fieldMaxRange The maximum value of the field
     * @param offset        An offset adjustment
     * @return a double value decoded from the short integer value passed in the the formula given by the constraints
     */
    protected double convertToDouble(short value, double localSetRange, double fieldMaxRange, double offset) {

        return (localSetRange / fieldMaxRange) * value + offset;
    }
    
    /**
     * Converts the unsigned integer value passed as byte[] to floating point representation
     * using the IMAPB algorithm defined in MISB ST 1201
     * @param min min parameter value
     * @param max max parameter value
     * @param len known uint length
     * @param data unsigned integer data
     * @return double value of parameter after mapping
     */
    protected double reverse_imapb(double min, double max, int len, byte[] data) {
        
        var a = min;
        var b = max;
        var bPow = Math.ceil(log2(b-a));
        var dPow = 8*len-1;
        var sR = Math.pow(2, bPow-dPow);
        var zOff = (a < 0 && b > 0.0) ? a/sR - Math.floor(a/sR) : 0.0;
        
        var y = new BigInteger(1, data).doubleValue(); // int value is always positive
        return sR * (y - zOff) + min;
    }
    
    static double LOG2_BASE = Math.log(2);
    protected double log2(double val) {
        return Math.log(val) / LOG2_BASE;
    }

    /**
     * Converts the value passed in as a long integer to a time in milliseconds since epoch ( Jan 1, 1970)
     *
     * @param value The raw value to convert
     * @return a computed value for milliseconds since Epoch time (Jan 1, 1970)
     */
    protected long convertToTimeInMillis(long value) {

        long epochSeconds = value / 1000000L;
        long nanoOffset = (value % 1000000L) * 1000L;
        Instant instant = Instant.ofEpochSecond(epochSeconds, nanoOffset);
        return instant.toEpochMilli();
    }

    /**
     * Decodes the data and returns a map of tags to decoded data values.
     *
     * @return A HashMap containing the tags and corresponding decoded values
     */
    public abstract HashMap<Tag, Object> decode();
}
