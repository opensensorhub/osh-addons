package uk.co.envsys.sensorhub.sensor.httpweather;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;

public class HttpWeatherModuleDescriptor implements IModuleProvider {

	@Override
	public String getModuleName() {
		return "HttpWeather sensor";
	}

	@Override
	public String getModuleDescription() {
		return "Runs a web server to take weather measurements from";
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
		return HttpWeatherSensor.class;
	}

	@Override
	public Class<? extends ModuleConfig> getModuleConfigClass() {
		return HttpWeatherConfig.class;
	}

}
