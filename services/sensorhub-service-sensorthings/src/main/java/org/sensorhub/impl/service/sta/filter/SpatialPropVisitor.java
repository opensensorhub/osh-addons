/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sta.filter;

import org.sensorhub.api.datastore.SpatialFilter;
import org.sensorhub.api.datastore.SpatialFilter.SpatialOp;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.constant.GeoJsonConstant;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.constant.LineStringConstant;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.constant.NumericConstant;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.constant.PointConstant;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.constant.PolygonConstant;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.comparison.Equal;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.spatialrelation.GeoDistance;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.spatialrelation.GeoIntersects;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.spatialrelation.GeoLength;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.spatialrelation.STContains;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.spatialrelation.STCrosses;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.spatialrelation.STDisjoint;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.spatialrelation.STEquals;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.spatialrelation.STIntersects;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.spatialrelation.STOverlaps;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.spatialrelation.STRelate;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.spatialrelation.STTouches;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.spatialrelation.STWithin;


/**
 * <p>
 * Visitor for filter expression applied to geometry properties
 * </p>
 *
 * @author Alex Robin
 * @since Apr 16, 2021
 */
public abstract class SpatialPropVisitor implements BaseExpressionVisitor<SpatialPropVisitor>
{
    protected SpatialFilter.Builder builder = new SpatialFilter.Builder();
    protected SpatialOp op;
    protected Geometry geom;
    
    
    protected abstract void assignFilter();


    protected void parseGeom(String wktString)
    {
        try
        {
            this.geom = new WKTReader().read(wktString);
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException("Invalid WKT geometry");
        }
    }
    
    
    protected SpatialPropVisitor setGeom(String wktString)
    {
        parseGeom(wktString);
        builder.withRoi(geom);
        return this;
    }
    
    
    @Override
    public SpatialPropVisitor visit(LineStringConstant node)
    {
        return setGeom(node.getSource());
    }


    @Override
    public SpatialPropVisitor visit(PointConstant node)
    {
        return setGeom(node.getSource());
    }


    @Override
    public SpatialPropVisitor visit(PolygonConstant node)
    {
        return setGeom(node.getSource());
    }


    @Override
    public SpatialPropVisitor visit(GeoDistance node)
    {
        var p1 = node.getParameters().get(1);
        if (p1 instanceof GeoJsonConstant)
        {
            parseGeom(((GeoJsonConstant<?>)p1).getSource());
            return this;
        }
        else
            throw new IllegalArgumentException("2nd argument of geo.distance function must be a geometry");
    }


    @Override
    public SpatialPropVisitor visit(GeoLength node)
    {
        throw new IllegalArgumentException("Unsupported spatial function: geo.length");
    }


    @Override
    public SpatialPropVisitor visit(Equal node)
    {
        var p1 = node.getParameters().get(1);
        
        // equal only valid in this context when compared to geodistance or geolength
        if (geom != null && p1 instanceof NumericConstant)
        {
            var dist = (double)((NumericConstant<?>)p1).getValue();
            builder.withDistanceToGeom(geom, dist);
            return this;
        }
        else
            throw new IllegalArgumentException("Unsupported syntax for geo.distance function");
    }


    @Override
    public SpatialPropVisitor visit(GeoIntersects node)
    {
        builder.withOperator(SpatialOp.INTERSECTS);
        node.getParameters().get(1).accept(this);
        assignFilter();
        return this;
    }


    @Override
    public SpatialPropVisitor visit(STContains node)
    {
        builder.withOperator(SpatialOp.CONTAINS);
        node.getParameters().get(1).accept(this);
        assignFilter();
        return this;
    }


    @Override
    public SpatialPropVisitor visit(STCrosses node)
    {
        builder.withOperator(SpatialOp.CROSSES);
        node.getParameters().get(1).accept(this);
        assignFilter();
        return this;
    }


    @Override
    public SpatialPropVisitor visit(STDisjoint node)
    {
        builder.withOperator(SpatialOp.DISJOINT);
        node.getParameters().get(1).accept(this);
        assignFilter();
        return this;
    }


    @Override
    public SpatialPropVisitor visit(STEquals node)
    {
        builder.withOperator(SpatialOp.EQUALS);
        node.getParameters().get(1).accept(this);
        assignFilter();
        return this;
    }


    @Override
    public SpatialPropVisitor visit(STIntersects node)
    {
        builder.withOperator(SpatialOp.INTERSECTS);
        node.getParameters().get(1).accept(this);
        assignFilter();
        return this;
    }


    @Override
    public SpatialPropVisitor visit(STOverlaps node)
    {
        builder.withOperator(SpatialOp.OVERLAPS);
        node.getParameters().get(1).accept(this);
        assignFilter();
        return this;
    }


    @Override
    public SpatialPropVisitor visit(STRelate node)
    {
        throw new IllegalArgumentException("Unsupported spatial operator: st_relate");
    }


    @Override
    public SpatialPropVisitor visit(STTouches node)
    {
        builder.withOperator(SpatialOp.TOUCHES);
        node.getParameters().get(1).accept(this);
        assignFilter();
        return this;
    }


    @Override
    public SpatialPropVisitor visit(STWithin node)
    {
        builder.withOperator(SpatialOp.WITHIN);
        node.getParameters().get(1).accept(this);
        assignFilter();
        return this;
    }
    
}
