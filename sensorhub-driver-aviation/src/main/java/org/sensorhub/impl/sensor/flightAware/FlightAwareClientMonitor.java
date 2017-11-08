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

import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.sensorhub.impl.sensor.flightAware.FlightAwareClient.MessageTimeThread;

public class FlightAwareClientMonitor extends TimerTask {
	FlightAwareClient client;

	  //  Warn us if messages start stacking up- note sys clock dependence
    private static final long MESSAGE_TIMER_CHECK_PERIOD = TimeUnit.MINUTES.toMillis(1);
    private static final long MESSAGE_LATENCY_WARN_LIMIT = TimeUnit.MINUTES.toSeconds(1);
    private static final long MESSAGE_LATENCY_RESTART_LIMIT = TimeUnit.MINUTES.toSeconds(2);  // experiment with these
	private MessageTimeThread messageTimeThread;

	public FlightAwareClientMonitor(FlightAwareClient client) {
		this.client = client;
	}
	
	@Override
	public void run() {
		long sysTime = System.currentTimeMillis() / 1000;
		if(sysTime - lastMessageTime > MESSAGE_LATENCY_RESTART_LIMIT) {
			// signal Client to restart thread
		}
		if(sysTime - lastMessageTime > MESSAGE_LATENCY_WARN_LIMIT) {
			log.error("Messages getting old: deltaTime seconds = {}", sysTime - lastMessageTime);
		}
	}

}
