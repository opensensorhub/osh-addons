/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2025 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.utils.aero;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.utils.Async;


/**
 * <p>
 * Interface for navigation databases
 * </p>
 *
 * @author Alex Robin
 * @since Mar 26, 2025
 */
public interface INavDatabase
{
    
    public interface IDecodedRoute
    {
        public List<IWaypoint> getWaypoints();
        public List<String> getUnknownCodes();
    }
    
    
    public interface INavDbWaypoint extends IWaypoint
    {
        public String getName();
    }
    
    
    public boolean isReady();
    
    
    public Map<String, INavDbWaypoint> getAirports();
    
    
    public Map<String, INavDbWaypoint> getNavaids();
    
    
    public Map<String, INavDbWaypoint> getWaypoints();
    
    
    public IDecodedRoute decodeRoute(String codedRouteString);
    
    
    public static INavDatabase getInstance(ISensorHub hub, String moduleID)
    {
        return getInstance(hub, moduleID, 30000);
    }
    
    
    public static INavDatabase getInstance(ISensorHub hub, String moduleID, int timeout)
    {
        ModuleRegistry reg;
        try
        {
            reg = hub.getModuleRegistry();
            var navDb = (INavDatabase)reg.getModuleById(moduleID);
            
            try {
                Async.waitForCondition(() -> navDb.isReady(), timeout);
            } catch (TimeoutException e) {
                throw new IllegalStateException("Navigation database did not start in the last " + timeout/1000 + "s");
            }
            
            return navDb;
        }
        catch (SensorHubException | ClassCastException e)
        {
            throw new IllegalStateException("No navigation database found", e);
        }
    }
}
