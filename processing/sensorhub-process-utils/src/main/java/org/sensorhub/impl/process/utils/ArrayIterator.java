/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.process.utils;

import org.sensorhub.api.processing.OSHProcessInfo;
import org.vast.process.DataConnectionList;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.IDataConnection;
import org.vast.process.ProcessException;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataComponent;


/**
 * <p>
 * Helper process to iterate over DataArray elements
 * </p>
 *
 * @author Alex Robin
 * @date Jun 18, 2021
 */
public class ArrayIterator extends ExecutableProcessImpl
{
    public static final OSHProcessInfo INFO = new OSHProcessInfo("utils:ArrayIterator", "Array Iterator", "Iterate over array elements and outputs them one by one", ArrayIterator.class);
    
    protected DataConnectionList arrayInConn;
    protected DataArray arrayIn;
    protected DataComponent itemOut;
    protected Count indexOut;
    protected Count beginIndexParam;
    protected Count endIndexParam;
    
    int beginIdx, endIdx, currentIdx, maxIdx = -1;
    
    
    public ArrayIterator()
    {
        super(INFO);
        var swe = new SWEHelper();
        
        // inputs
        inputData.add("array", new AutoWiringComponent());
        
        // outputs
        outputData.add("index", indexOut = swe.createCount()
            .definition(SWEConstants.DEF_COUNT)
            .label("Current Index")
            .build());
        outputData.add("item", new AutoWiringComponent());
        
        // params
        paramData.add("beginIndex", beginIndexParam = swe.createCount()
            .definition(SWEConstants.DEF_COUNT)
            .label("Begin Index")
            .description("Array index to start iteration from")
            .build());
        
        paramData.add("endIndex", endIndexParam = swe.createCount()
            .definition(SWEConstants.DEF_COUNT)
            .label("End Index")
            .description("Array index to end iteration at")
            .build());
    }
    
    
    /*
     * Need to override this method to handle auto-wire components
     */
    @Override
    public void connect(DataComponent component, IDataConnection connection) throws ProcessException
    {
        if (component instanceof AutoWiringComponent)
        {
            DataComponent newComponent;
            
            if ("array".equals(component.getName()))
            {
                newComponent = connection.getSourceComponent().copy();
                ((AutoWiringComponent)component).replaceWithComponent(this, newComponent);
                
                // handle case of var size array
                if (!(newComponent instanceof DataArray))
                {
                    if (newComponent.getComponentCount() == 2 &&
                        newComponent.getComponent(0) instanceof Count &&
                        newComponent.getComponent(1) instanceof DataArray)
                        arrayIn = (DataArray)newComponent.getComponent(1);
                    else
                        throw new IllegalStateException("The 'array' input must be connected to a DataArray");
                }
                else
                    arrayIn = (DataArray)newComponent;
                
                // create output = input array element
                itemOut = arrayIn.getElementType().copy();
                itemOut.setName("item");
                itemOut.assignNewDataBlock();
                ((AutoWiringComponent)outputData.get("item")).replaceWithComponent(this, itemOut);
            }
            
            else
                throw new IllegalStateException("Invalid auto-wire component");
            
            newComponent.setName(component.getName());
            component = newComponent;
        }
        
        super.connect(component, connection);
    }
    
   
    @Override
    public void init() throws ProcessException
    {
        super.init();
        
        arrayInConn = getInputConnections().get("array");
        
        beginIdx = beginIndexParam.getData().getIntValue();        
        endIdx = endIndexParam.getData().getIntValue();
        currentIdx = beginIdx;
    }
    
    
    @Override
    public void execute() throws ProcessException
    {
        // set max index so we never go beyond the array size
        if (maxIdx < 0)
            maxIdx = Math.min(endIdx, arrayIn.getComponentCount()-1);
        
        // keep iterating through array until we reach endIndex or end of array
        if (currentIdx <= maxIdx)
        {
            indexOut.getData().setIntValue(currentIdx);
            var eltData = arrayIn.getComponent(currentIdx++).getData();
            itemOut.setData(eltData.copy());            
        }
        
        // if above max index, reset and wait for next input array
        if (currentIdx > maxIdx)
        {
            currentIdx = beginIdx;
            maxIdx = -1;
            arrayInConn.setNeeded(true);
        }
        else
            arrayInConn.setNeeded(false);
    }
}