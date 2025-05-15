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
import org.sensorhub.utils.aero.IAircraftState;
import org.vast.data.DataBlockProxy;
import org.vast.data.IDataAccessor;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;


/**
 * <p>
 * Accessor interface for Aircraft State records.
 * </p>
 *
 * @author Alex Robin
 * @since Jan 30, 2025
 */
public interface AircraftStateRecord extends IDataAccessor, IAircraftState
{
    public static final String DEF_AIRCRAFT_STATE_RECORD = AeroHelper.AERO_RECORD_URI_PREFIX + "AircraftState";
    
    
    public static DataRecord getSchema(String name)
    {
        AeroHelper fac = new AeroHelper();
        
        return fac.createRecord()
            .name(name)
            .definition(DEF_AIRCRAFT_STATE_RECORD)
            .label("Aircraft State")
            .description("Record containing aircraft state parameters for a given tail")
            .addField("time", fac.createTime().asSamplingTimeIsoUTC())
            .addField("tail", fac.createTailNumber())
            .addField("callsign", fac.createCallSign())
            .addField("pos", fac.createAircraftLocation())
            .addField("alt_gnss", fac.createGeomAlt())
            .addField("alt_baro", fac.createBaroAlt())
            .addField("track", fac.createTrueTrack())
            .addField("heading", fac.createTrueHeading())
            .addField("gs", fac.createGroundSpeed())
            .addField("alt_rate", fac.createVerticalRate())
            .addField("cas", fac.createCalibratedAirspeed())
            .addField("tas", fac.createTrueAirspeed())
            .addField("mach", fac.createMachNumber())
            .addField("sat", fac.createStaticAirTemp())
            .addField("zfw", fac.createZeroFuelWeight())
            .addField("fob", fac.createFuelOnBoard())
            .build();
    }
    
    
    public static AircraftStateRecord create(DataBlock dblk)
    {
        var proxy = DataBlockProxy.generate(AircraftStateRecord.getSchema(""), AircraftStateRecord.class);
        proxy.wrap(dblk);
        return proxy;
    }
    
    
    @Override
    @SweMapping(path="time")
    Instant getTime();
    
    @SweMapping(path="time")
    void setTime(Instant val);
    
    @Override
    @SweMapping(path="tail")
    String getTailNumber();

    @SweMapping(path="tail")
    void setTailNumber(String val);
    
    @Override
    @SweMapping(path="callsign")
    String getCallSign();

    @SweMapping(path="callsign")
    void setCallSign(String val);

    @Override
    @SweMapping(path="pos/lat")
    double getLatitude();
    
    @SweMapping(path="pos/lat")
    void setLatitude(double val);

    @Override
    @SweMapping(path="pos/lon")
    double getLongitude();
    
    @SweMapping(path="pos/lon")
    void setLongitude(double val);

    @Override
    @SweMapping(path="alt_gnss")
    double getGnssAltitude();
    
    @SweMapping(path="alt_gnss")
    void setGnssAltitude(double val);

    @Override
    @SweMapping(path="alt_baro")
    double getBaroAltitude();
    
    @SweMapping(path="alt_baro")
    void setBaroAltitude(double val);

    @Override
    default double getBaroAltSetting()
    {
        return Double.NaN;
    }

    @Override
    @SweMapping(path="track")
    double getTrueTrack();
    
    @SweMapping(path="track")
    void setTrueTrack(double val);

    @Override
    default double getMagneticTrack()
    {
        return Double.NaN;
    }

    @Override
    @SweMapping(path="heading")
    double getTrueHeading();
    
    @SweMapping(path="heading")
    void setTrueHeading(double val);

    @Override
    default double getMagneticHeading()
    {
        return Double.NaN;
    }

    @Override
    @SweMapping(path="gs")
    double getGroundSpeed();
    
    @SweMapping(path="gs")
    void setGroundSpeed(double val);

    @Override
    @SweMapping(path="alt_rate")
    double getVerticalRate();
    
    @SweMapping(path="alt_rate")
    void setVerticalRate(double val);

    @Override
    @SweMapping(path="tas")
    double getTrueAirSpeed();
    
    @SweMapping(path="tas")
    void setTrueAirSpeed(double val);

    @Override
    @SweMapping(path="cas")
    double getCalibratedAirSpeed();
    
    @SweMapping(path="cas")
    void setCalibratedAirSpeed(double val);

    @Override
    @SweMapping(path="mach")
    double getMach();
    
    @SweMapping(path="mach")
    void setMach(double val);

    @Override
    @SweMapping(path="sat")
    double getStaticAirTemperature();
    
    @SweMapping(path="sat")
    void setStaticAirTemperature(double val);

    @Override
    default double getStaticAirPressure()
    {
        return Double.NaN;
    }

    @Override
    @SweMapping(path="zfw")
    double getZeroFuelWeight();
    
    @SweMapping(path="zfw")
    void setZeroFuelWeight(double val);

    @Override
    @SweMapping(path="fob")
    double getFuelOnBoard();
    
    @SweMapping(path="fob")
    void setFuelOnBoard(double val);
    
}
