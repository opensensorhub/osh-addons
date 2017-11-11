package org.sensorhub.impl.sensor.flightAware;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class FlightAwareClient implements FlightObjectListener, Runnable 
{
	private String serverUrl;
	String userName = "drgregswilson";
	String password = "2809b6196a2cfafeb89db0a00b117ac67e876220";
	private SSLSocket ssl_socket = null;  // Class var or thread var here?
	private static final boolean useCompression = false;
	List<FlightObjectListener> listeners = new ArrayList<>();
	List<String> messageTypes = new ArrayList<>();
	volatile boolean started;
	long lastMessageTime = 0L;

	private static FlightAwareClient instance;
	//  Warn us if messages start stacking up- note sys clock dependence
	private static final long MESSAGE_TIMER_CHECK_PERIOD = 120;
	private static final long MESSAGE_LATENCY_WARN_LIMIT = 120; //TimeUnit.MINUTES.toSeconds(1);
	private static final long MESSAGE_LATENCY_RESTART_LIMIT = 180; // TimeUnit.MINUTES.toSeconds(2);  // experiment with these
	private MessageTimeThread messageTimeThread;

	static final Logger log = LoggerFactory.getLogger(FlightAwareClient.class);

	private FlightAwareClient(){}

	// Ask Alex why he wanted to use Singleton
	public static synchronized FlightAwareClient getInstance(){
		if(instance == null){
			instance = new FlightAwareClient();
		}
		return instance;
	}

	public FlightAwareClient(String serverUrl, String uname, String pwd) {
		this.serverUrl = serverUrl;
		this.userName = uname;
		this.password = pwd;

		messageTimeThread = new MessageTimeThread(this);
		startMessageTimeThread();
	}    

	public static void main(String[] args) {
		String machineName = "firehose.flightaware.com";
		String userName = "drgregswilson";
		String password = "2809b6196a2cfafeb89db0a00b117ac67e876220";
		FlightAwareClient client = new FlightAwareClient(machineName, userName, password);
		client.messageTypes.add("flightplan position");
		client.addListener(client);
		Thread thread = new Thread(client);
		thread.start();
	}

	public void addListener(FlightObjectListener l) {
		listeners.add(l);
	}

	public boolean removeListener(FlightObjectListener l) {
		return listeners.remove(l);
	}

	public void startMessageTimeThread() {
//		Timer timer = new Timer(true);
//		timer.scheduleAtFixedRate(messageTimeThread, 10000L, MESSAGE_TIMER_CHECK_PERIOD );
		ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(5);
		scheduledThreadPool.scheduleWithFixedDelay(messageTimeThread, 10, MESSAGE_TIMER_CHECK_PERIOD, TimeUnit.SECONDS);

	}

	private void initiateConnection() {
		
	}
	
	@Override
	public void run() {
		log.debug("**Starting FlightAware Client Thread {}", this);
		started = true;
		OutputStreamWriter writer = null;
		InputStream inputStream = null;
		BufferedReader reader = null;
		try {
			//            SSLSocket ssl_socket;
			ssl_socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(serverUrl, 1501);
			// enable certifcate validation:
			SSLParameters sslParams = new SSLParameters();
			sslParams.setEndpointIdentificationAlgorithm("HTTPS");
			ssl_socket.setSSLParameters(sslParams);
			String initiation_command = "live username " + userName + " password " + password;

			if (useCompression) {
				initiation_command += " compression gzip";
			}

			initiation_command += " filter \"DAL\"";
			initiation_command += "\n";

			//  initiate connection with flight aware server
			writer = new OutputStreamWriter(ssl_socket.getOutputStream(), "UTF8");
			writer.write(initiation_command);
			writer.flush();

			inputStream = ssl_socket.getInputStream();
			if (useCompression) {
				inputStream = new java.util.zip.GZIPInputStream(inputStream);
			}

			// read messages from FlightAware
			reader = new BufferedReader(new InputStreamReader(inputStream));
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			String message = null;
			int cnt = 0;
			while (started && (message = reader.readLine()) != null) {
				try {
//					System.err.println(message);
					FlightObject flight = gson.fromJson(message, FlightObject.class);
					lastMessageTime = Long.parseLong(flight.pitr);
					if(messageTypes.contains(flight.type)) {
						for(FlightObjectListener l: listeners) {
							l.processMessage(flight);
						}
					}
					// simulate message timeout
//					if(cnt++ > 10) {
//						Thread.sleep(150_000L);
//					}
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
				
				//  simulate connection closed by peer
//				if(++cnt%40  == 0 ) {
//					throw new SSLException("Test closed by peer");
//				}
			}
			log.debug("***Stopping FlightAware Client Thread");
		} catch (IOException e) {
			e.printStackTrace();
			
		} finally {
			try {
				//  close streams
				if(writer != null)  writer.close();
				if(reader != null)  reader.close();
				if(inputStream != null)  inputStream.close();
				// close socket
				if(ssl_socket != null && !ssl_socket.isClosed())  ssl_socket.close();
				log.debug("***Socket close called");
			} catch (IOException e) {
				// if this happens, we are in an unknown state, so need to rethink 
				e.printStackTrace();
			}

		}
	}    

	public void stop() {
		started = false;
	}

	public void restartThread() {
		log.debug("***Call restart");
		stop();

		//  Force close from here
		try {
			if(!ssl_socket.isClosed())
				ssl_socket.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		//  wait until ssl_socket is closed before restarting
		while(!ssl_socket.isClosed()) {
			try {
				Thread.sleep(5000L);
				log.debug("Socket still not closed. Waiting ..");
			} catch (InterruptedException e) {
				log.debug(e.getMessage());
				continue;
			}
		}
		
		// Start firehose feed
		Thread thread = new Thread(this);
		thread.start();
	}

//	class MessageTimeThread extends TimerTask {
	class MessageTimeThread implements Runnable {
		FlightAwareClient client;
		boolean restarting = false;
		
		public MessageTimeThread(FlightAwareClient client) {
			this.client = client;
		}

		@Override
		public void run() {
			long sysTime = System.currentTimeMillis() / 1000;
			log.error("MessageTimeThread.run() entered");
			if(sysTime - lastMessageTime > MESSAGE_LATENCY_RESTART_LIMIT) {
				client.restartThread();
			} else 	if(sysTime - lastMessageTime > MESSAGE_LATENCY_WARN_LIMIT) {
				log.error("Messages getting old: deltaTime seconds = {}", sysTime - lastMessageTime);
			}
			log.error("MessageTimeThread.run() exit");
		}
	}

	@Override
	public void processMessage(FlightObject obj) {
		//		if(obj.id.startsWith("DAL915"))
		System.err.println(obj);
	}

	public boolean isStarted() {
		return started;
	}

	public long getLastMessageTime() {
		return lastMessageTime;
	}
}