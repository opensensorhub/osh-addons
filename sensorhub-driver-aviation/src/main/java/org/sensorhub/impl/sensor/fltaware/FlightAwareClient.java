package org.sensorhub.impl.sensor.fltaware;

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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

public class FlightAwareClient implements Runnable {

	private String serverUrl;
	String userName = "drgregswilson";
	String password = "2809b6196a2cfafeb89db0a00b117ac67e876220";
	private static String initiation_command = "live username drgregswilson password 2809b6196a2cfafeb89db0a00b117ac67e876220";
    private SSLSocket ssl_socket;
    private static final boolean useCompression = false;
    List<String> messageTypes = new ArrayList<>();
    List<FlightObjectListener> listeners = new ArrayList<>();
    volatile boolean started;
    
    public static void main(String[] args) {
        String machineName = "firehose.flightaware.com";
    	String userName = "drgregswilson";
    	String password = "2809b6196a2cfafeb89db0a00b117ac67e876220";
        FlightAwareClient client = new FlightAwareClient(machineName, userName, password);
        client.messageTypes.add("flightplan");
        Thread thread = new Thread(client);
        thread.start();
    }

    public FlightAwareClient(String serverUrl, String uname, String pwd) {
    	this.serverUrl = serverUrl;
    	this.userName = uname;
    	this.password = pwd;
    }
    
    public void addListener(FlightObjectListener l) {
    	listeners.add(l);
    }

    public boolean removeListener(FlightObjectListener l) {
    	return listeners.remove(l);
    }

    @Override
	public void run() {
    	System.out.println("Starting FlightAware Client");
    	started = true;
        try {
            SSLSocket ssl_socket;
            ssl_socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(serverUrl, 1501);
            // enable certifcate validation:
            SSLParameters sslParams = new SSLParameters();
            sslParams.setEndpointIdentificationAlgorithm("HTTPS");
            ssl_socket.setSSLParameters(sslParams);

            if (useCompression) {
                initiation_command += " compression gzip";
            }
            
            initiation_command += " filter \"DAL\"";
            initiation_command += "\n";

            //  initiate connection with flight aware server
            OutputStreamWriter writer = new OutputStreamWriter(ssl_socket.getOutputStream(), "UTF8");
            writer.write(initiation_command);
            writer.flush();

            InputStream inputStream = ssl_socket.getInputStream();
            if (useCompression) {
                inputStream = new java.util.zip.GZIPInputStream(inputStream);
            }

            // read messages from FlightAware
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String message = null;
//            int limit = 1000; //limit number messages for testing
           	while (started && (message = reader.readLine()) != null) {
                //parse message with gson
                try {
					FlightObject flight = gson.fromJson(message, FlightObject.class);
					boolean match = false;
					for(String msg: messageTypes) {
						if(flight.type.equals(msg)) {
							match = true;
							break;
						}
					}
					if(!match)  continue;
//					if(flight.status == null)  continue;  // probably don't want this
//					System.err.println(message);
					
					for(FlightObjectListener l: listeners) {
						l.processMessage(flight, message);
					}
//                System.out.println(flight);
				} catch (JsonSyntaxException e) {
					e.printStackTrace();   
				}
            }

            //done, close everything
            writer.close();
            reader.close();
            inputStream.close();
            ssl_socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }    
    
    public void stop() {
        started = false;
    }
}