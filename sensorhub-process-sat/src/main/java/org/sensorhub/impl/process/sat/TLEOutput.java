/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2014 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.process.sat;

import java.util.Timer;
import java.util.TimerTask;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.IDataProducerModule;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.impl.SensorHub;
import org.vast.physics.TLEInfo;
import org.vast.physics.TLEProvider;
import org.vast.swe.SWEHelper;


/**
 * <p>
 * TLE Record Output
 * </p>
 *
 * <p>Copyright (c) 2014 Sensia Software LLC</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Apr 5, 2015
 */
public class TLEOutput implements IStreamingDataInterface, TLEProvider
{
    TLEPredictorProcess parentProcess;
    SatelliteStateOutput predictionOutput;
    IEventHandler eventHandler;
    DataComponent outputDef;
    DataEncoding outputEncoding;
    long lastRecordTime = Long.MIN_VALUE;
    DataBlock lastRecord;
    
    Timer timer = new Timer();
    TimerTask tleFetchTask;
    TLEInfo lastTle;
    

    public TLEOutput(TLEPredictorProcess parentProcess)
    {
        this.parentProcess = parentProcess;
        
        // create output structure
        SWEHelper fac = new SWEHelper();        
        DataRecord rec = fac.newDataRecord();
        rec.setName(getName());
        rec.setDefinition(SWEHelper.getPropertyUri("TLEData"));
        rec.addField("time", fac.newTimeStampIsoUTC());
        rec.addField("satID", fac.newCount(SWEHelper.getPropertyUri("TLESatID"), "Satellite ID", null));
        rec.addField("bstar", fac.newQuantity(SWEHelper.getPropertyUri("TLEBstar"), "B* Drag Coefficient", null, "m-1", DataType.DOUBLE));
        rec.addField("inclination", fac.newQuantity(SWEHelper.getPropertyUri("TLEInclination"), "Inclination", null, "deg", DataType.DOUBLE));
        rec.addField("rightAscension", fac.newQuantity(SWEHelper.getPropertyUri("TLERightAscension"), "Right Ascension", null, "deg", DataType.DOUBLE));
        rec.addField("eccentricity", fac.newQuantity(SWEHelper.getPropertyUri("TLEEccentricity"), "Eccentricity", null, "1", DataType.DOUBLE));
        rec.addField("argOfPerigee", fac.newQuantity(SWEHelper.getPropertyUri("TLEArgOfPerigee"), "Argument of Perigee", null, "deg", DataType.DOUBLE));
        rec.addField("meanAnomaly", fac.newQuantity(SWEHelper.getPropertyUri("TLEMeanAnomaly"), "Mean Anomaly", null, "deg", DataType.DOUBLE));
        rec.addField("meanMotion", fac.newQuantity(SWEHelper.getPropertyUri("TLEMeanMotion"), "Mean Motion", null, "deg/s", DataType.DOUBLE));
        rec.addField("revNumber", fac.newCount(SWEHelper.getPropertyUri("TLERevNumber"), "Revolution Number", null));
        this.outputDef = rec;        
        this.outputEncoding = fac.newTextEncoding();
        
        // obtain an event handler for this output
        String moduleID = parentProcess.getLocalID();
        String topic = getName();
        this.eventHandler = SensorHub.getInstance().getEventBus().registerProducer(moduleID, topic);
    }
    
    
    protected void start()
    {
        // start task to fetch new TLE data
        tleFetchTask = new TimerTask() {
            @Override
            public void run()
            {
                fetchLatestTLE();
            }
            
        };
        timer.scheduleAtFixedRate(tleFetchTask, 0L, 24*3600*1000L); // once a day
    }
    
    
    protected void fetchLatestTLE()
    {
        TLEPredictorProcessConfig config = parentProcess.getConfiguration();
        
        try
        {
            // fetch latest TLE file from Celestrak website
            TLEUpdater tleParser = new TLEUpdater(config.tleFetchUrl, config.satID);
            
            // parse TLE for desired sat ID
            TLEInfo tle = tleParser.readNextTLE(Double.POSITIVE_INFINITY);
            if (lastTle == null || tle.getTleTime() > lastTle.getTleTime())
            {
                lastTle = tle;
                                
                // send to output
                DataBlock tleData = (lastRecord == null) ? outputDef.createDataBlock() : lastRecord.renew();
                tleData.setDoubleValue(0, tle.getTleTime());
                tleData.setDoubleValue(1, tle.getSatelliteID());
                tleData.setDoubleValue(2, tle.getBStar());
                tleData.setDoubleValue(3, Math.toDegrees(tle.getInclination()));
                tleData.setDoubleValue(4, Math.toDegrees(tle.getRightAscension()));
                tleData.setDoubleValue(5, tle.getEccentricity());
                tleData.setDoubleValue(6, Math.toDegrees(tle.getArgumentOfPerigee()));
                tleData.setDoubleValue(7, Math.toDegrees(tle.getMeanAnomaly()));
                tleData.setDoubleValue(8, Math.toDegrees(tle.getMeanMotion()));
                tleData.setIntValue(9, tle.getRevNumber());
                
                lastRecordTime = System.currentTimeMillis();
                lastRecord = tleData;
                eventHandler.publishEvent(new DataEvent(lastRecordTime, this, lastRecord));
                
                // notify new TLE received
                parentProcess.notifyNewTLE();
            }
        }
        catch (Exception e)
        {
            TLEPredictorProcess.log.error("Cannot parse TLE file " + config.tleFetchUrl, e);
        }
    }
    
    
    protected void stop()
    {
        if (tleFetchTask != null)
            tleFetchTask.cancel();
    }
    
    
    @Override
    public TLEInfo getClosestTLE(double desiredTime) throws Exception
    {
        return lastTle;
    }


    @Override
    public IDataProducerModule<?> getParentModule()
    {
        return parentProcess;
    }


    @Override
    public String getName()
    {
        return "tleData";
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
        return 3*24*3600; // 3 days
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
