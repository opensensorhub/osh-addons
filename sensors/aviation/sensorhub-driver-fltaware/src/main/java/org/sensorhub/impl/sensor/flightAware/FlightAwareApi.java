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

import java.io.IOException;
import java.nio.charset.Charset;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.sensorhub.impl.sensor.flightAware.DecodeFlightRouteResponse.DecodeFlightRouteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;


public class FlightAwareApi
{  
    static final Logger log = LoggerFactory.getLogger(FlightAwareApi.class);
    
    private final static String BASE_URL = "https://flightxml.flightaware.com/json/FlightXML2/";
	private final static String METAR_URL = BASE_URL + "MetarEx?airport=KAUS&startTime=0&howMany=1&offset=0";
	public final static String InFlightInfo_URL = BASE_URL + "InFlightInfo?";
	private final static String DecodeFlightRoute_URL = BASE_URL + "DecodeFlightRoute?"; // faFlightID=DAL1323-1506576332-airline-0231";
	private final static String GetFlightID_URL = BASE_URL + "GetFlightID?ident=SWA1878&departureTime=1506101700";
	private final static String Enroute_URL = BASE_URL + "Enroute?";
	private final static String Scheduled_URL = BASE_URL + "Scheduled?airport=KAUS&howMany=10&filter=airline";
	private final static String AirlineInfo_URL = BASE_URL + "AirlineInfo?airlineCode=DAL";
	private final static String FlightInfoEx_URL = BASE_URL + "FlightInfoEx?";
	
	String user;
    String passwd;
    
    
	public FlightAwareApi(String user, String passwd) {
		this.user = user;
		this.passwd = passwd;
	}

	public static String toJson(Object object) {
		Gson gson = new  GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(object);
		return json;
	}

	public String invokeNew(String url, String ... args) throws ClientProtocolException, IOException {
		for(String arg: args) {
			url = url + arg + "&";
		}
		if(url.endsWith("&"))
			url = url.substring(0, url.length() - 1);
//		System.err.println(url);
		HttpGet request = new HttpGet(url);
		String auth = user + ":" + passwd;
		byte [] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("ISO-8859-1")));
		String authHeader = "Basic " + new String(encodedAuth);
		request.setHeader(HttpHeaders.AUTHORIZATION, authHeader);

		try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
		    HttpResponse response = client.execute(request);
		    int statusCode = response.getStatusLine().getStatusCode();
    		if(statusCode >= 400) {
    			// doSomething
    		}
    		String resp = EntityUtils.toString(response.getEntity());
    //		System.err.println(statusCode);
    //		System.err.println(resp);
    		return resp;
		}
	}

	public void printJson(String json) {
		JsonParser parser = new JsonParser();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		JsonElement el = parser.parse(json);
		json = gson.toJson(el); // done
		System.err.println(json);
	}

	public static FlightAwareResult fromJson(String json, Class<? extends FlightAwareResult> clazz) {
		Gson gson = new Gson();
		FlightAwareResult info = gson.fromJson(json, clazz);
		return info;
	}
	
	/**
	 * 
	 * @param id - faFlightId used to retrieve plan
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public DecodeFlightRouteResult decodeFlightRoute(String id) throws ClientProtocolException, IOException {
		String json = invokeNew(DecodeFlightRoute_URL + "faFlightID=" + id);
		
//		System.err.println(DecodeFlightRoute_URL + "faFlightID=" + id);
		if(json.contains("error")) {
			log.debug("Error in decodeFlightRoute() for flight {}: {}", id, json);
			return null;
		}
		
		return ((DecodeFlightRouteResponse)fromJson(json, DecodeFlightRouteResponse.class)).DecodeFlightRouteResult;
	}

}
