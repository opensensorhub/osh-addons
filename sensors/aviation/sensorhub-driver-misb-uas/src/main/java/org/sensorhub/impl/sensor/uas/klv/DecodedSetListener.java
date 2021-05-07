/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.uas.klv;

import org.sensorhub.impl.sensor.uas.common.SyncTime;
import org.sensorhub.misb.stanag4609.tags.Tag;

import java.util.HashMap;

/**
 * Interface specification for client classes to receive decoded MISB-TS STANAG 4609 ST0601.16 UAS Metadata
 * with a {@linkplain SyncTime} object for stream synchronization.
 *
 * @author Nick Garay
 * @since Oct. 5, 2020
 */
public interface DecodedSetListener {

    void onSetDecoded(SyncTime syncTime, HashMap<Tag, Object> valuesMap);
}
