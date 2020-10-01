/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2019 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sta;

import org.sensorhub.api.feature.IFeatureStore;
import org.sensorhub.api.feature.IFeatureStore.FeatureField;
import org.sensorhub.impl.service.sta.ISTAObsPropStore.ObsPropDef;
import org.vast.ogc.gml.IFeature;


/**
 * <p>
 * Interface for SensorThings observation properties data stores.
 * </p>
 *
 * @author Alex Robin
 * @date March 23, 2020
 */
public interface ISTAObsPropStore extends IFeatureStore<ObsPropDef, FeatureField>
{
    
    public static class ObsPropDef implements IFeature
    {
        String uri;
        String name;
        String description;
        
        public ObsPropDef(String uri, String name, String description)
        {
            this.uri = uri;
            this.name = name;
            this.description = description;
        }
        
        @Override
        public String getName()
        {
            return name;
        }
        
        @Override
        public String getUniqueIdentifier()
        {
            return uri;
        }
        
        @Override
        public String getDescription()
        {
            return description;
        }
    }
}
