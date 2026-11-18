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
public final class ProtoArrays
{
    private ProtoArrays() {}


    /**
     * True if {@code component}'s subtree contains a {@link DataChoice}. Rectangular
     * arrays — fixed- or variable-size, including nested (Matrix) — stay flat
     * ({@code DataBlockMixed}), so the flat-index codec handles them and they
     * are <b>not</b> flagged. A {@code DataChoice} as (or inside) an array
     * element is the case the codec does not yet support there: the per-element
     * selector would need the pre-pass to apply selections inside the array,
     * which it doesn't, so such an element is rejected rather than mis-decoded.
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
