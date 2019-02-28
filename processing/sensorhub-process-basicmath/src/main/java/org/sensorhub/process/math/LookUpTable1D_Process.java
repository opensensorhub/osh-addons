/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License Version
 1.1 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at
 http://www.mozilla.org/MPL/MPL-1.1.html
 
 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.
 
 The Original Code is the "SensorML AbstractProcessing Engine".
 
 The Initial Developer of the Original Code is the VAST team at the University of Alabama in Huntsville (UAH). <http://vast.uah.edu> Portions created by the Initial Developer are Copyright (C) 2007 the Initial Developer. All Rights Reserved. Please Contact Mike Botts <mike.botts@uah.edu> for more information.
 
 Contributor(s): 
    Alexandre Robin <robin@nsstc.uah.edu>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.process.math;

import net.opengis.swe.v20.DataBlock;
import org.vast.data.*;
import org.vast.process.*;
import org.vast.sensorML.ExecutableProcessImpl;


/**
 * <p>
 * A look up table process allows to map one input variable (independent)
 * to several output variables (dependent) using discrete tie points from
 * a table and given interpolation/extrapolation methods.
 * </p>
 *
 * @author Alexandre Robin
 * @date Sep 2, 2005
 */
public class LookUpTable1D_Process extends ExecutableProcessImpl
{
	DataValue inputVar;
    DataValue[] outputVars;
    DataValue inputGain, outputGain, inputBias, outputBias;
    DataBlock tableData;
    int tableSize;
    int tupleSize;
    int interpolationMethod;
    int extrapolationMethod;
    double lastIndexVal = Double.POSITIVE_INFINITY;
    int lastTupleIndex = 0;
    
    
    public LookUpTable1D_Process()
    {
    	
    }

    
    public void init() throws SMLException
    {
        try
        {
            // input mapping
            inputVar = (DataValue)inputData.getComponent(0);
            
            // output mapping
            int outputCount = outputData.getComponent(0).getComponentCount();
            if (outputCount > 0)
            {
                // if output is a vector
                outputVars = new DataValue[outputCount];
                for (int i=0; i<outputCount; i++)
                {
                    outputVars[i] = (DataValue)outputData.getComponent(0).getComponent(i);
                }
            }
            else
            {
                // if output is a scalar
                outputVars = new DataValue[1];
                outputVars[0] = (DataValue)outputData.getComponent(0);
            }
            
            // params mappings
            inputGain = (DataValue)paramData.getComponent("inputGain");
            inputBias = (DataValue)paramData.getComponent("inputBias");
            outputGain = (DataValue)paramData.getComponent("outputGain");
            outputBias = (DataValue)paramData.getComponent("outputBias");
            tableData = paramData.getComponent("table").getData();
            tupleSize = paramData.getComponent("table").getComponent(0).getComponentCount();          
            tableSize = tableData.getAtomCount()/(tupleSize);
            if (tupleSize != outputVars.length+1)
                throw new SMLException("table and output should have the same size");

            // read interpolation method
            DataValue interp = (DataValue)paramData.getComponent("interpolationMethod");
            if (interp == null)
            {
            	interpolationMethod = 1;
            	extrapolationMethod = 0;
            }
            else
            {
                if (interp.getData().getStringValue().equalsIgnoreCase("step"))
                    interpolationMethod = 0;
                else if (interp.getData().getStringValue().equalsIgnoreCase("linear"))
                    interpolationMethod = 1;
                else if (interp.getData().getStringValue().equalsIgnoreCase("quadratic"))
                    interpolationMethod = 2;
                else if (interp.getData().getStringValue().equalsIgnoreCase("cubic"))
                    interpolationMethod = 3;
            }
            
            lastIndexVal = Double.POSITIVE_INFINITY;
            lastTupleIndex = 0;
        }
        catch (Exception e)
        {
            throw new SMLException(IO_ERROR_MSG, e);
        }
    }
    

    /**
     * Executes process algorithm on inputs and set output data
     */
    public void execute() throws SMLException
    {
    	double input = inputVar.getData().getDoubleValue(); 	
    	
    	switch (interpolationMethod)
    	{
    		case 1:
    			computeInterpolatedValue1D(input);
    			break;    			
    	}
    	
    	//System.out.println(getName() + ": " + input + " -> " + outputVars[0].getData().getDoubleValue());
    }
    
    
    /**
     * Computes 1D interpolated value for given index value
     * This method assumes that table index values are sorted 
     * from lowest to highest
     * @param indexVal
     */
    protected void computeInterpolatedValue1D(double indexVal)
    {
        int maxi = tableSize - 1;
        
        // 0th order extrapolation if more than max value
        if (indexVal >= tableData.getDoubleValue(maxi*tupleSize))
        {
            // extrapolated outputs
            for (int j=1; j<tupleSize; j++)
            {
                double value = tableData.getDoubleValue(maxi*tupleSize + j);
                outputVars[j-1].getData().setDoubleValue(value);
            }
        }
        // 0th order extrapolation if less than min value
        else if (indexVal <= tableData.getDoubleValue(0))
        {
            // extrapolated outputs
            for (int j=1; j<tupleSize; j++)
            {
                double value = tableData.getDoubleValue(j);
                outputVars[j-1].getData().setDoubleValue(value);
            }
        }
        else
        {
            int nextIndex;
            
            // shortcut when incrementing
            if (indexVal >= lastIndexVal)
                nextIndex = lastTupleIndex;
            else
                nextIndex = 0;
            
            // find first entry higher than index
            double tableVal = tableData.getDoubleValue(nextIndex);
            while (indexVal > tableVal)
            {
                nextIndex += tupleSize;
                tableVal = tableData.getDoubleValue(nextIndex);     
            }
            
            int prevIndex = nextIndex - tupleSize;
            
            // if index exactly equal to value in table
            if (indexVal == tableVal)
            {
                for (int j=1; j<tupleSize; j++)
                {
                    double value = tableData.getDoubleValue(nextIndex + j);                    
                    outputVars[j-1].getData().setDoubleValue(value);
                }
            }
            // otherwise, need interpolation
            else
            {
                // linear interpolation factor
                double prevVal = tableData.getDoubleValue(prevIndex);
                double nextVal = tableData.getDoubleValue(nextIndex);
                double a = (indexVal - prevVal) / (nextVal - prevVal);
                
                // interpolated outputs
                for (int j=1; j<tupleSize; j++)
                {
                    prevVal = tableData.getDoubleValue(prevIndex + j);
                    nextVal = tableData.getDoubleValue(nextIndex + j);
                    
                    double value = prevVal + a*(nextVal - prevVal);
                    outputVars[j-1].getData().setDoubleValue(value);
                }
            }
            
            // save last values
            lastTupleIndex = nextIndex;
            lastIndexVal = indexVal;
        }
    }
}