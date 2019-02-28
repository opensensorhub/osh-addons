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
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Text;
import org.vast.process.SMLException;
import org.vast.sensorML.ExecutableProcessImpl;
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
public class BinaryOperation_Process extends ExecutableProcessImpl
{
	private Quantity operand1, operand2, result;
	private Text operator;
	private OperatorEnum op;
    enum OperatorEnum {ADD, SUB, MUL, DIV, POW}
    
    
    public BinaryOperation_Process()
    {
    	SWEHelper sweHelper = new SWEHelper();
    	
    	// inputs
        operand1 = sweHelper.newQuantity(SWEConstants.NIL_TEMPLATE, "Operand1", null, "1");
        inputData.add("operand1", operand1);
        
        operand2 = sweHelper.newQuantity(SWEConstants.NIL_TEMPLATE, "Operand2", null, "1");
        inputData.add("operand2", operand2);
        
        // parameters
        operator = sweHelper.newText(SWEHelper.getPropertyUri("Operator"), "Operator", null);
        AllowedTokens operatorList = sweHelper.newAllowedTokens();
        for (OperatorEnum op: OperatorEnum.values())
            operatorList.addValue(op.toString());
        operator.setConstraint(operatorList);
        operator.assignNewDataBlock();
        paramData.add("operator", operator);
        
        // outputs
        result = sweHelper.newQuantity(SWEConstants.NIL_TEMPLATE, "Result", null, "1");
        outputData.add("result", result);
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