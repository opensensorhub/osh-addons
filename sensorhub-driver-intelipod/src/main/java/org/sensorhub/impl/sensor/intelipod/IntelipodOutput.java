/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.intelipod;

import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.sensor.SensorDataEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.TimeZone;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.Quantity;
import net.opengis.gml.v32.AbstractFeature;

import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;


public class IntelipodOutput extends AbstractSensorOutput<IntelipodSensor>
{
    private static String MSG_PREFIX = "$PLTIT";
    private static String MSG_TYPE_HV = "HV";
    //private static String MSG_TYPE_HT = "HT";
    //private static String MSG_TYPE_ML = "ML";
    private static double FEET_TO_METERS = 0.304800610;
    private static double YARDS_TO_METERS = 0.9144;
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    
    DataComponent intelipodData;
    DataEncoding dataEncoding;
    BufferedReader msgReader;
    boolean sendData;
    String[] lineSplit;
    
    
    public IntelipodOutput(IntelipodSensor parentSensor)
    {
        super(parentSensor);
    }


    @Override
    public String getName()
    {
        return "intelipodData";
    }


    public void init()
    {
        SWEHelper swe = new SWEHelper();
        GeoPosHelper geo = new GeoPosHelper();
        
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        // build SWE Common record structure
        intelipodData = swe.newDataRecord(7);
        intelipodData.setName(getName());
        intelipodData.setDefinition("http://sensorml.com/ont/swe/property/IntelipodData");
        
        // add sensor ID, time, location, num satellites, hdop, temperature, pressure
        intelipodData.addComponent("sensorID", swe.newText(SWEHelper.getPropertyUri("SensorID"), "Sensor ID", "ID of Intelipod Sensor: VNTDevXX"));
        intelipodData.addComponent("time", swe.newTimeStampIsoUTC());
        intelipodData.addComponent("location", geo.newLocationVectorLLA(SWEConstants.DEF_SENSOR_LOC));
        intelipodData.addComponent("numSatellites", swe.newQuantity(SWEHelper.getPropertyUri("NumberOfSatellites"), "Number of Satellites Reported by GPS", null, null));
        intelipodData.addComponent("gpsHDOP", swe.newQuantity(SWEHelper.getPropertyUri("HDOP"), "Horizontal Dilution of Precision", null, null));
        intelipodData.addComponent("temperature", swe.newQuantity(SWEHelper.getPropertyUri("Temperature"), "Air Temperature", null, "degC"));
        intelipodData.addComponent("pressure", swe.newQuantity(SWEHelper.getPropertyUri("BarometricPressure"), "Barometric Pressure", null, "hPa"));
        
        // also generate encoding definition as text block
        dataEncoding = swe.newTextEncoding(",", "\n");
    }
    

    /* TODO: only using HV message; add support for HT and ML */
    public void postMeasurement(String line) throws ParseException
    {	
    	lineSplit = line.split(",");
    	
    	LocalDateTime now = LocalDateTime.now();
    	Date date = dateFormat.parse(now.getYear() + "-" + 
    			now.getMonthValue() + "-" + 
    			now.getDayOfMonth() + "T" + 
    			lineSplit[1].trim());
    	
    	long seconds = date.getTime();

	    // create and populate datablock
        DataBlock dataBlock;
        if (latestRecord == null)
            dataBlock = intelipodData.createDataBlock();
        else
            dataBlock = latestRecord.renew();
        
        dataBlock.setStringValue(0, lineSplit[0].trim()); // Sensor ID
        dataBlock.setDoubleValue(1, seconds / 1000.); // GPS Time
        dataBlock.setDoubleValue(2, Double.parseDouble(lineSplit[2].trim())); // GPS Latitude 
        dataBlock.setDoubleValue(3, Double.parseDouble(lineSplit[3].trim())); // GPS Longitude
        dataBlock.setDoubleValue(4, Double.parseDouble(lineSplit[4].trim())); // GPS Altitude
        dataBlock.setIntValue(5, Integer.parseInt(lineSplit[5].trim()));      // GPS Num of Satellites
        dataBlock.setDoubleValue(6, Double.parseDouble(lineSplit[6].trim())); // GPS HDOP
        dataBlock.setDoubleValue(7, Double.parseDouble(lineSplit[7].trim())); // Temperature
        dataBlock.setDoubleValue(8, Double.parseDouble(lineSplit[8].trim())); // Pressure
        
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, IntelipodOutput.this, dataBlock));    
    }
    
    
    protected double convert(double val, String unit)
    {
        // feet to meters
        if (unit.equals("F"))
            return val * FEET_TO_METERS;
        
        // yards to meters
        else if (unit.equals("Y"))
            return val * YARDS_TO_METERS;
        
        return val;
    }


//    protected void start(ICommProvider<?> commProvider)
//    {
//        if (sendData)
//            return;
//        
//        sendData = true;
//        
//        // connect to data stream
//        try
//        {
//            msgReader = new BufferedReader(new InputStreamReader(commProvider.getInputStream()));
//            IntelipodSensor.log.info("Connected to Intelipod data stream");
//        }
//        catch (IOException e)
//        {
//            throw new RuntimeException("Error while initializing communications ", e);
//        }
//        
//        // start main measurement thread
//        Thread t = new Thread(new Runnable()
//        {
//            public void run()
//            {
//                while (sendData)
//                {
//                    pollAndSendMeasurement();
//                }
//            }
//        });
//        t.start();
//    }


//    protected void stop()
//    {
//        sendData = false;
//        
//        if (msgReader != null)
//        {
//            try { msgReader.close(); }
//            catch (IOException e) { }
//            msgReader = null;
//        }
//    }


    @Override
    public double getAverageSamplingPeriod()
    {
    	return 10.0; //10sec
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return intelipodData;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return dataEncoding;
    }
}
