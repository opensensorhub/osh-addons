/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.service;

import org.sensorhub.api.module.IModule;


/**
 * <p><b>Title:</b>
 * IServiceInterface
 * </p>
 *
 * <p><b>Description:</b><br/>
 * TODO: Common base to all service interfaces
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 5, 2010
 */
public interface IServiceInterface<ConfigType extends ServiceConfig> extends IModule<ConfigType>
{
    
}
