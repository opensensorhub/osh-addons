/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2018 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.h2;


/**
 * Key object used to create a composite producerID + time stamp index
 */
public class ProducerTimeKey
{
    final String producerID;
    final double timeStamp;
    
    
    public ProducerTimeKey(double timeStamp)
    {
        this.producerID = null;
        this.timeStamp = timeStamp;
    }
    
    
    public ProducerTimeKey(String producerID, double timeStamp)
    {
        this.producerID = producerID;
        this.timeStamp = timeStamp;
    }   
}
