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
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class FlightAwareApi
{  
    static final Logger log = LoggerFactory.getLogger(FlightAwareApi.class);
    
    private final static String BASE_URL = "http://flightxml.flightaware.com/json/FlightXML2/";
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
		String json =  gson.toJson(object);
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
	public FlightPlan getFlightPlan(String id) throws ClientProtocolException, IOException {
		String json = invokeNew(DecodeFlightRoute_URL + "faFlightID=" + id);
//		System.err.println(DecodeFlightRoute_URL + "faFlightID=" + id);
		if(json.contains("error")) {
			log.debug("FlightPlan.getFlightPlan(): Whoops for faFlightId  {} : {}", id,json);
			return null;
		}
		DecodeFlightResult decodedInfo = (DecodeFlightResult)fromJson(json, DecodeFlightResult.class);
		
		FlightPlan plan = new FlightPlan(decodedInfo);
		plan.faFlightId = id;
		int dashIdx = id.indexOf('-'); 
		if(dashIdx == -1) {
			log.debug("FltawareApi.getFltPlan(): Don't understand faFlightId: {} ", id);
		}
		String ident = id.substring(0, dashIdx);
		plan.oshFlightId = ident + "_" + plan.destinationAirport;
		plan.flightNumber = ident;
//		plan.time = comes from firehose only
		return plan;
	}

	// ident is tail number
	//  @TODO
	public FlightPlan getFlightPosition(String ident) throws ClientProtocolException, IOException {
		String json = invokeNew(InFlightInfo_URL+ "ident=" + ident);
//		System.err.println(DecodeFlightRoute_URL + "faFlightID=" + id);
		if(json.contains("error")) {
			log.debug("FlightPlan.getFlightPosition(): Whoops for flight ident{} : {}", ident,json);
			return null;
		}
//		DecodeFlightResult decodedInfo = (DecodeFlightResult)fromJson(json, DecodeFlightResult.class);
//		
//		FlightPosition pos = new FlightPosition();
//		//pos.ident = id;
//		int dashIdx = id.indexOf('-'); 
//		if(dashIdx == -1) {
//			log.debug("FltawareApi.getFltPlan(): Don't understand faFlightId: {} ", id);
//		}
//		String ident = id.substring(0, dashIdx);
//		plan.oshFlightId = ident + "_" + plan.destinationAirport;
////		plan.time = comes from firehose only
//		return plan;
		return null;
	}

	
	public static void main(String[] args) throws Exception {
		log.warn("Logging Works");
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");

		System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
		System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "debug");
		System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "debug");

		FlightAwareApi api = new FlightAwareApi("drgregswilson", "2809b6196a2cfafeb89db0a00b117ac67e876220");
		
		String json;
//		json = api.invokeNew(InFlightInfo_URL, "ident=DAL1174");
//		System.err.println(json);
//		InFlightInfo info = (InFlightInfo) api.fromJson(json, InFlightInfo.class);
//		Instant depTime = Instant.ofEpochSecond(info.InFlightInfoResult.departureTime);
//		System.err.println(info.InFlightInfoResult.ident + " departed from: " + info.InFlightInfoResult.destination + " at " + depTime);
		
		json = api.invokeNew(InFlightInfo_URL, "ident=DAL129");
		System.err.println(json);
		InFlightInfo info = (InFlightInfo) fromJson(json, InFlightInfo.class);
//		System.err.println(FlightInfoEx_URL + "ident=DAL1260");
		System.err.println(info.InFlightInfoResult.destination);

		
//		json = api.invokeNew(InFlightInfo_URL, "ident=DAL1323");
//		InFlightInfo info = (InFlightInfo) api.fromJson(json, InFlightInfo.class);
//		Instant depTime = Instant.ofEpochSecond(info.InFlightInfoResult.departureTime);
//		System.err.println(info.InFlightInfoResult.ident + " departed from: " + info.InFlightInfoResult.destination + " at " + depTime);
//		api.printJson(json);
//				List<Waypoint> waypoints = info.createWaypoints();
//				for(Waypoint p: waypoints) 
//					System.err.println(p);
	}

}
