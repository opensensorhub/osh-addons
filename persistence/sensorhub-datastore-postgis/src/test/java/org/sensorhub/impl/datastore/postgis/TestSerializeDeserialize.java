/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.Point;
import net.opengis.gml.v32.impl.GMLFactory;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataType;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.command.*;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.BigIdLong;
import org.sensorhub.api.data.DataStreamInfo;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.data.ObsData;
import org.sensorhub.api.feature.FeatureId;
import org.sensorhub.api.system.ISystemWithDesc;
import org.sensorhub.impl.datastore.DataStoreFiltersTypeAdapterFactory;
import org.sensorhub.impl.datastore.DataStoreUtils;
import org.sensorhub.impl.datastore.postgis.utils.BigIdTypeAdapter;
import org.sensorhub.impl.datastore.postgis.utils.DataStreamExclusionStrategy;
import org.sensorhub.impl.datastore.postgis.utils.SerializerUtils;
import org.sensorhub.impl.system.wrapper.SmlFeatureWrapper;
import org.sensorhub.impl.system.wrapper.SystemWrapper;
import org.vast.data.AbstractDataBlock;
import org.vast.data.DataBlockDouble;
import org.vast.data.DataBlockFactory;
import org.vast.data.TextEncodingImpl;
import org.vast.ogc.gml.GenericFeatureImpl;
import org.vast.ogc.gml.GeoJsonBindings;
import org.vast.ogc.gml.IFeature;
import org.vast.sensorML.SMLHelper;
import org.vast.sensorML.SMLJsonBindings;
import org.vast.swe.SWEBuilders;
import org.vast.swe.SWEHelper;

import static org.junit.Assert.*;

import org.vast.swe.fast.JsonDataParserGson;
import org.vast.swe.fast.JsonDataWriterGson;
import org.vast.util.TimeExtent;

import javax.xml.namespace.QName;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Collection;
import java.util.List;

public class TestSerializeDeserialize {


    //    public static void main(String[] args) {
//        testSerialize();
//        testSerializeDatablock();
//    }
    static final GsonBuilder builder = new GsonBuilder()
            .setLenient()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .serializeSpecialFloatingPointValues()
            .registerTypeAdapter(BigId.class, new BigIdTypeAdapter())
            .setFieldNamingStrategy(new DataStoreFiltersTypeAdapterFactory.FieldNamingStrategy())
            .registerTypeAdapterFactory(new RuntimeTypeAdapterFactory<>(Object.class, "objClass"))
            .setExclusionStrategies(new DataStreamExclusionStrategy());

        public static final Gson Gson = builder.create();

    @Test
    public void testSerializeDataStreamInfo() throws IOException {
        SWEHelper fac = new SWEHelper();
        var dataStruct = fac.createRecord()
                .name("test")
                .description("test serialize")
                .addField("t1", fac.createTime().asSamplingTimeIsoUTC().build())
                .addField("q2", fac.createQuantity().build())
                .addField("c3", fac.createCount().build())
                .addField("b4", fac.createBoolean().build())
                .addField("txt5", fac.createText().build())
                .build();

        var now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        var builder = new DataStreamInfo.Builder()
                .withName(dataStruct.getName())
                .withSystem(new FeatureId(BigId.fromLong(2, 1), "urn:osh:test:sensor:" + 2))
                .withRecordDescription(dataStruct)
                .withValidTime(TimeExtent.period(now.minus(365, ChronoUnit.DAYS), now.minus(10, ChronoUnit.DAYS)))
                .withRecordEncoding(new TextEncodingImpl());

        IDataStreamInfo dataStreamInfo = builder.build();
        String json = SerializerUtils.writeIDataStreamInfoToJson(dataStreamInfo);
        System.out.println(json);

        // read back
        IDataStreamInfo readBack = SerializerUtils.readIDataStreamInfoFromJson(json);
        System.out.println(readBack);

        json = SerializerUtils.writeIDataStreamInfoToJson(dataStreamInfo);
        System.out.println(json);
    }

    @Test
    public void testSerializeCommandStreamInfo() {
        SWEHelper fac = new SWEHelper();
        var dataStruct = fac.createRecord()
                .name("test")
                .description("test serialize")
                .addField("t1", fac.createTime().asSamplingTimeIsoUTC().build())
                .addField("q2", fac.createQuantity().build())
                .addField("c3", fac.createCount().build())
                .addField("b4", fac.createBoolean().build())
                .addField("txt5", fac.createText().build())
                .build();

        var now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        var builder = new CommandStreamInfo.Builder()
                .withName(dataStruct.getName())
                .withSystem(new FeatureId(BigId.fromLong(2, 1), "urn:osh:test:sensor:" + 2))
                .withRecordDescription(dataStruct)
                .withValidTime(TimeExtent.period(now.minus(365, ChronoUnit.DAYS), now.minus(10, ChronoUnit.DAYS)))
                .withRecordEncoding(new TextEncodingImpl());

        ICommandStreamInfo dataStreamInfo = builder.build();
        String json = Gson.toJson(dataStreamInfo);
        System.out.println(json);

        // read back
        ICommandStreamInfo readBack = Gson.fromJson(json, CommandStreamInfo.class);
        System.out.println(readBack);

        json = Gson.toJson(dataStreamInfo);
        System.out.println(json);
    }

    @Before
    public void init() {
    }

    @Test
    public void testSerializeWithKryo() {
        DataBlockDouble data = new DataBlockDouble(5);
        for (int s = 0; s < 5; s++)
            data.setDoubleValue(s, s);

        ObsData obs = new ObsData.Builder()
                .withDataStream(BigId.fromLong(0, 5L))
                .withFoi(BigId.fromLong(0, 15L))
                .withPhenomenonTime(Instant.parse("2000-06-21T14:36:12Z"))
                .withResult(data)
                .build();
    }

    @Test
    public void testSerializeObs() {
        DataBlockDouble data = new DataBlockDouble(5);
        for (int s = 0; s < 5; s++)
            data.setDoubleValue(s, s);

        ObsData obs = new ObsData.Builder()
                .withDataStream(BigId.fromLong(0, 5L))
                .withFoi(BigId.fromLong(0, 15L))
                .withPhenomenonTime(Instant.parse("2000-06-21T14:36:12Z"))
                .withResult(data)
                .build();

        String json = Gson.toJson(obs);
        long st = System.nanoTime();
        ObsData readObs = Gson.fromJson(json, ObsData.class);
        long end = System.nanoTime();
        long delta = end - st;
        System.out.println("[Gson] Took=" + (delta) + "ns");

        System.out.println(json);
    }

    @Test
    public void testSerializeObs1() {
        DataBlockDouble data = new DataBlockDouble(5);
        for (int s = 0; s < 5; s++)
            data.setDoubleValue(s, s);

        ObsData obs = new ObsData.Builder()
                .withDataStream(BigId.fromLong(0, 5L))
                .withFoi(BigId.fromLong(0, 15L))
                .withPhenomenonTime(Instant.parse("2000-06-21T14:36:12Z"))
                .withResult(data)
                .build();

        String json = TestSerializeDeserialize.Gson.toJson(obs);
        long st = System.nanoTime();
        ObsData readObs = TestSerializeDeserialize.Gson.fromJson(json, ObsData.class);
        long end = System.nanoTime();
        long delta = end - st;
        System.out.println("[Gson] Took=" + (delta) + "ns");

        System.out.println(json);
    }

    @Test
    public void testSerializeObs2() {
        DataBlockDouble data = new DataBlockDouble(5);
        for (int s = 0; s < 5; s++)
            data.setDoubleValue(s, s);

        ObsData obs = new ObsData.Builder()
                .withDataStream(BigId.fromLong(0, 5L))
                .withFoi(BigId.fromLong(0, 15L))
                .withPhenomenonTime(Instant.parse("2000-06-21T14:36:12Z"))
                .withResult(data)
                .build();

        String json = TestSerializeDeserialize.Gson.toJson(obs);
        long st = System.nanoTime();
        ObsData readObs = TestSerializeDeserialize.Gson.fromJson(json, ObsData.class);
        long end = System.nanoTime();
        long delta = end - st;
        System.out.println("[Gson] Took=" + (delta) + "ns");

        System.out.println(json);
    }

    //    @Test
    public void testSerializeDatablockLong() {
        final GsonBuilder builder = new GsonBuilder()
                .setLenient()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .serializeSpecialFloatingPointValues()
                .registerTypeAdapterFactory(new RuntimeTypeAdapterFactory<>(Object.class, "objClass"))
                .setFieldNamingStrategy(new DataStoreFiltersTypeAdapterFactory.FieldNamingStrategy());

        Gson gson = builder.create();
        DataBlock databBlock = DataBlockFactory.createBlock(new long[]{0L, 1L, 2L});
        String json = gson.toJson(databBlock);

        DataBlock dataBlockRead = gson.fromJson(json, DataBlock.class);
        assertEquals(DataType.LONG, dataBlockRead.getDataType());
        assertArrayEquals(new long[]{0L, 1L, 2L}, (long[]) dataBlockRead.getUnderlyingObject());
    }

    //    @Test
    public void testSerializeDatablockInt() {
        final GsonBuilder builder = new GsonBuilder()
                .setLenient()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .serializeSpecialFloatingPointValues()
                .registerTypeAdapterFactory(new RuntimeTypeAdapterFactory<>(Object.class, "objClass"))
                .setFieldNamingStrategy(new DataStoreFiltersTypeAdapterFactory.FieldNamingStrategy());

        int[] values = new int[]{0, 1, 2};
        Gson gson = builder.create();
        DataBlock databBlock = DataBlockFactory.createBlock(values);
        String json = gson.toJson(databBlock);

        DataBlock dataBlockRead = gson.fromJson(json, DataBlock.class);
        assertEquals(DataType.INT, dataBlockRead.getDataType());
        assertArrayEquals(values, (int[]) dataBlockRead.getUnderlyingObject());
    }

    //    @Test
    public void testSerializeDatablockMixed() {
        final GsonBuilder builder = new GsonBuilder()
                .setLenient()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .serializeSpecialFloatingPointValues()
                .registerTypeAdapterFactory(new RuntimeTypeAdapterFactory<>(Object.class, "objClass"))
                .setFieldNamingStrategy(new DataStoreFiltersTypeAdapterFactory.FieldNamingStrategy());
        Gson gson = builder.create();

        byte[] values0 = new byte[]{0, 0, 1, 1, 0, 45, 65, 15, 20};
        AbstractDataBlock databBlock0 = DataBlockFactory.createBlock(values0);

        int[] values1 = new int[]{0, 0, 1, 1, 0, 45, 65, 15, 20};
        AbstractDataBlock databBlock1 = DataBlockFactory.createBlock(values1);

        double[] values2 = new double[]{0.0, 0.0, 1.0, 1.0, 0.0, 45.0, 65.0, 15.0, 20.0};
        AbstractDataBlock databBlock2 = DataBlockFactory.createBlock(values2);

        DataBlock databBlock3 = DataBlockFactory.createMixedBlock(databBlock0, databBlock1, databBlock2);

        String json = gson.toJson(databBlock3);
        DataBlock dataBlockRead = gson.fromJson(json, DataBlock.class);
        assertEquals(DataType.MIXED, dataBlockRead.getDataType());
        System.out.println(json);
    }

    @Test
    public void testSerializeDatablockMixedUsingGsonWriter() {
        final GsonBuilder builder = new GsonBuilder()
                .setLenient()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .serializeSpecialFloatingPointValues()
                .registerTypeAdapterFactory(new RuntimeTypeAdapterFactory<>(Object.class, "objClass"))
                .setFieldNamingStrategy(new DataStoreFiltersTypeAdapterFactory.FieldNamingStrategy());
        Gson gson = builder.create();

        byte[] values0 = new byte[]{0, 0, 1, 1, 0, 45, 65, 15, 20};
        AbstractDataBlock databBlock0 = DataBlockFactory.createBlock(values0);

        int[] values1 = new int[]{0, 0, 1, 1, 0, 45, 65, 15, 20};
        AbstractDataBlock databBlock1 = DataBlockFactory.createBlock(values1);

        double[] values2 = new double[]{0.0, 0.0, 1.0, 1.0, 0.0, 45.0, 65.0, 15.0, 20.0};
        AbstractDataBlock databBlock2 = DataBlockFactory.createBlock(values2);

        DataBlock databBlock3 = DataBlockFactory.createMixedBlock(databBlock0, databBlock1, databBlock2);

        SWEHelper fac = new SWEHelper();
        var dataStruct = fac.createRecord()
                .name("test")
                .description("test serialize")
                .addField("t1", fac.createTime().asSamplingTimeIsoUTC().build())
                .addField("q2", fac.createQuantity().build())
                .addField("c3", fac.createCount().build())
                .addField("b4", fac.createBoolean().build())
                .addField("txt5", fac.createText().build())
                .build();

        DataBlock dataBlock4 = dataStruct.createDataBlock();
        JsonDataWriterGson jsonDataWriterGson = new JsonDataWriterGson();
        JsonDataParserGson jsonDataParserGson = new JsonDataParserGson();

        // set datastream schema
        jsonDataWriterGson.setDataComponents(dataStruct);
        jsonDataParserGson.setDataComponents(dataStruct);

        String json = null;
        try {
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                jsonDataWriterGson.setOutput(bos);

                jsonDataWriterGson.write(dataBlock4);
                DataBlock dataBlockRead = gson.fromJson(json, DataBlock.class);
//                assertEquals(DataType.MIXED, dataBlockRead.getDataType());
                jsonDataWriterGson.flush();
                json = new String(bos.toByteArray());
            }
            System.out.println(json);
            try (ByteArrayInputStream bis = new ByteArrayInputStream(json.getBytes())) {
                jsonDataParserGson.setInput(bis);
                DataBlock dataBlock = jsonDataParserGson.parseNextBlock();
                System.out.println(dataBlock);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testSerializeFeature() throws IOException {
        QName fType = new QName("http://mydomain/features", "MyPointFeature");
        GMLFactory gmlFac = new GMLFactory(true);
        String[] featureTypes = {"building", "road", "waterbody"};

        AbstractFeature f = new GenericFeatureImpl(fType);
        Point p = gmlFac.newPoint();
        p.setPos(new double[]{0, 15, 0.0});
        f.setGeometry(p);
        String idPrefix = "F";
        if (f.getValidTime() != null)
            idPrefix += "T";
        if (f.isSetGeometry())
            idPrefix += "G";

        String UID_PREFIX = "urn:domain:features:";
        f.setId(idPrefix + 0);
        f.setUniqueIdentifier(UID_PREFIX + idPrefix + 0);
        f.setName("Feature " + 0);
        f.setDescription("This is building #0");

        GeoJsonBindings geoJsonBindings = new GeoJsonBindings(true);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        var osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
        JsonWriter jsonWriter = new JsonWriter(osw);
        jsonWriter.setLenient(true);
        jsonWriter.setSerializeNulls(false);
        jsonWriter.setIndent("  ");

        jsonWriter.beginArray();
        geoJsonBindings.writeFeature(jsonWriter, f);
        jsonWriter.endArray();
        jsonWriter.flush();

       System.out.println(os.toString());

        /*try(var bis = new ByteArrayInputStream(os.toByteArray())) {
            try (var osr = new InputStreamReader(bis, StandardCharsets.UTF_8)) {
                try(JsonReader jsonReader = new JsonReader(osr)) {
                    jsonReader.setLenient(true);
                    jsonReader.beginArray();
                    IFeature readFeature = geoJsonBindings.readFeature(jsonReader);
                    System.out.println(readFeature.toString());
                    jsonReader.endArray();
                }
            }
        }*/
//            jsonDataParserGson.setInput(bis);
//
//            DataBlock dataBlock = jsonDataParserGson.parseNextBlock();
//            System.out.println(dataBlock);
        String value = "[{\"id\": \"FG10\", \"type\": \"Feature\", \"geometry\": {\"type\": \"Point\", \"coordinates\": " +
                "[10, 10, 0]}, \"properties\": {\"uid\": \"urn:domain:features:FG10\", \"name\": \"Feature 10\", " +
                "\"description\": \"This is road #10\", \"featureType\": \"http://mydomain/features#MyPointFeature\"}}]";

        System.out.println(value);
        try(var bis = new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8))) {
            try (var osr = new InputStreamReader(bis, StandardCharsets.UTF_8)) {
                try(JsonReader jsonReader = new JsonReader(osr)) {
                    jsonReader.setLenient(true);
                    jsonReader.beginArray();
                    IFeature readFeature = geoJsonBindings.readFeature(jsonReader);
                    System.out.println(readFeature.toString());
                    jsonReader.endArray();
                }
            }
        }
    }

    @Test
    public void testSerializeProcedure() throws IOException {
        AbstractProcess p = new SMLHelper().createPhysicalComponent()
                .uniqueID("123:145:4545:45")
                .name("System #36")
                .build();
        SmlFeatureWrapper procWrapper = new SmlFeatureWrapper(p);

        SMLJsonBindings smlJsonBindings = new SMLJsonBindings(false);

        StringWriter stringWriter = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(stringWriter);
        smlJsonBindings.writeDescribedObject(jsonWriter, procWrapper.getFullDescription());
        System.out.println(stringWriter);

        StringReader stringReader = new StringReader(stringWriter.toString());
        JsonReader reader = new JsonReader(stringReader);
        AbstractProcess result = (AbstractProcess) smlJsonBindings.readDescribedObject(reader);
        System.out.println(result);

    }

    @Test
    public void testSerializeSystem() {
        AbstractProcess p = new SMLHelper().createPhysicalSystem()
                .uniqueID("123:145:4545:45")
                .name("System #36")
                .build();
        ISystemWithDesc procWrapper = new SystemWrapper(p);
        String data = TestSerializeDeserialize.Gson.toJson(procWrapper);
        System.out.println(data);

        SmlFeatureWrapper smlFeatureWrapper = TestSerializeDeserialize.Gson.fromJson(data, SmlFeatureWrapper.class);
        System.out.println(smlFeatureWrapper);
    }

    @Test
    public void testSerializeTimeExtentUsingGson() {
        Gson gson = new GsonBuilder().create();
        TimeExtent timeExtent = TimeExtent.period(Instant.MIN, Instant.MAX);

        String json = gson.toJson(timeExtent, TimeExtent.class);
        System.out.println(json);

        timeExtent = TimeExtent.currentTime();

        json = gson.toJson(timeExtent, TimeExtent.class);
        System.out.println(json);

        timeExtent = TimeExtent.beginAt(Instant.now());

        json = gson.toJson(timeExtent, TimeExtent.class);
        System.out.println(json);

        timeExtent = TimeExtent.endAt(Instant.now().plus(100, ChronoUnit.DAYS));

        json = gson.toJson(timeExtent, TimeExtent.class);
        System.out.println(json);
    }

    @Test
    public void testSerializeTimeExtentJsonWriterReader() throws IOException {
        TimeExtent timeExtent;

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            try (var osw = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                try (JsonWriter jsonWriter = new JsonWriter(osw)) {
                    jsonWriter.beginObject();
                    jsonWriter.setLenient(true);
                    jsonWriter.setSerializeNulls(false);
                    jsonWriter.setIndent("  ");

                    // max period
                    timeExtent = TimeExtent.period(Instant.MIN, Instant.MAX);
                    SerializerUtils.writeTimeExtent(jsonWriter, "validTime", timeExtent);
                    jsonWriter.endObject();
                    jsonWriter.flush();

                    String json = os.toString();
                    System.out.println(json);

                    // current time
                    timeExtent = TimeExtent.currentTime();

                    os.reset();
                    jsonWriter.beginObject();
                    SerializerUtils.writeTimeExtent(jsonWriter, "validTime", timeExtent);
                    jsonWriter.endObject();
                    jsonWriter.flush();

                    json = os.toString();
                    System.out.println(json);

                    // now -> infinity
                    timeExtent = TimeExtent.beginAt(Instant.now());

                    os.reset();
                    jsonWriter.beginObject();
                    SerializerUtils.writeTimeExtent(jsonWriter, "validTime", timeExtent);
                    jsonWriter.endObject();
                    jsonWriter.flush();

                    json = os.toString();
                    System.out.println(json);

                    // -infinity -> now + 100 days
                    timeExtent = TimeExtent.endAt(Instant.now().plus(100, ChronoUnit.DAYS));

                    os.reset();
                    jsonWriter.beginObject();
                    SerializerUtils.writeTimeExtent(jsonWriter, "validTime", timeExtent);
                    jsonWriter.endObject();
                    jsonWriter.flush();

                    json = os.toString();
                    System.out.println(json);

                }
            }
        }
    }

    @Test
    public void testDeserializeIDataStream() throws IOException {
        String data = "{\"name\": \"New Simulate dWeather Station - weather\", " +
                "\"system@id\": {\"uniqueID\": \"urn:simweatherstation:A:001\", " +
                "\"internalID\": {\"id\": 2, \"scope\": 1}}, \"outputName\": \"weather\", " +
                "\"recordSchema\": {\"type\": \"DataRecord\", \"fields\": [{\"uom\": {\"href\": " +
                "\"http://www.opengis.net/def/uom/ISO-8601/0/Gregorian\"}, \"name\": \"time\", \"type\": \"Time\", " +
                "\"label\": \"Sampling Time\", \"definition\": \"http://www.opengis.net/def/property/OGC/0/SamplingTime\", " +
                "\"referenceFrame\": \"http://www.opengis.net/def/trs/BIPM/0/UTC\"}, {\"uom\": {\"code\": \"mbar\"}," +
                " \"name\": \"pressure\", \"type\": \"Quantity\", \"label\": \"Barometric Pressure\", \"definition\": " +
                "\"http://sensorml.com/ont/swe/property/BarometricPressure\"}, {\"uom\": {\"code\": \"degC\"}, \"name\": " +
                "\"temperature\", \"type\": \"Quantity\", \"label\": \"Air Temperature\", \"definition\":" +
                " \"http://sensorml.com/ont/swe/property/Temperature\"}, {\"uom\": {\"code\": \"%\"}, \"name\":" +
                " \"relHumidity\", \"type\": \"Quantity\", \"label\": \" Relative Humidity\", \"definition\": " +
                "\"http://sensorml.com/ont/swe/property/RelativeHumidity\"}, {\"uom\": {\"code\": \"tips\"}, " +
                "\"name\": \"rainAccum\", \"type\": \"Quantity\", \"label\": \"Rain Accumulation\", \"definition\": " +
                "\"http://sensorml.com/ont/swe/property/RainAccumulation\"}, {\"uom\": {\"code\": \"m/s\"}, \"name\":" +
                " \"windSpeed\", \"type\": \"Quantity\", \"label\": \"Wind Speed\", \"definition\": " +
                "\"http://sensorml.com/ont/swe/property/WindSpeed\"}, {\"uom\": {\"code\": \"deg\"}, " +
                "\"name\": \"windDir\", \"type\": \"Quantity\", \"label\": \"Wind Direction\", \"axisID\": " +
                "\"z\", \"definition\": \"http://sensorml.com/ont/swe/property/WindDirection\", \"referenceFrame\": " +
                "\"http://sensorml.com/ont/swe/property/NED\"}], \"definition\": \"http://sensorml.com/ont/swe/property/Weather\", " +
                "\"description\": \"Weather Station Data\"}, \"recordEncoding\": {\"type\": \"TextEncoding\"," +
                " \"blockSeparator\": \"\\n\", \"tokenSeparator\": \",\", \"decimalSeparator\": \".\", \"collapseWhiteSpaces\": true}}";

        IDataStreamInfo dataStreamInfo = SerializerUtils.readIDataStreamInfoFromJson(data);
    }

    private Collection<BigId> createExpectedBigIds() {
        return List.of(
                new BigIdLong(2, 4),
                new BigIdLong(1, 5),
                new BigIdLong(3, 10),
                new BigIdLong(5, 100)
                );
    }

    @Test
    public void testSerializeCommandResultWithDataStreamIds() throws IOException {
        var expectedIds = createExpectedBigIds();
        var cmdRes = CommandResult.withDatastreams(expectedIds);
        var cmdStatus = new CommandStatus.Builder()
                .withCommand(new BigIdLong(1, 1))
                .withResult(cmdRes)
                .build();
        var json = SerializerUtils.writeICommandStatusToJson(cmdStatus);
        var fromJson = SerializerUtils.readICommandStatusFromJson(json);
        assertTrue(fromJson.getResult().getDataStreamIDs().containsAll(expectedIds));
    }

    @Test
    public void testSerializeCommandResultWithObservationIds() throws IOException {
        var expectedIds = createExpectedBigIds();
        var cmdRes = CommandResult.withObservations(expectedIds);
        var cmdStatus = new CommandStatus.Builder()
                .withCommand(new BigIdLong(1, 2))
                .withResult(cmdRes)
                .build();
        var json = SerializerUtils.writeICommandStatusToJson(cmdStatus);
        var fromJson = SerializerUtils.readICommandStatusFromJson(json);
        assertTrue(fromJson.getResult().getObservationIDs().containsAll(expectedIds));

        var one = expectedIds.stream().toList().get(0);
        cmdRes = CommandResult.withObservation(one);
        cmdStatus = new CommandStatus.Builder()
                .withCommand(new BigIdLong(1, 3))
                .withResult(cmdRes)
                .build();
        json = SerializerUtils.writeICommandStatusToJson(cmdStatus);
        fromJson = SerializerUtils.readICommandStatusFromJson(json);
        assertTrue(fromJson.getResult().getObservationIDs().size() == 1 && fromJson.getResult().getObservationIDs().contains(one));
    }

    private DataComponent createTestStructure() {
        SWEHelper fac = new SWEHelper();
        return fac.createRecord()
                .name("testIO")
                .addField("test1", fac.createText())
                .addField("test2", fac.createCount())
                .build();
    }

    @Test
    public void testSerializeCommandResultWithInlineRecords() throws IOException {
        var struct = createTestStructure();
        var cmdStream = new CommandStreamInfo.Builder()
                .withName("test")
                .withRecordDescription(struct.copy())
                .withRecordEncoding(new TextEncodingImpl())
                .withResultDescription(struct.copy())
                .withResultEncoding(new TextEncodingImpl())
                .withSystem(new FeatureId(new BigIdLong(1, 5), "urn:osh:test:1"))
                .build();

        var data = struct.createDataBlock();
        var d1 = "I am a string";
        int d2 = 555;
        data.setStringValue(0, d1);
        data.setIntValue(1, d2);

        var cmdRes = CommandResult.withData(data);
        var cmdStatus = new CommandStatus.Builder()
                .withCommand(new BigIdLong(1, 5))
                .withResult(cmdRes)
                .build();
        var json = SerializerUtils.writeICommandStatusToJson(cmdStatus, cmdStream);
        var fromJson = SerializerUtils.readICommandStatusFromJson(json, cmdStream);
        var resRecords = fromJson.getResult().getInlineRecords().stream().toList();
        assertFalse(resRecords.isEmpty());
        assertEquals(d1, resRecords.get(0).getStringValue(0));
        assertEquals(d2, resRecords.get(0).getIntValue(1));
    }

    @Test
    public void testSerializeCommandStatus() throws IOException {
        var cmdStatus = CommandStatus.accepted(new BigIdLong(1, 12));
        var json = SerializerUtils.writeICommandStatusToJson(cmdStatus);
        var fromJson = SerializerUtils.readICommandStatusFromJson(json);
        assertEquals(ICommandStatus.CommandStatusCode.ACCEPTED, fromJson.getStatusCode());
    }

}
