/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sta.filter;

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


public interface BaseExpressionVisitor<T> extends ExpressionVisitor<T>
{
    static final String INVALID_FILTER = "Unsupported filter syntax";
    static final String INVALID_OP = "Unsupported filter operator: ";
    
    
    @Override
    default T visit(Path node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(BooleanConstant node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(DateConstant node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(DateTimeConstant node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(DoubleConstant node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(DurationConstant node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(IntervalConstant node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(IntegerConstant node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(LineStringConstant node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(PointConstant node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(PolygonConstant node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(StringConstant node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(TimeConstant node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(Before node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(After node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(Meets node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(During node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(Overlaps node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(Starts node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(Finishes node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(Add node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(Divide node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(Modulo node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(Multiply node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(Subtract node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(Equal node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(GreaterEqual node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(GreaterThan node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(LessEqual node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(LessThan node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(NotEqual node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(Date node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(Day node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(FractionalSeconds node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(Hour node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(MaxDateTime node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(MinDateTime node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(Minute node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(Month node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(Now node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(Second node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(Time node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(TotalOffsetMinutes node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(Year node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(GeoDistance node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(GeoIntersects node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(GeoLength node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(And node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(Not node)
    {
        throw new IllegalArgumentException(INVALID_OP + "NOT");
    }


    @Override
    default T visit(Or node)
    {
        throw new IllegalArgumentException(INVALID_OP + "OR");
    }


    @Override
    default T visit(Ceiling node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(Floor node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(Round node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(STContains node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(STCrosses node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(STDisjoint node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(STEquals node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(STIntersects node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(STOverlaps node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(STRelate node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(STTouches node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(STWithin node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(Concat node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(EndsWith node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(IndexOf node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(Length node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(StartsWith node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(Substring node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(SubstringOf node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(ToLower node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(ToUpper node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }


    @Override
    default T visit(Trim node)
    {
        throw new IllegalArgumentException(INVALID_FILTER);
    }

}
