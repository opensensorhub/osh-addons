/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2019 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.h2;


/**
 * <p>
 * Represents a time range of observations available for the given producer
 * and feature of interest. Used internally by H2 storage implementation.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Jun 12, 2019
 */
class ObsTimePeriod
{
    String producerID;
    String foiID;
    double start;
    double stop;
    
    
    static class Comparator implements java.util.Comparator<ObsTimePeriod>
    {
        public int compare(ObsTimePeriod p0, ObsTimePeriod p1)
        {
            int comp = Double.compare(p0.start, p1.start);
            if (comp == 0)
                comp = Double.compare(p0.stop, p1.stop);
            return comp;
        }        
    }
    
    
    ObsTimePeriod(String producerID, double start)
    {
        this.producerID = producerID;
        this.start = start;
        this.stop = Double.POSITIVE_INFINITY;
    }
    
    
    ObsTimePeriod(String producerID, String foiID, double start, double stop)
    {
        this.producerID = producerID;
        this.foiID = foiID;
        this.start = start;
        this.stop = stop;
    }
}