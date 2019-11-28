/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2019 Delta Air Lines, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.flightAware;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.common.collect.Range;


/**
 * Implementation of flight object filter using lists of flight number
 * ranges for one or more airlines.
 * @author Alex Robin
 * @since Nov 26, 2019
 */
public class FlightNumberRangeFilter implements IFlightObjectFilter
{
    Map<String, List<Range<Integer>>> fltNumRanges = new HashMap<>();
    
    
    protected FlightNumberRangeFilter(FlightNumberRangeFilterConfig config)
    {
        try (BufferedReader reader = new BufferedReader(new FileReader(config.dataFile)))
        {
            reader.lines().forEach(l -> {
                if (!l.trim().isEmpty())
                {
                    String[] cols = l.split(",");
                    String airline = cols[0].trim();
                    int low = Integer.parseInt(cols[1]);
                    int high = Integer.parseInt(cols[2]);
                    fltNumRanges.put(airline, Arrays.asList(Range.closed(low, high)));
                }
            });
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Error parsing flight number ranges file", e);
        }
    }
    
    
    @Override
    public boolean test(FlightObject fltObj)
    {
        try
        {
            String airline = fltObj.ident.substring(0, 3);
            int fltNum = Integer.parseInt(fltObj.ident.substring(3));        
        
            List<Range<Integer>> rangeList = fltNumRanges.get(airline);
            if (rangeList == null)
                return false;
            
            for (Range<Integer> range: rangeList)
            {
                if (range.contains(fltNum))
                    return true;
            }
            
            //System.err.println("Filter out " + fltObj.ident);
            return false;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }

}
