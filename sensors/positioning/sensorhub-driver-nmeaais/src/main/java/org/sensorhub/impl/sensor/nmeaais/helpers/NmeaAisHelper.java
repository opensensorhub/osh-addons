/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.nmeaais.helpers;

import net.opengis.swe.v20.Count;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

public class NmeaAisHelper extends SWEHelper{
    GeoPosHelper geoFac = new GeoPosHelper();

    public Count createNmeaMessageId() {
        return createCount()
                .label("Message Id")
                .description("AIS Message Identifier")
                .definition(getPropertyUri("MessageId"))
                .build();
    }

}
