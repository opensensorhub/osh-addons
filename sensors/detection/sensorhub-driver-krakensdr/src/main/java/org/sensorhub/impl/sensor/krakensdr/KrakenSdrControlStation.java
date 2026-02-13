package org.sensorhub.impl.sensor.krakensdr;

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
                .addField("stationId", fac.createText()
                        .label("Station ID")
                        .description("ID provided for the physical KrakenSDR")
                        .definition(SWEHelper.getPropertyUri("StationId"))
                )
                .addField("locationSource", fac.createCategory()
                        .label("Location Source")
                        .description("Current Location Source for the Kraken Station")
                        .definition(SWEHelper.getPropertyUri("LocationSource"))
                        .addAllowedValues("GPS", "Static")
                )
                .addField("latitude", fac.createText()
                        .label("Latitude")
                        .description("Latitude when station is Static")
                        .definition(SWEHelper.getPropertyUri("latitude"))
                )
                .addField("longitude", fac.createText()
                        .label("Longitude")
                        .description("Longitude when station is Static")
                        .definition(SWEHelper.getPropertyUri("longitude"))
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

        // UPDATE CURRENT JSON SETTINGS WITH UPDATED CONTROL SETTINGS
        // UPDATE Name of KrakenSDR IF UPDATED IN ADMIN PANEL
        Text oshStationId = (Text) commandData.getField("station_id");
        String oshStationIdValue = oshStationId.getValue();
        if(oshStationIdValue != null){
            currentSettings.addProperty("station_id", oshStationIdValue);
        }

        // UPDATE LOCATION SETTINGS IF GPS MODE OR STATIC MODE IS SELECTED
        Category oshLocationSource = (Category) commandData.getField("locationSource");
        String oshLocationSourceValue = oshLocationSource.getValue();
        if(oshLocationSourceValue != null && oshLocationSourceValue.equals("GPS")){
            currentSettings.addProperty("location_source", "gpsd");
        } else if (oshLocationSourceValue != null && oshLocationSourceValue.equals("Static")) {
            // IF STATIC, ALSO ADD LATITUDE AND LONGITUDE
            currentSettings.addProperty("location_source", "Static");

            Text oshLatitude = (Text) commandData.getField("latitude");
            String oshLatitudeValue = oshLatitude.getValue();
            currentSettings.addProperty("latitude", Double.parseDouble(oshLatitudeValue));

            Text oshLongitude = (Text) commandData.getField("longitude");
            String oshLongitudeValue = oshLongitude.getValue();
            currentSettings.addProperty("longitude",Double.parseDouble(oshLongitudeValue));
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
