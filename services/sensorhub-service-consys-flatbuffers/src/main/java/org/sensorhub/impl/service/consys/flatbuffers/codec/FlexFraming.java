/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

Author: Ian Patterson <ian.patterson@georobotix.us>

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.consys.flatbuffers.codec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * <p>
 * Length-prefixed framing for FlexBuffers payloads on a stream — a 4-byte
 * big-endian byte count followed by that many payload bytes. This is the
 * FlexBuffers analog of protobuf's {@code writeDelimitedTo}: a single
 * observation is one frame, and a collection/stream is a concatenation of frames
 * a reader can split.
 * </p>
 *
 * <p>
 * A length prefix is required because {@code FlexBuffers.getRoot} locates the
 * root at the <i>end</i> of its buffer — concatenated bare FlexBuffers would be
 * unsplittable. Over NATS each proactive observation is published as one message
 * whose payload is one such frame.
 * </p>
 *
 * @author Ian Patterson
 * @since 2026
 */
public final class FlexFraming
{
    private FlexFraming() {}


    /** Write {@code payload} as a 4-byte big-endian length prefix + the bytes. */
    public static void writeFrame(OutputStream out, byte[] payload) throws IOException
    {
        int n = payload.length;
        out.write((n >>> 24) & 0xFF);
        out.write((n >>> 16) & 0xFF);
        out.write((n >>> 8) & 0xFF);
        out.write(n & 0xFF);
        out.write(payload);
    }


    /**
     * Read one length-prefixed frame.
     *
     * @return the payload bytes, or {@code null} at a clean end of stream (no
     *         more frames).
     */
    public static byte[] readFrame(InputStream in) throws IOException
    {
        int b0 = in.read();
        if (b0 < 0)
            return null;   // clean EOF between frames
        int b1 = in.read();
        int b2 = in.read();
        int b3 = in.read();
        if ((b1 | b2 | b3) < 0)
            throw new IOException("truncated swe+flatbuffers frame length");
        int n = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
        if (n < 0 || n > 64 * 1024 * 1024)
            throw new IOException("invalid swe+flatbuffers frame length: " + n);

        var payload = new byte[n];
        int off = 0;
        while (off < n)
        {
            int r = in.read(payload, off, n - off);
            if (r < 0)
                throw new IOException("truncated swe+flatbuffers frame body (" + off + "/" + n + ")");
            off += r;
        }
        return payload;
    }
}
