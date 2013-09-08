/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.sensor;

import java.util.Map;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.IEventProducer;
import org.sensorhub.api.module.IModule;
import org.vast.sensorML.system.SMLSystem;
import org.vast.util.DateTime;


/**
 * <p><b>Title:</b>
 * ISensorInterface
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Interface to be implemented by all sensor drivers connected to the system
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 5, 2010
 */
public interface ISensorInterface<ConfigType extends SensorConfig> extends IModule<ConfigType>, IEventProducer
{
    /**
     * Checks sensor description update capability
     * @return true if sensor description updates is supported, false otherwise
     */
    public boolean isSensorDescriptionUpdateSupported();
    
    
    /**
     * Checks sensor description history capability
     * @return true if sensor description history is supported, false otherwise
     */
    public boolean isSensorDescriptionHistorySupported();
    
    
    /**
	 * Retrieves most current sensor description
	 * @return SMLSytem object containing all metadata about the sensor
	 */
	public SMLSystem getCurrentSensorDescription() throws SensorException;
	
	
	/**
	 * Retrieves historic sensor description valid at time t
	 * @param t
	 * @return SMLSytem object containing sensor metadata valid at time t
	 */
	public SMLSystem getSensorDescription(DateTime t) throws SensorException;
	
	
	/**
	 * Updates and historizes system description
	 * @param systemDesc SMLSystem object with validity period
	 */
	public void updateSensorDescription(SMLSystem systemDesc, boolean recordHistory) throws SensorException;
	
	
	/**
	 * Retrieves the list of interfaces to all sensor data outputs
	 * @return
	 */
	public Map<String, ISensorDataInterface> getAllOutputs() throws SensorException;
	
	
	/**
	 * Retrieves the list of interface to sensor status outputs
	 * @return
	 */
	public Map<String, ISensorDataInterface> getStatusOutputs() throws SensorException;
	
	
	/**
	 * Retrieves the list of interface to sensor observation outputs
	 * @return
	 */
	public Map<String, ISensorDataInterface> getObservationOutputs() throws SensorException;
	
	
	/**
	 * Retrieves the list of interface to sensor command inputs
	 * @return
	 */
	public Map<String, ISensorControlInterface> getCommandInputs() throws SensorException;
	
	
	/**
	 * Returns the sensor connection status
	 * @return true if sensor is actually connected and can communicate with the driver
	 */
	public boolean isConnected();
	
	
	/**
	 * Registers a listener to receive the following sensor events:
	 * connected/disconnected, activated/deactivated, sensor configuration change.
	 * @param listener
	 */
	@Override
	public void registerListener(IEventListener listener);
	
}