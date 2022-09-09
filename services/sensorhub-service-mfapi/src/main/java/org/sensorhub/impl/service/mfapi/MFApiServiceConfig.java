/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2020 Sensia Software LLC. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.mfapi;

import java.util.ArrayList;
import java.util.List;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.datastore.feature.FoiFilter;
import org.sensorhub.api.security.SecurityConfig;
import org.sensorhub.impl.datastore.view.ObsSystemDatabaseViewConfig;
import org.sensorhub.impl.service.ogc.OGCServiceConfig;


/**
 * <p>
 * Configuration class for the SWE API service module
 * </p>
 *
 * @author Alex Robin
 * @since Oct 12, 2020
 */
public class MFApiServiceConfig extends OGCServiceConfig
{
    public static class CollectionConfig
    {
        @DisplayInfo(desc="Name of the collection (used in the URL path)")
        public String name;
        
        @DisplayInfo(desc="Title of the collection")
        public String title;
        
        @DisplayInfo(desc="Description of the collection content")
        public String description;
        
        @DisplayInfo(desc="Filter to select collection items")
        public FoiFilter includeFilter;
    }
    
    
    @DisplayInfo(desc="Filtered view to select features exposed as read-only through this service")
    public ObsSystemDatabaseViewConfig exposedResources = null;


    @DisplayInfo(desc="Security related options")
    public SecurityConfig security = new SecurityConfig();


    @DisplayInfo(label="Max Limit", desc="Maximum number of resources returned in a single page")
    public int maxResponseLimit = 100000;
    
    
    @DisplayInfo(desc="Default live time-out for new offerings created via SOS-T")
    public double defaultLiveTimeout = 600.0;
    
    
    @DisplayInfo(desc="List of pre-filtered feature collections")
    public List<CollectionConfig> collections= new ArrayList<>();


    public MFApiServiceConfig()
    {
        this.moduleClass = MFApiService.class.getCanonicalName();
        this.endPoint = "/mfapi";
    }
}
