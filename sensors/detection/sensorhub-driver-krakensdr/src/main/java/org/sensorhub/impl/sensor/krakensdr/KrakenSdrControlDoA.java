package org.sensorhub.impl.sensor.krakensdr;

import com.google.gson.JsonObject;
import net.opengis.swe.v20.*;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.swe.SWEHelper;

import java.net.HttpURLConnection;

public class KrakenSdrControlDoA extends AbstractSensorControl<KrakenSdrSensor> {
    private DataRecord commandDataStruct;
    KrakenUTILITY util = parentSensor.util;
    HttpURLConnection conn;

    // CONSTRUCTOR
    public KrakenSdrControlDoA(KrakenSdrSensor krakenSDRSensor) {
        super("DoA Control", krakenSDRSensor);
    }

    // INITIALIZE CONTROL
    public void doInit(){
        SWEHelper fac = new SWEHelper();
        // The Master Control Data Structure is a Choice of individual controls for the KrakenSDR
        commandDataStruct = fac.createRecord()
                .name("DoA_control")
                .label("DoA Configuration")
                .description("Data Record for the DoA Configuration Settings")
                .definition(SWEHelper.getPropertyUri("DoA_control"))
                .addField("antennaArrangement", fac.createCategory()
                        .label("Antenna Arrangement")
                        .description("The Arrangement must be UCA or ULA")
                        .definition(SWEHelper.getPropertyUri("AntennaArrangement"))
                        .addAllowedValues("UCA", "ULA")
                )
                .addField("antennaSpacingMeters", fac.createQuantity()
                        .uom("m")
                        .label("Antenna Array Radius")
                        .description("Current spacing of the Antenna Array")
                        .definition(SWEHelper.getPropertyUri("AntennaSpacingMeters"))
                ).build();
    }

    @Override
    protected boolean execCommand(DataBlock cmdData) throws CommandException
    {

        // RETRIEVE INPUTS FROM ADMIN PANEL CONTROL
        DataRecord commandData = commandDataStruct.copy();
        commandData.setData(cmdData);


        // RETRIEVE CURRENT JSON SETTINGS AS A JSON OBJECT
        JsonObject currentSettings = null;
        JsonObject oldSettings = null;
        try {
            currentSettings = util.retrieveJSONFromAddr(parentSensor.SETTINGS_URL);
            oldSettings = currentSettings.deepCopy();
        } catch (SensorHubException e) {
            getLogger().debug("Failed to retrieve current json settings from kraken: ", e);
        }

        // UPDATE CURRENT JSON SETTINGS WITH UPDATED CONTROL SETTINGS
        // UPDATE ANTENNA ARRANGEMNENT IF UPDATED IN ADMIN PANEL
        Category oshAntArrangement = (Category) commandData.getField("ant_arrangement");
        String oshAntArrangementValue = oshAntArrangement.getValue();

        if(oshAntArrangementValue != null){
            currentSettings.addProperty("ant_arrangement", oshAntArrangementValue);
        }

        // UPDATE ANTENNA SPACING IF UPDATED IN ADMIN PANEL
        Quantity oshAntennaSpacing = (Quantity) commandData.getField("ant_spacing_meters");
        double oshAntennaSpacingValue = oshAntennaSpacing.getValue();
        if(oshAntennaSpacingValue != 0.0){
            currentSettings.addProperty("ant_spacing_meters", oshAntennaSpacingValue);
        }

        // REPLACE SETTINGS ON KRAKENSDR BASED ON CONTROL UPDATED ABOVE
        if(!currentSettings.equals(oldSettings)){
            util.replaceOldSettings(parentSensor.OUTPUT_URL , currentSettings);
        }

        return true;
    }


    @Override
    public DataComponent getCommandDescription() {
        return commandDataStruct;
    }





}
