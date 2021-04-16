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

import org.sensorhub.impl.service.sta.STAThingFilter;


/**
 * <p>
 * Visitor used to build a ThingFilter from STA $filter expression
 * </p>
 *
 * @author Alex Robin
 * @date Apr 16, 2021
 */
public class ThingFilterVisitor extends ResourceFilterVisitor<ThingFilterVisitor, STAThingFilter.Builder>
{
    
    public ThingFilterVisitor()
    {
        this(new STAThingFilter.Builder());
    }
    
    
    public ThingFilterVisitor(STAThingFilter.Builder builder)
    {
        super(builder);
        
        //this.propTypes.put("properties", new UomVisitor());
        //this.propTypes.put("Datastreams", new DatastreamAssocVisitor());
        //this.propTypes.put("Locations", new LocationAssocVisitor());
        //this.propTypes.put("HistoricalLocations", new HistoricalLocAssocVisitor());
    }


    @Override
    protected ThingFilterVisitor getNewInstance()
    {
        return new ThingFilterVisitor();
    }
    
}
