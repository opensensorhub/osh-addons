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

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.security.SecurityConfig;
import org.sensorhub.api.service.HttpServiceConfig;
import org.sensorhub.impl.datastore.view.ProcedureObsDatabaseViewConfig;
import org.sensorhub.impl.sensor.VirtualProcedureGroupConfig;


/**
 * <p>
 * Configuration class for the STA service module
 * </p>
 *
 * @author Alex Robin
 * @since Sep 25, 2019
 */
public class STAServiceConfig extends HttpServiceConfig
{
    public static class HubThingInfo
    {
        public String name = "SensorHub Node";
        public String description = "The local sensor hub and its sensors";
    }
    
    
    @DisplayInfo(label="Hub Thing Info", desc="Information used to generate a Thing that represents the local sensor hub")
    public HubThingInfo hubThing = new HubThingInfo();
    
    
    @DisplayInfo(desc="Metadata of procedure group that will be created to contain all sensors "
        + "registered through this service. Only sensors in this group will be modifiable by this service")
    public VirtualProcedureGroupConfig virtualSensorGroup;
    
    
    @DisplayInfo(label="Database Config", desc="Configuration of database used for persisting entities. "
        + "If none is provided, no Thing entities can be created. New sensors registered through this service will be "
        + "available on the hub, but with no persistence guarantee across restarts. Only the latest observation from "
        + "each datastream will be available and older observations will be discarded.")
    public STADatabaseConfig dbConfig = new STADatabaseConfig();
    
    
    @DisplayInfo(desc="Filtered view to select procedures/datastreams/observations exposed as read-only through this service")
    public ProcedureObsDatabaseViewConfig exposedResources;
    
    
    @DisplayInfo(desc="Security related options")
    public SecurityConfig security = new SecurityConfig();
    
    
    @DisplayInfo(desc="Set to true to enable transactional operations support")
    public boolean enableTransactional = false;
    
    
    @DisplayInfo(desc="Set to true to enable MQTT support")
    public boolean enableMqtt = false;
    
    
    @DisplayInfo(label="Max Observations Returned", desc="Maximum number of observations returned in a page (max limit)")
    public int maxObsCount = 1000;
    
    
    public STAServiceConfig()
    {
        this.moduleClass = STAService.class.getCanonicalName();
        this.endPoint = "/sta";
    }
}
