package org.sensorhub.impl.sensor.krakenSDR;

import com.google.gson.JsonObject;
import net.opengis.swe.v20.*;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorControl;

import org.vast.swe.SWEHelper;

import java.net.HttpURLConnection;

public class KrakenSdrRecieverControls extends AbstractSensorControl<KrakenSdrSensor> {
    private DataRecord commandDataStruct;
    KrakenUTILITY util = parentSensor.util;
    HttpURLConnection conn;

    // CONSTRUCTOR
    public KrakenSdrRecieverControls(KrakenSdrSensor krakenSDRSensor) {
        super("KrakenSDR Control", krakenSDRSensor);
    }

    // INITIALIZE CONTROL
    public void doInit(JsonObject initialSettings){
        SWEHelper fac = new SWEHelper();
        // Create data structure for the typical values that would need to be updated
        commandDataStruct = fac.createRecord()
                .name(getName())
                .updatable(true)
                .definition(SWEHelper.getPropertyUri("krakenSdrControls"))
                .label("RF Receiver Configuration Controls")
                .description("Update the settings.json")
                .addField("center_freq", fac.createQuantity()
                        .label("Center Frequency")
                        .uom("MHz")
                        .description("Input the Center Frequency in MHZ")
                        .definition(SWEHelper.getPropertyUri("center_freq"))
                        .value(initialSettings.get("center_freq").getAsDouble())
                )
                .addField("uniform_gain", fac.createCategory()
                        .label("Receiver Gain (dB)")
                        .description("Input the Receiver Gain in dB")
                        .definition(SWEHelper.getPropertyUri("uniform_gain"))
                        .addAllowedValues("0", "0.9", "1.4", "2.7", "3.7", "7.7", "8.7", "12.5", "14.4", "15.7", "16.6", "19.7", "20.7", "22.9", "25.4", "28.0", "29.7", "32.8", "33.8", "36.4", "37.2", "38.6", "40.2", "42.1", "43.4", "43.9", "44.5", "48.0", "49.6")
                        .value("2.7")
                )
                .build();

    }

    @Override
    protected boolean execCommand(DataBlock cmdData) throws CommandException
    {
        // RETRIEVE INPUTS FROM ADMIN PANEL CONTROL
        DataRecord commandData = commandDataStruct.copy();
        commandData.setData(cmdData);

        Quantity osh_freq = (Quantity) commandData.getField("center_freq");
        double osh_freq_value = osh_freq.getValue();
//        System.out.println(osh_freq_value);

        Category osh_gain = (Category) commandData.getField("uniform_gain");
        double osh_gain_value = Double.parseDouble(osh_gain.getValue());
//        System.out.println(osh_gain_value);

        Quantity osh_ant_spacing = (Quantity) commandData.getField("ant_spacing_meters");
        double osh_ant_spacing_value = osh_ant_spacing.getValue();
//        System.out.println(osh_ant_spacing_value);

        // RETRIEVE CURRENT JSON SETTINGS AS A JSON OBJECT
        JsonObject currentSettings = null;
        try {
            currentSettings = util.retrieveJSONFromAddr(parentSensor.settings_URL);

            currentSettings.addProperty("center_freq", osh_freq_value);
            currentSettings.addProperty("uniform_gain", osh_gain_value);
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
