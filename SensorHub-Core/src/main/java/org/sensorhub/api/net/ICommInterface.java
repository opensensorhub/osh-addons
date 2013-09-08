/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.net;

import org.sensorhub.api.module.IModule;


/**
 * <p><b>Title:</b>
 * ICommInterface
 * </p>
 *
 * <p><b>Description:</b><br/>
 * TODO: Interface for all concrete implementations of communication channels 
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 12, 2010
 */
public interface ICommInterface extends IModule<CommConfig>
{
    public void sendMessage();
}
