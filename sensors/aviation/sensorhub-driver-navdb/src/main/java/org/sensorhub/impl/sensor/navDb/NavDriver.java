/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2018 Delta Air Lines, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.navDb;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.utils.aero.INavDatabase;
import org.sensorhub.utils.aero.INavDatabase.INavDbWaypoint;
import org.sensorhub.utils.aero.impl.AeroUtils;


/**
 * Main driver class for exposing navigation database as sensor outputs
 * 
 * @author Tony Cook
 * @since Nov, 2017
 */
public class NavDriver extends AbstractSensorModule<NavConfig>
{
    AirportOutput airptOutput;
	WaypointOutput wayptOutput;
	NavaidOutput navaidOutput;

    INavDatabase navDB;
    boolean airports = true;
    boolean waypoints = true;
    boolean navaids = true;
    

	public NavDriver()
	{
	}


    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
            super.updateSensorDescription();
            sensorDescription.setDescription("NavDB offerings");
        }
    }


	@Override
    protected void doInit() throws SensorHubException
	{
		super.doInit();
		
		// IDs
		this.uniqueID = AeroUtils.AERO_SYSTEM_URI_PREFIX + "navDb";
		this.xmlID = "NAVDB";
        
        // Initialize Outputs
        try
        {
            if (airports)
            {
                this.airptOutput = new AirportOutput(this);
                addOutput(airptOutput, false);
                airptOutput.init();
            }

            if (navaids)
            {
                this.navaidOutput = new NavaidOutput(this);
                addOutput(navaidOutput, false);
                navaidOutput.init();
            }

            if (waypoints)
            {
                this.wayptOutput = new WaypointOutput(this);
                addOutput(wayptOutput, false);
                wayptOutput.init();
            }
        }
        catch (IOException e)
        {
            throw new SensorHubException("Cannot instantiate NavDB outputs", e);
        }
    }


	@Override
	protected void doStart() throws SensorHubException
	{
	    this.navDB = INavDatabase.getInstance(getParentHub(), config.navDbModuleId);
	    loadAirports();
	    loadNavaids();
	    loadWaypoints();
    }

	
    private void loadAirports()
    {
        var selectedAirports = (config.airportFilterPath != null) ?
            readSelectedAirportIcaos(config.airportFilterPath) : null;
        
        // build filtered list of airports
        var filteredAirports = navDB.getAirports().values().stream()
            .filter(airport -> {
                return selectedAirports == null || selectedAirports.contains(airport.getName());
            })
            .collect(Collectors.toList());
        
        // send entries to output
        airptOutput.sendEntries(filteredAirports);
        getLogger().info("{} airports loaded", filteredAirports.size());
    }


    private void loadNavaids()
    {
        var navaids = navDB.getNavaids().values();

        // send entries to output
        navaidOutput.sendEntries(navaids);
        getLogger().info("{} navaids loaded", navaids.size());
    }


    private void loadWaypoints()
    {
        var waypts = navDB.getWaypoints().values();

        // send entries to output
        wayptOutput.sendEntries(waypts);
        getLogger().info("{} waypoints loaded", waypts.size());
    }


    public Set<String> readSelectedAirportIcaos(String filterPath)
    {
        Set<String> selectedAirports = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filterPath)))
        {
            while (true)
            {
                String l = br.readLine();
                if (l == null)
                    break;
                String icao = l.substring(0, 4);
                selectedAirports.add(icao);
            }
        }
        catch (IOException e)
        {
            getLogger().error("Cannot read airport list", e);
        }

        return selectedAirports;
    }
    

    public List<INavDbWaypoint> getSelectedAirports(List<INavDbWaypoint> navDbEntries, String deltaPath)
    {
        var icaos = readSelectedAirportIcaos(deltaPath);
        List<INavDbWaypoint> deltaAirports = new ArrayList<>();
        
        for (var a: navDbEntries) {
            if (icaos.contains(a.getCode()) && a.getType().equals("AIRPORT")) {
                deltaAirports.add(a);
            }
        }
        
        return deltaAirports;
    }
	

	@Override
	protected void doStop() throws SensorHubException
	{
	    
    }


	@Override
	public boolean isConnected() 
	{
		return true;
	}
}
