/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.net;

import org.sensorhub.api.module.IModuleManager;


/**
 * <p><b>Title:</b>
 * ICommManager
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Management interface for communication devices (Ethernet, GSM, Satcom, etc.)
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 5, 2010
 */
public interface ICommManager extends IModuleManager<ICommInterface>
{
    /**
     * Installs a communication driver package (jar file) from the specified URL
     * @param driverPackageuURL URL of jar containing implementation of new driver
     * @return automatically assigned driver ID
     */
    public String installDriver(String driverPackageURL, boolean replace);
    
    
    /**
     * Uninstalls the driver with the specified ID
     * @param driverID
     */
    public void uninstallDriver(String driverID);
}
