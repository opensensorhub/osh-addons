package org.sensorhub.impl.sensor.krakensdr.controls;

import com.google.gson.JsonObject;
import net.opengis.swe.v20.*;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.sensorhub.impl.sensor.krakensdr.KrakenSdrConstants;
import org.sensorhub.impl.sensor.krakensdr.KrakenSdrDriver;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

public class KrakenSdrControlStation extends AbstractSensorControl<KrakenSdrDriver> {
    private DataRecord commandDataStruct;
    private static final String STATION_ID = "stationId";
    private static final String LOCATION_SRC = "locationSource" ;
    private static final String LAT = "latitude";
    private static final String LON = "longitude";
    private static final String HEADING = "heading";

    // CONSTRUCTOR
    public KrakenSdrControlStation(KrakenSdrDriver krakenSdrDriver) {
        super("stationControl", krakenSdrDriver);
    }

    // INITIALIZE CONTROL
    public void doInit(){
        SWEHelper fac = new SWEHelper();
        // The Master Control Data Structure is a Choice of individual controls for the KrakenSDR
        commandDataStruct = fac.createRecord()
                .name("stationControl")
                .label("Station Configuration Control")
                .description("Data Record for the Station Configuration")
                .definition(SWEHelper.getPropertyUri("StationControl"))
                .addField(STATION_ID, fac.createText()
                        .label("Station ID")
                        .description("ID provided for the physical KrakenSDR")
                        .definition(SWEHelper.getPropertyUri("StationId"))
                )
                .addField(LOCATION_SRC, fac.createCategory()
                        .label("Location Source")
                        .description("Current Location Source for the Kraken Station")
                        .definition(SWEHelper.getPropertyUri("LocationSource"))
                        .addAllowedValues("GPS", "Static")
                        .value("Static")
                )
                .addField(LAT, fac.createText()
                        .label("Latitude")
                        .description("Latitude when station is Static (-90.0 to 90.0)")
                        .definition(GeoPosHelper.DEF_LATITUDE_GEODETIC)
                )
                .addField(LON, fac.createText()
                        .label("Longitude")
                        .description("Longitude when station is Static (-180.0 to 180.0)")
                        .definition(GeoPosHelper.DEF_LONGITUDE)
                )
                .addField(HEADING, fac.createText()
                        .label("Heading")
                        .description("Heading when station is Static (0.0 to 360.0)")
                        .definition(GeoPosHelper.DEF_HEADING_TRUE)
                )
                .build();

    }

    @Override
    protected boolean execCommand(DataBlock cmdData) throws CommandException
    {

        // RETRIEVE INPUTS FROM ADMIN PANEL CONTROL
        DataRecord commandData = commandDataStruct.copy();
        commandData.setData(cmdData);

        JsonObject data = new JsonObject();

        // UPDATE Name of KrakenSDR IF UPDATED IN ADMIN PANEL
        Text oshStationId = (Text) commandData.getField(STATION_ID);
        String oshStationIdValue = oshStationId.getValue();
        if(oshStationIdValue != null){
            data.addProperty(KrakenSdrConstants.STATION_ID, oshStationIdValue);
        }

        // UPDATE LOCATION SETTINGS IF GPS MODE OR STATIC MODE IS SELECTED
        Category oshLocationSource = (Category) commandData.getField(LOCATION_SRC);
        String oshLocationSourceValue = oshLocationSource.getValue();
        if(oshLocationSourceValue != null && oshLocationSourceValue.equals("GPS")){
            data.addProperty(KrakenSdrConstants.LOCATION_SRC, "gpsd");
        } else if (oshLocationSourceValue != null && oshLocationSourceValue.equals("Static")) {
            // IF STATIC, ALSO ADD LATITUDE AND LONGITUDE AND HEADING
            data.addProperty(KrakenSdrConstants.LOCATION_SRC, "Static");

            Text oshLatitude = (Text) commandData.getField(LAT);
            float oshLatitudeValue = Float.parseFloat(oshLatitude.getValue());
            data.addProperty(KrakenSdrConstants.LAT, oshLatitudeValue);

            Text oshLongitude = (Text) commandData.getField(LON);
            float oshLongitudeValue = Float.parseFloat(oshLongitude.getValue());
            data.addProperty(KrakenSdrConstants.LON, oshLongitudeValue);

            Text oshHeading = (Text) commandData.getField(HEADING);
            float oshHeadingValue = Float.parseFloat(oshHeading.getValue());
            data.addProperty(KrakenSdrConstants.HEADING, oshHeadingValue);
        }

        parentSensor.updateKrakenSettings(data);
        return true;
    }


    @Override
    public DataComponent getCommandDescription() {
        return commandDataStruct;
    }


}
