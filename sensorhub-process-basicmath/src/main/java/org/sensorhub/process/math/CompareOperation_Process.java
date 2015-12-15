/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.process.math;

import java.util.Arrays;
import net.opengis.swe.v20.AllowedTokens;
import net.opengis.swe.v20.Boolean;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Text;
import org.vast.process.SMLException;
import org.vast.sensorML.ExecutableProcessImpl;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;


/**
 * <p>
 * Binary comparison operation between a value and a threshold. This outputs a
 * boolean giving the result of the comparison and also outputs the input value
 * to one of two outputs depending on the result of the comparison.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 13, 2015
 */
public class CompareOperation_Process extends ExecutableProcessImpl
{
	private Quantity value, threshold, outputIfTrue, outputIfFalse;
	private Boolean result;
	private Text operator;
	private OperatorEnum op;
    enum OperatorEnum {GreaterThan, LowerThan, GreaterOrEqual, LowerOrEqual}
    
    
    public CompareOperation_Process()
    {
    	SWEHelper sweHelper = new SWEHelper();
    	
    	// inputs
    	value = sweHelper.newQuantity(SWEConstants.NIL_TEMPLATE, "Value", null, "1");
        inputData.add("value", value);        
        threshold = sweHelper.newQuantity(SWEConstants.NIL_TEMPLATE, "Threshold", null, "1");
        inputData.add("threshold", threshold);
        
        // parameters
        operator = sweHelper.newText(SWEHelper.getPropertyUri("Operator"), "Operator", null);
        AllowedTokens operatorList = sweHelper.newAllowedTokens();
        for (OperatorEnum op: OperatorEnum.values())
            operatorList.addValue(op.toString());
        operator.setConstraint(operatorList);
        operator.assignNewDataBlock();
        paramData.add("operator", operator);
        
        // outputs
        result = sweHelper.newBoolean(SWEConstants.NIL_TEMPLATE, "Comparison Result", null);
        outputData.add("result", result);
        outputIfTrue = sweHelper.newQuantity(SWEConstants.NIL_TEMPLATE, "True Output", null, "1");
        outputData.add("outputIfTrue", outputIfTrue);
        outputIfFalse = sweHelper.newQuantity(SWEConstants.NIL_TEMPLATE, "False Output", null, "1");
        outputData.add("outputIfFalse", outputIfFalse);
    }

    
    @Override
    public void init() throws SMLException
    {
        // read operator
        try
        {
            op = OperatorEnum.valueOf(operator.getData().getStringValue());
        }
        catch (Exception e)
        {
            throw new SMLException("Invalid operator. Must be one of " + Arrays.toString(OperatorEnum.values()));
        }
    }
    

    @Override
    public void execute() throws SMLException
    {
        double val = value.getData().getDoubleValue();
        double thresh = threshold.getData().getDoubleValue();
        boolean res = false;
        
        switch (op)
        {
            case GreaterThan:
                res = val > thresh;
                break;
                
            case LowerThan:
                res = val < thresh;
                break;
                
            case GreaterOrEqual:
                res = val >= thresh;
                break;
                
            case LowerOrEqual:
                res = val <= thresh;
                break;
        }

        result.getData().setBooleanValue(res);
        
        if (res)
            outputIfTrue.getData().setDoubleValue(val);
        else
            outputIfFalse.getData().setDoubleValue(val);
    } 
}