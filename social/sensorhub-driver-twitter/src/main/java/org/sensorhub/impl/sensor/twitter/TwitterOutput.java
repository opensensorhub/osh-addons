package org.sensorhub.impl.sensor.twitter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.Location;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.data.DataRecordImpl;
import org.vast.swe.SWEConstants;
import org.vast.swe.helper.GeoPosHelper;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.Vector;

/**
 * <p>
 * Implementation of sensor interface for USGS Water Data using IP
 * protocol. This particular class stores configuration parameters.
 * </p>
 * 
 * @author Joshua Wolfe <developer.wolfe@gmail.com>
 * @since March 24, 2017
 */
public class TwitterOutput extends AbstractSensorOutput<TwitterSensor> 
{
	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

	DataRecord twitterData;
	DataEncoding twitterEncoding;
	
	Boolean started;
	Client client;
	TweetsModel tweetData;
	BlockingQueue<String> queue;
	
	public TwitterOutput(TwitterSensor parentSensor) 
	{
		super(parentSensor);
	}

	protected void init()
	{
	    // Create a queue for processing
		queue = new LinkedBlockingQueue<String>(10000);

		GeoPosHelper fac = new GeoPosHelper(); 

		// Build SWE Common record structure
		twitterData = new DataRecordImpl();
	    twitterData.setName(getName());
	    twitterData.setDefinition("http://sensorml.com/ont/swe/property/Event");
		
		// Add timestamp, location, text, and user
	    twitterData.addComponent("time", fac.newTimeStampIsoUTC());

        Vector locVector = fac.newLocationVectorLatLon(SWEConstants.DEF_SENSOR_LOC);
        locVector.setLabel("Location");
        locVector.setDescription("Lat/Long vector of a Tweet");
        twitterData.addComponent("location", locVector);
        
        twitterData.addComponent("text", fac.newText(GeoPosHelper.getPropertyUri("Text"), "Text", "The text content of a Tweet"));
        twitterData.addComponent("user", fac.newText(GeoPosHelper.getPropertyUri("User"), "Uesr", "The user who pulbished a Tweet"));

	    // Generate encoding definition
		twitterEncoding = fac.newTextEncoding(",", "\n");
	}
	
	protected void start(TwitterConfig config) throws InterruptedException 
	{
		// Authenticate with api
		String apiKey = config.apiKey.replace(" ", ""); 
		String apiSecret = config.apiSecret.replace(" ", ""); 
		String accessToken = config.accessToken.replace(" ", ""); 
		String accessTokenSecret = config.accessTokenSecret.replace(" ", ""); 

	    Authentication auth = new OAuth1(apiKey, apiSecret, accessToken, accessTokenSecret);
	    // Authentication auth = new BasicAuth(username, password);
	    
	    // Apply specified filters
	    StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
		
		if (config.NortheastLongitude != null && config.NortheastLatitude != null &&
				 config.SouthwestLongitude != null && config.SouthwestLatitude!= null)
		{
			Location.Coordinate southwest = new Location.Coordinate(config.SouthwestLongitude, config.SouthwestLatitude);
			Location.Coordinate northeast = new Location.Coordinate(config.NortheastLongitude, config.NortheastLatitude);
			Location location = new Location(southwest, northeast);

			ArrayList<Location> locs = new ArrayList<Location>();
			locs.add(location);

			endpoint.locations(locs);
		} 
		else if (config.keywords.size() > 0)
		{
			endpoint.trackTerms(config.keywords);
		} 
		else
		{
			return;
		}

	    // Create a new BasicClient. By default gzip is enabled.
	    client = new ClientBuilder()
	            .hosts(Constants.STREAM_HOST)
	            .endpoint(endpoint)
	            .authentication(auth)
	            .processor(new StringDelimitedProcessor(queue))
	            .build();

	    // Establish a connection
	    client.connect();

	    Thread t = new Thread(new Runnable()
        {
            public void run()
            {
                while (started)
                {
					if(queue.isEmpty())
						continue;

					try {
						publishData(queue.take());
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                }
                client.stop();
				client = null;
                queue.clear();
            }
        });
	    
	    started = true;
	    t.start();
	}
	
	private void publishData(String tweet)
	{
		//Gson gson = new Gson();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		tweetData = gson.fromJson(tweet, TweetsModel.class);
		//System.out.println(gson.toJson(tweetData));
		
		// Edge case when there is no tweet or it was deleted after hitting the stream (i guess)
		if (tweetData.id == 0)
			return;
		
        double time = System.currentTimeMillis() / 1000.0;
        double lon = tweetData.coordinates != null ? tweetData.coordinates.coordinates[0] : 0.0;
        double lat = tweetData.coordinates != null ? tweetData.coordinates.coordinates[1] : 0.0;

		//DataBlock dataBlock = latestRecord == null ? twitterData.createDataBlock() : latestRecord.renew();
		DataBlock dataBlock = twitterData.createDataBlock();
		dataBlock.setDoubleValue(0, time);
		dataBlock.setDoubleValue(1, lat);
		dataBlock.setDoubleValue(2, lon);
		dataBlock.setStringValue(3, tweetData.text);
		dataBlock.setStringValue(4, tweetData.user.id_str);
		
		latestRecord = dataBlock;
		latestRecordTime = System.currentTimeMillis();
		eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, TwitterOutput.this, dataBlock));
	}

	protected void stop() 
	{
		started = false;

		if (client == null)
			return;

		client.stop();
		client = null;
	}

	public boolean isConnected() {
		return client != null ? true : false;
	}

	@Override
	public String getName() {
		return "Twitter_Output";
	}

	@Override
	public DataComponent getRecordDescription() 
	{
		return twitterData;
	}

	@Override
	public DataEncoding getRecommendedEncoding() 
	{
		return twitterEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() 
	{
		return 0.5;
	}
}
