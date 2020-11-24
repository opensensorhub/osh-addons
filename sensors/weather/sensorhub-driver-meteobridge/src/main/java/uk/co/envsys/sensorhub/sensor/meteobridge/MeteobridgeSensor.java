package uk.co.envsys.sensorhub.sensor.meteobridge;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;


public class MeteobridgeSensor extends AbstractSensorModule<MeteobridgeConfig>{
	private static final String UID_PREFIX = "urn:envsys:sensors:meteobridge:";
	
	private boolean connected;
	private MeteobridgeOutput dataInterface;
	
	
	public MeteobridgeSensor() {}	// blank constructor
	
	public void init() throws SensorHubException {
		super.init();
		
		dataInterface = new MeteobridgeOutput(this);
		addOutput(dataInterface, false);
		dataInterface.init();
		connected = false;
	}
	
	@Override
    public void updateConfig(MeteobridgeConfig config) throws SensorHubException {
        super.updateConfig(config);
        dataInterface.updateConfig(config);
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
