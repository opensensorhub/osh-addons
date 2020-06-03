package org.sensorhub.impl.sensor.intellisense;

import java.io.IOException;
import java.util.TimerTask;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;

import com.google.gson.Gson;

public class FloodPoller extends TimerTask {
	IntellisenseConfig config;
	IntellisenseOutput output;
	String token;
	Logger logger; // = getLogger();
	
	public FloodPoller(IntellisenseConfig config, IntellisenseOutput output, Logger logger) {
		super();
		this.config = config;
		this.output = output;
		this.logger = logger;
	}

	@Override
	public void run() {
//		System.err.println("Running at " + System.currentTimeMillis() + ": " + config.reportingMode);
		try {
			if(token == null) {
				token = getAuthToken();
				System.err.println("New token is " + token);
			}

			for(String id: config.deviceIds) {
				FloodRecord record = getData(token, id);
				output.addRecord(record);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.debug("Requesting new token...");
			try {
				token = getAuthToken();
				logger.debug("New token is " + token);
			}
			catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				System.exit(-1);
			}
		}
	}

	public FloodRecord getData(String token, String deviceId) throws ClientProtocolException, IOException {
		try(CloseableHttpClient client = HttpClients.createDefault()) {
			String getStr = config.apiUrl + "/" + config.obsPath.replaceFirst("DEVICE_ID", deviceId) + config.getKeyString() ;

			HttpGet httpGet = new HttpGet(getStr);

			httpGet.setHeader("Content-Type", "application/json");
			httpGet.setHeader("X-Authorization", "Bearer " + token);

			CloseableHttpResponse response = client.execute(httpGet);
			if(response.getStatusLine().getStatusCode() >= 400) {
				throw new IOException("Status Code: " + response.getStatusLine().getStatusCode());
			}
			String resp = EntityUtils.toString(response.getEntity());
//			System.err.println(resp);
			Gson gson = new Gson();
			FloodRecord rec = gson.fromJson(resp, FloodRecord.class);
			rec.deviceId = deviceId;
			// Take timestamp from FloodIndicator timestamp, as the value in the actual time property in the json is not correct
			rec.timeMs = rec.ffi1[0].ts;     
			return rec;
		} 
	}

	public String getAuthToken() throws ClientProtocolException, IOException {
		try(CloseableHttpClient client = HttpClients.createDefault()) {
			HttpPost post = new HttpPost(config.apiUrl + "/" + config.authPath);
			StringBuilder b = new StringBuilder();
			b.append("{");
			b.append("\"username\" : \"" + config.username + "\",");
			b.append("\"password\" : \"" + config.password + "\"");
			b.append("}");
			StringEntity entity = new StringEntity(b.toString());
			post.setEntity(entity);
			CloseableHttpResponse response = client.execute(post);
			if(response.getStatusLine().getStatusCode() >= 400) {
				logger.debug("HttpRequest failed with reason: {}" , response.getStatusLine().getReasonPhrase());
				throw new IOException("Status Code: " + response.getStatusLine().getStatusCode());
			}
			String resp = EntityUtils.toString(response.getEntity());
			Gson gson = new Gson();
			Token token = gson.fromJson(resp, Token.class);
			return token.token;
		}
	}

	class Token {
		String token;
		String refreshToken;
	}

}
