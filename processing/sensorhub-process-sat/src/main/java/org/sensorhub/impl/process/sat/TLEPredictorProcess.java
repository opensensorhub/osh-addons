/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2014 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.process.sat;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.processing.DataSourceConfig;
import org.sensorhub.api.processing.ProcessException;
import org.sensorhub.impl.processing.AbstractStreamProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Example process for fetching latest TLE and computing satellite pos/vel
 * </p>
 *
 * <p>Copyright (c) 2014 Sensia Software LLC</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Apr 5, 2015
 */
public class TLEPredictorProcess extends AbstractStreamProcess<TLEPredictorProcessConfig>
{
    protected static final Logger log = LoggerFactory.getLogger(TLEPredictorProcess.class);
    
    protected TLEOutput tleOutput;
    protected SatelliteStateOutput predictedStateOutput;
    protected SatelliteStateOutput realTimeStateOutput;
    
    double lastComputedPosTime = Double.NaN;
    double realTimeOutputPeriod = 1.0;
    double predictOutputPeriod = 10.0;
    double predictHorizon = 3 * 24 * 3600; // 3 days
    
    
    @Override
    public void init(TLEPredictorProcessConfig config) throws SensorHubException
    {
        this.config = config;        
        
        // create outputs
        tleOutput = new TLEOutput(this);
        addOutput(tleOutput);
        
        predictedStateOutput = new SatelliteStateOutput(this, "predictedState", tleOutput, predictOutputPeriod, predictHorizon);
        addOutput(predictedStateOutput);
        
        // real-time state output
        realTimeStateOutput = new SatelliteStateOutput(this, "currentState", tleOutput, realTimeOutputPeriod, 0.0);
        addOutput(realTimeStateOutput);
    }
    
    
    @Override
    public void start() throws SensorHubException
    {
        tleOutput.start();        
    }
    
    
    @Override
    public void stop()
    {
        tleOutput.stop();
        realTimeStateOutput.stop();
    }
    
    
    protected void notifyNewTLE()
    {
        if (Double.isNaN(lastComputedPosTime))
        {
            realTimeStateOutput.start();
            predictedStateOutput.start();
        }
    }
    
    
    @Override
    protected void process(DataEvent lastEvent) throws ProcessException
    {
        // do nothing here, everything happends in methods called by timers
    }
    
    
    @Override
    public boolean isPauseSupported()
    {
        return false;
    }

    
    @Override
    public boolean isCompatibleDataSource(DataSourceConfig dataSource)
    {
        return false;
    }
}
