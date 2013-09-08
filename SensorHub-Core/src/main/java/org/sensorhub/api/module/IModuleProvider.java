/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.module;


/**
 * <p><b>Title:</b>
 * IModuleProvider
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Interface to be implemented to enable automatic module discovery
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @date Sep 7, 2013
 */
public interface IModuleProvider
{
    public String getModuleTypeName();
    
    public Class<? extends IModule<?>> getModuleClass();
    
    public Class<? extends ModuleConfig> getModuleConfigClass();    
}
