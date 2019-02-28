package org.sensorhub.impl.sensor.uahweather;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.Quantity;

import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEHelper;

public class UAHweatherOutput extends AbstractSensorOutput<UAHweatherSensor>
{
    DataComponent dataStruct;
    DataEncoding dataEnc;
    
    public UAHweatherOutput(UAHweatherSensor parentSensor)
    {
        super(parentSensor);
    }


    @Override
    public String getName()
    {
        return "UAH Weather";
    }


    protected void init()
    {
        SWEHelper fac = new SWEHelper();
        dataStruct = fac.newDataRecord(7);
        dataStruct.setName(getName());
        
        // build SWE Common record structure
        dataStruct.setDefinition("http://sensorml.com/ont/swe/property/Weather");
        dataStruct.setDescription("Weather Station Data");
        
        /************************* Add appropriate data fields *******************************************************************************/
        // add time, average, and instantaneous radiation exposure levels
        dataStruct.addComponent("time", fac.newTimeStampIsoUTC());
        dataStruct.addComponent("pressure", fac.newQuantity(SWEHelper.getPropertyUri("BarometricPressure"), "Barometric Pressure", null, "mbar"));
        dataStruct.addComponent("temperature", fac.newQuantity(SWEHelper.getPropertyUri("Temperature"), "Air Temperature", null, "degC"));
        dataStruct.addComponent("relHumidity", fac.newQuantity(SWEHelper.getPropertyUri("RelativeHumidity"), " Relative Humidity", null, "%"));
        dataStruct.addComponent("rainAccum", fac.newQuantity(SWEHelper.getPropertyUri("RainAccumulation"), "Rain Accumulation", null, "tips"));
        dataStruct.addComponent("windSpeed", fac.newQuantity(SWEHelper.getPropertyUri("WindSpeed"), "Wind Speed", null, "m/s"));
        Quantity q = fac.newQuantity(SWEHelper.getPropertyUri("WindDirection"), "Wind Direction", null, "deg");
        q.setReferenceFrame("http://sensorml.com/ont/swe/property/NED");
        q.setAxisID("z");
        dataStruct.addComponent("windDir", q);
        /*************************************************************************************************************************************/
        
        // also generate encoding definition
        dataEnc = fac.newTextEncoding(",", "\n");
    }


    @Override
    public double getAverageSamplingPeriod()
    {
    	// sample every 15 seconds
        return 15.0;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return dataStruct;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return dataEnc;
    }
    
    
    protected void sendOutput(long msgTime, double airPres, float airTemp, float humid, byte rainCnt, float windSpeed, double windDirDeg)
    {
    	DataBlock dataBlock;
    	if (latestRecord == null)
    	    dataBlock = dataStruct.createDataBlock();
    	else
    	    dataBlock = latestRecord.renew();
    	
    	dataBlock.setLongValue(0, msgTime/1000);
    	dataBlock.setDoubleValue(1, Math.round(airPres*100.0)/100.0);
    	dataBlock.setFloatValue(2, Math.round(airTemp*100)/100);
    	dataBlock.setFloatValue(3, Math.round(humid*100)/100);
    	dataBlock.setByteValue(4, rainCnt);
    	dataBlock.setFloatValue(5, Math.round(windSpeed*100)/100);
    	dataBlock.setDoubleValue(6, windDirDeg);
    	
    	// update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = msgTime;
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, UAHweatherOutput.this, dataBlock));
    	
    }
}