package org.sensorhub.impl.sensor.twitter;

import java.util.ArrayList;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.Required;
import org.sensorhub.api.sensor.SensorConfig;

import com.google.common.collect.Lists;

/**
 * <p>
 * Implementation of Twitter streaming API. This particular class stores 
 * configuration parameters.
 * </p>
 * 
 * @author Joshua Wolfe <developer.wolfe@gmail.com>
 * @since March 24, 2017
 */
public class TwitterConfig extends SensorConfig
{
	@Required
	@DisplayInfo(desc="Uniquely generated string from http://apps.twitter.com for a single application registered with Twitter.")
	public String apiKey = null;

	@Required
	@DisplayInfo(desc="Uniquely generated string that is linked to the apiKey. Keep the \"API Secret\" a secret. This key should never be human-readable in your application.")
	public String apiSecret = null;

	@Required
	@DisplayInfo(desc="Uniquely generated string from http://apps.twitter.com for a single Twitter user.")
	public String accessToken = null;

	@Required
	@DisplayInfo(desc="Uniquely generated string that is linked to the accessToken. This access token can be used to make API requests on your own account's behalf. Do not share your access token secret with anyone.")
	public String accessTokenSecret = null;
	
	@DisplayInfo(desc="Unique ID for sensor")
	public String streamID = null;
	
	@DisplayInfo(desc="List of keywords and terms to retrieve from Twitter's stream of tweets.")
	public ArrayList<String> keywords = Lists.newArrayList();
	
	@DisplayInfo(desc="This is a desc")
	public Double SouthwestLongitude = null;
	
	@DisplayInfo(desc="This is a desc")
	public Double SouthwestLatitude = null;
	
	@DisplayInfo(desc="This is a desc")
	public Double NortheastLongitude = null;
	
	@DisplayInfo(desc="This is a desc")
	public Double NortheastLatitude = null;
}
