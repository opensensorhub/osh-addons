package org.sensorhub.impl.sensor.krakenSDR;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.krakenSDR.KrakenSdrSensor;



public class KrakenUTILITY {
    private  KrakenSdrSensor parentSensor;

    public KrakenUTILITY(KrakenSdrSensor parentSensor) {
        this.parentSensor = parentSensor;

    }

    public HttpURLConnection createKrakenConnection(String httpURL) throws SensorHubException {
        try{
            URL url = new URL(httpURL);

            // Configure connection parameters
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new SensorHubException("Server returned non-OK status: " + responseCode + " from URL: " + httpURL);
            }
            return conn;
        } catch (ProtocolException | MalformedURLException e) {
            throw new SensorHubException("Invalid URL format: " + httpURL, e);
        } catch (IOException e) {
            throw new SensorHubException("Failed to connect to: " + httpURL, e);
        }
    }

    public JsonObject retrieveJSONFromAddr(String httpAddress) throws SensorHubException {
        try {
            HttpURLConnection conn = createKrakenConnection(httpAddress);

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

        } catch (ProtocolException | MalformedURLException e) {
            parentSensor.doStop();
            throw new SensorHubException("Invalid URL format: " + httpAddress, e);
        } catch (IOException e) {
            parentSensor.doStop();
            throw new SensorHubException("Failed to connect to: " + httpAddress, e);
        }
    }

    public void replaceOldSettings(String OUTPUT_URL,JsonObject json){
        try {
            // Start curl command to upload file content from stdin
            ProcessBuilder pb = new ProcessBuilder(
                    "curl",
                    "-F", "path=@-;filename=settings.json", // read from stdin, send with filename
                    OUTPUT_URL + "/upload?path=/"
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
