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

import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;


/** Shared structural predicates for the swe+proto array codec paths. */
final class ProtoArrays
{
    private ProtoArrays() {}


    /**
     * True if {@code c}'s subtree would back a {@code DataArray} with a
     * <b>non-flat</b> ({@code DataBlockList}) layout — i.e. it contains a
     * {@link DataChoice} or a variable-size {@link DataArray}. The swe+proto
     * codec walks one flat atom index across the whole record, which only holds
     * when every array is fixed-size and every element is itself fixed-size; a
     * list-backed array is not flat-addressable, so the codec rejects it rather
     * than silently mis-reading atoms. (A nested <i>fixed</i>-size array is
     * fine — it stays flat — so it is not flagged.)
     */
    static boolean hasNonFlatLayout(DataComponent c)
    {
        if (c instanceof DataChoice)
            return true;
        if (c instanceof DataArray)
        {
            var a = (DataArray) c;
            return a.isVariableSize() || hasNonFlatLayout(a.getElementType());
        }
        for (int i = 0; i < c.getComponentCount(); i++)
            if (hasNonFlatLayout(c.getComponent(i)))
                return true;
        return false;
    }
}
