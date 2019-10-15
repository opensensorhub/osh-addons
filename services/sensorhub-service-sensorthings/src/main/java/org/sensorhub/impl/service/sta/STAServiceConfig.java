/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sta;

import java.util.HashSet;
import java.util.Set;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.security.SecurityConfig;
import org.sensorhub.api.service.ServiceConfig;
import org.sensorhub.impl.sensor.VirtualProcedureGroupConfig;
import org.sensorhub.impl.service.ogc.OGCServiceConfig.CapabilitiesInfo;


/**
 * <p>
 * Configuration class for the STA service module
 * </p>
 *
 * @author Alex Robin
 * @since Sep 25, 2019
 */
public class STAServiceConfig extends ServiceConfig
{
    
    @DisplayInfo(label="Service Information", desc="Information included in the service metadata document")
    public CapabilitiesInfo ogcCapabilitiesInfo = new CapabilitiesInfo();
    
    
    @DisplayInfo(desc="Set to true to enable transactional operations support")
    public boolean enableTransactional = false;
    
    
    @DisplayInfo(label="Max Observations Returned", desc="Maximum number of observations returned in a page (max limit)")
    public int maxObsCount = 1000;
    
    
    @DisplayInfo(label="Database Config", desc="Configuration of database for persisting entities. "
        + "If none is provided, only live sensors are exposed and no new Sensor or Thing entities can "
        + "be created.")
    public STADatabaseConfig dbConfig = new STADatabaseConfig();
    
    
    @DisplayInfo(desc="Metadata of procedure group that will be created to contain all sensors "
        + "registered through this service. Only sensors in this group will be modifiable by this service")
    public VirtualProcedureGroupConfig virtualSensorGroup;
    
    
    @DisplayInfo(desc="Unique IDs of other procedures or procedure groups to expose as read-only through this service")
    public Set<String> exposedProcedures = new HashSet<>();
    
    
    //@DisplayInfo(desc="Mapping of custom formats mime-types to custom serializer classes")
    //public List<SOSCustomFormatConfig> customFormats = new ArrayList<>();
    
    
    @DisplayInfo(desc="Security related options")
    public SecurityConfig security = new SecurityConfig();
    
    
    public STAServiceConfig()
    {
        this.moduleClass = STAService.class.getCanonicalName();
        this.endPoint = "/sta";
    }
}
