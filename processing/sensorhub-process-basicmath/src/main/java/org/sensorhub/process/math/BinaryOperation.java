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
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Text;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;


/**
 * <p>
 * Implementation of a binary operation with respect to a parameter (addition, 
 * soustraction, multiplication, division, power)
 * </p>
 *
 * @author Alexandre Robin & Gregoire Berthiau
 * @date Mar 7, 2007
 */
public class BinaryOperation extends ExecutableProcessImpl
{
	public static final OSHProcessInfo INFO = new OSHProcessInfo("binaryOp", "Binary Operation", null, BinaryOperation.class);
    private Quantity operand1;
    private Quantity operand2;
    private Quantity result;
	private Text operator;
	private OperatorEnum op;
    enum OperatorEnum {ADD, SUB, MUL, DIV, POW}
    
    
    public BinaryOperation()
    {
    	super(INFO);
        SWEHelper sweHelper = new SWEHelper();
    	
    	// inputs
        operand1 = sweHelper.createQuantity()
            .definition(SWEConstants.DEF_DN)
            .label("Operand1")
            .uomUri(SWEConstants.UOM_ANY)
            .build();
        inputData.add("operand1", operand1);
        
        operand2 = sweHelper.createQuantity()
            .definition(SWEConstants.DEF_DN)
            .label("Operand2")
            .uomUri(SWEConstants.UOM_ANY)
            .build();
        inputData.add("operand2", operand2);
        
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
        result = sweHelper.createQuantity()
            .definition(SWEConstants.DEF_DN)
            .label("Result")
            .uomUri(SWEConstants.UOM_ANY)
            .build();
        outputData.add("result", result);
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
        double N1 = operand1.getData().getDoubleValue();
        double N2 = operand2.getData().getDoubleValue();
        double Nr = 0.0;
        
        switch (op)
        {
            case ADD:
                Nr = N1 + N2;
                break;
                
            case SUB:
                Nr = N1 - N2;
                break;
                
            case MUL:
                Nr = N1 * N2;
                break;
                
            case DIV:
                Nr = N1 / N2;
                break;
                
            case POW:
                Nr = Math.pow(N1, N2);
                break;
        }

        //System.out.println(operator + " = " + Nr);
        result.getData().setDoubleValue(Nr);
    } 
}