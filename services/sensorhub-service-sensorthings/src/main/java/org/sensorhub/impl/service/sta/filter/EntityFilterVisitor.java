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

import java.util.HashMap;
import java.util.Map;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.ExpressionVisitor;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.Path;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.Function;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.comparison.Equal;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.comparison.GreaterEqual;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.comparison.GreaterThan;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.comparison.LessEqual;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.comparison.LessThan;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.comparison.NotEqual;
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
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.string.EndsWith;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.string.StartsWith;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.temporal.After;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.temporal.Before;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.temporal.During;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.temporal.Meets;


public abstract class EntityFilterVisitor<T extends EntityFilterVisitor<T>> implements BaseExpressionVisitor<T>
{    
    protected Map<String, Class<? extends ExpressionVisitor<?>>> propTypes = new HashMap<>();
        
    
    /*
     * To be overriden by subclasses to assign filter to the parent filter
     */
    protected void assignFilter()
    {
    }
    
    
    @SuppressWarnings("unchecked")
    protected T visitBinaryOp(Function f)
    {
        var p0 = f.getParameters().get(0);
        
        if (p0 instanceof Function)
            p0 = ((Function) p0).getParameters().get(0);
                
        if (p0 instanceof Path)
        {
            var path = ((Path)p0).getElements();
            
            var prop = path.get(0);
            if (path.size() > 1) // pop first part of path before scanning nested property
                path.remove(0);
            
            var visitorClass = propTypes.get(prop.getName());
            if (visitorClass == null)
                throw new IllegalArgumentException("Filtering on '" + prop.getName() + "' property is unsupported");
            
            ExpressionVisitor<?> visitorInstance;
            try {
                var parentClass = visitorClass.getEnclosingClass();
                visitorInstance = visitorClass.getDeclaredConstructor(parentClass).newInstance(this);
            } catch (Exception e) {
                throw new IllegalStateException("Cannot create visitor for property: " + prop.getName(), e);
            }
            
            f.accept(visitorInstance);
            assignFilter();
            return (T)this;
        }
        else
            throw new IllegalArgumentException("Left operand must be an entity property");
    }
    
    
    protected abstract T getNewInstance();
    
    
    // equality operators
    
    @Override
    public T visit(Equal node)
    {
        return this.visitBinaryOp(node);
    }
    
    
    @Override
    public T visit(NotEqual node)
    {
        return this.visitBinaryOp(node);
    }
    
    
    // comparison operators
    
    @Override
    public T visit(GreaterEqual node)
    {
        return this.visitBinaryOp(node);
    }


    @Override
    public T visit(GreaterThan node)
    {
        return this.visitBinaryOp(node);
    }


    @Override
    public T visit(LessEqual node)
    {
        return this.visitBinaryOp(node);
    }


    @Override
    public T visit(LessThan node)
    {
        return this.visitBinaryOp(node);
    }
    
    
    // string operators
    
    @Override
    public T visit(EndsWith node)
    {
        return this.visitBinaryOp(node);
    }


    @Override
    public T visit(StartsWith node)
    {
        return this.visitBinaryOp(node);
    }
    
    
    // temporal operators

    @Override
    public T visit(Before node)
    {
        return this.visitBinaryOp(node);
    }


    @Override
    public T visit(After node)
    {
        return this.visitBinaryOp(node);
    }


    @Override
    public T visit(Meets node)
    {
        return this.visitBinaryOp(node);
    }


    @Override
    public T visit(During node)
    {
        return this.visitBinaryOp(node);
    }
    
    
    
    // spatial operators

    @Override
    public T visit(GeoDistance node)
    {
        return this.visitBinaryOp(node);
    }


    @Override
    public T visit(GeoIntersects node)
    {
        return this.visitBinaryOp(node);
    }


    @Override
    public T visit(GeoLength node)
    {
        // TODO Auto-generated method stub
        return BaseExpressionVisitor.super.visit(node);
    }


    @Override
    public T visit(STContains node)
    {
        return this.visitBinaryOp(node);
    }


    @Override
    public T visit(STCrosses node)
    {
        return this.visitBinaryOp(node);
    }


    @Override
    public T visit(STDisjoint node)
    {
        return this.visitBinaryOp(node);
    }


    @Override
    public T visit(STEquals node)
    {
        return this.visitBinaryOp(node);
    }


    @Override
    public T visit(STIntersects node)
    {
        return this.visitBinaryOp(node);
    }


    @Override
    public T visit(STOverlaps node)
    {
        return this.visitBinaryOp(node);
    }


    @Override
    public T visit(STRelate node)
    {
        return this.visitBinaryOp(node);
    }


    @Override
    public T visit(STTouches node)
    {
        return this.visitBinaryOp(node);
    }


    @Override
    public T visit(STWithin node)
    {
        return this.visitBinaryOp(node);
    }
    
    
    
    
    

}
