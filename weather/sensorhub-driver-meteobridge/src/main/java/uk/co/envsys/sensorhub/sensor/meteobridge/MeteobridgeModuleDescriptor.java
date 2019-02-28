package uk.co.envsys.sensorhub.sensor.meteobridge;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;

public class MeteobridgeModuleDescriptor implements IModuleProvider 	{

	@Override
	public String getModuleName() {
		return "Meteobridge Sensor";
	}

	@Override
	public String getModuleDescription() {
		return "Weather measurements from a Meteobridge connected station";
	}

	@Override
	public String getModuleVersion() {
		return "0.1";
	}

	@Override
	public String getProviderName() {
		return "Environment Systems Ltd";
	}

	@Override
	public Class<? extends IModule<?>> getModuleClass() {
		return MeteobridgeSensor.class;
	}

	@Override
	public Class<? extends ModuleConfig> getModuleConfigClass() {
		return MeteobridgeConfig.class;
	}

}
