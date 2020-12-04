/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.perst;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import net.opengis.gml.v32.AbstractFeature;
import org.garret.perst.Index;
import org.garret.perst.IteratorWrapper;
import org.garret.perst.PersistentResource;
import org.garret.perst.RectangleRn;
import org.garret.perst.SpatialIndexRn;
import org.garret.perst.Storage;
import org.sensorhub.api.persistence.IFeatureFilter;
import org.sensorhub.api.persistence.IFeatureStorage;
import org.vast.util.Bbox;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;


/**
 * <p>
 * PERST implementation of FoI store with indexes and search methods
 * </p>
 *
 * @author Alex Robin
 * @since May 8, 2015
 */
class FeatureStoreImpl extends PersistentResource implements IFeatureStorage
{
    Index<AbstractFeature> idIndex;
    SpatialIndexRn<AbstractFeature> geoIndex;
    
    
    // default constructor needed on Android JVM
    FeatureStoreImpl() {}

    
    FeatureStoreImpl(Storage db)
    {
        idIndex = db.createIndex(String.class, true);
        geoIndex = db.createSpatialIndexRn();
    }
    
    
    @Override
    public int getNumFeatures()
    {
        try
        {
            idIndex.sharedLock();            
            return idIndex.size();
        }
        finally
        {
            idIndex.unlock();
        }
    }
    
    
    @Override
    public int getNumMatchingFeatures(IFeatureFilter filter)
    {
        int count = 0;
        Iterator<String> it = getFeatureIDs(filter);
        while (it.hasNext())
        {
            it.next();
            count++;
        }
        return count;
    }
    
    
    @Override
    public Bbox getFeaturesSpatialExtent()
    {
        try
        {
            geoIndex.sharedLock();
            RectangleRn boundingRect = geoIndex.getWrappingRectangle();
            if (boundingRect == null)
                return null;
            
            return PerstUtils.toBbox(boundingRect);
        }
        finally
        {
            geoIndex.unlock();
        }
    }
    
    
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
    
    
    public Iterator<AbstractFeature> getFeatures(IFeatureFilter filter)
    {        
        // TODO handle ROI + IDs but it's not very useful in practice 
                
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
                    try
                    {
                        idIndex.sharedLock();
                        AbstractFeature currentFeature = nextFeature;
                        nextFeature = null;                        
                        while (nextFeature == null && idsIt.hasNext())
                            nextFeature = idIndex.get(idsIt.next());                                            
                        return currentFeature;
                    }
                    finally
                    {
                        idIndex.unlock();
                    }
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
            double[] coords = new double[] {env.getMinX(), env.getMinY(), Double.NEGATIVE_INFINITY, env.getMaxX(), env.getMaxY(), Double.POSITIVE_INFINITY};
            final Iterator<AbstractFeature> geoIt = geoIndex.iterator(new RectangleRn(coords));
            
            // geoIndex iterator cannot be read concurrently with writes
            // thus when iterating using geo filter, care must be taken to synchronize on the storage
            
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
                        AbstractFeature f = geoIt.next();
                        Geometry geom = (Geometry)f.getGeometry();
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
            try
            {
                idIndex.sharedLock();
                
                // return iterator protected against concurrent writes
                Iterator<AbstractFeature> it = new IteratorWrapper<AbstractFeature>(idIndex.iterator())
                {
                    AbstractFeature nextFeature;
                    
                    public boolean hasNext()
                    {
                        return (nextFeature != null);
                    }
    
                    public AbstractFeature next()
                    {
                        try
                        {
                            idIndex.sharedLock();
                            AbstractFeature currentFeature = nextFeature;
                            if (super.hasNext())
                                nextFeature = super.next();
                            else
                                nextFeature = null;
                            return currentFeature;
                        }
                        finally
                        {
                            idIndex.unlock();
                        }
                    }
                    
                    public final void remove()
                    {
                        throw new UnsupportedOperationException();
                    }
                };
                
                it.next();
                return it;
            }
            finally
            {
                idIndex.unlock();
            }
        }
    }
    
    
    public void store(AbstractFeature foi)
    {
        boolean newFoi;
        
        try
        {
            idIndex.exclusiveLock();
            geoIndex.exclusiveLock();
            
            newFoi = idIndex.put(foi.getUniqueIdentifier(), foi);
            if (newFoi && foi.getGeometry() != null)
            {
                RectangleRn rect = PerstUtils.getBoundingRectangle(foi.getGeometry());
                geoIndex.put(rect, foi);
            }
        }
        finally
        {
            idIndex.unlock();
            geoIndex.unlock();
        }
    }
    
    
    void remove(String fid)
    {
        try
        {
            idIndex.exclusiveLock();
            geoIndex.exclusiveLock();
            
            AbstractFeature oldFoi = idIndex.remove(fid);
            if (oldFoi != null)
            {
                geoIndex.remove(oldFoi);
                getStorage().deallocate(oldFoi);
            }
        }
        finally
        {
            idIndex.unlock();
            geoIndex.unlock();
        }
    }

}
