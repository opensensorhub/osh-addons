package org.sensorhub.impl.sensor.krakensdr;

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
    public void doInit(){
        SWEHelper fac = new SWEHelper();
        // The Master Control Data Structure is a Choice of individual controls for the KrakenSDR
        commandDataStruct = fac.createRecord()
                .updatable(true)
                .name("receiver_control")
                .label("RF Receiver Configuration")
                .description("Data Record for the RF Receiver Configuration")
                .definition(SWEHelper.getPropertyUri("receiver_control"))
                .addField("centerFreq", fac.createQuantity()
                        .uomCode("MHz")
                        .label("Center Frequency")
                        .description("The transmission frequency of the event in MegaHertz")
                        .definition(SWEHelper.getPropertyUri("frequency"))
                )
                .addField("uniformGain", fac.createCategory()
                        .label("Receiver Gain (dB)")
                        .description("Input the Receiver Gain in dB")
                        .definition(SWEHelper.getPropertyUri("UniformGain"))
                        .addAllowedValues("0", "0.9", "1.4", "2.7", "3.7", "7.7", "8.7", "12.5", "14.4", "15.7", "16.6", "19.7", "20.7", "22.9", "25.4", "28.0", "29.7", "32.8", "33.8", "36.4", "37.2", "38.6", "40.2", "42.1", "43.4", "43.9", "44.5", "48.0", "49.6")
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
            currentSettings = util.retrieveJSONFromAddr(parentSensor.SETTINGS_URL);
            oldSettings = currentSettings.deepCopy();
        } catch (SensorHubException e) {
            getLogger().debug("Failed to retrieve current json settings from kraken: ", e);
        }

        // UPDATE CURRENT JSON SETTINGS WITH UPDATED CONTROL SETTINGS BASED ON WHICH IS SELECTED
        // UPDATE FREQUENCY IF UPDATED IN ADMIN PANEL
        Quantity oshFrequency = (Quantity) commandData.getField("center_freq");
        double oshFrequencyValue = oshFrequency.getValue();
        if(oshFrequencyValue != 0.0){
            currentSettings.addProperty("center_freq", oshFrequencyValue);
        }

        // UPDATE GAIN IF UPDATED IN ADMIN PANEL
        Category oshGain = (Category) commandData.getField("uniform_gain");
        String oshGainValue = oshGain.getValue();
        if(oshGainValue != null){
            currentSettings.addProperty("uniform_gain", Double.parseDouble(oshGainValue));
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
