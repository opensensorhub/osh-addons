/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.h2;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import org.h2.mvstore.Cursor;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.Page;
import org.h2.mvstore.rtree.MVRTreeMap;
import org.h2.mvstore.rtree.SpatialKey;
import org.sensorhub.api.persistence.IFeatureFilter;
import org.sensorhub.api.persistence.IFeatureStorage;
import org.vast.util.Bbox;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import net.opengis.gml.v32.AbstractFeature;


public class MVFeatureStoreImpl implements IFeatureStorage
{
    MVMap<String, AbstractFeature> idIndex;
    MVRTreeMap<String> spatialIndex;
    
    
    public MVFeatureStoreImpl(MVStore mvStore)
    {
        idIndex = mvStore.openMap(":fid", new MVMap.Builder<String, AbstractFeature>().valueType(new KryoDataType()));
        spatialIndex = mvStore.openMap(":frt", new MVRTreeMap.Builder<String>().dimensions(3));
    }
    
    
    @Override
    public int getNumFeatures()
    {
        return idIndex.size();
    }


    @Override
    public int getNumMatchingFeatures(IFeatureFilter filter)
    {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public Bbox getFeaturesSpatialExtent()
    {
        Bbox extent = new Bbox();
        
        Page root = spatialIndex.getRoot();
        for (int i = 0; i < root.getKeyCount(); i++)
        {
            SpatialKey key = (SpatialKey)root.getKey(i);
            extent.add(new Bbox(key.min(0), key.min(1), key.min(2),
                                key.max(0), key.max(1), key.max(2)));
        }
        
        return extent;
    }
    

    @Override
    public Iterator<String> getFeatureIDs(IFeatureFilter filter)
    {
        // could we optimize implementation to avoid loading whole feature objects?
        // -> not worth it since with spatial filter we need to read geometries anyway
        
        final Iterator<AbstractFeature> it = getFeatures(filter);
        
        return new Iterator<String>()
        {
            public boolean hasNext()
            {
                return it.hasNext();
            }

            public String next()
            {
                return (String)it.next().getUniqueIdentifier();
            }

            public void remove()
            {
                throw new UnsupportedOperationException();
            }            
        };
    }


    @Override
    public Iterator<AbstractFeature> getFeatures(IFeatureFilter filter)
    {
        // case of requesting by IDs
        Collection<String> foiIDs = filter.getFeatureIDs();
        if (foiIDs != null && !foiIDs.isEmpty())
        {
            final Set<String> ids = new LinkedHashSet<String>();
            ids.addAll(filter.getFeatureIDs());
            final Iterator<String> idsIt = ids.iterator();
            
            // return iterator protected against concurrent writes
            Iterator<AbstractFeature> it = new Iterator<AbstractFeature>()
            {
                AbstractFeature nextFeature;
                
                public boolean hasNext()
                {
                    return (nextFeature != null);
                }

                public AbstractFeature next()
                {
                    AbstractFeature currentFeature = nextFeature;
                    nextFeature = null;                        
                    while (nextFeature == null && idsIt.hasNext())
                        nextFeature = idIndex.get(idsIt.next());                                            
                    return currentFeature;
                }

                public void remove()
                {
                    throw new UnsupportedOperationException();
                }
            };
            
            it.next();
            return it;
        }
            
        // case of ROI
        else if (filter.getRoi() != null)
        {
            final Polygon roi = filter.getRoi();
            
            // iterate through spatial index using bounding rectangle
            Envelope env = roi.getEnvelopeInternal();
            SpatialKey bbox = new SpatialKey(0, (float)env.getMinX(), (float)env.getMinY(), Float.NEGATIVE_INFINITY,
                                                (float)env.getMaxX(), (float)env.getMaxY(), Float.POSITIVE_INFINITY);
            final Iterator<SpatialKey> geoIt = spatialIndex.findContainedKeys(bbox);
            
            // wrap with iterator to filter on exact polygon geometry using JTS
            Iterator<AbstractFeature> it =  new Iterator<AbstractFeature>()
            {
                AbstractFeature nextFeature;
                
                public boolean hasNext()
                {
                    return (nextFeature != null);
                }

                public AbstractFeature next()
                {
                    AbstractFeature currentFeature = nextFeature;
                    nextFeature = null;
                    
                    while (nextFeature == null && geoIt.hasNext())
                    {
                        String fid = spatialIndex.get(geoIt.next());
                        AbstractFeature f = idIndex.get(fid);
                        Geometry geom = (Geometry)f.getLocation();
                        if (geom != null && roi.intersects(geom))
                            nextFeature = f;
                    }
                    
                    return currentFeature;
                }

                public void remove()
                {
                    throw new UnsupportedOperationException();
                } 
            };
            
            it.next();
            return it;
        }
                
        // else return all features
        else
        {
            final Cursor<String, AbstractFeature> cursor = idIndex.cursor(idIndex.firstKey());
            return new Iterator<AbstractFeature>()
            {
                @Override
                public boolean hasNext()
                {
                    return cursor.hasNext();
                }

                @Override
                public AbstractFeature next()
                {
                    cursor.next();
                    return cursor.getValue();
                }

                @Override
                public void remove()
                {
                    throw new UnsupportedOperationException();                    
                }                
            };
        }
    }
    
    
    public void store(AbstractFeature f)
    {
        
    }

}
