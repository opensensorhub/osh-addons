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

import net.opengis.swe.v20.Category;
import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.SimpleComponent;
import net.opengis.swe.v20.Text;
import net.opengis.swe.v20.Time;
import net.opengis.swe.v20.DataComponent;


/**
 * <p>
 * Maps a scalar SWE component to the FlexBuffers value type used to carry its
 * flat {@code DataBlock} atom, and reads/writes that atom in a single place so
 * {@link FlexEncoder} and {@link FlexDecoder} stay symmetric. This is the
 * FlexBuffers analog of {@code ProtoSchemaWriter.scalarProtoType} — the same SWE
 * type → wire type decision, just targeting FlexBuffers' six scalar kinds
 * instead of protobuf field types.
 * </p>
 *
 * @author Ian Patterson
 * @since 2026
 */
public enum FlexTypes
{
    BOOL, INT, LONG, FLOAT, DOUBLE, STRING;


    /**
     * Classify a scalar SWE component (or a {@code RangeComponent}, whose two
     * bounds share this element type). Throws for shapes with no scalar mapping
     * (e.g. Geometry, or a numeric with an unrecognized data type) so callers can
     * treat swe+flatbuffers as unsupported for that datastream — matching the
     * proto writer's fail-loud behavior rather than silently mis-encoding.
     */
    public static FlexTypes of(DataComponent comp)
    {
        if (comp instanceof net.opengis.swe.v20.Boolean)
            return BOOL;
        if (comp instanceof Text || comp instanceof Category)
            return STRING;
        if (comp instanceof Time && !((Time) comp).isIsoTime())
            return DOUBLE;

        if (comp instanceof SimpleComponent)
        {
            var dt = ((SimpleComponent) comp).getDataType();
            if (dt != null)
            {
                switch (dt)
                {
                    case FLOAT:  return FLOAT;
                    case DOUBLE: return DOUBLE;
                    case BYTE: case SHORT: case INT:
                    case UBYTE: case USHORT: case UINT: return INT;
                    case LONG: case ULONG: return LONG;
                    default: break;
                }
            }
        }

        // sensible defaults when no explicit data type is set
        if (comp instanceof Quantity) return DOUBLE;
        if (comp instanceof Count) return INT;

        throw new UnsupportedOperationException(
            "Unsupported SWE scalar type for swe+flatbuffers: " + comp.getClass().getSimpleName()
            + " (field '" + comp.getName() + "')");
    }
}
