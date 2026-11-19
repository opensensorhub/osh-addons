/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.misb.stanag4609.klv;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import org.sensorhub.misb.stanag4609.tags.Tag;

/**
 * <p>
 * Abstract class to decode a series of identical element, each one
 * conforming to the same local set
 * </p>
 *
 * @author Alex Robin
 * @since May 10, 2021
 */
public abstract class AbstractSeries extends AbstractDataSet
{
    private int numElts;
    
    
    public AbstractSeries(int length, byte[] payload, int numElts) {

        this.length = length;
        this.payload = payload;
        this.numElts = numElts;
    }
    
    
    protected abstract HashMap<Tag, Object> decodeElement(byte[] payload);
    
    
    public Collection<HashMap<Tag, Object>> decodeSeries() {
        
        var decodedElts = new ArrayList<HashMap<Tag, Object>>();
        
        for (int i = 0; i < numElts; i++)
        {
            int length = decodeLength();
            byte[] value = new byte[length];

            for (int idx = 0; idx < length; ++idx) {
                value[idx] = payload[position++];
            }
            
            var elt = decodeElement(value);
            decodedElts.add(elt);
        }
        
        return decodedElts;
    }
    
    
    @Override
    public HashMap<Tag, Object> decode()
    {
        throw new UnsupportedOperationException("Use decodeSeries()");
    }

}
