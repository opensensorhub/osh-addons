/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2017 Botts Innovative Research, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.flightAware;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlightAwareConverter implements FlightObjectListener
{
	static final Logger log = LoggerFactory.getLogger(FlightAwareConverter.class);
	//  Do I really need to retain in maps?  Or just let listener classes deal with that
    List<FlightPlanListener> planListeners = new ArrayList<>();
    List<PositionListener> positionListeners = new ArrayList<>();

    public void addPlanListener(FlightPlanListener l) {
    	planListeners.add(l);
    }

    public boolean removePlanListener(FlightPlanListener l) {
    	return planListeners.remove(l);
    }
	
    public void addPositionListener(PositionListener l) {
    	positionListeners.add(l);
    }

    public boolean removePositiobListener(PositionListener l) {
    	return positionListeners.remove(l);
    }
	
	@Override
	public void processMessage(FlightObject obj) {
		// call api and get flightplan
		if(!obj.type.equals("flightplan") && !obj.type.equals("position") ) {
			log.warn("FlightAwareConverter does not yet support: " + obj.type);
			return;
		}

		switch(obj.type) {
		case "flightplan":
			Thread thread = new Thread(new ProcessPlanThread(this, obj));
			thread.start();
			break;
		case "position":
			Thread posThread = new Thread(new ProcessPositionThread(this, obj));
			posThread.start();
			break;
		default:
			log.error("Unknown message slipped through somehow: " + obj.type);
		}
	}

	protected void newFlightPlan(FlightPlan plan) {
		for(FlightPlanListener l: planListeners)
			l.newFlightPlan(plan);
	}
	
	protected void newFlightPosition(FlightObject pos) {
		for(PositionListener l: positionListeners)
			l.newPosition(pos);
	}

}
