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

package org.sensorhub.impl.service.consys.flatbuffers;

import org.sensorhub.api.service.ServiceConfig;


/**
 * <p>
 * Configuration class for the Connected Systems API FlatBuffers service module.
 * Placeholder — the {@code swe+flatbuffers} codec needs no per-node runtime
 * state (FlexBuffers payloads are self-describing and the record structure is
 * held by the datastream registry), so unlike the proto service this module
 * carries no schema-cache settings yet.
 * </p>
 *
 * @author Ian Patterson
 * @since 2026
 */
public class ConSysApiFlatBuffersServiceConfig extends ServiceConfig
{
    // TODO: framing options (length-prefix width), FlexBuffers builder flags,
    //       and whether to advertise the format as auto-selectable.
}
