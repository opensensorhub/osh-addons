/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2014 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.process.trupulse;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.processing.DataSourceConfig;
import org.sensorhub.api.processing.ProcessException;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.processing.AbstractStreamProcess;
import org.sensorhub.impl.sensor.trupulse.TruPulseConfig;
import org.sensorhub.impl.sensor.trupulse.TruPulseSensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataBlockFloat;
import org.vast.math.Matrix3d;
import org.vast.math.Vector3d;
import org.vast.physics.MapProjection;
import org.vast.physics.NadirPointing;
import org.vast.process.DataQueue;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;


/**
 * <p>
 * Example process for geolocating the range finder target knowing the 
 * device position either as provided by user, or from an associated GPS.
 * This works because TruPulse range finder also provides the azimuth
 * and inclination/elevation of the beam, in addition to the range.
 * </p>
 *
 * <p>Copyright (c) 2015 Sensia Software LLC</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since June 21, 2015
 */
public class TargetGeolocProcess extends AbstractStreamProcess<TargetGeolocConfig>
{
    protected static final Logger log = LoggerFactory.getLogger(TargetGeolocProcess.class);
        
    protected TargetGeolocOutput targetLocOutput;
    protected double[] lastSensorPosEcef = new double[3];
    
    protected DataRecord sensorLocInput;
    protected DataRecord rangeMeasInput;    
    protected DataQueue sensorLocQueue;
    protected DataQueue rangeMeasQueue;
    
    
    @Override
    public void init(TargetGeolocConfig config) throws SensorHubException
    {
        this.config = config;
        
        // initialize with fixed pos if set
        if (config.fixedPosLLA != null)
        {
            double[] lla = config.fixedPosLLA;
            MapProjection.LLAtoECF(Math.toRadians(lla[1]), Math.toRadians(lla[0]), lla[2], lastSensorPosEcef, null);
        }
        
        // create inputs
        SWEHelper fac = new SWEHelper();   
        
        sensorLocInput = fac.newDataRecord();
        sensorLocInput.setName("sensorLocation");
        sensorLocInput.addField("time", fac.newTimeStampIsoUTC());
        sensorLocInput.addField("loc", fac.newLocationVectorLLA(SWEConstants.DEF_SENSOR_LOC));
        inputs.put(sensorLocInput.getName(), sensorLocInput);
        
        rangeMeasInput = fac.newDataRecord();
        rangeMeasInput.setName("rangeMeasurement");
        rangeMeasInput.addField("time", fac.newTimeStampIsoUTC());
        rangeMeasInput.addField("range", fac.newQuantity(SWEHelper.getPropertyUri("LineOfSightDistance"), "Laser Range Measurement", null, "m"));
        inputs.put(rangeMeasInput.getName(), rangeMeasInput);
        
        // create outputs
        targetLocOutput = new TargetGeolocOutput(this);
        addOutput(targetLocOutput);
        
        super.init(config);
    }
    
    
    @Override
    protected void connectInput(String inputName, String dataPath, DataQueue inputQueue) throws Exception
    {        
        super.connectInput(inputName, dataPath, inputQueue);
        
        if (inputName.equals(sensorLocInput.getName()))
            sensorLocQueue = inputQueue;
        
        else if (inputName.equals(rangeMeasInput.getName()))
            rangeMeasQueue = inputQueue;
    }
    
    
    @Override
    protected void process(DataEvent lastEvent) throws ProcessException
    {
        if (sensorLocQueue.isDataAvailable())
        {
            DataBlock dataBlk = lastEvent.getRecords()[0];
            double lat = Math.toRadians(dataBlk.getDoubleValue(1));
            double lon = Math.toRadians(dataBlk.getDoubleValue(2));
            double alt = dataBlk.getDoubleValue(3);
            MapProjection.LLAtoECF(lon, lat, alt, lastSensorPosEcef, null);
        }
        
        else if (rangeMeasQueue.isDataAvailable())
        {
            DataBlock dataBlk = lastEvent.getRecords()[0];
            double time = dataBlk.getDoubleValue(0);
            double range = dataBlk.getDoubleValue(2);
            double az = Math.toRadians(dataBlk.getDoubleValue(3));
            double inc = Math.toRadians(dataBlk.getDoubleValue(4));
            Vector3d los = new Vector3d(range, 0.0, 0.0);
            los.rotateZ(az);
            los.rotateY(inc);
            
            Vector3d ecefPos = new Vector3d(lastSensorPosEcef);
            Matrix3d ecefNedRot = NadirPointing.getNEDRotationMatrix(ecefPos);
            los.rotate(ecefNedRot);
            los.add(ecefPos);
            
            double[] lla = MapProjection.ECFtoLLA(los.x, los.y, los.z, null, null);
            targetLocOutput.sendLocation(time, Math.toDegrees(lla[1]), Math.toDegrees(lla[0]), lla[2]);
        }
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
    
    
    public static void main(String[] args) throws Exception
    {
        TargetGeolocProcess p = new TargetGeolocProcess();
        TargetGeolocConfig processConf = new TargetGeolocConfig();
        processConf.fixedPosLLA = new double[] {0.0, 0.0, 0.0};
        p.init(processConf);
        p.sensorLocQueue = new DataQueue();
        p.rangeMeasQueue = new DataQueue();
        
        TruPulseSensor sensor = new TruPulseSensor();
        TruPulseConfig sensorConf = new TruPulseConfig();
        sensor.init(sensorConf);
        ISensorDataInterface sensorOutput = sensor.getAllOutputs().values().iterator().next();
        DataComponent outputDef = sensorOutput.getRecordDescription();
                
        IStreamingDataInterface processOutput = p.getAllOutputs().values().iterator().next();
        IEventListener l = new IEventListener() {
            public void handleEvent(Event<?> e)
            {
                DataBlock data = ((DataEvent)e).getRecords()[0];
                double lat = data.getDoubleValue(1);
                double lon = data.getDoubleValue(2);
                double alt = data.getDoubleValue(3);
                System.out.println(lat + "," + lon + "," + alt);
            }
        };
        processOutput.registerListener(l);
        
        DataBlock dataBlk = outputDef.createDataBlock();
        long now = System.currentTimeMillis();
        double range = 10.0;
        double az = 90.0;
        double inc = 0.0;
        dataBlk.setDoubleValue(0, now / 1000.);
        dataBlk.setDoubleValue(2, range);
        dataBlk.setDoubleValue(3, az);
        dataBlk.setDoubleValue(4, inc);
        p.rangeMeasQueue.add(new DataBlockFloat());
        p.process(new SensorDataEvent(now, sensorOutput, dataBlk));
    }
}
