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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.opengis.swe.v20.Boolean;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Text;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;


/**
 * <p>
 * Binary comparison operation between a value and a threshold. This outputs a
 * boolean giving the result of the comparison and also outputs the input value
 * to one of two outputs depending on the result of the comparison.
 * </p>
 *
 * @author Alex Robin
 * @since Nov 13, 2015
 */
public class CompareOperation extends ExecutableProcessImpl
{
    public static final OSHProcessInfo INFO = new OSHProcessInfo("compareOp", "Comparison Operation", null, CompareOperation.class);
    private Quantity value, threshold, outputIfTrue, outputIfFalse;
	private Boolean result;
	private Text operator;
	private OperatorEnum op;
    enum OperatorEnum {GreaterThan, LowerThan, GreaterOrEqual, LowerOrEqual}
    
    
    public CompareOperation()
    {
    	super(INFO);
        SWEHelper sweHelper = new SWEHelper();
        
        // inputs
        value = sweHelper.createQuantity()
            .definition(SWEConstants.DEF_DN)
            .label("Value")
            .uomUri(SWEConstants.UOM_ANY)
            .build();
        inputData.add("value", value);
        
        threshold = sweHelper.createQuantity()
            .definition(SWEConstants.DEF_DN)
            .label("Threshold")
            .uomUri(SWEConstants.UOM_ANY)
            .build();
        inputData.add("threshold", threshold);
        
        // parameters
        operator = sweHelper.createText()
            .definition(SWEHelper.getPropertyUri("Operator"))
            .label("Operator")
            .addAllowedValues(Stream.of(OperatorEnum.values())
                .map(e -> e.toString())
                .collect(Collectors.toList()))
            .build();
        operator.assignNewDataBlock();
        paramData.add("operator", operator);
        
        // outputs
        result = sweHelper.createBoolean()
            .definition(SWEConstants.DEF_FLAG)
            .label("Comparison Result")
            .build();
        outputData.add("result", result);
                
        outputIfTrue = sweHelper.createQuantity()
            .definition(SWEConstants.DEF_DN)
            .label("True Output")
            .uomUri(SWEConstants.UOM_ANY)
            .build();
        outputData.add("outputIfTrue", outputIfTrue);
        
        outputIfFalse = sweHelper.createQuantity()
            .definition(SWEConstants.DEF_DN)
            .label("False Output")
            .uomUri(SWEConstants.UOM_ANY)
            .build();
        outputData.add("outputIfFalse", outputIfFalse);
    }

    
    @Override
    public void init() throws ProcessException
    {
        super.init();
        
        // read operator
        try
        {
            op = OperatorEnum.valueOf(operator.getData().getStringValue());
        }
        catch (IllegalArgumentException e)
        {
            throw new ProcessException("Invalid operator. Must be one of " + Arrays.toString(OperatorEnum.values()), e);
        }
    }
    

    @Override
    public void execute() throws ProcessException
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