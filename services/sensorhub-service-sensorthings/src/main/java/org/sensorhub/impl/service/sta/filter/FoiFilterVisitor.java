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

import org.sensorhub.api.datastore.feature.FoiFilter;


/**
 * <p>
 * Visitor used to build an FoiFilter from STA $filter expression
 * </p>
 *
 * @author Alex Robin
 * @date Apr 16, 2021
 */
public class FoiFilterVisitor extends ResourceFilterVisitor<FoiFilterVisitor, FoiFilter.Builder>
{
        
    public FoiFilterVisitor()
    {
        this(new FoiFilter.Builder());
    }
    
    
    public FoiFilterVisitor(FoiFilter.Builder builder)
    {
        super(builder);
        
        //this.propTypes.put("feature", FeatureVisitor.class);
        this.propTypes.put("Observations", ObsAssocVisitor.class);
    }
    
    
    class ObsAssocVisitor extends ObsFilterVisitor
    {
        @Override
        protected void assignFilter()
        {
            var obsFilter = builder.build();
            FoiFilterVisitor.this.builder.withObservations(obsFilter);
        }
    }


    @Override
    protected FoiFilterVisitor getNewInstance()
    {
        return new FoiFilterVisitor();
    }

}
