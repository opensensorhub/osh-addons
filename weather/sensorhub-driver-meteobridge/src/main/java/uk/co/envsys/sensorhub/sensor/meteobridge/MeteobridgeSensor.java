package uk.co.envsys.sensorhub.sensor.meteobridge;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.vast.ogc.om.SamplingPoint;
import org.vast.swe.SWEConstants;

import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.Point;
import net.opengis.gml.v32.impl.GMLFactory;

public class MeteobridgeSensor extends AbstractSensorModule<MeteobridgeConfig>{
	private static final String UID_PREFIX = "urn:envsys:sensors:meteobridge:";
	
	private boolean connected;
	private MeteobridgeOutput dataInterface;
	private SamplingPoint foi;
	
	
	public MeteobridgeSensor() {}	// blank constructor
	
	public void init() throws SensorHubException {
		super.init();
		
		dataInterface = new MeteobridgeOutput(this);
		addOutput(dataInterface, false);
		dataInterface.init();
		connected = false;
		
		generateFoi();
	}
	
	protected void generateFoi() {
        // create FoI
        GMLFactory gml = new GMLFactory();
        foi = new SamplingPoint();
        foi.setUniqueIdentifier(UID_PREFIX + config.serialNumber + ":foi");
        foi.setName("Weather Station Location");
        Point p = gml.newPoint();
        p.setSrsName(SWEConstants.REF_FRAME_4979);
        p.setPos(new double[] {config.centerLatitude, config.centerLongitude, config.centerAltitude});
        foi.setShape(p);
    }
	
	@Override
    public void updateConfig(MeteobridgeConfig config) throws SensorHubException {
        super.updateConfig(config);
        dataInterface.updateConfig(config);
        generateFoi();
    }
	
	@Override
    public AbstractFeature getCurrentFeatureOfInterest() {
        return foi;
    }
	
	@Override
    protected void updateSensorDescription() {
        synchronized (sensorDescLock) {
            super.updateSensorDescription();
            sensorDescription.setId("METEOBRIDGE_WEATHER_STATION");
            sensorDescription.setUniqueIdentifier(UID_PREFIX + config.serialNumber);
            sensorDescription.setDescription("Weather station connected to a Meteobridge device");
        }
    }

	@Override
	public boolean isConnected() {
		return connected;
	}

	@Override
	public void start() throws SensorHubException {
		connected = true;
		dataInterface.start();
	}

	@Override
	public void stop() throws SensorHubException {

		connected = false;
		dataInterface.stop();
	}

	@Override
	public void cleanup() throws SensorHubException {
	}

}
