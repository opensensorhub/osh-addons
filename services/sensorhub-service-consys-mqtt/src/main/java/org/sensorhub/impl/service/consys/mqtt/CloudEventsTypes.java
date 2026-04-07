/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2026 GeoRobotix innovative Research. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.consys.mqtt;


/**
 * <p>
 * CloudEvents v1.0 type string constants for CS API resource lifecycle events,
 * as defined by OGC CS API Part 3 (Pub/Sub).
 * </p>
 *
 * <p>
 * The {@code enable} and {@code disable} types are OSH extensions beyond the
 * three types (create/update/delete) explicitly listed in the spec.
 * </p>
 */
public final class CloudEventsTypes
{
    private CloudEventsTypes() {}

    public static final String SPECVERSION = "1.0";

    // System lifecycle
    public static final String TYPE_SYSTEM_CREATE  = "org.ogc.api.consys.system.create";
    public static final String TYPE_SYSTEM_UPDATE  = "org.ogc.api.consys.system.update";
    public static final String TYPE_SYSTEM_DELETE  = "org.ogc.api.consys.system.delete";
    public static final String TYPE_SYSTEM_ENABLE  = "org.ogc.api.consys.system.enable";
    public static final String TYPE_SYSTEM_DISABLE = "org.ogc.api.consys.system.disable";

    // DataStream lifecycle
    public static final String TYPE_DATASTREAM_CREATE  = "org.ogc.api.consys.datastream.create";
    public static final String TYPE_DATASTREAM_UPDATE  = "org.ogc.api.consys.datastream.update";
    public static final String TYPE_DATASTREAM_DELETE  = "org.ogc.api.consys.datastream.delete";
    public static final String TYPE_DATASTREAM_ENABLE  = "org.ogc.api.consys.datastream.enable";
    public static final String TYPE_DATASTREAM_DISABLE = "org.ogc.api.consys.datastream.disable";

    // ControlStream (CommandStream) lifecycle
    public static final String TYPE_CONTROLSTREAM_CREATE  = "org.ogc.api.consys.controlstream.create";
    public static final String TYPE_CONTROLSTREAM_UPDATE  = "org.ogc.api.consys.controlstream.update";
    public static final String TYPE_CONTROLSTREAM_DELETE  = "org.ogc.api.consys.controlstream.delete";
    public static final String TYPE_CONTROLSTREAM_ENABLE  = "org.ogc.api.consys.controlstream.enable";
    public static final String TYPE_CONTROLSTREAM_DISABLE = "org.ogc.api.consys.controlstream.disable";

    // Observation events
    // Note: the obs ID used in MQTT topics is the phenomenon time in epoch-ms because
    // the internal database ID is not yet assigned when ObsEvent is published.
    public static final String TYPE_OBS_CREATE = "org.ogc.api.consys.observation.create";
}
