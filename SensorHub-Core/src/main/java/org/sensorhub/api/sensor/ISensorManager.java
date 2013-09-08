package org.sensorhub.api.sensor;

import java.util.List;
import org.sensorhub.api.module.IModuleManager;
import org.sensorhub.api.module.IModuleProvider;


/**
 * <p><b>Title:</b>
 * ISensorManager
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Management interface for sensors connected to the system
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 5, 2010
 */
public interface ISensorManager extends IModuleManager<ISensorInterface<?>>
{ 
    
    /**
     * Helper method to find a sensor by its global UID
     * @param uid
     * @return
     */
    public ISensorInterface<?> findSensor(String uid);
    
    
	/**
     * Helper method to get the list of connected sensors only
     * @return the list of sensors actually connected to the system
     */
    public List<ISensorInterface<?>> getConnectedSensors();
    
    
    /**
	 * Installs a driver package (jar file) from the specified URL
	 * @param driverPackageuURL URL of jar containing implementation of new driver
	 * @return automatically assigned driver ID
	 */
	public String installDriver(String driverPackageURL, boolean replace);
	
	
	/**
	 * Uninstalls the driver with the specified ID
	 * @param driverID
	 */
	public void uninstallDriver(String driverID);
	
	
	/**
	 * @return the list of all sensor drivers installed on the system
	 */
	public List<IModuleProvider> getInstalledSensorDrivers();	

}
