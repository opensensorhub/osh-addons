package uk.co.envsys.sensorhub.sensor.httpweather;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class WeatherWebServer extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private HttpWeatherSensor parentSensor;
	
	public WeatherWebServer(HttpWeatherSensor parent) {
		super();
		this.parentSensor = parent;
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		double inTemp, outTemp, outHum, windSpeed, avgWindSpeed, windDir, rain, 
			pressure, seaPressure, uvIndex, solarRadiation, heatIndex, dewPoint, windChill;
		String sunrise, sunset;
	
		inTemp = outTemp = outHum = windSpeed = avgWindSpeed = windDir = rain =
			pressure = seaPressure = uvIndex = solarRadiation = heatIndex = dewPoint = windChill = -99999;
		sunrise = sunset = "";
		
		if(request.getParameter("intemp") != null) {
			inTemp = Double.valueOf(request.getParameter("intemp"));
		}
		if(request.getParameter("outtemp") != null) {
			outTemp = Double.valueOf(request.getParameter("outtemp"));
		}
		if(request.getParameter("outhum") != null) {
			outHum = Double.valueOf(request.getParameter("outhum"));
		}
		if(request.getParameter("windspeed") != null) {
			windSpeed = Double.valueOf(request.getParameter("windspeed"));
		}
		if(request.getParameter("avgwindspeed") != null) {
			avgWindSpeed = Double.valueOf(request.getParameter("avgwindspeed"));
		}
		if(request.getParameter("winddir") != null) {
			windDir = Double.valueOf(request.getParameter("winddir"));
		}
		if(request.getParameter("rain") != null) {
			rain = Double.valueOf(request.getParameter("rain"));
		}
		if(request.getParameter("sunrise") != null) {
			sunrise = request.getParameter("sunrise");
		}
		if(request.getParameter("sunset") != null) {
			sunset = request.getParameter("sunset");
		}
		if(request.getParameter("pressure") != null) {
			pressure = Double.valueOf(request.getParameter("pressure"));
		}
		if(request.getParameter("seapressure") != null) {
			seaPressure = Double.valueOf(request.getParameter("seapressure"));
		}
		if(request.getParameter("uvindex") != null) {
			uvIndex = Double.valueOf(request.getParameter("uvindex"));
		}
		if(request.getParameter("solarradiation") != null) {
			 solarRadiation = Double.valueOf(request.getParameter("solarradiation"));
		}
		if(request.getParameter("heatindex") != null) {
			heatIndex = Double.valueOf(request.getParameter("heatindex"));
		}
		if(request.getParameter("dewpoint") != null) {
			dewPoint = Double.valueOf(request.getParameter("dewpoint"));
		}
		if(request.getParameter("windchill") != null) {
			windChill = Double.valueOf(request.getParameter("windchill"));
		}
		
		parentSensor.getOutput().sendMeasurement(inTemp, outTemp, outHum, windSpeed, avgWindSpeed, 
				windDir, rain, sunrise, sunset, pressure, seaPressure, uvIndex, solarRadiation, heatIndex, dewPoint, windChill);
		
		response.setContentType("text/html");
		response.getWriter().println("OK");
	}
}
