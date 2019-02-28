package uk.co.envsys.sensorhub.sensor.httpweather;

import java.util.UUID;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.service.HttpServer;
import org.sensorhub.impl.service.HttpServerConfig;
import org.vast.ogc.om.SamplingPoint;
import org.vast.swe.SWEConstants;

import net.opengis.gml.v32.Point;
import net.opengis.gml.v32.impl.GMLFactory;

public class HttpWeatherSensor extends AbstractSensorModule<HttpWeatherConfig> {
	private static final String UID_PREFIX = "urn:envsys:sensors:httpweather:";
	private static final String SENSOR_DESCRIPTION = "Provides HTTP endpoint on which it can receive weather data";
	private boolean serverRunning;
	private HttpWeatherOutput dataInterface;
	private WeatherWebServer server;
	private SamplingPoint foi;
	
	public HttpWeatherSensor() {}
	
	public void init() throws SensorHubException {
		super.init();
		dataInterface = new HttpWeatherOutput(this);
		addOutput(dataInterface, false);
		dataInterface.init();
		generateFoi();
		server = new WeatherWebServer(this);
		serverRunning = false;
	}
	
	protected void generateFoi() {
        // create FoI
        GMLFactory gml = new GMLFactory();
        foi = new SamplingPoint();
        foi.setUniqueIdentifier(UID_PREFIX + config.urlBase + ":foi");
        foi.setName("Weather Station Location");
        Point p = gml.newPoint();
        p.setSrsName(SWEConstants.REF_FRAME_4979);
        p.setPos(new double[] {config.centerLatitude, config.centerLongitude, config.centerAltitude});
        foi.setShape(p);
    }
	
	public HttpWeatherOutput getOutput() {
		return dataInterface;
	}
	
	@Override
    public void updateConfig(HttpWeatherConfig config) throws SensorHubException {
        super.updateConfig(config);
        dataInterface.updateConfig();
        generateFoi();
    }
	
	@Override
    protected void updateSensorDescription() {
        synchronized (sensorDescLock) {
            super.updateSensorDescription();
            sensorDescription.setId("HTTP_WEATHER");
            sensorDescription.setUniqueIdentifier(UID_PREFIX + UUID.randomUUID());
            sensorDescription.setDescription(SENSOR_DESCRIPTION);
        }
    }
	
	@Override
	public boolean isConnected() {
		return serverRunning;
	}

	@Override
	public void start() throws SensorHubException {
		HttpServer instance = HttpServer.getInstance();
		if(instance == null) {
			// NO RUNNING SERVER FOUND - probably running unit tests... start one?
			instance = new HttpServer();
			instance.init(new HttpServerConfig());
			instance.start();
		}
		
		instance.deployServlet(server, "/httpweather/" + config.urlBase + "/*");
		serverRunning = true;
	}

	@Override
	public void stop() throws SensorHubException {
		HttpServer.getInstance().undeployServlet(server);
		serverRunning = false;
	}

	@Override
	public void cleanup() throws SensorHubException {
		HttpServer.getInstance().undeployServlet(server);		
	}

}
