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

import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;


/**
 * Shared structural predicates for the swe+flatbuffers array codec paths.
 *
 * <p>Mirrors {@code ProtoArrays}: the constraint is a property of the SWE
 * {@code DataBlock} layout (a {@code DataChoice} in an array element makes the
 * block a {@code DataBlockList} the flat-index walk can't address), not of the
 * wire format, so it applies equally to the FlexBuffers codec.</p>
 */
public final class FlexArrays
{
    private FlexArrays() {}


    /**
     * True if {@code component}'s subtree contains a {@link DataChoice}. Rectangular
     * arrays — fixed- or variable-size, including nested (Matrix) — stay flat
     * ({@code DataBlockMixed}), so the flat-index codec handles them and they are
     * <b>not</b> flagged. A {@code DataChoice} as (or inside) an array element is
     * the case the flat-index walk cannot address and is rejected rather than
     * mis-encoded.
     */
    public static boolean elementHasChoice(DataComponent component)
    {
        if (component instanceof DataChoice)
            return true;
        if (component instanceof DataArray)
            return elementHasChoice(((DataArray) component).getElementType());
        for (int i = 0; i < component.getComponentCount(); i++)
            if (elementHasChoice(component.getComponent(i)))
                return true;
        return false;
    }
}
