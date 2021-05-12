/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sta;

import java.security.AccessControlException;
import org.sensorhub.api.security.IPermission;
import org.sensorhub.impl.module.ModuleSecurity;
import org.sensorhub.impl.security.ItemPermission;
import org.sensorhub.impl.security.ModulePermissions;


public class STASecurity extends ModuleSecurity
{    
    private static final String NAME_THING = "things";
    private static final String NAME_SENSOR = "sensors";
    private static final String NAME_OBSPROP = "obsprops";
    private static final String NAME_DATASTREAM = "datastreams";
    private static final String NAME_FOI = "fois";
    private static final String NAME_OBS = "obs";
    private static final String NAME_LOCATION = "locations";
    
    private static final String LABEL_THING = "Things";
    private static final String LABEL_SENSOR = "Sensors";
    private static final String LABEL_OBSPROP = "Observed Properties";
    private static final String LABEL_DATASTREAM = "Datastreams";
    private static final String LABEL_FOI = "Features of Interest";
    private static final String LABEL_OBS = "Observations";
    private static final String LABEL_LOCATION = "Locations";
    
    final IPermission sta_read;
    final IPermission sta_read_thing;
    final IPermission sta_read_sensor;
    final IPermission sta_read_obsprop;
    final IPermission sta_read_datastream;
    final IPermission sta_read_foi;
    final IPermission sta_read_obs;
    final IPermission sta_read_location;
    final IPermission sta_insert;
    final IPermission sta_insert_thing;
    final IPermission sta_insert_sensor;
    final IPermission sta_insert_obsprop;
    final IPermission sta_insert_datastream;
    final IPermission sta_insert_foi;
    final IPermission sta_insert_obs;
    final IPermission sta_insert_location;
    final IPermission sta_update;
    final IPermission sta_update_thing;
    final IPermission sta_update_sensor;
    final IPermission sta_update_obsprop;
    final IPermission sta_update_datastream;
    final IPermission sta_update_foi;
    final IPermission sta_update_obs;
    final IPermission sta_update_location;
    final IPermission sta_delete;
    final IPermission sta_delete_thing;
    final IPermission sta_delete_sensor;
    final IPermission sta_delete_obsprop;
    final IPermission sta_delete_datastream;
    final IPermission sta_delete_foi;
    final IPermission sta_delete_obs;
    final IPermission sta_delete_location;    

    ThreadLocal<Exception> authError = new ThreadLocal<>();
    
    
    public STASecurity(STAService service, boolean enable)
    {
        super(service, "stapi", enable);
        
        // register permission structure
        sta_read = new ItemPermission(rootPerm, "get");
        sta_read_thing = new ItemPermission(sta_read, NAME_THING, LABEL_THING);
        sta_read_sensor = new ItemPermission(sta_read, NAME_SENSOR, LABEL_SENSOR);
        sta_read_obsprop = new ItemPermission(sta_read, NAME_OBSPROP, LABEL_OBSPROP);
        sta_read_datastream = new ItemPermission(sta_read, NAME_DATASTREAM, LABEL_DATASTREAM);
        sta_read_foi = new ItemPermission(sta_read, NAME_FOI, LABEL_FOI);
        sta_read_obs = new ItemPermission(sta_read, NAME_OBS, LABEL_OBS);
        sta_read_location = new ItemPermission(sta_read, NAME_LOCATION, LABEL_LOCATION);
        
        sta_insert = new ItemPermission(rootPerm, "create");
        sta_insert_thing = new ItemPermission(sta_insert, NAME_THING, LABEL_THING);
        sta_insert_sensor = new ItemPermission(sta_insert, NAME_SENSOR, LABEL_SENSOR);
        sta_insert_obsprop = new ItemPermission(sta_insert, NAME_OBSPROP, LABEL_OBSPROP);
        sta_insert_datastream = new ItemPermission(sta_insert, NAME_DATASTREAM, LABEL_DATASTREAM);
        sta_insert_foi = new ItemPermission(sta_insert, NAME_FOI, LABEL_FOI);
        sta_insert_obs = new ItemPermission(sta_insert, NAME_OBS, LABEL_OBS);
        sta_insert_location = new ItemPermission(sta_insert, NAME_LOCATION, LABEL_LOCATION);
        
        sta_update = new ItemPermission(rootPerm, "update");
        sta_update_thing = new ItemPermission(sta_update, NAME_THING, LABEL_THING);
        sta_update_sensor = new ItemPermission(sta_update, NAME_SENSOR, LABEL_SENSOR);
        sta_update_obsprop = new ItemPermission(sta_update, NAME_OBSPROP, LABEL_OBSPROP);
        sta_update_datastream = new ItemPermission(sta_update, NAME_DATASTREAM, LABEL_DATASTREAM);
        sta_update_foi = new ItemPermission(sta_update, NAME_FOI, LABEL_FOI);
        sta_update_obs = new ItemPermission(sta_update, "obs", LABEL_OBS);
        sta_update_location = new ItemPermission(sta_update, NAME_LOCATION, LABEL_LOCATION);
        
        sta_delete = new ItemPermission(rootPerm, "delete");
        sta_delete_thing = new ItemPermission(sta_delete, NAME_THING, LABEL_THING);
        sta_delete_sensor = new ItemPermission(sta_delete, NAME_SENSOR, LABEL_SENSOR);
        sta_delete_obsprop = new ItemPermission(sta_delete, NAME_OBSPROP, LABEL_OBSPROP);
        sta_delete_datastream = new ItemPermission(sta_delete, NAME_DATASTREAM, LABEL_DATASTREAM);
        sta_delete_foi = new ItemPermission(sta_delete, NAME_FOI, LABEL_FOI);
        sta_delete_obs = new ItemPermission(sta_delete, NAME_OBS, LABEL_OBS);
        sta_delete_location = new ItemPermission(sta_delete, NAME_LOCATION, LABEL_LOCATION);
        
        // register wildcard permission tree usable for all SOS services
        // do it at this point so we don't include specific offering permissions
        ModulePermissions wildcardPerm = rootPerm.cloneAsTemplatePermission("SensorThings Services");
        service.getParentHub().getSecurityManager().registerModulePermissions(wildcardPerm);
                
        // create permissions for each offering
        //for (SOSProviderConfig offering: sos.getConfiguration().dataProviders)
        //    addOfferingPermissions(offering.offeringID);
        
        // register permission tree
        service.getParentHub().getSecurityManager().registerModulePermissions(rootPerm);
    }
    
    
    @Override
    public void checkPermission(IPermission perm)
    {
        try
        {
            authError.set(null);
            super.checkPermission(perm);
        }
        catch (AccessControlException e)
        {
            authError.set(e);
            throw e;
        }
    }
    
    
    public Exception getPermissionError()
    {
        Exception e = authError.get();
        authError.set(null);
        return e;
    }
    
    
    /*protected void addOfferingPermissions(String offeringUri)
    {
        String permName = getOfferingPermissionName(offeringUri);
        new ItemPermission(sta_read_thing, permName);
        new ItemPermission(sta_read_sensor, permName);
        new ItemPermission(sta_read_foi, permName);
        new ItemPermission(sta_read_obs, permName);
        new ItemPermission(sta_insert_obs, permName);
        new ItemPermission(sta_update_obs, permName);
        new ItemPermission(sta_delete_obs, permName);
        new ItemPermission(sta_update_sensor, permName);
        new ItemPermission(sta_delete_sensor, permName);
    }
    
    
    public void checkPermission(String offeringUri, IPermission perm) throws SecurityException
    {
        String permName = getOfferingPermissionName(offeringUri);
        IPermission offPerm = perm.getChildren().get(permName);
        Asserts.checkNotNull(offPerm, "Invalid permission check");
        checkPermission(offPerm);
    }
    
    
    protected String getOfferingPermissionName(String offeringUri)
    {
        return "offering[" + offeringUri + "]";
    }*/
}
