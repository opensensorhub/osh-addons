/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2022 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.mfapi.mf;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.data.IObsData;
import org.vast.swe.fast.DataBlockProcessor;
import org.vast.util.Asserts;
import net.opengis.swe.v20.Boolean;
import net.opengis.swe.v20.Category;
import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.ScalarComponent;
import net.opengis.swe.v20.Text;
import net.opengis.swe.v20.Time;


public class TemporalPropCollector extends DataBlockProcessor
{
    static final int VALUE_LIST_INIT_SIZE = 20;
    
    BigId dsId;
    ArrayList<Instant> dateTimes = new ArrayList<>(VALUE_LIST_INIT_SIZE);
    Collection<ValueCollector<?>> temporalProperties = new ArrayList<>();

    protected abstract class ValueCollector<T> extends BaseProcessor
    {
        ScalarComponent comp;
        List<T> values = new ArrayList<>(VALUE_LIST_INIT_SIZE);
    }

    protected class BooleanCollector extends ValueCollector<java.lang.Boolean>
    {
        public BooleanCollector(ScalarComponent comp)
        {
            this.comp = comp;
        }

        @Override
        public int process(DataBlock data, int index) throws IOException
        {
            var val = data.getBooleanValue(index);
            values.add(val);
            return ++index;
        }
    }

    protected class DoubleCollector extends ValueCollector<Double>
    {
        public DoubleCollector(ScalarComponent comp)
        {
            this.comp = comp;
        }

        @Override
        public int process(DataBlock data, int index) throws IOException
        {
            var val = data.getDoubleValue(index);
            values.add(val);
            return ++index;
        }
    }

    protected class FloatCollector extends ValueCollector<Float>
    {
        public FloatCollector(ScalarComponent comp)
        {
            this.comp = comp;
        }

        @Override
        public int process(DataBlock data, int index) throws IOException
        {
            var val = data.getFloatValue(index);
            values.add(val);
            return ++index;
        }
    }

    protected class IntegerCollector extends ValueCollector<Integer>
    {
        public IntegerCollector(ScalarComponent comp)
        {
            this.comp = comp;
        }

        @Override
        public int process(DataBlock data, int index) throws IOException
        {
            var val = data.getIntValue(index);
            values.add(val);
            return ++index;
        }
    }

    protected class StringCollector extends ValueCollector<String>
    {
        public StringCollector(ScalarComponent comp)
        {
            this.comp = comp;
        }

        @Override
        public int process(DataBlock data, int index) throws IOException
        {
            var val = data.getStringValue(index);
            values.add(val);
            return ++index;
        }
    }
    
    protected class SkipValue extends BaseProcessor
    {
        @Override
        public int process(DataBlock data, int index) throws IOException
        {
            return ++index;
        }
    }
    
    
    public TemporalPropCollector(BigId dsId)
    {
        this.dsId = dsId;
    }
    
    
    public void collect(IObsData obs) throws IOException
    {
        Asserts.checkNotNull(obs, IObsData.class);
        
        try
        {
            if (!processorTreeReady)
            {
                init();
                checkEnabled(dataComponents);
                dataComponents.accept(this);
                processorTreeReady = true;
            }
            
            // go once through the tree of parser atoms
            dateTimes.add(obs.getPhenomenonTime());
            int index = rootProcessor.process(obs.getResult(), 0);
            Asserts.checkState(index == obs.getResult().getAtomCount(), "Data block wasn't fully processed");
        }
        catch (Exception e)
        {
            throw new IOException("Error while processing record", e);
        }
    }
    
    
    @Override
    public void visit(Boolean component)
    {
        if (!isInArrayOrChoice(component))
        {
            var col = new BooleanCollector(component);
            addToProcessorTree(col);
            temporalProperties.add(col);
        }
        else
            addToProcessorTree(new SkipValue());
    }


    @Override
    public void visit(Count component)
    {
        if (!isInArrayOrChoice(component))
        {
            var col = new IntegerCollector(component);
            addToProcessorTree(col);
            temporalProperties.add(col);
        }
        else
            addToProcessorTree(new SkipValue());
    }


    @Override
    public void visit(Quantity component)
    {
        if (!isInArrayOrChoice(component))
        {
            var col = new DoubleCollector(component);
            addToProcessorTree(col);
            temporalProperties.add(col);
        }
        else
            addToProcessorTree(new SkipValue());
    }


    @Override
    public void visit(Time component)
    {
        if (!isInArrayOrChoice(component))
        {
            var col = new DoubleCollector(component);
            addToProcessorTree(col);
            temporalProperties.add(col);
        }
        else
            addToProcessorTree(new SkipValue());
    }


    @Override
    public void visit(Category component)
    {
        if (!isInArrayOrChoice(component))
        {
            var col = new StringCollector(component);
            addToProcessorTree(col);
            temporalProperties.add(col);
        }
        else
            addToProcessorTree(new SkipValue());
    }


    @Override
    public void visit(Text component)
    {
        if (!isInArrayOrChoice(component))
        {
            var col = new StringCollector(component);
            addToProcessorTree(col);
            temporalProperties.add(col);
        }
        else
            addToProcessorTree(new SkipValue());
    }
    
    
    protected boolean isInArrayOrChoice(DataComponent comp)
    {
        while (comp != null)
        {
            var parent = comp.getParent();
            if (parent instanceof DataArray || parent instanceof DataChoice)
                return true;
            comp = parent;
        }
        
        return false;
    }


    @Override
    protected void init() throws IOException
    {
        // TODO Auto-generated method stub

    }
    
    
    protected ArrayProcessor getArrayProcessor(DataArray array)
    {
        return new ArrayProcessor();
    }


    @Override
    protected ChoiceProcessor getChoiceProcessor(DataChoice choice)
    {
        return null;
    }


    @Override
    protected ImplicitSizeProcessor getImplicitSizeProcessor(DataArray array)
    {
        return new ImplicitSizeProcessor();
    }


    @Override
    protected ArraySizeSupplier getArraySizeSupplier(String refId)
    {
        return null;
    }

}
