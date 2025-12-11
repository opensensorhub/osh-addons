/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2025 the Initial Developer. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.kestrel;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;

import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEHelper;


/**
 * @author Kalyn Stricklin
 * @since Dec 1, 2025
 */
public class EnvironmentalOutput extends AbstractSensorOutput<Kestrel> {
    DataComponent dataStruct;
    DataEncoding dataEncoding;
    private static final String SENSOR_OUTPUT_NAME = "environmental";
    private static final String SENSOR_OUTPUT_LABEL = "Environmental Output";
    private static final Logger logger = LoggerFactory.getLogger(EnvironmentalOutput.class);

    protected EnvironmentalOutput(Kestrel parent) {
        super(SENSOR_OUTPUT_NAME, parent);
    }

    public void doInit() {
        SWEHelper fac = new SWEHelper();
        dataStruct = fac.createRecord()
                .name(SENSOR_OUTPUT_NAME)
                .definition(SWEHelper.getPropertyUri(SENSOR_OUTPUT_NAME))
                .label(SENSOR_OUTPUT_LABEL)
                .addField("samplingTime", fac.createTime()
                        .asSamplingTimeIsoUTC())
                .addField("windSpeed", fac.createQuantity()
                        .label("Wind Speed")
                        .definition(SWEHelper.getPropertyUri("WindSpeed"))
                        .uom("m/s")
                        .addAllowedInterval(0.0, 65.0)
                        .build())
                .addField("temperature", fac.createQuantity()
                        .label("Ambient Temperature (DryBulb)")
                        .definition(SWEHelper.getPropertyUri("Temperature"))
                        .uomCode("Cel")
                        .addAllowedInterval(-55.0, 150.0)
                        .build())
                .addField("globeTemperature", fac.createQuantity()
                        .label("Globe Temperature")
                        .definition(SWEHelper.getPropertyUri("GlobeTemperature"))
                        .uomCode("Cel")
                        .addAllowedInterval(-55.0, 150.0)
                        .build())
                .addField("humidity", fac.createQuantity()
                        .label("Humidity")
                        .definition(SWEHelper.getPropertyUri("Humidity"))
                        .uom("%")
                        .addAllowedInterval(0, 100.0)
                        .build())
                .addField("stationPressure", fac.createQuantity()
                        .label("Station Air Pressure")
                        .definition(SWEHelper.getPropertyUri("StationPressure"))
                        .description("Absolute Pressure")
                        .uom("hPa")
                        .addAllowedInterval(10.0, 1600.0)
                        .build())
                .addField("magDirection", fac.createQuantity()
                        .label("Magnetic Direction")
                        .definition(SWEHelper.getPropertyUri("MagneticDirection"))
                        .uom("deg")
                        .addAllowedInterval(0, 360.0)
                        .build())
                .addField("airSpeed", fac.createQuantity()
                        .label("Air Speed")
                        .definition(SWEHelper.getPropertyUri("AirSpeed"))
                        .uom("m/s")
                        .build())
                .addField("trueDirection", fac.createQuantity()
                        .label("True Direction")
                        .definition(SWEHelper.getPropertyUri("TrueDirection"))
                        .addAllowedInterval(0, 360.0)
                        .uom("deg")
                        .build())
                .addField("airDensity", fac.createQuantity()
                        .label("Air Density")
                        .definition(SWEHelper.getPropertyUri("AirDensity"))
//                        .uomCode("Kg/m^3")
                        .addAllowedInterval(0.0, 2.033)
                        .build())
                .addField("altitude", fac.createQuantity()
                        .label("Altitude")
                        .definition(SWEHelper.getPropertyUri("Altitude"))
                        .uom("m")
                        .addAllowedInterval(-2000, 9200)
                        .build())
                .addField("pressure", fac.createQuantity()
                        .label("Barometric Pressure")
                        .definition(SWEHelper.getPropertyUri("BarometricPressure"))
                        .uom("hPa")
                        .addAllowedInterval(10.0, 1654.7)
                        .build())
                .addField("crosswind", fac.createQuantity()
                        .label("Crosswind")
                        .definition(SWEHelper.getPropertyUri("Crosswind"))
                        .uom("m/s")
                        .addAllowedInterval(0.0, 65.0)
                        .build())
                .addField("headwind", fac.createQuantity()
                        .label("Headwind")
                        .definition(SWEHelper.getPropertyUri("Headwind"))
                        .uom("m/s")
                        .addAllowedInterval(0.0, 65.0)
                        .build())
                .addField("densityAltitude", fac.createQuantity()
                        .label("Density Altitude")
                        .definition(SWEHelper.getPropertyUri("DensityAltitude"))
                        .uom("m")
                        .addAllowedInterval(-4913.1, 14789.6)
                        .build())
                .addField("relativeAirDensity", fac.createQuantity()
                        .label("Relative Air Density")
                        .definition(SWEHelper.getPropertyUri("RelativeAirDensity"))
                        .uom("%")
                        .addAllowedInterval(0.0, 100.0)
                        .build())
                .addField("dewPoint", fac.createQuantity()
                        .label("Dew Point")
                        .definition(SWEHelper.getPropertyUri("DewPoint"))
                        .uomCode("Cel")
                        .addAllowedInterval(-50.0, 150.0)
                        .build())
                .addField("heatIndex", fac.createQuantity()
                        .label("Heat Stress Index")
                        .definition(SWEHelper.getPropertyUri("HeatStressIndex"))
                        .uomCode("Cel")
                        .addAllowedInterval(-55.0, 370.8)
                        .build())
                .addField("wetBulb", fac.createQuantity()
                        .label("Wet Bulb")
                        .definition(SWEHelper.getPropertyUri("WetBulb"))
                        .uomCode("Cel")
                        .build())
                .addField("windChill", fac.createQuantity()
                        .label("Wind Chill")
                        .definition(SWEHelper.getPropertyUri("WindChill"))
                        .uomCode("Cel")
                        .addAllowedInterval(-99.4, 150.0)
                        .build())
                .build();

        dataEncoding = fac.newTextEncoding(",", "\n");
    }

    @Override
    public double getAverageSamplingPeriod() {
        return 0;
    }

    @Override
    public DataComponent getRecordDescription() {
        return dataStruct;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }

    public void setData(KestrelEnvData env) {
        DataBlock dataBlock = dataStruct.createDataBlock();

        dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000d);
        dataBlock.setDoubleValue(1, env.windSpeed);
        dataBlock.setDoubleValue(2, env.dryBulbTemp);
        dataBlock.setDoubleValue(3, env.globeTemp);
        dataBlock.setDoubleValue(4, env.relativeHumidity);
        dataBlock.setDoubleValue(5, env.stationPress);
        dataBlock.setDoubleValue(6, env.magDirection);
        dataBlock.setDoubleValue(7, env.airSpeed);
        dataBlock.setDoubleValue(8, env.trueDirection);
        dataBlock.setDoubleValue(9, env.airDensity);
        dataBlock.setDoubleValue(10, env.altitude);
        dataBlock.setDoubleValue(11, env.pressure);
        dataBlock.setDoubleValue(12, env.crosswind);
        dataBlock.setDoubleValue(13, env.headwind);
        dataBlock.setDoubleValue(14, env.densityAlt);
        dataBlock.setDoubleValue(15, env.relativeAirDensity);
        dataBlock.setDoubleValue(16, env.dewPoint);
        dataBlock.setDoubleValue(17, env.heatIndex);
        dataBlock.setDoubleValue(18, env.wetBulb);
        dataBlock.setDoubleValue(19, env.chill);

        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));
    }

}
