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

import org.sensorhub.api.procedure.ProcedureFilter;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.ExpressionVisitor;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.Path;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.constant.BooleanConstant;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.constant.DateConstant;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.constant.DateTimeConstant;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.constant.DoubleConstant;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.constant.DurationConstant;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.constant.IntegerConstant;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.constant.IntervalConstant;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.constant.LineStringConstant;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.constant.PointConstant;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.constant.PolygonConstant;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.constant.StringConstant;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.constant.TimeConstant;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.arithmetic.Add;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.arithmetic.Divide;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.arithmetic.Modulo;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.arithmetic.Multiply;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.arithmetic.Subtract;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.comparison.Equal;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.comparison.GreaterEqual;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.comparison.GreaterThan;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.comparison.LessEqual;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.comparison.LessThan;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.comparison.NotEqual;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.date.Date;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.date.Day;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.date.FractionalSeconds;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.date.Hour;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.date.MaxDateTime;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.date.MinDateTime;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.date.Minute;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.date.Month;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.date.Now;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.date.Second;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.date.Time;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.date.TotalOffsetMinutes;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.date.Year;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.logical.And;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.logical.Not;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.logical.Or;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.math.Ceiling;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.math.Floor;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.math.Round;
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
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.string.Concat;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.string.EndsWith;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.string.IndexOf;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.string.Length;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.string.StartsWith;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.string.Substring;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.string.SubstringOf;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.string.ToLower;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.string.ToUpper;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.string.Trim;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.temporal.After;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.temporal.Before;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.temporal.During;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.temporal.Finishes;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.temporal.Meets;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.temporal.Overlaps;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.temporal.Starts;

/**
 * <p>
 * Visitor used to build a ProcedureFilter from STA $filter expression
 * </p>
 *
 * @author Alex Robin
 * @date Sep 7, 2019
 */
public class SensorFilterVisitor implements ExpressionVisitor<SensorFilterVisitor>
{
    ProcedureFilter.Builder filter;
    
    
    SensorFilterVisitor(ProcedureFilter.Builder filter)
    {
        this.filter = filter;
    }
    
    
    @Override
    public SensorFilterVisitor visit(Path node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(BooleanConstant node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(DateConstant node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(DateTimeConstant node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(DoubleConstant node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(DurationConstant node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(IntervalConstant node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(IntegerConstant node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(LineStringConstant node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(PointConstant node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(PolygonConstant node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(StringConstant node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(TimeConstant node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(Before node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(After node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(Meets node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(During node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(Overlaps node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(Starts node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(Finishes node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(Add node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(Divide node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(Modulo node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(Multiply node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(Subtract node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(Equal node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(GreaterEqual node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(GreaterThan node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(LessEqual node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(LessThan node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(NotEqual node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(Date node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(Day node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(FractionalSeconds node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(Hour node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(MaxDateTime node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(MinDateTime node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(Minute node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(Month node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(Now node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(Second node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(Time node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(TotalOffsetMinutes node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(Year node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(GeoDistance node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(GeoIntersects node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(GeoLength node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(And node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(Not node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(Or node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(Ceiling node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(Floor node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(Round node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(STContains node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(STCrosses node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(STDisjoint node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(STEquals node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(STIntersects node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(STOverlaps node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(STRelate node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(STTouches node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(STWithin node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(Concat node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(EndsWith node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(IndexOf node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(Length node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(StartsWith node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(Substring node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(SubstringOf node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(ToLower node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(ToUpper node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SensorFilterVisitor visit(Trim node)
    {
        // TODO Auto-generated method stub
        return null;
    }

}
