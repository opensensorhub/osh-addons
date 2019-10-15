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
public class DatastreamFilterVisitor implements ExpressionVisitor<DatastreamFilterVisitor>
{
    STADataStreamFilter.Builder filter;
    
    
    DatastreamFilterVisitor(STADataStreamFilter.Builder filter)
    {
        this.filter = filter;
    }
    
    
    @Override
    public DatastreamFilterVisitor visit(Path node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(BooleanConstant node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(DateConstant node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(DateTimeConstant node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(DoubleConstant node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(DurationConstant node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(IntervalConstant node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(IntegerConstant node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(LineStringConstant node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(PointConstant node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(PolygonConstant node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(StringConstant node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(TimeConstant node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(Before node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(After node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(Meets node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(During node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(Overlaps node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(Starts node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(Finishes node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(Add node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(Divide node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(Modulo node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(Multiply node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(Subtract node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(Equal node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(GreaterEqual node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(GreaterThan node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(LessEqual node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(LessThan node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(NotEqual node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(Date node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(Day node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(FractionalSeconds node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(Hour node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(MaxDateTime node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(MinDateTime node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(Minute node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(Month node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(Now node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(Second node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(Time node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(TotalOffsetMinutes node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(Year node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(GeoDistance node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(GeoIntersects node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(GeoLength node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(And node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(Not node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(Or node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(Ceiling node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(Floor node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(Round node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(STContains node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(STCrosses node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(STDisjoint node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(STEquals node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(STIntersects node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(STOverlaps node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(STRelate node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(STTouches node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(STWithin node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(Concat node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(EndsWith node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(IndexOf node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(Length node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(StartsWith node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(Substring node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(SubstringOf node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(ToLower node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(ToUpper node)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DatastreamFilterVisitor visit(Trim node)
    {
        // TODO Auto-generated method stub
        return null;
    }

}
