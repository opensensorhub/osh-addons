/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2020 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sta;

import static org.junit.Assert.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.jglue.fluentjson.JsonBuilderFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.service.HttpServer;
import org.sensorhub.impl.service.HttpServerConfig;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.fraunhofer.iosb.ilt.frostserver.persistence.PersistenceManagerFactory;


public class TestSTAService
{
    static final int SERVER_PORT = 8888;
    static final long TIMEOUT = 10000;
    static final String ID_PROP = "@iot.id";
    SensorHub hub;
    File dbFile;
    STAService sta;
    String staRoot;
    
    
    @Before
    public void setup() throws IOException, SensorHubException
    {
        // use temp DB file
        dbFile = File.createTempFile("sta-db-", ".dat");
        dbFile.deleteOnExit();
        
        // get instance with in-memory DB
        hub = new SensorHub();
        hub.start();
        var moduleRegistry = hub.getModuleRegistry();
        
        // start HTTP server
        HttpServerConfig httpConfig = new HttpServerConfig();
        httpConfig.httpPort = SERVER_PORT;
        var httpServer = (HttpServer)moduleRegistry.loadModule(httpConfig, TIMEOUT);
        
        // start SensorThings service
        STAServiceConfig staCfg = new STAServiceConfig();
        staCfg.autoStart = true;
        staCfg.dbConfig.storagePath = dbFile.getAbsolutePath();
        staCfg.dbConfig.databaseNum = 3;
        sta = (STAService)moduleRegistry.loadModule(staCfg, TIMEOUT);
        staRoot = httpServer.getPublicEndpointUrl(staCfg.endPoint + "/v1.0/");
    }
    
    
    @Test
    public void testDeepInsertThing() throws Exception
    {
        deepInsertThing(1);
        
        assertEquals(2, sta.writeDatabase.getThingStore().size());
        assertEquals(1, sta.writeDatabase.getThingLocationStore().size());
        assertEquals(2, sta.writeDatabase.getDataStreamStore().size());
    }
    
    
    @Test
    public void testGetAllThings() throws Exception
    {
        var thing1 = deepInsertThing(5);
        
        JsonObject obj = sendGetRequest("Things").getAsJsonObject();
        assertEquals(2, obj.getAsJsonArray("value").size());
        
        assertThingEquals(
            thing1,
            obj.getAsJsonArray("value").get(1).getAsJsonObject());
    }
    
    
    @Test
    public void testGetThingById() throws Exception
    {
        var thing1 = deepInsertThing(3);
        var thingID = thing1.get(ID_PROP).getAsString();
        
        JsonObject obj = sendGetRequest("Things(" + thingID + ")").getAsJsonObject();
        assertEquals(thingID, obj.get("@iot.id").getAsString());
        assertThingEquals(thing1, obj);
    }
    
    
    @Test
    public void testGetThingDatastreams() throws Exception
    {
        var thing1 = deepInsertThing(3);
        var thing1Id = thing1.get(ID_PROP).getAsString();
        
        var thing2 = deepInsertThing(5);
        var thing2Id = thing2.get(ID_PROP).getAsString();
        
        JsonObject obj = sendGetRequest("Things(" + thing1Id + ")/Datastreams").getAsJsonObject();
        
        assertDatastreamEquals(
            thing1.getAsJsonArray("Datastreams").get(0).getAsJsonObject(),
            obj.getAsJsonArray("value").get(0).getAsJsonObject());
        
        assertDatastreamEquals(
            thing1.getAsJsonArray("Datastreams").get(1).getAsJsonObject(),
            obj.getAsJsonArray("value").get(1).getAsJsonObject());
        
        
        obj = sendGetRequest("Things(" + thing2Id + ")/Datastreams").getAsJsonObject();
        
        assertDatastreamEquals(
            thing2.getAsJsonArray("Datastreams").get(0).getAsJsonObject(),
            obj.getAsJsonArray("value").get(0).getAsJsonObject());
        
        assertDatastreamEquals(
            thing2.getAsJsonArray("Datastreams").get(1).getAsJsonObject(),
            obj.getAsJsonArray("value").get(1).getAsJsonObject());
    }
    
    
    @Test
    public void testGetThingLocations() throws Exception
    {
        var thing1 = deepInsertThing(3);
        var thingId = thing1.get(ID_PROP).getAsString();
        
        JsonObject obj = sendGetRequest("Things(" + thingId + ")/Locations").getAsJsonObject();
        
        assertLocationEquals(
            thing1.getAsJsonArray("Locations").get(0).getAsJsonObject(),
            obj.getAsJsonArray("value").get(0).getAsJsonObject());
    }
    
    
    @Test
    public void testGetAllDatastreams() throws Exception
    {
        var thing1 = deepInsertThing(2);
        JsonObject obj = sendGetRequest("Datastreams").getAsJsonObject();
        
        assertDatastreamEquals(
            thing1.getAsJsonArray("Datastreams").get(0).getAsJsonObject(),
            obj.getAsJsonArray("value").get(0).getAsJsonObject());
        
        assertDatastreamEquals(
            thing1.getAsJsonArray("Datastreams").get(1).getAsJsonObject(),
            obj.getAsJsonArray("value").get(1).getAsJsonObject());
    }
    
    
    protected String getDataStreamId(int idx) throws Exception
    {
        JsonObject col = sendGetRequest("Datastreams").getAsJsonObject();
        return col.getAsJsonArray("value").get(idx).getAsJsonObject().get(ID_PROP).getAsString();
    }
    
    
    @Test
    public void testGetDatastreamById() throws Exception
    {
        var thing1 = deepInsertThing(1);

        for (int i = 0; i < 2; i++)
        {
            var dsId = getDataStreamId(i);
            JsonObject obj = sendGetRequest("Datastreams(" + dsId + ")").getAsJsonObject();
            assertEquals(dsId, obj.get("@iot.id").getAsString());
            assertDatastreamEquals(
                thing1.getAsJsonArray("Datastreams").get(i).getAsJsonObject(),
                obj);
        }
    }
    
    
    @Test
    public void testGetDatastreamSensor() throws Exception
    {
        var thing1 = deepInsertThing(4);
        
        for (int i = 0; i < 2; i++)
        {
            var dsId = getDataStreamId(i);
            JsonObject obj = sendGetRequest("Datastreams(" + dsId + ")/Sensor").getAsJsonObject();
            assertSensorEquals(
                thing1.getAsJsonArray("Datastreams").get(i).getAsJsonObject().get("Sensor").getAsJsonObject(),
                obj);
        }
    }
    
    
    @Test
    public void testGetDatastreamObsProps() throws Exception
    {
        var thing1 = deepInsertThing(8);
        
        for (int i = 0; i < 2; i++)
        {
            var dsId = getDataStreamId(i);
            JsonObject obj = sendGetRequest("Datastreams(" + dsId + ")/ObservedProperty").getAsJsonObject();
            assertObsPropEquals(
                thing1.getAsJsonArray("Datastreams").get(i).getAsJsonObject().get("ObservedProperty").getAsJsonObject(),
                obj);
        }
    }
    
    
    protected JsonObject deepInsertThing(int thingNum) throws Exception
    {
        var builder = JsonBuilderFactory.buildObject()
            .add("description", "thing " + thingNum)
            .add("name", "thing name " + thingNum)
            .addArray("Locations")
                .addObject()
                    .add("description", "location of thing " + thingNum)
                    .add("name", "location name " + thingNum + ".1")
                    .add("encodingType", "application/vnd.geo+json")
                    .addObject("location")
                        .add("type", "Point")
                        .addArray("coordinates")
                            .add(-117.05)
                            .add(51.05)
                        .end()
                    .end()
                .end()
            .end()
            .addArray("Datastreams")
                .addObject()
                    .add("description", "datastream " + thingNum + ".1")
                    .add("name", "datastream name " + thingNum + ".1")
                    .add("observationType", "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement")
                    .addObject("unitOfMeasurement")
                        .add("name", "Lumen")
                        .add("symbol", "lm")
                        .add("definition", "http://www.qudt.org/qudt/owl/1.0.0/unit/Instances.html/Lumen" + thingNum)
                    .end()
                    .addObject("ObservedProperty")
                        .add("name", "Luminous Flux")
                        .add("description", "observedProperty " + thingNum + ".1")
                        .add("definition", "http://www.qudt.org/qudt/owl/1.0.0/quantity/Instances.html/LuminousFlux" + thingNum)
                    .end()
                    .addObject("Sensor")
                        .add("description", "sensor " + thingNum + ".1")
                        .add("name", "sensor name " + thingNum + ".1")
                        .add("encodingType", "application/pdf")
                        .add("metadata", "Light flux sensor")
                    .end()
                    .addArray("Observations")
                        .addObject()
                            .add("phenomenonTime", "2015-03-03T00:00:00Z")
                            .add("result", 3)
                        .end()
                        .addObject()
                            .add("phenomenonTime", "2015-03-04T00:00:00Z")
                            .add("result", 4)
                            .end()
                    .end()
                .end()
                .addObject()
                    .add("description", "datastream " + thingNum + ".2")
                    .add("name", "datastream name " + thingNum + ".2")
                    .add("observationType", "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement")
                    .addObject("unitOfMeasurement")
                        .add("name", "Centigrade")
                        .add("symbol", "°C")
                        .add("definition", "http://www.qudt.org/qudt/owl/1.0.0/unit/Instances.html/Celsius" + thingNum)
                    .end()
                    .addObject("ObservedProperty")
                        .add("name", "Temperature")
                        .add("description", "observedProperty " + thingNum + ".2")
                        .add("definition", "http://www.qudt.org/qudt/owl/1.0.0/quantity/Instances.html/Temperature" + thingNum)
                    .end()
                    .addObject("Sensor")
                        .add("description", "sensor " + thingNum + ".2")
                        .add("name", "sensor name " + thingNum + ".2")
                        .add("encodingType", "application/pdf")
                        .add("metadata", "Light flux sensor")
                    .end()
                    .addArray("Observations")
                        .addObject()
                            .add("phenomenonTime", "2015-03-05T00:00:00Z")
                            .add("result", 5)
                        .end()
                        .addObject()
                            .add("phenomenonTime", "2015-03-06T00:00:00Z")
                            .add("result", 6)
                            .end()
                    .end()
                .end()
            .end();
        
        var thing = builder.getJson();
        HttpResponse<String> response = sendPostRequest("Things", thing);
        
        // add assigned ID if one was received
        response.headers().firstValue("Location").ifPresent(url -> {
            var id = url.substring(url.lastIndexOf('(')+1, url.lastIndexOf(')'));
            thing.addProperty(ID_PROP, id);
        });
        
        return thing;
    }
    
    
    protected void assertThingEquals(JsonObject expected, JsonObject actual)
    {
        assertEquals(expected.get("name"), actual.get("name"));
        assertEquals(expected.get("description"), actual.get("description"));
    }
    
    
    protected void assertSensorEquals(JsonObject expected, JsonObject actual)
    {
        assertEquals(expected.get("name"), actual.get("name"));
        assertEquals(expected.get("description"), actual.get("description"));
        assertEquals(expected.get("encodingType"), actual.get("encodingType"));
    }
    
    
    protected void assertLocationEquals(JsonObject expected, JsonObject actual)
    {
        assertEquals(expected.get("name"), actual.get("name"));
        assertEquals(expected.get("description"), actual.get("description"));
        assertEquals(expected.get("encodingType"), actual.get("encodingType"));        
        assertGeomEquals(expected.get("location").getAsJsonObject(), actual.get("location").getAsJsonObject());
    }
    
    
    protected void assertGeomEquals(JsonObject expected, JsonObject actual)
    {
        assertEquals(expected.get("type"), actual.get("type"));
        assertEquals(expected.getAsJsonArray("coordinates"), actual.getAsJsonArray("coordinates"));
    }
    
    
    protected void assertDatastreamEquals(JsonObject expected, JsonObject actual)
    {
        assertEquals(expected.get("description"), actual.get("description"));
        assertEquals(expected.get("observationType"), actual.get("observationType"));
        assertUomEquals(expected.get("unitOfMeasurement").getAsJsonObject(), actual.get("unitOfMeasurement").getAsJsonObject());
    }
    
    
    protected void assertUomEquals(JsonObject expected, JsonObject actual)
    {
        //assertEquals(expected.get("name"), actual.get("name"));
        //assertEquals(expected.get("symbol"), actual.get("symbol"));
        assertEquals(expected.get("definition"), actual.get("definition"));
    }
    
    
    protected void assertObsPropEquals(JsonObject expected, JsonObject actual)
    {
        assertEquals(expected.get("name"), actual.get("name"));
        assertEquals(expected.get("description"), actual.get("description"));
        assertEquals(expected.get("definition"), actual.get("definition"));
    }
    
    
    protected HttpResponse<String> sendPostRequest(String path, JsonElement json) throws IOException
    {
        try
        {
            HttpClient client = HttpClient.newHttpClient();
            
            HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(json)))
                .uri(URI.create(staRoot + path))
                .header("Content-Type", "application/json")
                .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            if (statusCode != 200)
                System.err.println(response.body());
            assertTrue("Received HTTP error status", statusCode < 300);
            
            return response;
        }
        catch (InterruptedException e)
        {
            throw new IOException(e);
        }
    }
    
    
    protected JsonElement sendGetRequest(String path) throws IOException
    {
        try
        {
            HttpClient client = HttpClient.newHttpClient();
            
            HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(staRoot + path))
                .build();
                    
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());            
            int statusCode = response.statusCode();
            if (statusCode != 200)
                System.err.println(response.body());
            assertEquals(200, statusCode);
            
            return JsonParser.parseString(response.body());
        }
        catch (InterruptedException e)
        {
            throw new IOException(e);
        }
    }
    
    
    @After
    public void cleanup()
    {
        try
        {
            if (hub != null)
                hub.stop();
                        
            // HACK to reset FROST PM factory so we can create a new servlet with new settings
            Field instanceField = PersistenceManagerFactory.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (dbFile != null)
                dbFile.delete();
        }
    }
}
