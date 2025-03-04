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

package com.botts.process.weather;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEHelper;


public class WeatherProcess extends ExecutableProcessImpl
{
    private static final String WEATHER = "weather";
    public static final OSHProcessInfo INFO = new OSHProcessInfo(
            WEATHER,
            "Weather Process",
            "Simple process for testing purposes",
            WeatherProcess.class);
    SWEHelper fac = new SWEHelper();
    
    
    public WeatherProcess() {
        super(INFO);

        var weatherInput = createWeatherInput();
        var weatherOutput = createWeatherOutput();
        inputData.add(weatherInput.getName(), weatherInput);
        outputData.add(weatherOutput.getName(), weatherOutput);
    }

    private DataComponent createWeatherInput() {
        return fac.createRecord()
                .name(WEATHER)
                .definition("http://sensorml.com/ont/swe/property/Weather")
                .description("Weather measurements")
                .addField("time", fac.createTime().asSamplingTimeIsoUTC())
                .addField("temperature", fac.createQuantity()
                        .definition(SWEHelper.getPropertyUri("AirTemperature"))
                        .label("Air Temperature")
                        .uom("Cel"))
                .addField("pressure", fac.createQuantity()
                        .definition(SWEHelper.getPropertyUri("AtmosphericPressure"))
                        .label("Air Pressure")
                        .uom("hPa"))
                .addField("windSpeed", fac.createQuantity()
                        .definition(SWEHelper.getPropertyUri("WindSpeed"))
                        .label("Wind Speed")
                        .uom("m/s"))
                .addField("windDirection", fac.createQuantity()
                        .definition(SWEHelper.getPropertyUri("WindDirection"))
                        .label("Wind Direction")
                        .uom("deg")
                        .refFrame("http://sensorml.com/ont/swe/property/NED")
                        .axisId("z"))
                .build();
    }

    private DataComponent createWeatherOutput() {
        return fac.createRecord()
                .name(WEATHER)
                .definition("http://sensorml.com/ont/swe/property/Weather")
                .description("Weather measurements (translated units)")
                .addField("time", fac.createTime().asSamplingTimeIsoUTC())
                .addField("temperature", fac.createQuantity()
                        .definition(SWEHelper.getPropertyUri("AirTemperature"))
                        .label("Air Temperature")
                        .uom("[degF]"))
                .addField("pressure", fac.createQuantity()
                        .definition(SWEHelper.getPropertyUri("AtmosphericPressure"))
                        .label("Air Pressure")
                        .uom("[psi]"))
                .addField("windSpeed", fac.createQuantity()
                        .definition(SWEHelper.getPropertyUri("WindSpeed"))
                        .label("Wind Speed")
                        .uom("[mi_us]/h"))
                .addField("windDirection", fac.createQuantity()
                        .definition(SWEHelper.getPropertyUri("WindDirection"))
                        .label("Wind Direction")
                        .uom("deg")
                        .refFrame("http://sensorml.com/ont/swe/property/NED")
                        .axisId("z"))
                .build();
    }

    
    @Override
    public void init() throws ProcessException {
        super.init();
    }

    private double convertTemperatureToF(double tempC) {
        return (tempC * 9.0/5.0) + 32;
    }

    private double convertPressureToPSI(double pressHpa) {
        return pressHpa * 0.0145038;
    }

    private double convertSpeedToMph(double speedMetersPS) {
        return speedMetersPS * 2.23694;
    }

    @Override
    public void execute() {
        DataBlock inputDataBlock = inputData.getComponent(WEATHER).getData();
        DataBlock outputDataBlock = inputDataBlock.clone();
        // Convert units
        outputDataBlock.setDoubleValue(1, convertTemperatureToF(inputDataBlock.getDoubleValue(1)));
        outputDataBlock.setDoubleValue(2, convertPressureToPSI(inputDataBlock.getDoubleValue(2)));
        outputDataBlock.setDoubleValue(3, convertSpeedToMph(inputDataBlock.getDoubleValue(3)));
        outputData.getComponent(WEATHER).setData(outputDataBlock);
    } 
}