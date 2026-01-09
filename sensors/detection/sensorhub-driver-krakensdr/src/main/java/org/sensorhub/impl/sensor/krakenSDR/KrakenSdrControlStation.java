package org.sensorhub.impl.sensor.krakenSDR;

import com.google.gson.JsonObject;
import net.opengis.swe.v20.*;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.swe.SWEHelper;

import java.net.HttpURLConnection;

public class KrakenSdrControlStation extends AbstractSensorControl<KrakenSdrSensor> {
    private DataRecord commandDataStruct;
    KrakenUTILITY util = parentSensor.util;
    HttpURLConnection conn;

    // CONSTRUCTOR
    public KrakenSdrControlStation(KrakenSdrSensor krakenSDRSensor) {
        super("Station Control", krakenSDRSensor);
    }

    // INITIALIZE CONTROL
    public void doInit(JsonObject initialSettings){
        SWEHelper fac = new SWEHelper();
        // The Master Control Data Structure is a Choice of individual controls for the KrakenSDR
        commandDataStruct = fac.createRecord()
                .name("station_control")
                .label("Station Configuration")
                .description("Data Record for the Station Configuration")
                .definition(SWEHelper.getPropertyUri("station_control"))
                .addField("station_id", fac.createText()
                        .label("Station ID")
                        .description("ID provided for the physical KrakenSDR")
                        .definition(SWEHelper.getPropertyUri("station_id"))
                        .value((initialSettings.get("station_id") != null) ? initialSettings.get("station_id").getAsString() : "NoName" ))
                .addField("location_source", fac.createCategory()
                        .label("Location Source")
                        .description("Current Location Source for the Kraken Station")
                        .definition(SWEHelper.getPropertyUri("location_source"))
                        .addAllowedValues("GPS", "Static")
                        .value(initialSettings.get("location_source").getAsString()))
                .addField("latitude", fac.createText()
                        .label("Latitude")
                        .description("Latitude when station is Static")
                        .definition(SWEHelper.getPropertyUri("latitude"))
                        .value(initialSettings.get("latitude").getAsString()))
                .addField("longitude", fac.createText()
                        .label("Longitude")
                        .description("Longitude when station is Static")
                        .definition(SWEHelper.getPropertyUri("longitude"))
                        .value(initialSettings.get("longitude").getAsString())
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

        // UPDATE CURRENT JSON SETTINGS WITH UPDATED CONTROL SETTINGS
        // UPDATE Name of KrakenSDR IF UPDATED IN ADMIN PANEL
        Text osh_station_id = (Text) commandData.getField("station_id");
        String osh_station_id_value = osh_station_id.getValue();
        if(osh_station_id_value != null){
            currentSettings.addProperty("station_id", osh_station_id_value);
        }

        // UPDATE LOCATION SETTINGS IF GPS MODE OR STATIC MODE IS SELECTED
        Category osh_location_source = (Category) commandData.getField("location_source");
        String osh_location_source_value = osh_location_source.getValue();
        if(osh_location_source_value != null && osh_location_source_value.equals("GPS")){
            currentSettings.addProperty("location_source", "gpsd");
        } else if (osh_location_source_value != null && osh_location_source_value.equals("Static")) {
            // IF STATIC, ALSO ADD LATITUDE AND LONGITUDE
            currentSettings.addProperty("location_source", "Static");

            Text osh_latitude = (Text) commandData.getField("latitude");
            String osh_latitude_value = osh_latitude.getValue();
            System.out.println(osh_latitude_value);
            currentSettings.addProperty("latitude", Double.parseDouble(osh_latitude_value));

            Text osh_longitude = (Text) commandData.getField("longitude");
            String osh_longitude_value = osh_longitude.getValue();
            System.out.println(osh_longitude_value);
            currentSettings.addProperty("longitude",Double.parseDouble(osh_longitude_value));
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
