/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.h2;

import org.h2.mvstore.Cursor;
import org.h2.mvstore.MVMap;
import org.sensorhub.impl.persistence.IteratorWrapper;


/**
 * <p>
 * Custom MVMap cursor adding support for an end key to stop iteration.
 * </p>
 *
 * @author Alex Robin
 * @param <K> Key Type
 * @param <V> Value Type
 * @since Oct 25, 2016
 */
public class RangeCursor<K, V> extends IteratorWrapper<K, K>
{
    MVMap<K, V> map;
    K to;
    
    
    public RangeCursor(MVMap<K, V> map, K from, K to)
    {
        super(map.cursor(from));
        this.map = map;
        this.to = to;
    }
    
    
    @Override
    protected void preloadNext()
    {
        next = null;
        
        if (it.hasNext())
        {
            next = it.next();
            if (map.getKeyType().compare(next, to) > 0)
                next = null;
        }
    }
    
    
    public K getKey()
    {
        return ((Cursor<K, V>)it).getKey();
    }
    
    
    public V getValue()
    {
        return ((Cursor<K, V>)it).getValue();
    }

}
