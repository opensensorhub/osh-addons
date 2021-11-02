/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.isa;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;


/**
 * <p>
 * Clock used for replaying data over a soecific period
 * </p>
 *
 * @author Alex Robin
 * @since Oct 13, 2021
 */
public class ReplayClock extends Clock
{
    long refTime, simDuration;
    Instant startTime;
    boolean loop;
    
    
    public ReplayClock(Instant startTime, Instant stopTime, boolean loop)
    {
        this.refTime = System.currentTimeMillis();
        this.startTime = startTime;
        this.loop = loop;
        this.simDuration = stopTime != null ? stopTime.toEpochMilli() - startTime.toEpochMilli() : 2^32;
    }
    
    
    @Override
    public ZoneId getZone()
    {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone)
    {
        return null;
    }

    @Override
    public Instant instant()
    {
     // compute simulated time (loop?)
        var dt = (System.currentTimeMillis() - refTime) % simDuration;
        var simTime = startTime.plusMillis(dt);
        return simTime;
    }

}
