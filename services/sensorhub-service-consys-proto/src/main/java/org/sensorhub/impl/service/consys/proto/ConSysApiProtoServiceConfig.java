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

package org.sensorhub.impl.service.consys.proto;

import org.sensorhub.api.service.ServiceConfig;


/**
 * <p>
 * Configuration class for the Connected Systems API Protobuf service module.
 * Settings TBD — placeholder until schema-registry and codec options are designed.
 * </p>
 *
 * @author Ian Patterson
 * @since 2026
 */
public class ConSysApiProtoServiceConfig extends ServiceConfig
{
    // TODO: schema cache size / TTL
    // TODO: well-known type imports (timestamp.proto, any.proto, etc.)
    // TODO: package prefix for generated .proto files (e.g. "org.sensorhub.consys")
    // TODO: toggle for accepting client-supplied schemas vs. server-generated only
}