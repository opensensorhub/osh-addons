/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.client;


/**
 * <p><b>Title:</b>
 * IServiceClient
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Base interface for all client implementations receiving or sending data to external entities.
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 12, 2010
 */
public interface IServiceClient<ClientConfig extends IClientConfig>
{
    public String getLocalId();
    
    
    public void init(ClientConfig config);
    
    
    
}
