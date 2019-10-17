/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.persistence.h2;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.persistence.IObsFilter;
import org.sensorhub.api.persistence.ObsFilter;
import org.sensorhub.api.persistence.ObsKey;
import org.sensorhub.impl.persistence.h2.MVMultiStorageImpl;
import org.sensorhub.impl.persistence.h2.MVStorageConfig;
import org.sensorhub.test.persistence.AbstractTestMultiObsStorage;
import org.vast.data.TextEncodingImpl;
import org.vast.swe.SWEConstants;
import org.vast.swe.helper.GeoPosHelper;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;


public class TestMVMultiStorage extends AbstractTestMultiObsStorage<MVMultiStorageImpl>
{
    File dbFile;
    
    
    @Before
    public void init() throws Exception
    {
        MVStorageConfig config = new MVStorageConfig();
        config.memoryCacheSize = 1024;
        dbFile = File.createTempFile("testdb", ".dat");
        dbFile.deleteOnExit();
        config.storagePath = dbFile.getAbsolutePath();
        
        storage = new MVMultiStorageImpl();
        storage.init(config);
        storage.start();
    }
    

    @Override
    protected void forceReadBackFromStorage() throws Exception
    {
        storage.stop();
        storage.start();
    }
    
    
    @After
    public void cleanup() throws Exception
    {
        storage.stop();
        System.out.println("DB file size was " + dbFile.length()/1024 + "KB");
        dbFile.delete();
    }
    
    
    protected DataComponent createLocationDs() throws Exception
    {
        GeoPosHelper fac = new GeoPosHelper();
        DataComponent rec = fac.newDataRecord();
        rec.setName("track");
        rec.addComponent("time", fac.newTimeIsoUTC(SWEConstants.DEF_SAMPLING_TIME, null, null));
        rec.addComponent("loc", fac.newLocationVectorLLA(SWEConstants.DEF_SAMPLING_LOC));
        storage.addRecordStore(rec.getName(), rec, new TextEncodingImpl());
        return rec;
    }
    
    
    protected String getFoiID(double time)
    {
        return "foi" + (int)(time / 100.);
    }
    
    
    protected List<DataBlock> addLocationObsToStorage(DataComponent recordDef, boolean reopenStorage) throws Exception
    {
        // write N records
        final double timeStep = 0.1;
        final int numRecords = 50030;
        List<DataBlock> dataList = new ArrayList<>(numRecords);
        
        long t0 = System.currentTimeMillis();
        for (int i=0; i<numRecords; i++)
        {
            double time = i*timeStep;
            DataBlock data = recordDef.createDataBlock();
            data.setDoubleValue(0, time);
            data.setDoubleValue(1, (double)i*10);
            data.setDoubleValue(2, (double)i*5+1);
            data.setDoubleValue(3, 0.0);
            dataList.add(data);
            ObsKey key = new ObsKey(recordDef.getName(), producerID, getFoiID(time), i*timeStep);
            storage.storeRecord(key, data);
        }
        
        storage.commit();
        System.out.println(numRecords + " records inserted in " + (System.currentTimeMillis()-t0) + " ms");
        
        if (reopenStorage)
            forceReadBackFromStorage();
        
        return dataList;
    }
    
    
    protected IObsFilter buildSpatialLocationFilter(DataComponent recordDef, List<DataBlock> dataList, final Polygon roi, final double[] timeRange)
    {
        return buildSpatialLocationFilter(recordDef, dataList, roi, timeRange, null);
    }
    
    
    protected IObsFilter buildSpatialLocationFilter(DataComponent recordDef, List<DataBlock> dataList, final Polygon roi, final double[] timeRange, final Set<String> foiIDs)
    {
        IObsFilter filter = new ObsFilter(recordDef.getName()) {
            @Override
            public double[] getTimeStampRange() { return timeRange; }
            @Override
            public Polygon getRoi() { return roi; }
            @Override
            public Set<String> getFoiIDs() { return foiIDs; }
            @Override
            public Set<String> getProducerIDs() {return producerFilterList; };
        };
        
        // generate test list
        //long t0 = System.currentTimeMillis();
        Iterator<DataBlock> it = dataList.iterator();
        Coordinate coord = new Coordinate();
        Point point = new GeometryFactory().createPoint(coord);
        while (it.hasNext())
        {
            DataBlock dataBlk = it.next();
            
            // keep record only if within time range
            double time = dataBlk.getDoubleValue(0);
            if (timeRange != null && (time < timeRange[0] || time > timeRange[1]))
            {
                it.remove();
                continue;
            }
            
            // keep record only if from one of the selected FOIs
            if (foiIDs != null && !foiIDs.contains(getFoiID(time)))
            {
                it.remove();
                continue;
            }
            
            // keep record only if intersecting roi
            coord.x = dataBlk.getDoubleValue(1);
            coord.y = dataBlk.getDoubleValue(2);
            point.geometryChanged();
            if (!roi.intersects(point))
                it.remove();
        }
        //System.out.println(dataList.size() + " test records scanned in " + (System.currentTimeMillis()-t0) + " ms");
        
        return filter;
    }
        
    
    @Test
    public void testFilterOnSamplingLocation() throws Exception
    {
        storage.getConfiguration().indexObsLocation = true;
        addProducersToStorage();
        addFoisToStorage(null);
        
        DataComponent recordDef = createLocationDs();
        List<DataBlock> dataList = addLocationObsToStorage(recordDef, true);
        List<DataBlock> testList = new ArrayList<>(dataList.size());
        IObsFilter filter;
        double[] timeRange = null;
        Polygon roi;
        
        // small region: bbox
        testList.clear();
        testList.addAll(dataList);
        roi = new GeometryFactory().createPolygon(new Coordinate[] {
            new Coordinate( 1400.,  25.),
            new Coordinate(1500.,  25.),
            new Coordinate(1500., 1300.),
            new Coordinate( 1400., 1300.),
            new Coordinate( 1400.,  25.)
        });
        filter = buildSpatialLocationFilter(recordDef, testList, roi, timeRange);
        long t0 = System.currentTimeMillis();
        checkFilteredResults(filter, testList);
        System.out.println(testList.size() + " records read in " + (System.currentTimeMillis()-t0) + " ms");
        
        // larger region: polygon
        testList.clear();
        testList.addAll(dataList);
        roi = new GeometryFactory().createPolygon(new Coordinate[] {
            new Coordinate( 400.,  46.),
            new Coordinate(20500.,  23.),
            new Coordinate(35600., 13560.),
            new Coordinate( 400., 21355.),
            new Coordinate( 400.,  46.)
        });
        filter = buildSpatialLocationFilter(recordDef, testList, roi, timeRange);        
        t0 = System.currentTimeMillis();
        checkFilteredResults(filter, testList);
        System.out.println(testList.size() + " records read in " + (System.currentTimeMillis()-t0) + " ms");
        
        // small region intersecting last cluster
        testList.clear();
        testList.addAll(dataList);
        roi = new GeometryFactory().createPolygon(new Coordinate[] {
            new Coordinate(490000., 236652.),
            new Coordinate(500100., 236652.),
            new Coordinate(500100., 250146.),
            new Coordinate(490000., 250146.),
            new Coordinate(490000., 236652.)
        });
        filter = buildSpatialLocationFilter(recordDef, testList, roi, timeRange);        
        t0 = System.currentTimeMillis();
        checkFilteredResults(filter, testList);
        System.out.println(testList.size() + " records read in " + (System.currentTimeMillis()-t0) + " ms");
    }
    
    
    @Test
    public void testFilterOnSamplingLocationAndTime() throws Exception
    {
        storage.getConfiguration().indexObsLocation = true;
        addProducersToStorage();
        addFoisToStorage(null);
        
        DataComponent recordDef = createLocationDs();
        List<DataBlock> dataList = addLocationObsToStorage(recordDef, false);
        List<DataBlock> testList = new ArrayList<>(dataList.size());
        IObsFilter filter;
        double[] timeRange = null;
        Polygon roi;       
        
        // larger region: polygon
        testList.clear();
        testList.addAll(dataList);
        timeRange = new double[] {100.0, 1600.0};
        roi = new GeometryFactory().createPolygon(new Coordinate[] {
            new Coordinate( 400.,  46.),
            new Coordinate(20500.,  23.),
            new Coordinate(35600., 13560.),
            new Coordinate( 400., 21355.),
            new Coordinate( 400.,  46.)
        });
        filter = buildSpatialLocationFilter(recordDef, testList, roi, timeRange);        
        long t0 = System.currentTimeMillis();
        checkFilteredResults(filter, testList);
        System.out.println(testList.size() + " records read in " + (System.currentTimeMillis()-t0) + " ms");
    }
    
    
    @Test
    public void testFilterOnSamplingLocationAndFoiID() throws Exception
    {
        storage.getConfiguration().indexObsLocation = true;
        addProducersToStorage();
        addFoisToStorage(null);
        
        DataComponent recordDef = createLocationDs();
        List<DataBlock> dataList = addLocationObsToStorage(recordDef, false);
        List<DataBlock> testList = new ArrayList<>(dataList.size());
        IObsFilter filter;
        double[] timeRange = null;
        Polygon roi;       
        
        // polygon + 1 foi
        testList.clear();
        testList.addAll(dataList);
        roi = new GeometryFactory().createPolygon(new Coordinate[] {
            new Coordinate( 400.,  46.),
            new Coordinate(20500.,  23.),
            new Coordinate(35600., 13560.),
            new Coordinate( 400., 21355.),
            new Coordinate( 400.,  46.)
        });
        filter = buildSpatialLocationFilter(recordDef, testList, roi, timeRange, Sets.newHashSet("foi2"));
        long t0 = System.currentTimeMillis();
        checkFilteredResults(filter, testList);
        System.out.println(testList.size() + " records read in " + (System.currentTimeMillis()-t0) + " ms");
        
        // polygon + several disjoint fois
        testList.clear();
        testList.addAll(dataList);
        roi = new GeometryFactory().createPolygon(new Coordinate[] {
            new Coordinate( 400.,  46.),
            new Coordinate(20500.,  23.),
            new Coordinate(35600., 13560.),
            new Coordinate( 400., 21355.),
            new Coordinate( 400.,  46.)
        });
        filter = buildSpatialLocationFilter(recordDef, testList, roi, timeRange, Sets.newHashSet("foi1", "foi3"));        
        t0 = System.currentTimeMillis();
        checkFilteredResults(filter, testList);
        System.out.println(testList.size() + " records read in " + (System.currentTimeMillis()-t0) + " ms");
    }
    
    
    @Test
    public void testFilterOnSamplingLocationWithCache() throws Exception
    {
        storage.getConfiguration().indexObsLocation = true;
        addProducersToStorage();
        addFoisToStorage(null);
        
        DataComponent recordDef = createLocationDs();
        List<DataBlock> dataList = addLocationObsToStorage(recordDef, false);
        List<DataBlock> testList = new ArrayList<>(dataList.size());
        IObsFilter filter;
        double[] timeRange = null;
        Polygon roi;       
        
        // small region intersecting cache and stored clusters
        testList.clear();
        testList.addAll(dataList);
        roi = new GeometryFactory().createPolygon(new Coordinate[] {
            new Coordinate(490000., 236652.),
            new Coordinate(500100., 236652.),
            new Coordinate(500100., 250146.),
            new Coordinate(490000., 250146.),
            new Coordinate(490000., 236652.)
        });
        filter = buildSpatialLocationFilter(recordDef, testList, roi, timeRange);        
        long t0 = System.currentTimeMillis();
        checkFilteredResults(filter, testList);
        System.out.println(testList.size() + " records read in " + (System.currentTimeMillis()-t0) + " ms");
        
        // small region only in cache
        testList.clear();
        testList.addAll(dataList);
        roi = new GeometryFactory().createPolygon(new Coordinate[] {
            new Coordinate(500000., 236652.),
            new Coordinate(500100., 236652.),
            new Coordinate(500100., 250146.),
            new Coordinate(500000., 250146.),
            new Coordinate(500000., 236652.)
        });
        filter = buildSpatialLocationFilter(recordDef, testList, roi, timeRange);        
        t0 = System.currentTimeMillis();
        checkFilteredResults(filter, testList);
        System.out.println(testList.size() + " records read in " + (System.currentTimeMillis()-t0) + " ms");
    }
    
}
