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

import java.time.Instant;
import org.sensorhub.api.datastore.RangeFilter.RangeOp;
import org.sensorhub.api.datastore.TemporalFilter;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.constant.DateConstant;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.constant.DateTimeConstant;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.constant.IntervalConstant;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.constant.StringConstant;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.constant.TimeConstant;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.comparison.Equal;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.comparison.GreaterEqual;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.comparison.GreaterThan;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.comparison.LessEqual;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.comparison.LessThan;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.comparison.NotEqual;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.temporal.After;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.temporal.Before;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.temporal.During;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.temporal.Finishes;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.temporal.Meets;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.temporal.Overlaps;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.temporal.Starts;


public abstract class TemporalPropVisitor implements BaseExpressionVisitor<TemporalPropVisitor>
{
    protected TemporalFilter.Builder builder = new TemporalFilter.Builder();
    protected RangeOp op;
    protected boolean after;
    protected boolean before;
    protected boolean exclusive;
    protected boolean notEqual;
    
    
    protected abstract void assignFilter();


    @Override
    public TemporalPropVisitor visit(Equal node)
    {
        op = RangeOp.EQUALS;
        node.getParameters().get(1).accept(this);
        assignFilter();
        return this;
    }


    @Override
    public TemporalPropVisitor visit(NotEqual node)
    {
        notEqual = true;
        op = RangeOp.EQUALS;
        node.getParameters().get(1).accept(this);
        assignFilter();
        return this;
    }
    
    
    @Override
    public TemporalPropVisitor visit(Before node)
    {
        before = true;
        exclusive = true;
        node.getParameters().get(1).accept(this);
        assignFilter();
        return this;
    }
    
    
    @Override
    public TemporalPropVisitor visit(LessThan node)
    {
        before = true;
        exclusive = true;
        node.getParameters().get(1).accept(this);
        assignFilter();
        return this;
    }
    
    
    @Override
    public TemporalPropVisitor visit(LessEqual node)
    {
        before = true;
        exclusive = false;
        node.getParameters().get(1).accept(this);
        assignFilter();
        return this;
    }


    @Override
    public TemporalPropVisitor visit(After node)
    {
        after = true;
        node.getParameters().get(1).accept(this);
        assignFilter();
        return this;
    }
    
    
    @Override
    public TemporalPropVisitor visit(GreaterThan node)
    {
        after = true;
        exclusive = true;
        node.getParameters().get(1).accept(this);
        assignFilter();
        return this;
    }
    
    
    @Override
    public TemporalPropVisitor visit(GreaterEqual node)
    {
        after = true;
        exclusive = false;
        node.getParameters().get(1).accept(this);
        assignFilter();
        return this;
    }


    @Override
    public TemporalPropVisitor visit(Meets node)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public TemporalPropVisitor visit(Overlaps node)
    {
        op = RangeOp.INTERSECTS;
        node.getParameters().get(1).accept(this);
        assignFilter();
        return this;
    }


    @Override
    public TemporalPropVisitor visit(During node)
    {
        op = RangeOp.INTERSECTS;
        node.getParameters().get(1).accept(this);
        assignFilter();
        return this;
    }


    @Override
    public TemporalPropVisitor visit(Starts node)
    {
        op = RangeOp.CONTAINS;
        after = true;
        node.getParameters().get(1).accept(this);
        assignFilter();
        return this;
    }
    
    
    @Override
    public TemporalPropVisitor visit(Finishes node)
    {
        op = RangeOp.CONTAINS;
        before = true;
        node.getParameters().get(1).accept(this);
        assignFilter();
        return this;
    }


    @Override
    public TemporalPropVisitor visit(IntervalConstant node)
    {
        var val = node.getValue();
        builder.withRange(
            Instant.ofEpochMilli(val.getStartMillis()),
            Instant.ofEpochMilli(val.getEndMillis()));
        return this;
    }
    
    
    protected TemporalPropVisitor setDateTime(long unixTime)
    {
        var t = Instant.ofEpochMilli(unixTime);
        
        if (op == RangeOp.EQUALS)
            builder.withSingleValue(t);
        else if (after)
            builder.withRange(exclusive ? t.plusNanos(1) : t, Instant.MAX);
        else if (before)
            builder.withRange(Instant.MIN, exclusive ? t.minusNanos(1) : t);
            
        return this;
    }
    
    
    @Override
    public TemporalPropVisitor visit(DateConstant node)
    {
        return setDateTime(node.getValue().toDateTimeAtStartOfDay().getMillis());
    }


    @Override
    public TemporalPropVisitor visit(DateTimeConstant node)
    {
        return setDateTime(node.getValue().getMillis());
    }


    @Override
    public TemporalPropVisitor visit(TimeConstant node)
    {
        return setDateTime(node.getValue().toDateTimeToday().getMillis());
    }


    @Override
    public TemporalPropVisitor visit(StringConstant node)
    {
        return setDateTime(Instant.parse(node.getValue()).toEpochMilli());
    }
}
