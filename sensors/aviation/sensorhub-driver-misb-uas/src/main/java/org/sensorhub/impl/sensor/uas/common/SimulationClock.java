/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2023 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.uas.common;


public class SimulationClock
{
    double refPrecisionTimeStamp = 0.0;
    double refCurrentTime = 0.0;
    double lastSimulatedTimeStamp = Double.POSITIVE_INFINITY;
    
    
    public synchronized double getSimlatedTimeStamp(double precisionTimeStamp)
    {
        // do nothing if timestamp has already been converted
        if (precisionTimeStamp == lastSimulatedTimeStamp)
            return precisionTimeStamp;
        
        // init reference if this is the first timestamp in the dataset (or after we loop around)
        if (precisionTimeStamp <= lastSimulatedTimeStamp)
        {
            refPrecisionTimeStamp = precisionTimeStamp;
            refCurrentTime = System.currentTimeMillis() / 1000.0;
        }
        
        lastSimulatedTimeStamp = refCurrentTime + precisionTimeStamp - refPrecisionTimeStamp;
        return lastSimulatedTimeStamp;
    }
}
