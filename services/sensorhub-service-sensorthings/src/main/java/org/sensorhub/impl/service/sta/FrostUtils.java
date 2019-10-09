/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2019 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sta;

import java.util.ArrayList;
import java.util.List;
import org.geojson.LngLatAlt;
import org.vast.ogc.om.SamplingCurve;
import org.vast.ogc.om.SamplingPoint;
import org.vast.swe.SWEConstants;
import org.vast.util.Asserts;
import de.fraunhofer.iosb.ilt.frostserver.model.core.Id;
import de.fraunhofer.iosb.ilt.frostserver.path.EntityPathElement;
import de.fraunhofer.iosb.ilt.frostserver.path.EntitySetPathElement;
import de.fraunhofer.iosb.ilt.frostserver.path.EntityType;
import de.fraunhofer.iosb.ilt.frostserver.path.ResourcePath;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.LinearRing;
import net.opengis.gml.v32.impl.GMLFactory;

/**
 * <p>
 * Helper methods to deal with FROST objects
 * </p>
 *
 * @author Alex Robin
 * @date Sep 12, 2019
 */
public class FrostUtils
{

    public static final ResourcePath copy(ResourcePath path)
    {
        ResourcePath copy = new ResourcePath(path.getServiceRootUrl(), path.getPathUrl());
        
        for (int i = 0; i < path.size(); i++)
            copy.addPathElement(path.get(i));
        
        return copy;
    }
    
    
    public static final ResourcePath getNavigationLinkPath(Id parentId, EntityType parentType, EntityType linkedType)
    {
        ResourcePath linkedPath = new ResourcePath(null, null);
        linkedPath.setIdentifiedElement(new EntityPathElement(parentId, parentType, null));
        linkedPath.addPathElement(linkedPath.getIdentifiedElement());
        linkedPath.addPathElement(new EntitySetPathElement(linkedType, linkedPath.getLastElement()));
        return linkedPath;
    }
    
    
    public static final ResourcePath getNavigationLinkPath(Id parentId, EntityType parentType, Id linkedId, EntityType linkedType)
    {
        ResourcePath linkedPath = new ResourcePath(null, null);
        linkedPath.addPathElement(new EntityPathElement(parentId, parentType, null));
        linkedPath.setIdentifiedElement(new EntityPathElement(linkedId, linkedType, linkedPath.getLastElement()));
        linkedPath.addPathElement(linkedPath.getIdentifiedElement());
        return linkedPath;
    }
    
    
    public static AbstractFeature toGmlFeature(org.geojson.GeoJsonObject geojson)
    {
        GMLFactory fac = new GMLFactory();
        if (geojson instanceof org.geojson.Feature)
            geojson = ((org.geojson.Feature)geojson).getGeometry();
        
        if (geojson instanceof org.geojson.Point)
        {
            LngLatAlt coords = ((org.geojson.Point)geojson).getCoordinates();
            
            var p = fac.newPoint();
            setGeomSrs(p, coords);
            p.setPos(coords.hasAltitude() ?
                new double[] {coords.getLatitude(), coords.getLongitude(), coords.getAltitude()} :
                new double[] {coords.getLatitude(), coords.getLongitude()});            
            
            SamplingPoint sf = new SamplingPoint();
            sf.setGeometry(p);
            return sf;
        }
        else if (geojson instanceof org.geojson.LineString)
        {
            var coords = ((org.geojson.LineString)geojson).getCoordinates();
            Asserts.checkArgument(coords.size() >= 2, "LineString must contain at least 2 points");
            
            var line = fac.newLineString();
            setGeomSrs(line, coords.get(0));
            line.setPosList(toPosList(coords, line.getSrsDimension()));
            
            SamplingCurve sf = new SamplingCurve();
            sf.setGeometry(line);
            return sf;
        }
        else if (geojson instanceof org.geojson.Polygon)
        {
            
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
    
    
    public static org.geojson.GeoJsonObject toGeoJsonGeom(net.opengis.gml.v32.AbstractGeometry gmlGeom)
    {
        Asserts.checkArgument(SWEConstants.REF_FRAME_4326.equals(gmlGeom.getSrsName()) ||
            SWEConstants.REF_FRAME_4979.equals(gmlGeom.getSrsName()), "Only EPSG:4326 and EPSG:4979 CRS are supported in GeoJson");
        
        if (gmlGeom instanceof net.opengis.gml.v32.Point)
        {
            var pos = ((net.opengis.gml.v32.Point)gmlGeom).getPos();
            if (pos.length == 2)
                return new org.geojson.Point(pos[1], pos[0]);
            else
                return new org.geojson.Point(pos[1], pos[0], pos[2]);
        }
        else if (gmlGeom instanceof net.opengis.gml.v32.LineString)
        {
            var line = new org.geojson.LineString();            
            var posList = ((net.opengis.gml.v32.LineString)gmlGeom).getPosList();
            var coords = toGeoJsonCoords(gmlGeom.getSrsDimension(), posList);
            line.setCoordinates(coords);
        }
        else if (gmlGeom instanceof net.opengis.gml.v32.Polygon)
        {
            var poly = new org.geojson.Polygon();            
            var posList = ((net.opengis.gml.v32.Polygon)gmlGeom).getExterior().getPosList();
            var ring = toGeoJsonCoords(gmlGeom.getSrsDimension(), posList);
            poly.setExteriorRing(ring);
            
            for (LinearRing hole: ((net.opengis.gml.v32.Polygon)gmlGeom).getInteriorList())
            {
                posList = hole.getPosList();
                ring = toGeoJsonCoords(gmlGeom.getSrsDimension(), posList);
                poly.addInteriorRing(ring);
            }
        }
        
        return null;        
    }
    
    
    protected static ArrayList<LngLatAlt> toGeoJsonCoords(int numDims, double[] posList)
    {
        if (numDims == 2) // 2D case
        {
            var coords = new ArrayList<LngLatAlt>(posList.length / 2);
            for (int i = 0; i < posList.length; i+=2)
                coords.add(new LngLatAlt(posList[i+1], posList[i]));
            return coords;
        }
        else // 3D case
        {
            var coords = new ArrayList<LngLatAlt>(posList.length / 3);
            for (int i = 0; i < posList.length; i+=3)
                coords.add(new LngLatAlt(posList[i+1], posList[i], posList[i+2]));
            return coords;
        }
    }
}
