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

import java.util.Iterator;
import org.h2.mvstore.CursorPos;
import org.h2.mvstore.DataUtils;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.Page;


/**
 * <p>
 * Custom MVMap cursor adding support an end key to stop iteration.
 * </p>
 *
 * @author Alex Robin
 * @param <K> Key Type
 * @param <V> Value Type
 * @since Oct 25, 2016
 */
public class RangeCursor<K, V> implements Iterator<K>
{
    private final MVMap<K, ?> map;
    private final K from, to;
    private CursorPos pos;
    private K current, last;
    private V currentValue, lastValue;
    private Page lastPage;
    private final Page root;
    private boolean initialized;


    public RangeCursor(MVMap<K, V> map, K from, K to)
    {
        this(map, map.getRoot(), from, to);
    }
    
    
    public RangeCursor(MVMap<K, V> map, Page root, K from, K to)
    {
        this.map = map;
        this.root = root;
        this.from = from;
        this.to = to;
    }


    @Override
    public boolean hasNext()
    {
        if (!initialized)
        {
            min(root, from);
            initialized = true;
            fetchNext();
        }
        return current != null;
    }


    @Override
    public K next()
    {
        hasNext();
        K c = current;
        last = current;
        lastValue = currentValue;
        lastPage = pos == null ? null : pos.page;
        fetchNext();
        return c;
    }


    /**
     * Get the last read key if there was one.
     *
     * @return the key or null
     */
    public K getKey()
    {
        return last;
    }


    /**
     * Get the last read value if there was one.
     *
     * @return the value or null
     */
    public V getValue()
    {
        return lastValue;
    }


    Page getPage()
    {
        return lastPage;
    }


    /**
     * Skip over that many entries. This method is relatively fast (for this map
     * implementation) even if many entries need to be skipped.
     *
     * @param n the number of entries to skip
     */
    public void skip(long n)
    {
        if (!hasNext())
        {
            return;
        }
        if (n < 10)
        {
            while (n-- > 0)
            {
                fetchNext();
            }
            return;
        }
        long index = map.getKeyIndex(current);
        K k = map.getKey(index + n);
        pos = null;
        min(root, k);
        fetchNext();
    }


    @Override
    public void remove()
    {
        throw DataUtils.newUnsupportedOperationException("Removing is not supported");
    }


    /**
     * Fetch the next entry that is equal or larger than the given key, starting
     * from the given page. This method retains the stack.
     *
     * @param p the page to start
     * @param from the key to search
     */
    private void min(Page p, K from)
    {
        while (true)
        {
            if (p.isLeaf())
            {
                int x = from == null ? 0 : p.binarySearch(from);
                if (x < 0)
                {
                    x = -x - 1;
                }
                pos = new CursorPos(p, x, pos);
                break;
            }
            int x = from == null ? -1 : p.binarySearch(from);
            if (x < 0)
            {
                x = -x - 1;
            }
            else
            {
                x++;
            }
            pos = new CursorPos(p, x + 1, pos);
            p = p.getChildPage(x);
        }
    }


    /**
     * Fetch the next entry if there is one.
     */
    @SuppressWarnings("unchecked")
    private void fetchNext()
    {
        while (pos != null)
        {
            if (pos.index < pos.page.getKeyCount())
            {
                int index = pos.index++;
                current = (K) pos.page.getKey(index);
                if (map.getKeyType().compare(current, to) > 0)
                    break;                
                currentValue = (V) pos.page.getValue(index);
                return;
            }
            pos = pos.parent;
            if (pos == null)
            {
                break;
            }
            if (pos.index < pos.page.getRawChildPageCount())
            {
                min(pos.page.getChildPage(pos.index++), null);
            }
        }
        current = null;
    }

}
