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

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlightAwareClientMonitor extends TimerTask {
	FlightAwareClient client;

	  //  Warn us if messages start stacking up- note sys clock dependence
    private static final long MESSAGE_TIMER_CHECK_PERIOD = 10_000L; //TimeUnit.MINUTES.toMillis(1);
    private static final long MESSAGE_LATENCY_WARN_LIMIT = 10;//TimeUnit.MINUTES.toSeconds(1);
    private static final long MESSAGE_LATENCY_RESTART_LIMIT = 20;//TimeUnit.MINUTES.toSeconds(2);  // experiment with these
	static final Logger log = LoggerFactory.getLogger(FlightAwareClientMonitor.class);
	boolean restarting = false; 
	
	public FlightAwareClientMonitor(FlightAwareClient client) {
		this.client = client;
		Timer timer = new Timer(true);
		timer.scheduleAtFixedRate(this, 10000L, MESSAGE_TIMER_CHECK_PERIOD );
	}
	
	@Override
	public void run() {
		if(restarting ) {
			log.debug("*** still restarting");
			return;
		}
		long sysTime = System.currentTimeMillis() / 1000;
		long lastMessageTime = client.getLastMessageTime();
		if(sysTime - lastMessageTime > MESSAGE_LATENCY_RESTART_LIMIT) {
			log.error("Messages older than latency restart limit. Restarting connection: deltaTime seconds = {}", sysTime - lastMessageTime);
			restarting = true;
			client.restartThread();
			restarting = false;
		}
		if(sysTime - lastMessageTime > MESSAGE_LATENCY_WARN_LIMIT) {
			log.error("Messages getting old: deltaTime seconds = {}", sysTime - lastMessageTime);
		}
	}

}
