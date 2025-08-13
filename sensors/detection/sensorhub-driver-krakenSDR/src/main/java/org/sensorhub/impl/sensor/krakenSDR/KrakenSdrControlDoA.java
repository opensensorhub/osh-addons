package org.sensorhub.impl.sensor.krakenSDR;

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
    public void doInit(JsonObject initialSettings){
        SWEHelper fac = new SWEHelper();
        // The Master Control Data Structure is a Choice of individual controls for the KrakenSDR
        commandDataStruct = fac.createRecord()
                .name("DoA_control")
                .label("DoA Configuration")
                .description("Data Record for the DoA Configuration Settings")
                .definition(SWEHelper.getPropertyUri("DoA_control"))
                .addField("ant_arrangement", fac.createCategory()
                        .label("Antenna Arrangement")
                        .description("The Arrangement must be UCA or ULA")
                        .definition(SWEHelper.getPropertyUri("ant_arrangement"))
                        .addAllowedValues("UCA", "ULA")
                        .value(initialSettings.get("ant_arrangement").getAsString()))
                .addField("ant_spacing_meters", fac.createQuantity()
                        .uom("m")
                        .label("Antenna Array Radius")
                        .description("Current spacing of the Antenna Array")
                        .definition(SWEHelper.getPropertyUri("ant_spacing_meters"))
                        .value(initialSettings.get("ant_spacing_meters").getAsDouble())
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
            currentSettings = util.retrieveJSONFromAddr(parentSensor.settings_URL);
            oldSettings = currentSettings.deepCopy();
        } catch (SensorHubException e) {
            throw new RuntimeException(e);
        }

        // UPDATE CURRENT JSON SETTINGS WITH UPDATED CONTROL SETTINGS
        // UPDATE ANTENNA ARRANGEMNENT IF UPDATED IN ADMIN PANEL
        Category osh_ant_arrangement = (Category) commandData.getField("ant_arrangement");
        String osh_ant_arrangement_value = osh_ant_arrangement.getValue();

        if(osh_ant_arrangement_value != null){
            currentSettings.addProperty("ant_arrangement", osh_ant_arrangement_value);
        }

        // UPDATE ANTENNA SPACING IF UPDATED IN ADMIN PANEL
        Quantity osh_ant_spacing = (Quantity) commandData.getField("ant_spacing_meters");
        double osh_ant_spacing_value = osh_ant_spacing.getValue();
        if(osh_ant_spacing_value != 0.0){
            currentSettings.addProperty("ant_spacing_meters", osh_ant_spacing_value);
        }

        System.out.println("Settings Equal: " + (!currentSettings.equals(oldSettings)));

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
