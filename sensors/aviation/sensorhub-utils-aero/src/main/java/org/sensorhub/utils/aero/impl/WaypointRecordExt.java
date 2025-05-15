/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2025 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.utils.aero.impl;

import java.time.Instant;
import org.sensorhub.utils.aero.AeroHelper;
import org.sensorhub.utils.aero.IWaypointWithState;
import org.vast.data.DataBlockProxy;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;


/**
 * <p>
 * Accessor interface for advanced Waypoint records.
 * This type of waypoint record also includes ownship state estimates.
 * </p>
 *
 * @author Alex Robin
 * @since Jan 30, 2025
 */
public interface WaypointRecordExt extends WaypointRecord, IWaypointWithState
{
    
    public static DataRecord getSchema(String name)
    {
        AeroHelper fac = new AeroHelper();
        
        return WaypointRecord.getRecordBuilder(name)
            .addField("time", fac.createTime().asForecastTimeIsoUTC()
                .label("Estimated Time")
                .description("Estimated time over waypoint"))
            .addField("gs", fac.createGroundSpeed()
                .label("Estimated Ground Speed")
                .description("Estimated ground speed at waypoint"))
            .addField("tas", fac.createTrueAirspeed()
                .label("Estimated Airspeed")
                .description("Estimated true airspeed at waypoint"))
            .addField("mach", fac.createTrueAirspeed()
                .label("Estimated Mach")
                .description("Estimated mach at waypoint"))
            .addField("fob", fac.createFuelOnBoard()
                .label("Estimated Fuel")
                .description("Estimated fuel on board at waypoint"))
            .build();
    }
    
    
    public static WaypointRecordExt create(DataBlock dblk)
    {
        var proxy = DataBlockProxy.generate(WaypointRecordExt.getSchema(""), WaypointRecordExt.class);
        proxy.wrap(dblk);
        return proxy;
    }
    
    
    @Override
    @SweMapping(path="time")
    Instant getTime();

    @SweMapping(path="time")
    void setTime(Instant val);
    
    @Override
    @SweMapping(path="gs")
    double getGroundSpeed();

    @SweMapping(path="gs")
    void setGroundSpeed(double val);
    
    @Override
    @SweMapping(path="tas")
    double getTrueAirSpeed();

    @SweMapping(path="tas")
    void setTrueAirSpeed(double val);
    
    @Override
    @SweMapping(path="mach")
    double getMach();

    @SweMapping(path="mach")
    void setMach(double val);
    
    @Override
    @SweMapping(path="fob")
    double getFuelOnBoard();

    @SweMapping(path="fob")
    void setFuelOnBoard(double val);
    
}
