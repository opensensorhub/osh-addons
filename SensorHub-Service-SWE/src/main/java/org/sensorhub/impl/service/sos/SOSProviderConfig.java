/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sos;


public abstract class SOSProviderConfig
{

    /**
     * Provider URI
     * If null, it will be auto-generated from server URL
     */
    public String uri;
    
    
    /**
     * Provider name
     * If null, it will be auto-generated from sensor name
     */
    public String name;
    
    
    /**
     * Provider description
     * It null, it will be auto-generated from sensor description
     */
    public String description;
    

}