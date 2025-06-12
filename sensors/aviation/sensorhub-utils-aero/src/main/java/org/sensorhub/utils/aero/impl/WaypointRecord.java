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

import org.sensorhub.utils.aero.AeroHelper;
import org.sensorhub.utils.aero.IWaypoint;
import org.vast.data.DataBlockProxy;
import org.vast.data.IDataAccessor;
import org.vast.swe.SWEBuilders.DataRecordBuilder;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;


/**
 * <p>
 * Accessor interface for basic Waypoint records.
 * </p>
 *
 * @author Alex Robin
 * @since Jan 30, 2025
 */
public interface WaypointRecord extends IDataAccessor, IWaypoint
{
    public static final String DEF_WAYPOINT_REC = AeroHelper.AERO_RECORD_URI_PREFIX + "Waypoint";
    
    public static final DataRecord SCHEMA = getSchema("");
    
    
    public static DataRecord getSchema(String name)
    {
        return getRecordBuilder(name).build();
    }
    
    static DataRecordBuilder getRecordBuilder(String name)
    {
        AeroHelper fac = new AeroHelper();
        
        return fac.createRecord()
            .name(name)
            .definition(DEF_WAYPOINT_REC) 
            .label("Waypoint")
            .addField("code", fac.createWaypointCode())
            .addField("type", fac.createCategory()
                .definition(AeroHelper.DEF_WAYPOINT_TYPE)
                .label("Waypoint Type")
                .addAllowedValues(WaypointType.class))
            .addField("lat", fac.createLatitude())
            .addField("lon", fac.createLongitude())
            .addField("alt", fac.createBaroAlt());
    }
    
    
    public static WaypointRecord create()
    {
        return create(SCHEMA.createDataBlock());
    }
    
    
    public static WaypointRecord create(DataBlock dblk)
    {
        var proxy = DataBlockProxy.generate(SCHEMA, WaypointRecord.class);
        proxy.wrap(dblk);
        return proxy;
    }
    
    
    @Override
    @SweMapping(path="code")
    String getCode();

    @SweMapping(path="code")
    void setCode(String val);
    
    @Override
    @SweMapping(path="type")
    String getType();

    @SweMapping(path="type")
    void setType(String val);
    
    @Override
    @SweMapping(path="lat")
    double getLatitude();

    @SweMapping(path="lat")
    void setLatitude(double val);
    
    @Override
    @SweMapping(path="lon")
    double getLongitude();

    @SweMapping(path="lon")
    void setLongitude(double val);
    
    @Override
    @SweMapping(path="alt")
    double getBaroAltitude();

    @SweMapping(path="alt")
    void setBaroAltitude(double val);
    
}
