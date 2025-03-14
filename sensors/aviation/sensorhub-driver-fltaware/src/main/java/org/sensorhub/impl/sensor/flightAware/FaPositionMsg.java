/***************************** BEGIN COPYRIGHT BLOCK **************************

Copyright (C) 2024 Delta Air Lines, Inc. All Rights Reserved.

Notice: All information contained herein is, and remains the property of
Delta Air Lines, Inc. The intellectual and technical concepts contained herein
are proprietary to Delta Air Lines, Inc. and may be covered by U.S. and Foreign
Patents, patents in process, and are protected by trade secret or copyright law.
Dissemination, reproduction or modification of this material is strictly
forbidden unless prior written permission is obtained from Delta Air Lines, Inc.

******************************* END COPYRIGHT BLOCK ***************************/

package org.sensorhub.impl.sensor.flightAware;

import java.time.Instant;
import org.sensorhub.utils.aero.AeroHelper;
import org.vast.data.IDataAccessor;
import net.opengis.swe.v20.DataRecord;


/**
 * <p>
 * Accessor interface for FlightAware Firehose Position messages.
 * </p>
 *
 * @author Alex Robin
 * @since Jan 20, 2025
 */
public interface FaPositionMsg extends IDataAccessor
{
    public static final String DEF_FA_POS_REC = AeroHelper.AERO_RECORD_URI_PREFIX + "fa:pos";
    
    public enum UpdateType
    {
        A("Ground-based ADS-B"),
        Z("Ground Radar"),
        O("Oceanic Reports"),
        P("Estimated"),
        D("Datalink"),
        M("MLAT"),
        X("ASDE-X"),
        S("Space-based ADS-B");
        
        String desc;
        UpdateType(String desc) { this.desc = desc; }
    }
    
    
    public static DataRecord getSchema(String name)
    {
        AeroHelper fac = new AeroHelper(); 
        return fac.createRecord()
            .definition(DEF_FA_POS_REC)
            .addField("time", fac.createIssueTime())
            .addField("tail", fac.createTailNumber())
            .addField("callsign", fac.createCallSign())
            .addField("lat", fac.createLatitude())
            .addField("lon", fac.createLongitude())
            .addField("updateType", fac.createText()
                .definition("")
                .label("Update Type")
                .addAllowedValues(UpdateType.class))
            .addField("gs", fac.createGroundSpeed())
            .addField("track", fac.createTrueTrack())
            .addField("alt_gnss", fac.createGeomAlt())
            .addField("alt_baro", fac.createBaroAlt())
            .addField("alt_rate", fac.createVerticalRate())
            .addField("heading", fac.createTrueHeading())
            .addField("ias", fac.createIndicatedAirspeed())
            .addField("tas", fac.createTrueAirspeed())
            .addField("mach", fac.createMachNumber())
            .build();
    }
    
    @SweMapping(path="time")
    Instant getTime();
    
    @SweMapping(path="time")
    void setTime(Instant val);
    
    @SweMapping(path="tail")
    String getTailNumber();

    @SweMapping(path="tail")
    void setTailNumber(String val);
    
    @SweMapping(path="callsign")
    String getCallSign();

    @SweMapping(path="callsign")
    void setCallSign(String val);
    
    @SweMapping(path="lat")
    double getLatitude();
    
    @SweMapping(path="lat")
    void setLatitude(double val);
    
    @SweMapping(path="lon")
    double getLongitude();
    
    @SweMapping(path="lon")
    void setLongitude(double val);
    
    @SweMapping(path="updateType")
    String getUpdateType();

    @SweMapping(path="updateType")
    void setUpdateType(String val);
    
    @SweMapping(path="gs")
    double getGroundSpeed();
    
    @SweMapping(path="gs")
    void setGroundSpeed(double val);
    
    @SweMapping(path="track")
    double getTrueTrack();
    
    @SweMapping(path="track")
    void setTrueTrack(double val);
    
    @SweMapping(path="alt_gnss")
    double getGnssAltitude();
    
    @SweMapping(path="alt_gnss")
    void setGnssAltitude(double val);
    
    @SweMapping(path="alt_baro")
    double getBaroAltitude();
    
    @SweMapping(path="alt_baro")
    void setBaroAltitude(double val);

    @SweMapping(path="alt_rate")
    double getVerticalRate();
    
    @SweMapping(path="alt_rate")
    void setVerticalRate(double val);

    @SweMapping(path="heading")
    double getTrueHeading();
    
    @SweMapping(path="heading")
    void setTrueHeading(double val);
    
    @SweMapping(path="ias")
    double getIndicatedAirspeed();
    
    @SweMapping(path="ias")
    void setIndicatedAirspeed(double val);
    
    @SweMapping(path="tas")
    double getTrueAirSpeed();
    
    @SweMapping(path="tas")
    void setTrueAirSpeed(double val);
    
    @SweMapping(path="mach")
    double getMach();
    
    @SweMapping(path="mach")
    void setMach(double val);
    
}
