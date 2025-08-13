package org.sensorhub.impl.sensor.krakenSDR;

import com.google.gson.JsonObject;
import net.opengis.swe.v20.*;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.swe.SWEHelper;

import java.net.HttpURLConnection;

public class KrakenSdrControlReceiver extends AbstractSensorControl<KrakenSdrSensor> {
    private DataRecord commandDataStruct;
    KrakenUTILITY util = parentSensor.util;
    HttpURLConnection conn;

    // CONSTRUCTOR
    public KrakenSdrControlReceiver(KrakenSdrSensor krakenSDRSensor) {
        super("Receiver Control", krakenSDRSensor);
    }

    // INITIALIZE CONTROL
    public void doInit(JsonObject initialSettings){
        SWEHelper fac = new SWEHelper();
        // The Master Control Data Structure is a Choice of individual controls for the KrakenSDR
        commandDataStruct = fac.createRecord()
                .updatable(true)
                .name("receiver_control")
                .label("RF Receiver Configuration")
                .description("Data Record for the RF Receiver Configuration")
                .definition(SWEHelper.getPropertyUri("receiver_control"))
                .addField("center_freq", fac.createQuantity()
                        .uomCode("MHz")
                        .label("Center Frequency")
                        .description("The transmission frequency of the event in MegaHertz")
                        .definition(SWEHelper.getPropertyUri("frequency"))
                        .value(initialSettings.get("center_freq").getAsDouble())
                )
                .addField("uniform_gain", fac.createCategory()
                        .label("Receiver Gain (dB)")
                        .description("Input the Receiver Gain in dB")
                        .definition(SWEHelper.getPropertyUri("uniform_gain"))
                        .addAllowedValues("0", "0.9", "1.4", "2.7", "3.7", "7.7", "8.7", "12.5", "14.4", "15.7", "16.6", "19.7", "20.7", "22.9", "25.4", "28.0", "29.7", "32.8", "33.8", "36.4", "37.2", "38.6", "40.2", "42.1", "43.4", "43.9", "44.5", "48.0", "49.6")
                        .value(initialSettings.get("uniform_gain").getAsString())
                )
                .build();
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

        // UPDATE CURRENT JSON SETTINGS WITH UPDATED CONTROL SETTINGS BASED ON WHICH IS SELECTED
        // UPDATE FREQUENCY IF UPDATED IN ADMIN PANEL
        Quantity osh_freq = (Quantity) commandData.getField("center_freq");
        double osh_freq_value = osh_freq.getValue();
        if(osh_freq_value != 0.0){
            currentSettings.addProperty("center_freq", osh_freq_value);
        }

        // UPDATE GAIN IF UPDATED IN ADMIN PANEL
        Category osh_gain = (Category) commandData.getField("uniform_gain");
        String osh_gain_value = osh_gain.getValue();;
        if(osh_gain_value != null){
            currentSettings.addProperty("uniform_gain", Double.parseDouble(osh_gain_value));
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
