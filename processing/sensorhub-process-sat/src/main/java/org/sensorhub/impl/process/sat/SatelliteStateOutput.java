/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2014 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.process.sat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.IDataProducerModule;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.impl.SensorHub;
import org.vast.physics.MechanicalState;
import org.vast.physics.OrbitPredictor;
import org.vast.physics.TLEOrbitPredictor;
import org.vast.swe.SWEConstants;
import org.vast.swe.helper.GeoPosHelper;


/**
 * <p>
 * Satellite State Vector Output (position + velocity)
 * </p>
 *
 * <p>Copyright (c) 2014 Sensia Software LLC</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Apr 5, 2015
 */
public class SatelliteStateOutput implements IStreamingDataInterface
{
    TLEPredictorProcess parentProcess;
    IEventHandler eventHandler;
    DataComponent outputDef;
    DataEncoding outputEncoding;    
    DataBlock lastRecord;
    long lastRecordTime = Long.MIN_VALUE;
    
    String name;
    Timer timer = new Timer();
    TimerTask posUpdateTask;
    OrbitPredictor orbitPredictor;
    double outputPeriod = 1.0;
    double predictHorizon = 0.0;
    

    public SatelliteStateOutput(TLEPredictorProcess parentProcess, String name, TLEOutput tleOutput, double outputPeriod, double predictHorizon)
    {
        this.parentProcess = parentProcess;
        this.orbitPredictor = new TLEOrbitPredictor(tleOutput);
        this.name = name;
        this.outputPeriod = outputPeriod;
        this.predictHorizon = predictHorizon;
        
        // create output structure
        GeoPosHelper fac = new GeoPosHelper();
        DataRecord rec = fac.newDataRecord();
        rec.setName(getName());
        rec.setDefinition(SWEConstants.DEF_PLATFORM_LOC.replace("Location", "State"));
        rec.addField("time", fac.newTimeStampIsoUTC());
        rec.addField("position", fac.newLocationVectorECEF(SWEConstants.DEF_PLATFORM_LOC));
        rec.addField("velocity", fac.newVelocityVectorECEF(SWEConstants.DEF_PLATFORM_LOC.replace("Location", "Velocity"), "m/s"));
        this.outputDef = rec;
        this.outputEncoding = fac.newTextEncoding();
        
        // obtain an event handler for this output
        String moduleID = parentProcess.getLocalID();
        String topic = getName();
        this.eventHandler = SensorHub.getInstance().getEventBus().registerProducer(moduleID, topic);
    }
    
    
    protected void start()
    {
        // start task to compute new satellite position
        posUpdateTask = new TimerTask() {
            @Override
            public void run()
            {
                double posTime = scheduledExecutionTime() / 1000.0 + predictHorizon;
                TLEPredictorProcess.log.trace("Computing position @ {}", posTime);
                computeAndOutputState(posTime);              
            }            
        };
        
        long outputDtInMillis = (long)(outputPeriod * 1000L);
        long firstTime = (System.currentTimeMillis() / outputDtInMillis + 1) * outputDtInMillis;
        timer.scheduleAtFixedRate(posUpdateTask, new Date(firstTime), outputDtInMillis); // every dT
        
        // batch compute start of predicted data
        if (predictHorizon > 0.0)
            batchComputePredictedTrajectory(firstTime / 1000.0 + predictHorizon);
    }
    
    
    protected void stop()
    {
        if (posUpdateTask != null)
            posUpdateTask.cancel();
    }
    
    
    protected DataBlock computeState(double time)
    {
        MechanicalState state = orbitPredictor.getECEFState(time);
        
        // send to output
        DataBlock stateData = (lastRecord == null) ? outputDef.createDataBlock() : lastRecord.renew();
        stateData.setDoubleValue(0, state.julianTime);
        stateData.setDoubleValue(1, state.linearPosition.x);
        stateData.setDoubleValue(2, state.linearPosition.y);
        stateData.setDoubleValue(3, state.linearPosition.z);
        stateData.setDoubleValue(4, state.linearVelocity.x);
        stateData.setDoubleValue(5, state.linearVelocity.y);
        stateData.setDoubleValue(6, state.linearVelocity.z);
        
        return stateData;
    }
    
    
    protected void computeAndOutputState(double time)
    {
        DataBlock stateData = computeState(time);
        
        lastRecordTime = System.currentTimeMillis();
        lastRecord = stateData;
        eventHandler.publishEvent(new DataEvent(lastRecordTime, this, lastRecord));
    }
    
    
    protected void batchComputePredictedTrajectory(double stopTime)
    {
        long outputDtInMillis = (long)(outputPeriod * 1000L);
        long firstTime = (System.currentTimeMillis() / outputDtInMillis + 1) * outputDtInMillis;
        
        double startTime = firstTime / 1000.0;
        
        TLEPredictorProcess.log.debug("Batch computing predicted measurements from {} to {}", startTime, stopTime);
        int listSize = (int)((stopTime - startTime) / outputPeriod);
        List<DataBlock> dataRecords = new ArrayList<DataBlock>(listSize);
        for (double time = startTime; time < stopTime; time += outputPeriod)
        {
            DataBlock stateData = computeState(time);
            dataRecords.add(stateData);            
        }
        
        lastRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new DataEvent(lastRecordTime, this, dataRecords.toArray(new DataBlock[0])));
        TLEPredictorProcess.log.debug("Done batch processing of predicted states");
    }


    @Override
    public IDataProducerModule<?> getParentModule()
    {
        return parentProcess;
    }


    @Override
    public String getName()
    {
        return name;
    }


    @Override
    public boolean isEnabled()
    {
        return true;
    }
    
    
    @Override
    public DataComponent getRecordDescription()
    {
        return outputDef;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return outputEncoding;
    }


    @Override
    public DataBlock getLatestRecord()
    {
        return lastRecord;
    }


    @Override
    public long getLatestRecordTime()
    {
        return lastRecordTime;
    }


    @Override
    public double getAverageSamplingPeriod()
    {
        return outputPeriod;
    }
    
    
    @Override
    public void registerListener(IEventListener listener)
    {
        eventHandler.registerListener(listener);
    }


    @Override
    public void unregisterListener(IEventListener listener)
    {
        eventHandler.unregisterListener(listener);
    }

}
