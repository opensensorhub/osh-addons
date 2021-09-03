/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2019 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sta.filter;


/**
 * <p>
 * Visitor used to build a predicate for filtering on UoM properties
 * </p>
 *
 * @author Alex Robin
 * @date Apr 26, 2021
 */
public class UomFilterVisitor extends EntityFilterVisitor<UomFilterVisitor>
{
    
    
    
    public UomFilterVisitor()
    {
        //this.propTypes.put("name", new NameVisitor());
        //this.propTypes.put("symbol", new NameVisitor());
        //this.propTypes.put("definition", new NameVisitor());
    }


    @Override
    protected UomFilterVisitor getNewInstance()
    {
        return new UomFilterVisitor();
    }

}
