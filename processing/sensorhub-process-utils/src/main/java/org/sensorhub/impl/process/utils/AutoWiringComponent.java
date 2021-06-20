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

import java.util.List;
import org.vast.data.AbstractDataBlock;
import org.vast.data.AbstractDataComponentImpl;
import org.vast.data.AbstractSimpleComponentImpl;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.IDataConnection;
import net.opengis.sensorml.v20.IOPropertyList;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataComponentVisitor;
import net.opengis.swe.v20.ValidationException;


public class AutoWiringComponent extends AbstractDataComponentImpl
{
    public static final String AUTO_WIRING_URI = "urn:osh:process:wiring:auto";
    
    
    public void replaceWithComponent(ExecutableProcessImpl process, DataComponent comp)
    {
        var name = getName();
        IOPropertyList portList;
        
        // check if it's an input, output or parameter
        if (process.getInputList().hasProperty(name) && process.getInputList().get(name) == this)
            portList = process.getInputList();
        else if (process.getOutputList().hasProperty(name) && process.getOutputList().get(name) == this)
            portList = process.getOutputList();
        else if (process.getParameterList().hasProperty(name) && process.getParameterList().get(name) == this)
            portList = process.getParameterList();
        else
            throw new IllegalStateException("Auto component is not in any port list");
        
        portList.getProperty(name).setValue(comp);
    }
    
    
    public void replaceOutputComponent(ExecutableProcessImpl process, DataComponent comp)
    {
        var outputIdx = process.getOutputList().getComponentIndex(this.getName());
        process.getOutputList().set(outputIdx, comp);
    }
    
    
    
    
    
    @Override
    public void accept(DataComponentVisitor visitor)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public AbstractDataComponentImpl copy()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getComponentCount()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public AbstractDataBlock createDataBlock()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setData(DataBlock dataBlock)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void validateData(List<ValidationException> errorList)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean hasConstraints()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected void updateStartIndex(int startIndex)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String toString(String indent)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DataComponent removeComponent(int index)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DataComponent removeComponent(String name)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addComponent(String name, DataComponent component)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public AbstractDataComponentImpl getComponent(int index)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getComponentIndex(String name)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public AbstractDataComponentImpl getComponent(String name)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void clearData()
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void updateAtomCount(int childOffsetCount)
    {
        // TODO Auto-generated method stub
        
    }

}
