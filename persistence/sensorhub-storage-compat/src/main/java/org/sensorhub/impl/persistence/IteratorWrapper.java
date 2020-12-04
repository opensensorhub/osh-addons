/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2017 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.vast.util.Asserts;


/**
 * <p>
 * Helper class to wrap iterators with some logic used to further filter or
 * modify the returned elements
 * </p>
 *
 * @author Alex Robin
 * @param <IN> Input Type
 * @param <OUT> Output Type
 * @since Apr 15, 2017
 */
public abstract class IteratorWrapper<IN, OUT> implements Iterator<OUT>
{
    protected OUT next;
    protected Iterator<IN> it;
    
    
    public IteratorWrapper(Iterator<IN> it)
    {
        Asserts.checkNotNull(it, Iterator.class);
        this.it = it;
    }
    
    
    @Override
    public boolean hasNext()
    {
        if (next == null)
            preloadNext();
        return next != null;
    }
    

    @Override
    public OUT next()
    {
        if (next == null && !hasNext())
            throw new NoSuchElementException();
        OUT nextElt = next;
        next = null;
        return nextElt;
    }
    

    @Override
    public void remove()
    {
        it.remove();
    }
    
    
    /**
     * Preload next element
     */
    protected void preloadNext()
    {
        next = null;
        
        // loop until we find the next acceptable item
        // or end of iteration
        while (next == null && it.hasNext())
        {
            IN elt = it.next();
            next = process(elt);
        }
    }
    
    
    protected abstract OUT process(IN elt);
}
