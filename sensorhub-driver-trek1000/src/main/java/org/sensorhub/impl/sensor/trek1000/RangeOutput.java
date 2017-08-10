
package org.sensorhub.impl.sensor.trek1000;

import java.util.ArrayList;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.data.DataRecordImpl;
import org.vast.swe.SWEHelper;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;


/**
 * <p>
 * Output of raw anchor to anchor or tag to anchor ranges
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Aug 4, 2017
 */
public class RangeOutput extends AbstractSensorOutput<Trek1000Sensor>
{
    DataRecord dataStruct;
    DataEncoding dataEncoding;
    ArrayList<LLALocation> anchorLocations = null;


    public RangeOutput(Trek1000Sensor parentSensor)
    {
        super(parentSensor);
    }


    protected void init()
    {
        SWEHelper fac = new SWEHelper();
        
        dataStruct = new DataRecordImpl();
        dataStruct.setName(getName());
        dataStruct.setDefinition("http://sensorml.com/ont/swe/property/Event");
        dataStruct.addComponent("time", fac.newTimeStampIsoUTC());
        dataStruct.addComponent("beacon1", fac.newText(SWEHelper.getPropertyUri("BeaconID"), "Beacon1 ID", null));
        dataStruct.addComponent("beacon2", fac.newText(SWEHelper.getPropertyUri("BeaconID"), "Beacon2 ID", null));
        dataStruct.addComponent("range", fac.newQuantity(SWEHelper.getPropertyUri("Range"), "Range", null, "m", DataType.FLOAT));
        
        dataEncoding = fac.newTextEncoding(",", "\n");
    }


    protected void sendData(long msgTime, String tag1, String tag2, double range)
    {
        DataBlock dataBlock = (latestRecord == null) ? dataStruct.createDataBlock() : latestRecord.renew();
        
        dataBlock.setDoubleValue(0, msgTime/1000.0);
        dataBlock.setStringValue(1, tag1);
        dataBlock.setStringValue(2, tag2);
        dataBlock.setDoubleValue(3, range);
        
        latestRecord = dataBlock;
        latestRecordTime = msgTime;
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, this, dataBlock));
    }


    @Override
    public String getName()
    {
        return "ranges";
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return dataStruct;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return dataEncoding;
    }


    @Override
    public double getAverageSamplingPeriod()
    {
        return 1.0;
    }
}
