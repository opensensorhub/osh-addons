/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.es.mock;

import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Vector;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.persistence.DataFilter;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.impl.persistence.es.ESBasicStorageConfig;
import org.sensorhub.impl.persistence.es.ESBasicStorageImpl;
import org.sensorhub.test.TestUtils;
import org.sensorhub.test.persistence.AbstractTestBasicStorage;
import org.vast.data.TextEncodingImpl;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class TestEsBasicStorage extends AbstractTestBasicStorage<ESBasicStorageImpl> {

    protected static final String CLUSTER_NAME = "elasticsearch";

    private static final boolean clean_index = true;

    @Before
    public void init() throws Exception {


        ESBasicStorageConfig config = new ESBasicStorageConfig();
        config.autoStart = true;
        config.clusterName = CLUSTER_NAME;
        List<String> nodes = new ArrayList<String>();
        nodes.add("localhost:9200");
        nodes.add("localhost:9201");

        config.nodeUrls = nodes;
        config.bulkConcurrentRequests = 0;
        config.id = "junit_testesbasicstorage_" + System.currentTimeMillis();
        config.indexNamePrepend = "data_" + config.id + "_";
        config.indexNameMetaData = "meta_" + config.id + "_";

        storage = new ESBasicStorageImpl();
        storage.init(config);
        storage.start();
    }

    @After
    public void after() throws SensorHubException {
        // Delete added index
        storage.commit();
        if (clean_index) {
            DeleteIndexRequest request = new DeleteIndexRequest(storage.getAddedIndex().toArray(new String[storage.getAddedIndex().size()]));
            try {
                storage.getClient().indices().delete(request);
            } catch (IOException ex) {
                throw new SensorHubException(ex.getLocalizedMessage(), ex);
            }
        }
        storage.stop();
    }

    @Override
    protected void forceReadBackFromStorage() throws Exception {
        // Let the time to ES to write the data
        // if some tests are not passed,  try to increase this value first!!
        storage.commit();
    }
    public static void assertDataBlockEquals(DataBlock data1, DataBlock data2) throws Exception {
        Assert.assertEquals("Data blocks are not the same size", (long)data1.getAtomCount(), (long)data2.getAtomCount());

        for(int i = 0; i < data1.getAtomCount(); ++i) {
            Assert.assertEquals(data1.getDataType(i), data2.getDataType(i));

            if(data1.getDataType(i) == DataType.DOUBLE || data1.getDataType(i) == DataType.FLOAT) {
                Assert.assertEquals(data1.getDoubleValue(i), data2.getDoubleValue(i), 1e-6);
            } else {
                Assert.assertEquals("Data blocks values are not equal at index=" + i, data1.getStringValue(i), data2.getStringValue(i));
            }
        }

    }
    @Test
    public void testLocationOutput() throws Exception {
        GeoPosHelper fac = new GeoPosHelper();
        Vector locVector = fac.newLocationVectorLLA(SWEConstants.DEF_SENSOR_LOC);
        locVector.setName("location");
        locVector.setLocalFrame('#' + "locationjunit");
        DataComponent outputStruct = fac.wrapWithTimeStampUTC(locVector);
        outputStruct.setName("sensorLocation");
        outputStruct.setId("SENSOR_LOCATION");
        DataEncoding outputEncoding = new TextEncodingImpl();

        storage.addRecordStore(outputStruct.getName(), outputStruct, outputEncoding);

        double timeStamp = new Date().getTime() / 1000.;
        // build new datablock
        DataBlock dataBlock = outputStruct.createDataBlock();
        Coordinate location = new Coordinate(-1.55336, 47.21725, 15);
        dataBlock.setDoubleValue(0, timeStamp);
        dataBlock.setDoubleValue(1, location.y); //y
        dataBlock.setDoubleValue(2, location.x); //x
        dataBlock.setDoubleValue(3, location.z); //z

        storage.storeRecord(new DataKey(outputStruct.getName(), "e44cb499-3b6c-4305-b479-ebacc965579f", timeStamp), dataBlock);

        forceReadBackFromStorage();

        // Read back
        Iterator<? extends IDataRecord> it = storage.getRecordIterator(new DataFilter(outputStruct.getName()) {
            @Override
            public double[] getTimeStampRange() {
                return new double[]{timeStamp - 5, Double.MAX_VALUE};
            }
        });
        int i = 0;
        while (it.hasNext()) {
            assertDataBlockEquals(dataBlock, it.next().getData());
            i++;
        }
        assertEquals(1, i);
    }

    private static final float[] freqs = new float[]{20, 25, 31.5f, 40, 50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630, 800, 1000, 1250, 1600, 2000, 2500, 3150, 4000, 5000, 6300, 8000, 10000, 12500};

    @Test
    public void testNestedDataBlock() throws Exception {
        SWEHelper fac = new SWEHelper();
        DataComponent acousticData  = fac.newDataRecord();
        acousticData.setName("acoustic_fast");
        acousticData.setDefinition("http://sensorml.com/ont/swe/property/Acoustic");
        acousticData.setDescription("Acoustic indicators measurements");

        // add time, temperature, pressure, wind speed and wind direction fields
        acousticData.addComponent("time", fac.newTimeStampIsoUTC());
        acousticData.addComponent("leq", fac.newQuantity(SWEHelper.getPropertyUri("dBsplFast"), "Leq", null, "dB", DataType.FLOAT));
        acousticData.addComponent("laeq", fac.newQuantity(SWEHelper.getPropertyUri("dBsplFast"), "LAeq", null, "dB(A)", DataType.FLOAT));

        DataRecord nestedRec = fac.newDataRecord(2);
        nestedRec.addComponent("freq", fac.newQuantity(SWEHelper.getPropertyUri("frequency"), "freq", null, "Hz", DataType.FLOAT));
        nestedRec.addComponent("spl", fac.newQuantity(SWEHelper.getPropertyUri("spl"), "spl", null, "dB", DataType.FLOAT));

        DataArray recordDesc = fac.newDataArray(freqs.length);
        recordDesc.setName("spectrum");
        recordDesc.setDefinition("urn:spectrum:third-octave");
        recordDesc.setElementType("elt", nestedRec);
        acousticData.addComponent("spectrum", recordDesc);

        // also generate encoding definition
        DataEncoding acousticEncoding = fac.newTextEncoding(",", "\n");


        storage.addRecordStore(acousticData.getName(), acousticData, acousticEncoding);

        forceReadBackFromStorage();

        DataBlock dataBlock = acousticData.createDataBlock();
        int index = 0;
        dataBlock.setDoubleValue(index++, 1531297249.125);
        dataBlock.setFloatValue(index++, 45.4f);
        dataBlock.setFloatValue(index++, 44.6f);
        for(float freq : freqs) {
            dataBlock.setFloatValue(index++, freq);
            dataBlock.setFloatValue(index++, (float)(22.1 + Math.log10(freq)));
        }
        DataKey dataKey = new DataKey(acousticData.getName(),
                "e44cb499-3b6c-4305-b479-ebacc965579f", dataBlock.getDoubleValue(0));

        storage.storeRecord(dataKey, dataBlock);

        forceReadBackFromStorage();

        DataBlock dataBlock1 = storage.getDataBlock(dataKey);

        TestUtils.assertEquals(dataBlock, dataBlock1);
    }


    @Test
    public void testSimpleArrayDataBlock() throws Exception {
        SWEHelper fac = new SWEHelper();
        DataComponent acousticData  = fac.newDataRecord();
        acousticData.setName("acoustic_fast");
        acousticData.setDefinition("http://sensorml.com/ont/swe/property/Acoustic");
        acousticData.setDescription("Acoustic indicators measurements");

        Count elementCount = fac.newCount();
        elementCount.setValue(8); // 8x125ms

        // add time, temperature, pressure, wind speed and wind direction fields
        acousticData.addComponent("time", fac.newTimeStampIsoUTC());
        acousticData.addComponent("leq", fac.newArray(elementCount, "leq", fac.newQuantity(SWEHelper.getPropertyUri("dBsplFast"), "Leq", null, "dB", DataType.FLOAT)));
        acousticData.addComponent("laeq", fac.newArray(elementCount, "laeq", fac.newQuantity(SWEHelper.getPropertyUri("dBsplFast"), "LAeq", null, "dB(A)", DataType.FLOAT)));
        for(double freq : freqs) {
            String name = "leq_" + Double.valueOf(freq).intValue();
            acousticData.addComponent(name, fac.newArray(elementCount, name, fac.newQuantity(SWEHelper.getPropertyUri("dBsplFast"), name, null, "dB", DataType.FLOAT)));
        }

        // also generate encoding definition
        DataEncoding acousticEncoding = fac.newTextEncoding(",", "\n");


        storage.addRecordStore(acousticData.getName(), acousticData, acousticEncoding);

        forceReadBackFromStorage();

        DataBlock dataBlock = acousticData.createDataBlock();
        int index = 0;
        dataBlock.setDoubleValue(index++, 1531297249.125);
        for(int idStep = 0; idStep < 8; idStep++) {
            dataBlock.setDoubleValue(index++, idStep + 0.1);
        }
        for(int idStep = 0; idStep < 8; idStep++) {
            dataBlock.setDoubleValue(index++, idStep + 0.2);
        }
        for (float freq : freqs) {
            for(int idStep = 0; idStep < 8; idStep++) {
            dataBlock.setDoubleValue(index++, idStep + freq / 100000.);
            }
        }
        DataKey dataKey = new DataKey(acousticData.getName(),
                "e44cb499-3b6c-4305-b479-ebacc965579f", dataBlock.getDoubleValue(0));

        storage.storeRecord(dataKey, dataBlock);

        forceReadBackFromStorage();

        DataBlock dataBlock1 = storage.getDataBlock(dataKey);

        TestUtils.assertEquals(dataBlock, dataBlock1);

    }


    @Test
    public void testNaNSimpleArrayDataBlock() throws Exception {
        SWEHelper fac = new SWEHelper();
        DataComponent acousticData  = fac.newDataRecord();
        acousticData.setName("acoustic_fast");
        acousticData.setDefinition("http://sensorml.com/ont/swe/property/Acoustic");
        acousticData.setDescription("Acoustic indicators measurements");

        Count elementCount = fac.newCount();
        elementCount.setValue(8); // 8x125ms

        // add time, temperature, pressure, wind speed and wind direction fields
        acousticData.addComponent("time", fac.newTimeStampIsoUTC());
        acousticData.addComponent("leq", fac.newArray(elementCount, "leq", fac.newQuantity(SWEHelper.getPropertyUri("dBsplFast"), "Leq", null, "dB", DataType.FLOAT)));
        acousticData.addComponent("laeq", fac.newArray(elementCount, "laeq", fac.newQuantity(SWEHelper.getPropertyUri("dBsplFast"), "LAeq", null, "dB(A)", DataType.FLOAT)));
        for(double freq : freqs) {
            String name = "leq_" + Double.valueOf(freq).intValue();
            acousticData.addComponent(name, fac.newArray(elementCount, name, fac.newQuantity(SWEHelper.getPropertyUri("dBsplFast"), name, null, "dB", DataType.FLOAT)));
        }

        // also generate encoding definition
        DataEncoding acousticEncoding = fac.newTextEncoding(",", "\n");


        storage.addRecordStore(acousticData.getName(), acousticData, acousticEncoding);

        forceReadBackFromStorage();

        DataBlock dataBlock = acousticData.createDataBlock();
        int index = 0;
        dataBlock.setDoubleValue(index++, 1531297249.125);
        for(int idStep = 0; idStep < 8; idStep++) {
            dataBlock.setDoubleValue(index++, idStep + 0.1);
        }
        for(int idStep = 0; idStep < 8; idStep++) {
            dataBlock.setDoubleValue(index++, idStep + 0.2);
        }
        for (float freq : freqs) {
            for(int idStep = 0; idStep < 8; idStep++) {
                if(idStep != 2) {
                    dataBlock.setDoubleValue(index++, idStep + freq / 100000.);
                } else {
                    dataBlock.setDoubleValue(index++, Double.NaN);
                }
            }
        }
        DataKey dataKey = new DataKey(acousticData.getName(),
                "e44cb499-3b6c-4305-b479-ebacc965579f", dataBlock.getDoubleValue(0));

        storage.storeRecord(dataKey, dataBlock);

        forceReadBackFromStorage();

        DataBlock dataBlock1 = storage.getDataBlock(dataKey);

        TestUtils.assertEquals(dataBlock, dataBlock1);

    }
}
