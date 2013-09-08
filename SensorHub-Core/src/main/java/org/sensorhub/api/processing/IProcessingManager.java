/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.processing;

import java.util.List;
import javax.print.DocFlavor.URL;
import org.sensorhub.api.module.IModuleManager;


/**
 * <p><b>Title:</b>
 * IProcessingManager
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Management interface for process chains
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 5, 2010
 */
public interface IProcessingManager extends IModuleManager<IProcess>
{
	/**
	 * @return the list of all process code configured on the system
	 */
	public List<String> getAllProcessCodePackages();
	
	
	/**
	 * Installs a process code package (jar file) from the specified URL for the process URI
	 * @param processURI URI of process type that this code should be used for
	 * @param codePackage URL of jar containing classes necessary to run the specified method
	 * @param replace if true, old code associated to the same URI will be destroyed 
	 */
	public void installProcessCode(String processURI, URL codePackage, boolean replace);
	
	
	/**
	 * Uninstalls process code with specified URI
	 * @param processURI
	 */
	public void uninstallProcessCode(String processURI);
}
