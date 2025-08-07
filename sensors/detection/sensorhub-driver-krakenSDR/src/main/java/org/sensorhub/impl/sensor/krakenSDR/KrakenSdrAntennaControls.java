package org.sensorhub.impl.sensor.krakenSDR;

import com.google.gson.JsonObject;
import net.opengis.swe.v20.*;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.swe.SWEHelper;

import java.net.HttpURLConnection;

public class KrakenSdrAntennaControls extends AbstractSensorControl<KrakenSdrSensor> {
    private DataRecord commandDataStruct;
    KrakenUTILITY util = parentSensor.util;
    HttpURLConnection conn;

    // CONSTRUCTOR
    public KrakenSdrAntennaControls(KrakenSdrSensor krakenSDRSensor) {
        super("KrakenSDR Control", krakenSDRSensor);
    }

    // INITIALIZE CONTROL
    public void doInit(JsonObject initialSettings){
        SWEHelper fac = new SWEHelper();
        // Create data structure for the typical values that would need to be updated
        commandDataStruct = fac.createRecord()
                .name(getName())
                .updatable(true)
                .definition(SWEHelper.getPropertyUri("krakenSDRAntennaControls"))
                .label("Antenna Configuration Controls")
                .description("Update the settings.json")
                .addField("ant_arrangement", fac.createCategory()
                        .label("Antenna Arrangement")
                        .description("The Arrangement must be UCA or ULA")
                        .definition(SWEHelper.getPropertyUri("ant_arrangement"))
                        .addAllowedValues("UCA", "ULA")
                )
                .addField("ant_spacing_meters", fac.createQuantity()
                        .label("Antenna Array Radius")
                        .uom("m")
                        .description("Input the Antenna Array Radius in Meters")
                        .definition(SWEHelper.getPropertyUri("ant_spacing_meters"))
                        .value(initialSettings.get("ant_spacing_meters").getAsDouble())
                )
                .build();

    }

    @Override
    protected boolean execCommand(DataBlock cmdData) throws CommandException
    {
        // RETRIEVE INPUTS FROM ADMIN PANEL CONTROL
        DataRecord commandData = commandDataStruct.copy();
        commandData.setData(cmdData);

        Category osh_ant_arrangement = (Category) commandData.getField("ant_arrangement");
        String osh_ant_arrangement_value = osh_ant_arrangement.getValue();

        Quantity osh_ant_spacing = (Quantity) commandData.getField("ant_spacing_meters");
        double osh_ant_spacing_value = osh_ant_spacing.getValue();

        // RETRIEVE CURRENT JSON SETTINGS AS A JSON OBJECT
        JsonObject currentSettings = null;
        try {
            currentSettings = util.retrieveJSONFromAddr(parentSensor.settings_URL);

            currentSettings.addProperty("ant_arrangement", osh_ant_arrangement_value);
            currentSettings.addProperty("ant_spacing_meters", osh_ant_spacing_value);

            util.replaceOldSettings(parentSensor.OUTPUT_URL , currentSettings);

        } catch (SensorHubException e) {
            throw new RuntimeException(e);
        }

        return true;
    }



    @Override
    public DataComponent getCommandDescription() {
        return commandDataStruct;
    }





}
