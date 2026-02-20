package org.sensorhub.impl.sensor.krakensdr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.sensorhub.api.common.SensorHubException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;

public class KrakenUTILITY {
    private final String settingsUrl;
    private final String doaCsvUrl;
    private final String doaXmlUrl;
    private final KrakenSdrSensor sensor;

    private static final int CONNECT_TIMEOUT_MS = 2000;
    private static final int READ_TIMEOUT_MS = 2000;


    public KrakenUTILITY(KrakenSdrSensor krakenSdrSensor) {
        this.sensor = krakenSdrSensor;
        this.settingsUrl = krakenSdrSensor.SETTINGS_URL;
        this.doaCsvUrl = krakenSdrSensor.DOA_CSV_URL;
        this.doaXmlUrl = krakenSdrSensor.DOA_XML_URL;
    }

    public String connectAndGetDataAsString(String httpURL) throws IOException {
        // Configure connection parameters
        HttpURLConnection conn = (HttpURLConnection) new URL(httpURL).openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestMethod("GET");

        // Get Data from connection as a string
        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                )
        ) {
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }


    public JsonObject getSettings() throws SensorHubException {
        try {
            String json = connectAndGetDataAsString(settingsUrl);
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e){
            sensor.getLogger().error("Failed to retrieve Kraken settings", e);
            throw new SensorHubException("Failed to retrieve Kraken settings", e);
        }
    }

    public JsonObject getDoA() throws SensorHubException {
        try {
            // Attempt XML File First
            String xml = connectAndGetDataAsString(doaXmlUrl);
            return parseDoaXml(xml);
        } catch (Exception xmlError) {
            sensor.getLogger().debug("XML DoA unavailable at {}, falling back to CSV", doaXmlUrl, xmlError);
        }
        try {
            String csv = connectAndGetDataAsString(doaCsvUrl);
            return parseDoACsv(csv);
        } catch (Exception csvError) {
            sensor.getLogger().error("Failed to retrieve Kraken DoA data at {}", doaCsvUrl, csvError);
            throw new SensorHubException("Failed to retrieve Kraken DoA data", csvError);
        }
    }

    private JsonObject parseDoaXml(String xmlString) throws Exception{
        Document doc = DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(new java.io.ByteArrayInputStream(xmlString.getBytes()));

        Element root = doc.getDocumentElement();
        Element location = (Element) root.getElementsByTagName("LOCATION").item(0);

        JsonObject json = new JsonObject();
        json.addProperty("time", getTag(root, "TIME"));
        json.addProperty("doa", getTag(root, "DOA"));
        json.addProperty("confidence", getTag(root, "CONF"));
        json.addProperty("rssi", getTag(root, "PWR"));  // CSV calls it rssi
        json.addProperty("frequency", getTag(root, "FREQUENCY"));
        json.addProperty("stationId", getTag(root, "STATION_ID"));
        json.addProperty("latency", getTag(root, "LATENCY"));
        // Location fields
        json.addProperty("latitude", getTag(location, "LATITUDE"));
        json.addProperty("longitude", getTag(location, "LONGITUDE"));
        json.addProperty("heading", getTag(location, "HEADING"));

        return json;
    }

    private JsonObject parseDoACsv(String csv) {
        String[] fields = csv.trim().split(",");

        if (fields.length < 13) {
            throw new IllegalArgumentException("Invalid DoA CSV format");
        }

        JsonObject json = new JsonObject();

        json.addProperty("time", fields[0]);  //
        json.addProperty("doa", fields[1]); //
        json.addProperty("confidence", fields[2]); //
        json.addProperty("rssi", fields[3]);
        json.addProperty("frequency", fields[4]); //
        // json.addProperty("antennaArrangement", fields[5]); not in XML
        json.addProperty("stationId", fields[7]); //
        json.addProperty("latency", fields[6]); //
        json.addProperty("latitude", fields[8]); //
        json.addProperty("longitude", fields[9]); //
        json.addProperty("heading", fields[10]); //

        return json;
    }

    private String getTag(Element root, String tag) {
        return root.getElementsByTagName(tag).item(0).getTextContent();
    }


    public void replaceOldSettings(String OUTPUT_URL, JsonObject json){
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
                sensor.getLogger().info("✅ settings.json uploaded via curl.");
            } else {
                sensor.getLogger().info("❌ curl upload failed. Exit code: {}", exitCode);

            }
        } catch (InterruptedException e){
            Thread.currentThread().interrupt(); // Restore interrupt flag
            sensor.getLogger().debug("Curl upload interrupted", e);
        } catch (IOException e){
            sensor.getLogger().debug("Error uploading settings.json via curl", e);
        }
    }

    public void uploadSettings(JsonObject json) throws SensorHubException {
        String boundary = "----KrakenBoundary" + System.currentTimeMillis();
        String lineFeed = "\r\n";

        HttpURLConnection conn = null;

        try {
            URL url = new URL(sensor.OUTPUT_URL + "/upload?path=/");
            conn = (HttpURLConnection) url.openConnection();

            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setUseCaches(false);

            conn.setRequestProperty(
                    "Content-Type",
                    "multipart/form-data; boundary=" + boundary
            );

            try (OutputStream out = conn.getOutputStream()) {

                out.write(("--" + boundary + lineFeed).getBytes());
                out.write(("Content-Disposition: form-data; name=\"path\"; filename=\"settings.json\"" + lineFeed).getBytes());
                out.write(("Content-Type: application/json" + lineFeed).getBytes());
                out.write(lineFeed.getBytes());

                out.write(json.toString().getBytes());

                out.write(lineFeed.getBytes());
                out.write(("--" + boundary + "--" + lineFeed).getBytes());
                out.flush();
            }

            int response = conn.getResponseCode();
            if (response != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error " + response);
            }

            sensor.getLogger().info("✅ Kraken settings.json uploaded successfully");

        } catch (Exception e) {
            sensor.getLogger().error("❌ Kraken settings upload failed. Most likely a permission error or miniserve is not set up on kraken device", e);
            throw new SensorHubException("Kraken settings upload failed. Most likely a permission error or miniserve is not set up on kraken device", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

}
