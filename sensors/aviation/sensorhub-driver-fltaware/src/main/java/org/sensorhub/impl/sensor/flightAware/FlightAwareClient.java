/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2018 Delta Air Lines, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.flightAware;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.slf4j.Logger;

public class FlightAwareClient implements Runnable 
{
    private static final boolean USE_COMPRESSION = false;
    
    Logger log;
    String serverUrl;
	String userName;
	String password;
	long replayDuration = 0; // in seconds before current time
	List<String> messageTypes = new ArrayList<>();
	List<String> filterAirlines = new ArrayList<>();
	SSLSocket ssl_socket = null;
	IMessageHandler msgHandler;
	volatile boolean started;
    

	public FlightAwareClient(String serverUrl, String uname, String pwd, Logger log, IMessageHandler msgHandler) {
	    this.log = log;
		this.serverUrl = serverUrl;
		this.userName = uname;
		this.password = pwd;
		this.msgHandler = msgHandler;
	}

	private String  buildInitiationCommand() 
	{
	    long pitr = System.currentTimeMillis()/1000 - replayDuration;
	    String initiationCmd = (replayDuration > 0) ? "pitr " + pitr : "live";
	    initiationCmd += " username " + userName + " password " + password + " keepalive 15";
	    
		if (USE_COMPRESSION) {
			initiationCmd += " compression gzip";
		}

		if (!filterAirlines.isEmpty()) {
			StringBuilder b = new StringBuilder();
			b.append(" filter \"");
			for(String code: filterAirlines)
				b.append(code + " ");  
			b.append("\"");
			initiationCmd += b.toString();
		}
		
		if(!messageTypes.isEmpty()) {
			StringBuilder b = new StringBuilder();
			b.append(" events \"");
			for(String type: messageTypes)
				b.append(type + " ");  
			b.append("\"");
			initiationCmd += b.toString();
		}
		initiationCmd += "\n";

		return initiationCmd;
	}
	
	@Override
	public void run() {
		log.info("Firehose client thread started");
		
		started = true;
		OutputStreamWriter writer = null;
		InputStream inputStream = null;
		BufferedReader reader = null;
		
		try {
    		try {
    			ssl_socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(serverUrl, 1501);
    			// enable certifcate validation:
    			SSLParameters sslParams = new SSLParameters();
    			sslParams.setEndpointIdentificationAlgorithm("HTTPS");
    			ssl_socket.setSSLParameters(sslParams);
    
    			//  initiate connection with flight aware server
    			writer = new OutputStreamWriter(ssl_socket.getOutputStream(), "UTF8");
    			String initiationCmd = buildInitiationCommand();
    			writer.write(initiationCmd);
    			writer.flush();
    
    			// read messages from FlightAware
    			inputStream = ssl_socket.getInputStream();
    			reader = new BufferedReader(new InputStreamReader(inputStream));    			
    		} catch (IOException e) {
                log.error("Cannot connect to Firehose", e);
                return;
            }
			
    		try {
    			String message = null;
    			//int cnt = 0;
    			while (started && (message = reader.readLine()) != null) {
    			    msgHandler.handle(message);
    				/*//  simulate connection closed by peer
    				if(++cnt >= 100) {
    					throw new IOException("Test closed by peer");
    				}*/
    			}    			
    		} catch (IOException e) {
                if (started)
                    log.error("Error processing Firehose message", e);         
            }
		} finally {
		    try {
				//  close streams
				if (writer != null)
				    writer.close();
				if (reader != null)
				    reader.close();
				if (inputStream != null)
				    inputStream.close();
				
				// close socket
				log.debug("Closing Firehose client socket");
				if (ssl_socket != null && !ssl_socket.isClosed())
				    ssl_socket.close();
				
			} catch (IOException e) {
				log.error("Error closing Firehose client socket");
			}
			
		    started = false;
			log.info("Firehose client thread stopped");
		}
	}
	
	protected synchronized void start() {
	    Thread thread = new Thread(this, "FirehoseClient");
        thread.start();
	}

	protected synchronized void stop() {
		started = false;
		
		// force close from here
        try {
            if (ssl_socket != null && !ssl_socket.isClosed())
                ssl_socket.close();
        } catch (IOException e) {
            log.error("Error closing Firehose client socket", e);
        }
	}

	public synchronized void restart() {
		log.info("Restarting Firehose client thread");
		stop();
		
		//  wait until ssl_socket is closed before restarting
		while(!ssl_socket.isClosed()) {
			try {
			    log.debug("Socket still not closed. Waiting ..");
			    Thread.sleep(5000L);				
			} catch (InterruptedException e) {
				log.debug(e.getMessage());
				continue;
			}
		}
		
		// restart feed
		start();
	}

	public boolean isStarted() {
		return started;
	}
	
	public void setReplayDuration(long replayDuration) {
	    this.replayDuration = replayDuration;
	}
	
	public void addAirline(String airline) {
		filterAirlines.add(airline);
	}
	
	public void addMessageType(String messageType) {
		messageTypes.add(messageType);
	}

}