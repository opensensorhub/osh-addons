/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.isa;

import org.vast.sensorML.SMLHelper;
import org.vast.swe.SWEBuilders.QuantityBuilder;
import org.vast.swe.SWEConstants;


public class ISAHelper extends SMLHelper
{
    private static final String ISA_DEF_URI_BASE = "http://sensorml.com/ont/isa/property/";
    
    
    /*
     * Generate a definition URI resolving to the ISA ontology.
     * See http://sensorml.com/ont/isa/property for registered terms
     */        
    public static String getIsaUri(String concept)
    {
        return ISA_DEF_URI_BASE + concept;
    }
    
    
    QuantityBuilder errorField(String uom)
    {
        return createQuantity()
            .description("The accuracy of the measurement")
            .definition(ISAHelper.getPropertyUri("AbsoluteAccuracy"))
            .uomCode(uom)
            .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN);
    }
}
