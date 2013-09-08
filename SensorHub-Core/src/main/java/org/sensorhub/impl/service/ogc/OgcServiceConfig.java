/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.ogc;

import org.sensorhub.api.service.ServiceConfig;
import org.vast.util.ResponsibleParty;


/**
 * <p><b>Title:</b>
 * OgcServiceConfig
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Abstract configuration class for all OGC service types
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @date Sep 6, 2013
 */
@SuppressWarnings("serial")
public abstract class OgcServiceConfig extends ServiceConfig
{
    public class CapabilitiesInfo
    {
        public String title;
        public String description;
        public String[] keywords;
        public String fees;
        public String accessConstraints;
        public String providerName;
        public String providerURL;
        public ResponsibleParty contact = new ResponsibleParty(); 
    }

    
    /**
     * Information to include in the service capabilities document
     */
    public CapabilitiesInfo ogcCapabilitiesInfo = new CapabilitiesInfo();
    
    
    /**
     * Enables/disables HTTP GET bindings on operations that support it
     */
    public boolean enableHttpGET = true;
    
    
    /**
     * Enables/disables HTTP POST bindings on operations that support it
     */
    public boolean enableHttpPOST = true;
    
    
    /**
     * Enables/disables HTTP SOAP bindings on operations that support it
     */
    public boolean enableSOAP = true;
}
