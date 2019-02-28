
package org.sensorhub.impl.sensor.trek1000;

import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.data.DataRecordImpl;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.Vector;


/**
 * <p>
 * Output of raw anchor to anchor or tag to anchor ranges
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Aug 4, 2017
 */
public class LocalPosOutput extends AbstractSensorOutput<Trek1000Sensor>
{
    DataRecord dataStruct;
    DataEncoding dataEncoding;
 

    public LocalPosOutput(Trek1000Sensor parentSensor)
    {
        super(parentSensor);
    }


    protected void init()
    {
        dataStruct = new DataRecordImpl();
        dataStruct.setName(getName());
        dataStruct.setDefinition(SWEHelper.getPropertyUri("LocationXYZ"));

        GeoPosHelper fac = new GeoPosHelper();
        Vector locVector = fac.newLocationVectorXYZ(SWEConstants.DEF_SENSOR_LOC, "#UWB_FRAME", "m");
        locVector.setLabel("Tag Location");
        locVector.setDescription("Local XYZ location of tag");

        // Add timestamp, location of tag
        dataStruct.addComponent("time", fac.newTimeStampIsoUTC());
        dataStruct.addComponent("tagID", fac.newText(SWEHelper.getPropertyUri("BeaconID"), "Tag ID", null));
        dataStruct.addComponent("location", locVector);

        dataEncoding = fac.newTextEncoding(",", "\n");
    }


    protected void sendData(long msgTime, String tagID, double x, double y, double z)
    {
        DataBlock dataBlock = (latestRecord == null) ? dataStruct.createDataBlock() : latestRecord.renew();
        
        dataBlock.setDoubleValue(0, msgTime/1000.0);
        dataBlock.setStringValue(1, tagID);
        dataBlock.setDoubleValue(2, x);
        dataBlock.setDoubleValue(3, y);
        dataBlock.setDoubleValue(4, z);
        
        latestRecord = dataBlock;
        latestRecordTime = msgTime;
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, this, dataBlock));
    }


    @Override
    public String getName()
    {
        return "xyzLoc";
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
