/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sos;

import org.vast.cdm.common.DataBlock;
import org.vast.cdm.common.DataComponent;
import org.vast.cdm.common.DataEncoding;
import org.vast.ogc.om.IObservation;
import org.vast.ows.sos.ISOSDataProvider;


/**
 * <p><b>Title:</b>
 * SOSServiceDataProvider
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Implementation of SOS data provider connecting to a sensor via 
 * SensorHub's sensor API (ISensorDataInterface)
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @date Sep 7, 2013
 */
public class SensorDataProvider implements ISOSDataProvider
{

    public SensorDataProvider(SensorDataProviderConfig config)
    {
        
    }
    
    
    @Override
    public IObservation getNextObservation() throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }
    

    @Override
    public DataBlock getNextResultRecord() throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }
    

    @Override
    public DataComponent getResultStructure() throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }
    

    @Override
    public DataEncoding getDefaultResultEncoding() throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

}
