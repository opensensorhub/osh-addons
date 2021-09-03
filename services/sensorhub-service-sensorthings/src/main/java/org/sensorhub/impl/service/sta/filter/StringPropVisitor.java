/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sta.filter;

import de.fraunhofer.iosb.ilt.frostserver.query.expression.constant.StringConstant;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.Function;


public abstract class StringPropVisitor implements BaseExpressionVisitor<StringPropVisitor>
{
    
    
    protected String getStringLiteral(Function f)
    {
        var p2 = f.getParameters().get(1);
        if (p2 instanceof StringConstant)
            return ((StringConstant) p2).getValue();
        else
            throw new IllegalArgumentException("Invalid String argument"); 
    }
}
