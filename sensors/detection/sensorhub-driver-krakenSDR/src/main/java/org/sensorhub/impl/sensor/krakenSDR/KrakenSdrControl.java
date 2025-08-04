package org.sensorhub.impl.sensor.krakenSDR;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.opengis.swe.v20.*;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.swe.SWEHelper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class KrakenSdrControl extends AbstractSensorControl<KrakenSdrSensor> {

    private DataRecord commandDataStruct;

    // CONSTRUCTOR
    public KrakenSdrControl(KrakenSdrSensor krakenSDRSensor) {
        super("KrakenSDR Control", krakenSDRSensor);
    }

    // INITIALIZE CONTROL
    public void doInit() {

        // GET INTIIAL FROM CURRENT SETTINGS
        JsonObject currentSettings = retrieveJSONObj(parentSensor.OUTPUT_URL + "/settings.json");

        System.out.println("Control Initializing");
        System.out.println("freq: " + currentSettings.get("center_freq").getAsDouble());

        SWEHelper fac = new SWEHelper();


        // Create data structure for the typical values that would need to be updated
        commandDataStruct = fac.createRecord()
                .name(getName())
                .updatable(true)
                .definition(SWEHelper.getPropertyUri("krakenSdrControls"))
                .label("Update KrakenSDR config")
                .description("Update the settings.json")
                    .addField("center_freq", fac.createQuantity()
                            .label("Center Frequency")
                            .uom("MHz")
                            .description("Input the Center Frequency in MHZ")
                            .definition(SWEHelper.getPropertyUri("center_freq"))
                    )
                    .addField("uniform_gain", fac.createQuantity()
                            .label("Receiver Gain")
                            .uom("dB")
                            .description("Input the Receiver Gain in dB")
                            .definition(SWEHelper.getPropertyUri("uniform_gain"))
                    )
                    .addField("ant_spacing_meters", fac.createQuantity()
                            .label("Antenna Array Radius")
                            .uom("m")
                            .description("Input the Antenna Array Radius in Meters")
                            .definition(SWEHelper.getPropertyUri("ant_spacing_meters"))
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

        Quantity osh_gain = (Quantity) commandData.getField("uniform_gain");
        double osh_gain_value = osh_gain.getValue();

        Quantity osh_ant_spacing = (Quantity) commandData.getField("ant_spacing_meters");
        double osh_ant_spacing_value = osh_ant_spacing.getValue();

        // RETRIEVE CURRENT JSON SETTINGS AS A JSON OBJECT
        JsonObject currentSettings = retrieveJSONObj(parentSensor.OUTPUT_URL + "/settings.json");

        currentSettings.addProperty("center_freq", osh_freq_value);
        currentSettings.addProperty("uniform_gain", osh_gain_value);
        currentSettings.addProperty("ant_spacing_meters", osh_ant_spacing_value);

        replaceOldSettings(currentSettings);





        return true;
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandDataStruct;
    }


    public JsonObject retrieveJSONObj(String httpURL ){
        try {
            URL url = new URL(httpURL);

            System.out.println(url);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // Read the JSON response into a String
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder jsonString = new StringBuilder();
            String line;

            while ((line = in.readLine()) != null) {
                jsonString.append(line);
            }

            in.close();
            conn.disconnect();

            // Parse JSON string into a JSON object
            return JsonParser.parseString(jsonString.toString()).getAsJsonObject();

        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (ProtocolException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    public void replaceOldSettings(JsonObject json){
        try {
            // Start curl command to upload file content from stdin
            ProcessBuilder pb = new ProcessBuilder(
                    "curl",
                    "-F", "path=@-;filename=settings.json", // read from stdin, send with filename
                    "http://192.168.50.186:8081/upload?path=/"
            );

            Process process = pb.start();

            // Send the JSON data to curl via stdin
            try (OutputStream os = process.getOutputStream()) {
                os.write(json.toString().getBytes());
                os.flush();
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("✅ settings.json uploaded via curl.");
            } else {
                System.out.println("❌ curl upload failed. Exit code: " + exitCode);
            }

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error uploading settings.json via curl", e);
        }
    }

}
