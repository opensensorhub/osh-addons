/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2017 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.usgs.water;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import org.junit.Test;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.IFoiFilter;
import org.sensorhub.api.persistence.IObsFilter;
import org.sensorhub.api.persistence.IRecordStoreInfo;
import org.sensorhub.impl.usgs.water.CodeEnums.ObsParam;
import org.sensorhub.impl.usgs.water.CodeEnums.StateCode;
import org.sensorhub.impl.usgs.water.USGSWaterDataConfig;
import org.sensorhub.impl.usgs.water.USGSWaterDataArchive;
import org.vast.data.TextEncodingImpl;
import org.vast.ogc.om.SamplingPoint;
import org.vast.swe.fast.TextDataWriter;
import org.vast.util.Bbox;
import org.vast.util.DateTimeFormat;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Polygon;
import net.opengis.gml.v32.AbstractFeature;


public class TestWebArchiveConnector
{

    @Test
    public void test() throws Exception
    {
        USGSWaterDataArchive storage = new USGSWaterDataArchive();
        USGSWaterDataConfig config = new USGSWaterDataConfig();
        config.id = "USGS_TEST_STORAGE";
        config.exposeFilter.stateCodes.add(StateCode.AL);
        //config.exposeFilter.countyCodes.add(1089);
        //config.exposeFilter.siteTypes.add(SiteType.ST);
        //config.exposeFilter.siteIds.add("02369800");
        config.exposeFilter.parameters.add(ObsParam.DISCHARGE);
        config.exposeFilter.parameters.add(ObsParam.GAGE_HEIGHT);
        storage.init(config);
        storage.start();
        
        /////////////////
        // lookup FOIs //
        /////////////////
        IFoiFilter filter = new IFoiFilter() {
            @Override
            public Set<String> getFeatureIDs()
            {
                return null;//Arrays.asList("urn:usgs:water:site:02465000");
            }

            @Override
            public Polygon getRoi()
            {
                return new Bbox(30.40477778, -87.8483611, 31.0412902, -85.8521574).toJtsPolygon();
            }

            @Override
            public Set<String> getProducerIDs()
            {
                return null;
            }
        };
        
        Iterator<AbstractFeature> it = storage.getFois(filter);
        while (it.hasNext())
        {
            AbstractFeature f = it.next();
            System.out.println(f.getUniqueIdentifier() + " -> " + ((SamplingPoint)f).getShape());
        }
        
        ////////////////
        // fetch data //
        ////////////////
        final IRecordStoreInfo rs = storage.getRecordStores().values().iterator().next();
        DateTimeFormat time = new DateTimeFormat();
        final double[] timeRange = new double[] {time.parseIso("2017-03-13Z"), time.parseIso("2017-03-14T03:15:00Z")};
        IDataFilter dFilter = new IObsFilter() {
            @Override
            public String getRecordType()
            {
                return rs.getName();
            }

            @Override
            public double[] getTimeStampRange()
            {
                return timeRange;
            }

            @Override
            public Set<String> getProducerIDs()
            {
                return Sets.newHashSet("02362000");
            }

            @Override
            public double[] getResultTimeRange()
            {
                return null;
            }

            @Override
            public Set<String> getFoiIDs()
            {
                return Sets.newHashSet("urn:usgs:water:site:02423555");
            }

            @Override
            public Polygon getRoi()
            {
                return null;
            }            
        };
        
        TextDataWriter writer = new TextDataWriter();
        writer.setOutput(System.out);
        writer.setDataComponents(rs.getRecordDescription());
        writer.setDataEncoding(new TextEncodingImpl());
        Iterator<? extends IDataRecord> it2 = storage.getRecordIterator(dFilter);
        while (it2.hasNext())
        {
            IDataRecord rec = it2.next();
            writer.write(rec.getData());
            writer.flush();
        }
    }

}
