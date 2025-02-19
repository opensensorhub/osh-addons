package org.sensorhub.impl.service.sta.ingest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.NodeType;
import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.model.*;
import de.fraunhofer.iosb.ilt.sta.model.ext.UnitOfMeasurement;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.AbstractGeometry;
import net.opengis.gml.v32.impl.AbstractGeometryImpl;
import net.opengis.gml.v32.impl.GMLFactory;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.sensorml.v20.DocumentList;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.geojson.Feature;
import org.geojson.GeoJsonObject;
import org.geojson.LngLatAlt;
import org.geojson.Polygon;
import org.isotc211.v2005.gmd.CIOnlineResource;
import org.isotc211.v2005.gmd.impl.GMDFactory;
import org.sensorhub.api.command.ICommandStreamInfo;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.data.DataStreamInfo;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.data.ObsData;
import org.sensorhub.api.feature.FeatureId;
import org.sensorhub.utils.SWEDataUtils;
import org.vast.data.*;
import org.vast.ogc.gml.GenericFeature;
import org.vast.ogc.gml.GenericFeatureImpl;
import org.vast.ogc.om.*;
import org.vast.sensorML.SMLFactory;
import org.vast.sensorML.SMLHelper;
import org.vast.swe.SWEBuilders;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.util.Asserts;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class STAUtils {

    private static SWEHelper fac = new SWEHelper();
    static final String staUrnPrefix = "urn:osh:sta:";
    static final String thingUidPrefix = staUrnPrefix + "thing:";
    static final String sensorUidPrefix = staUrnPrefix + "sensor:";
    static final String featureUidPrefix = staUrnPrefix + "feature:";
    static final String VND_GEOJSON_FORMAT = "application/vnd.geo+json";
    static final String GEOJSON_FORMAT = "application/geo+json";
    static final String UCUM_URI_PREFIX = "http://unitsofmeasure.org/ucum.html#";
    static final String THING_LOC_OUTPUT_NAME = "thingLocation";

    private STAUtils() {}

    public static String toUid(String name, Id<?> id)
    {
        return SWEDataUtils.toNCName(name + ":" + id);
    }

    public static AbstractProcess toSmlProcess(Sensor sensor)
    {
        String uid = sensorUidPrefix + toUid(sensor.getName(), sensor.getId());

        var sys = new SMLHelper().createPhysicalSystem()
                .uniqueID(uid)
                .name(sensor.getName())
                .description(sensor.getDescription())
                .validFrom(OffsetDateTime.now())
                .build();

        if(sensor.getMetadata() instanceof String)
        {
            CIOnlineResource doc = new GMDFactory().newCIOnlineResource();
            doc.setProtocol(sensor.getEncodingType());
            doc.setLinkage((String)sensor.getMetadata());

            DocumentList docList = new SMLFactory().newDocumentList();
            docList.addDocument(doc);
            sys.getDocumentationList().add("sta_metadata", docList);
        }

        return sys;
    }

    public static AbstractProcess toSmlProcess(Thing thing)
    {
        String uid = thingUidPrefix + SWEDataUtils.toNCName(thing.getName()) + ":" + thing.getId();

        var sys = new SMLHelper().createPhysicalSystem()
                .uniqueID(uid)
                .name(thing.getName())
                .description(thing.getDescription())
                .validFrom(OffsetDateTime.now())
                .build();

        return sys;
    }

    protected IDataStreamInfo toSweDataStream(FeatureId systemId, Datastream ds) throws ServiceFailureException {
        var recordStruct = toSweCommon(ds);
        return new DataStreamInfo.Builder()
                .withName(ds.getName())
                .withDescription(ds.getDescription())
                .withSystem(systemId)
                .withRecordDescription(recordStruct)
                .withRecordEncoding(new TextEncodingImpl())
                .build();
    }

    protected static DataRecord toSweCommon(Entity<?> entity) throws ServiceFailureException {
        SWEBuilders.DataRecordBuilder rec = null;
        if(entity instanceof Datastream) {
            Datastream ds = (Datastream)entity;

            rec = fac.createRecord()
                    .name(SWEDataUtils.toNCName(ds.getName()))
                    .label(ds.getName())
                    .description(ds.getDescription())
                    .addField("time", fac.createTime().asPhenomenonTimeIsoUTC()
                            .label("Sampling Time"));

            ObservedProperty obsProp = ds.getObservedProperty();

            DataComponent comp = toComponent(
                    ds.getObservationType(),
                    obsProp,
                    ds.getUnitOfMeasurement());

            if (comp != null)
                rec.addField(comp.getName(), comp);
        } else if(entity instanceof MultiDatastream) {
            MultiDatastream ds = (MultiDatastream)entity;

            int i = 0;
            for (ObservedProperty obsProp: ds.getObservedProperties())
            {

                DataComponent comp = toComponent(
                        ds.getMultiObservationDataTypes().get(i),
                        obsProp,
                        ds.getUnitOfMeasurements().get(i));
                i++;

                rec.addField(comp.getName(), comp);
            }
        }

        return rec.build();
    }

    private static boolean checkDefinition(String defOrUri, String sillyDefinition)
    {
        String[] endOfPath = sillyDefinition.split("/");
        String def = null;
        if(endOfPath.length > 1)
            def = endOfPath[endOfPath.length-1];
        return defOrUri.equals(sillyDefinition) || defOrUri.endsWith(sillyDefinition) || (def != null && defOrUri.endsWith(def));
    }

    private static String cleanUom(String uom)
    {
        return uom.replace(" ", "")
                .replace("ucum:", "")
                .replace("[", "")
                .replace("]", "");
    }

    private static Set<String> TW_SPECIAL_CASES = new HashSet<>() {{
        add("video");
        add("videostream");
        add("time");
        add("pollutant");
        add("status");
        add("weather");
        add("na");
        add("視訊監測影格照片");
    }};

    protected static DataComponent toComponent(String obsType, ObservedProperty obsProp, UnitOfMeasurement uom)
    {
        SWEBuilders.DataComponentBuilder<? extends SWEBuilders.DataComponentBuilder<?,?>, ? extends DataComponent> comp = null;

        boolean test = TW_SPECIAL_CASES.contains(uom.getName().toLowerCase());
        if(test) {
            var name = uom.getName();
            var obs = obsProp.getDefinition();
            var uomDef = uom.getDefinition();
        }

        if (checkDefinition(IObservation.OBS_TYPE_MEAS, obsType)
                && !(TW_SPECIAL_CASES.contains(uom.getName().toLowerCase()))
                && !(TW_SPECIAL_CASES.contains(obsProp.getName().toLowerCase()))
        )
        {
            comp = fac.createQuantity();

            if (uom.getDefinition() != null && uom.getDefinition().startsWith(UCUM_URI_PREFIX))
                ((SWEBuilders.QuantityBuilder)comp).uomCode(uom.getDefinition().replace(UCUM_URI_PREFIX, ""));
            else if(uom.getDefinition() != null && uom.getDefinition().startsWith(UCUM_URI_PREFIX))
                ((SWEBuilders.QuantityBuilder)comp).uomCode(cleanUom(uom.getDefinition()));
            else
                ((SWEBuilders.QuantityBuilder) comp).uomUri(cleanUom(uom.getDefinition())); // TODO: Fix erroneous units
        }
        else if (checkDefinition(IObservation.OBS_TYPE_CATEGORY, obsType))
            comp = fac.createCategory();
        else if (checkDefinition(IObservation.OBS_TYPE_COUNT, obsType))
            comp = fac.createCount();
        else if (checkDefinition(IObservation.OBS_TYPE_RECORD, obsType))
            comp = fac.createRecord();
        else
            comp = fac.createText();

        var definition = obsProp.getDefinition();
        try {
            URI.create(definition);
        } catch (IllegalArgumentException e) {
            definition = SWEHelper.getPropertyUri(definition.replace(" ", ""));
        }

        if (comp != null)
        {
            return comp.id(obsProp.getId().toString())
                    .name(SWEDataUtils.toNCName(obsProp.getName()))
                    .label(obsProp.getName())
                    .description(obsProp.getDescription())
                    .definition(definition)
                    .build();
        }

        return null;
    }

    public static ObsData toObsData(Observation obs, BigId dsId, BigId foiId)
    {
        // Check getAsDateTime vs getInterval
        Instant phenomenonTime = null;
        if(obs.getPhenomenonTime() != null && !obs.getPhenomenonTime().isInterval())
            phenomenonTime = obs.getPhenomenonTime().getAsDateTime().toInstant().truncatedTo(ChronoUnit.MILLIS);
        else if (obs.getPhenomenonTime() != null && obs.getPhenomenonTime().isInterval())
            phenomenonTime = obs.getPhenomenonTime().getAsInterval().getStart();

        Instant resultTime = null;
        if(obs.getResultTime() != null)
            resultTime = obs.getResultTime().toInstant().truncatedTo(ChronoUnit.MILLIS);

        DataBlock dataBlock = createDataBlock(phenomenonTime, obs.getResult());

        return new ObsData.Builder()
                .withDataStream(dsId)
                .withFoi(foiId == null ? IObsData.NO_FOI : foiId)
                .withPhenomenonTime(phenomenonTime)
                .withResultTime(resultTime)
                .withResult(dataBlock)
                .build();
    }

//    public ObsData toObsData(Location location, BigId dsId, BigId foiId)
//    {
//        // Check getAsDateTime vs getInterval
//        Instant phenomenonTime = null;
//        if(obs.getPhenomenonTime() != null && !obs.getPhenomenonTime().isInterval())
//            phenomenonTime = obs.getPhenomenonTime().getAsDateTime().toInstant().truncatedTo(ChronoUnit.MILLIS);
//        else if (obs.getPhenomenonTime() != null && obs.getPhenomenonTime().isInterval())
//            phenomenonTime = obs.getPhenomenonTime().getAsInterval().getStart();
//
//        Instant resultTime = null;
//        if(obs.getResultTime() != null)
//            resultTime = obs.getResultTime().toInstant().truncatedTo(ChronoUnit.MILLIS);
//
//        DataBlock dataBlock = createDataBlock(phenomenonTime, obs.getResult());
//
//        return new ObsData.Builder()
//                .withDataStream(dsId)
//                .withFoi(foiId == null ? IObsData.NO_FOI : foiId)
//                .withPhenomenonTime(phenomenonTime)
//                .withResultTime(resultTime)
//                .withResult(dataBlock)
//                .build();
//    }

//    public DataComponent toLocationOutput()
//    {
//        var record = fac.createRecord()
//    }
//
//    public ObsData toLocationObs(Location location, BigId dsId)
//    {
//
//    }

    public static DataBlock createDataBlock(Instant timestamp, Object val)
    {
        var timestampBlock = new DataBlockDouble(1);
        timestampBlock.setDoubleValue(timestamp.toEpochMilli() / 1000.);

        var dataBlock = createDataBlock(val);
        if(dataBlock instanceof DataBlockMixed)
        {
            ((DataBlockMixed)dataBlock).getUnderlyingObject()[0] = timestampBlock;
            return dataBlock;
        }
        else
        {
            var wrapperBlock = new DataBlockMixed(2, 2);
            wrapperBlock.getUnderlyingObject()[0] = timestampBlock;
            wrapperBlock.getUnderlyingObject()[1] = (AbstractDataBlock) dataBlock;
            return wrapperBlock;
        }
    }

    public static DataBlock createDataBlock(Object val)
    {
        DataBlock dataBlock;

        if(val instanceof Integer)
        {
            dataBlock = new DataBlockInt(1);
            dataBlock.setIntValue((Integer)val);
        }
        else if (val instanceof Long)
        {
            dataBlock = new DataBlockLong(1);
            dataBlock.setLongValue((Long)val);
        }
        else if (val instanceof Number)
        {
            dataBlock = new DataBlockDouble(1);
            dataBlock.setDoubleValue(((Number)val).doubleValue());
        }
        else if (val instanceof String)
        {
            if(((String) val).isEmpty())
                dataBlock = new DataBlockString(1);
            else
                dataBlock = new DataBlockString(((String) val).length());
            dataBlock.setStringValue((String)val);
        }
        else if (val instanceof ArrayList)
        {
            var elements = (ArrayList)val;
            var numElements = elements.size();
            var blockSize = numElements + 1;
            dataBlock = new DataBlockMixed(blockSize, blockSize);
            for (int i = 0; i < numElements; i++)
            {
                var childBlock = (AbstractDataBlock)createDataBlock(elements.get(i));
                ((DataBlockMixed)dataBlock).getUnderlyingObject()[i+1] = childBlock;
            }
        }
        else
            throw new IllegalArgumentException("Unsupported result type: " + val.getClass().getSimpleName());

        return dataBlock;
    }

    protected static AbstractFeature toGmlFeature(FeatureOfInterest foi, String uid)
    {
        Asserts.checkArgument(GEOJSON_FORMAT.equals(foi.getEncodingType()) || VND_GEOJSON_FORMAT.equals(foi.getEncodingType()), "Unsupported feature format: %s", foi.getEncodingType());
        GeoJsonObject geojson = (GeoJsonObject)foi.getFeature();

        AbstractFeature f;
        if (geojson != null)
            f = toSamplingFeature(geojson);
        else
            f = new GenericFeatureImpl(new QName("Feature"));

        f.setUniqueIdentifier(featureUidPrefix + uid);
        f.setName(foi.getName());
        f.setDescription(foi.getDescription());
        return f;
    }

    protected static AbstractFeature toGmlFeature(Location location, String uid)
    {
        GenericFeature f;
        f = new GenericFeatureImpl(new QName("Location"));
        f.setUniqueIdentifier(featureUidPrefix + uid);
        f.setName(location.getName());
        f.setDescription(location.getDescription());

        if(GEOJSON_FORMAT.equals(location.getEncodingType()) || VND_GEOJSON_FORMAT.equals(location.getEncodingType())) {
            GeoJsonObject geojson = (GeoJsonObject) location.getLocation();

            if (geojson != null)
                f.setGeometry(toGmlGeometry(geojson));
        } else {
            var featureProperties = new AbstractGeometryImpl();

            if(location.getLocation() instanceof ObjectNode) {
                var fields = ((ObjectNode) location.getLocation()).fields();
                while(fields.hasNext()) {
                    var field = fields.next();
                    f.setProperty(new QName(field.getKey()), fromJsonNode(field.getValue()));
                }
            }

            f.setGeometry(null);
        }

        return f;
    }

    private static Serializable fromJsonNode(JsonNode node) {
        switch (node.getNodeType()) {
            case NUMBER:
                return node.asDouble();
            case STRING:
                return node.asText();
            case BOOLEAN:
                return node.asBoolean();
        }

        return null;
    }

    public static AbstractFeature toSamplingFeature(org.geojson.GeoJsonObject geojson)
    {
        if (geojson instanceof org.geojson.Feature)
            geojson = ((org.geojson.Feature)geojson).getGeometry();

        if (geojson instanceof org.geojson.Point)
        {
            SamplingPoint sf = new SamplingPoint();
            sf.setGeometry(toGmlGeometry(geojson));
            return sf;
        }
        else if (geojson instanceof org.geojson.LineString)
        {
            SamplingCurve sf = new SamplingCurve();
            sf.setGeometry(toGmlGeometry(geojson));
            return sf;
        }
        else if (geojson instanceof org.geojson.Polygon)
        {
            SamplingSurface sf = new SamplingSurface();
            sf.setGeometry(toGmlGeometry(geojson));
            return sf;
        }
        else if (geojson instanceof org.geojson.Feature)
        {
            SamplingFeature<?> sf = new SamplingFeature<>();
            sf.setGeometry(toGmlGeometry(geojson));
            return sf;
        }
        else
            throw new IllegalArgumentException("Unsupported geometry: " + geojson.getClass().getSimpleName());
    }

    public static AbstractGeometry toGmlGeometry(org.geojson.GeoJsonObject geojson)
    {
        GMLFactory fac = new GMLFactory(true);

        if (geojson instanceof org.geojson.Point)
        {
            LngLatAlt coords = ((org.geojson.Point)geojson).getCoordinates();

            var p = fac.newPoint();
            setGeomSrs(p, coords);
            p.setPos(coords.hasAltitude() ?
                    new double[] {coords.getLatitude(), coords.getLongitude(), coords.getAltitude()} :
                    new double[] {coords.getLatitude(), coords.getLongitude()});

            return p;
        }
        else if (geojson instanceof org.geojson.LineString)
        {
            var coords = ((org.geojson.LineString)geojson).getCoordinates();
            Asserts.checkArgument(coords.size() >= 2, "LineString must contain at least 2 points");

            var line = fac.newLineString();
            setGeomSrs(line, coords.get(0));
            line.setPosList(toPosList(coords, line.getSrsDimension()));

            return line;
        }
        else if (geojson instanceof org.geojson.Polygon)
        {
            Polygon polygon = (Polygon) geojson;
            var p = fac.newPolygon();
            // TODO: Translate polygon
            return p;
        }
        else if (geojson instanceof org.geojson.Feature)
        {
            if (!(((Feature) geojson).getGeometry() instanceof Feature))
                return toGmlGeometry(((Feature) geojson).getGeometry());
        }

        throw new IllegalArgumentException("Unsupported geometry: " + geojson.getClass().getSimpleName());
    }

    public static void setGeomSrs(net.opengis.gml.v32.AbstractGeometry geom, LngLatAlt lla)
    {
        if (lla.hasAltitude())
        {
            geom.setSrsDimension(3);
            geom.setSrsName(SWEConstants.REF_FRAME_4979);
        }
        else
        {
            geom.setSrsDimension(2);
            geom.setSrsName(SWEConstants.REF_FRAME_4326);
        }
    }

    public static double[] toPosList(List<LngLatAlt> coords, int numDims)
    {
        int i = 0;
        double[] posList = new double[coords.size()*numDims];

        for (LngLatAlt p: coords)
        {
            posList[i++] = p.getLatitude();
            posList[i++] = p.getLongitude();
            if (numDims == 3)
                posList[i++] = p.getAltitude();
        }

        return posList;
    }

}
