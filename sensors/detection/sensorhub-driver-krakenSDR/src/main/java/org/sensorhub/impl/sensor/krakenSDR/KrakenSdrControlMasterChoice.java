package org.sensorhub.impl.sensor.krakenSDR;

import com.google.gson.JsonObject;
import net.opengis.swe.v20.*;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.swe.SWEHelper;

import java.net.HttpURLConnection;

public class KrakenSdrControlMasterChoice extends AbstractSensorControl<KrakenSdrSensor> {
    private DataChoice commandDataStruct;
    KrakenUTILITY util = parentSensor.util;
    HttpURLConnection conn;

    // CONSTRUCTOR
    public KrakenSdrControlMasterChoice(KrakenSdrSensor krakenSDRSensor) {
        super("KrakenSDR Master Control", krakenSDRSensor);
    }

    // INITIALIZE CONTROL
    public void doInit(JsonObject initialSettings){
        SWEHelper fac = new SWEHelper();
        // The Master Control Data Structure is a Choice of individual controls for the KrakenSDR
        commandDataStruct = fac.createChoice()
                .name(getName())
                .updatable(true)
                .definition(SWEHelper.getPropertyUri("krakenMasterControl"))
                .label("KrakenSDR Control")
                .description("Update the settings.json for the KrakenSDR by selecting a specific control choice")
                .build();
        DataRecord ReceiverControl = fac.createRecord()
                .addField("receiver_control", fac.createRecord()
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
                ).build();
        DataRecord DoaControl = fac.createRecord()
                .addField("DoA_control", fac.createRecord()
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
                                .value(initialSettings.get("ant_spacing_meters").getAsDouble()))
                ).build();
        DataRecord StationControl = fac.createRecord()
                .addField("station_control", fac.createRecord()
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
                                .value(initialSettings.get("longitude").getAsString()))
                )
                .build();
        commandDataStruct.addItem("Receiver Settings", ReceiverControl);
        commandDataStruct.addItem("DoA Settings", DoaControl);
        commandDataStruct.addItem("Station Settings", StationControl);

    }

    @Override
    protected boolean execCommand(DataBlock cmdData) throws CommandException
    {

        // RETRIEVE INPUTS FROM ADMIN PANEL CONTROL
        DataChoice commandData = commandDataStruct.copy();
        commandData.setData(cmdData);
        // GET SELECTED ITEM FROM MASTER CONTROL DATA CHOICE
        DataComponent SelectedChoice = commandData.getSelectedItem();
        DataRecord SelectedRecord = (DataRecord) SelectedChoice.getComponent(0);

        // RETRIEVE CURRENT JSON SETTINGS AS A JSON OBJECT
        JsonObject currentSettings = null;
        try {
            currentSettings = util.retrieveJSONFromAddr(parentSensor.settings_URL);
        } catch (SensorHubException e) {
            throw new RuntimeException(e);
        }

        // UPDATE CURRENT JSON SETTINGS WITH UPDATED CONTROL SETTINGS BASED ON WHICH IS SELECTED
        switch (SelectedChoice.getName()){
            case "Receiver Settings":
                // UPDATE FREQUENCY IF UPDATED IN ADMIN PANEL
                Quantity osh_freq = (Quantity) SelectedRecord.getField("center_freq");
                double osh_freq_value = osh_freq.getValue();
                if(osh_freq_value != 0.0){
                    currentSettings.addProperty("center_freq", osh_freq_value);
                }
                // UPDATE GAIN IF UPDATED IN ADMIN PANEL
                Category osh_gain = (Category) SelectedRecord.getField("uniform_gain");
                String osh_gain_value = osh_gain.getValue();
                if(osh_gain_value != null){
                    currentSettings.addProperty("uniform_gain", Double.parseDouble(osh_gain_value));
                }

                break;
            case "DoA Settings":
                // UPDATE ANTENNA ARRANGEMNENT IF UPDATED IN ADMIN PANEL
                Category osh_ant_arrangement = (Category) SelectedRecord.getField("ant_arrangement");
                String osh_ant_arrangement_value = osh_ant_arrangement.getValue();
                if(osh_ant_arrangement_value != null){
                    currentSettings.addProperty("ant_arrangement", osh_ant_arrangement_value);
                }
                // UPDATE ANTENNA SPACING IF UPDATED IN ADMIN PANEL
                Quantity osh_ant_spacing = (Quantity) SelectedRecord.getField("ant_spacing_meters");
                double osh_ant_spacing_value = osh_ant_spacing.getValue();
                if(osh_ant_spacing_value != 0.0){
                    currentSettings.addProperty("ant_spacing_meters", osh_ant_spacing_value);
                }
                break;
            case "Station Settings":
                Text osh_station_id = (Text) SelectedRecord.getField("station_id");
                String osh_station_id_value = osh_station_id.getValue();
                if(osh_station_id_value != null){
                    currentSettings.addProperty("station_id", osh_station_id_value);
                }

                // UPDATE LOCATION SETTINGS IF GPS MODE OR STATIC MODE IS SELECTED
                Category osh_location_source = (Category) SelectedRecord.getField("location_source");
                String osh_location_source_value = osh_location_source.getValue();
                if(osh_location_source_value != null && osh_location_source_value.equals("GPS")){
                    currentSettings.addProperty("location_source", "gpsd");
                } else if (osh_location_source_value != null && osh_location_source_value.equals("Static")) {
                    currentSettings.addProperty("location_source", "Static");

                    Text osh_latitude = (Text) SelectedRecord.getField("latitude");
                    String osh_latitude_value = osh_latitude.getValue();
                    System.out.println(osh_latitude_value);
                    currentSettings.addProperty("latitude", Double.parseDouble(osh_latitude_value));

                    Text osh_longitude = (Text) SelectedRecord.getField("longitude");
                    String osh_longitude_value = osh_longitude.getValue();
                    System.out.println(osh_longitude_value);
                    currentSettings.addProperty("longitude",Double.parseDouble(osh_longitude_value));
                }

                break;
            default:
                break;
        }


        // REPLACE SETTINGS ON KRAKENSDR BASED ON CONTROL UPDATED ABOVE
        util.replaceOldSettings(parentSensor.OUTPUT_URL , currentSettings);

        return true;
    }


    @Override
    public DataComponent getCommandDescription() {
        return commandDataStruct;
    }





}
