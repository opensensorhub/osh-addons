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

package org.sensorhub.impl.service.consys.proto.codec;

import net.opengis.swe.v20.BinaryBlock;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.vast.data.AbstractDataBlock;
import org.vast.data.AbstractDataComponentImpl;
import org.vast.data.DataBlockCompressed;
import org.vast.data.DataBlockMixed;
import org.vast.data.DataBlockTuple;


/**
 * <p>
 * Support for SWE components whose payload is an opaque <b>compressed binary
 * block</b> (e.g. a JPEG/H264 video frame): a component tagged — via
 * {@code SWEHelper.assignBinaryEncoding} — with a {@link BinaryBlock} encoding
 * member carrying a compression codec id. At runtime such a component's data is
 * a {@link DataBlockCompressed}: an opaque carrier of the compressed
 * {@code byte[]} whose atom getters fail ({@code ensureUncompressed()} is a
 * stub) and whose atom setters throw. The codec therefore maps it to a single
 * proto {@code bytes} field and <b>never walks its atoms</b> — the same
 * pass-through contract as the classic SWE {@code BinaryDataWriter}/{@code
 * BinaryDataParser} pair.
 * </p>
 *
 * <p>
 * The compressed child still occupies its <i>uncompressed</i> logical atom span
 * ({@code getAtomCount()}, e.g. {@code width*height*3}) in the parent block's
 * flat index space, so the encoder/decoder advance their running index by that
 * span to keep trailing sibling components aligned.
 * </p>
 *
 * @see ProtoEncoder
 * @see ProtoDecoder
 * @author Ian Patterson
 * @since 2026
 */
public final class ProtoCompressed
{
    private ProtoCompressed() {}


    /** @return the compression codec id ("JPEG", "H264", …) if {@code comp} is a
     *  compressed binary-block component, else null. Only populated after
     *  {@code SWEHelper.assignBinaryEncoding} has run on the component tree. */
    public static String compressionOf(DataComponent comp)
    {
        if (!(comp instanceof AbstractDataComponentImpl))
            return null;
        var enc = ((AbstractDataComponentImpl) comp).getEncodingInfo();
        if (!(enc instanceof BinaryBlock))
            return null;
        return ((BinaryBlock) enc).getCompression();
    }


    /** @return true if {@code comp} carries a compressed {@link BinaryBlock}
     *  encoding member (its runtime data is a {@link DataBlockCompressed}). */
    public static boolean isCompressed(DataComponent comp)
    {
        return compressionOf(comp) != null;
    }


    /**
     * Locate the {@link DataBlockCompressed} child that starts at flat atom
     * index {@code flatIdx} of {@code root}, descending composite blocks
     * ({@link DataBlockMixed}/{@link DataBlockTuple}) by cumulative atom count —
     * the same routing the flat accessors use. The compressed block must start
     * exactly at {@code flatIdx} (it always does when the schema walk and the
     * block layout agree).
     */
    public static DataBlockCompressed findBlock(DataBlock root, int flatIdx, String compName)
    {
        DataBlock b = root;
        int offset = flatIdx;

        while (!(b instanceof DataBlockCompressed))
        {
            AbstractDataBlock[] children;
            if (b instanceof DataBlockMixed)
                children = ((DataBlockMixed) b).getUnderlyingObject();
            else if (b instanceof DataBlockTuple)
                children = ((DataBlockTuple) b).getUnderlyingObject();
            else
                throw new IllegalStateException(
                    "swe+proto: compressed component '" + compName + "' expected a DataBlockCompressed at flat index "
                    + flatIdx + " but found " + b.getClass().getSimpleName());

            AbstractDataBlock next = null;
            for (var c : children)
            {
                if (offset < c.getAtomCount())
                {
                    next = c;
                    break;
                }
                offset -= c.getAtomCount();
            }
            if (next == null)
                throw new IllegalStateException(
                    "swe+proto: flat index " + flatIdx + " out of range while locating compressed component '"
                    + compName + "'");
            b = next;
        }

        if (offset != 0)
            throw new IllegalStateException(
                "swe+proto: compressed component '" + compName + "' does not start at flat index " + flatIdx
                + " (offset " + offset + " into the compressed block)");
        return (DataBlockCompressed) b;
    }
}
