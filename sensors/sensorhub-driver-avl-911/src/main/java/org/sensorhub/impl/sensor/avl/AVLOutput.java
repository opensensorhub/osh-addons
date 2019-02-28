/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Botts Innovative Research Inc. Portions created by
the Initial Developer are Copyright (C) 2015 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.avl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Map;
import net.opengis.swe.v20.AllowedTokens;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.Vector;
import net.opengis.swe.v20.Category;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.data.IMultiSourceDataInterface;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;


public class AVLOutput extends AbstractSensorOutput<AVLDriver> implements IMultiSourceDataInterface
{
    GregorianCalendar cal;

    DataRecord dataStruct;
    DataEncoding dataEncoding;
    BufferedReader msgReader;
    boolean sendData;
    SimpleDateFormat timeFormat;
    Map<String, DataBlock> latestRecords = new LinkedHashMap<String, DataBlock>();


    public AVLOutput(AVLDriver parentSensor)
    {
        super(parentSensor);

        // Intergraph 911 System format (20140329002208CD) + add T (before parsing)
        timeFormat = new SimpleDateFormat("yyyyMMddHHmmssz");

        //this.cal = new GregorianCalendar();
        //cal.setTimeZone(TimeZone.getTimeZone("UTC"));
    }


    @Override
    public String getName()
    {
        return "avlData";
    }


    protected void init()
    {
        GeoPosHelper fac = new GeoPosHelper();

        // SWE Common data structure
        dataStruct = fac.newDataRecord(7);
        dataStruct.setName(getName());
        dataStruct.setDefinition(SWEHelper.getPropertyUri("AVLData"));

        // time
        dataStruct.addComponent("time", fac.newTimeStampIsoUTC());

        // Mobile Data Terminal ID
        dataStruct.addComponent("mdt-id", fac.newCategory(SWEHelper.getPropertyUri("MDT-ID"), "MDT-ID", "Mobile Data Terminal ID", null));

        // Unit ID
        dataStruct.addComponent("unit-id", fac.newCategory(SWEHelper.getPropertyUri("Unit-ID"), "Unit ID", "Mobile Unit ID", null));
        
        // Vehicle ID
        dataStruct.addComponent("veh-id", fac.newCategory(SWEHelper.getPropertyUri("Vehicle-ID"), "Vehicle ID", "Mobile Vehicle Identification", null));
        dataStruct.getFieldList().getProperty("veh-id").setRole(ENTITY_ID_URI); // tag with entity ID role

        // location (latitude-longitude)	        
        Vector locVector = fac.newLocationVectorLatLon(SWEConstants.DEF_SENSOR_LOC);
        locVector.setLabel("Vehicle Location");
        dataStruct.addComponent("location", locVector);

        // status constraints: (AQ - at-station; ER - enroute; AR - arrived?, OS - out-of-service, AK - completed-returning)
        Category status = fac.newCategory(SWEHelper.getPropertyUri("Vehicle-Status"), "Unit Status", "Unit-Vehicle Status (AQ, OS, AK, ER, AR)", null);
        AllowedTokens constraints = fac.newAllowedTokens();
        constraints.addValue("AQ");
        constraints.addValue("ER");
        constraints.addValue("AR");
        constraints.addValue("OS");
        constraints.addValue("AK");
        status.setConstraint(constraints);
        dataStruct.addComponent("status", status);

        // event (empty if AQ, OS, or AK; event number if ER or AR)
        dataStruct.addComponent("event-id", fac.newCategory(SWEHelper.getPropertyUri("Event-ID"), "Event ID", "Assigned ID to an emergency event", null));

        // set encoding to CSV
        dataEncoding = fac.newTextEncoding(",", "\n");
    }


    private void pollAndSendMeasurement()
    {
        double julianTime = 0.0;
        String mdtID = " ";
        String vehID = " ";
        String unitID = " ";
        double lat = Double.NaN;
        double lon = Double.NaN;
        String status = " ";
        String eventID = " ";

        try
        {
            boolean gotMsg = false;
            while (!gotMsg)
            {
                String line = msgReader.readLine();
                if (line == null || line.length() == 0)
                    return;
                String timeString;
                Date time;

                // parse the data string
                AVLDriver.log.debug("Message received: {}", line);

                // split tokens based on one or more white spaces
                String[] tokens = line.trim().split("\\s+");

                // get time tag in pseudo standard string
                timeString = tokens[0];

                // add "T" at end to make recognizable time zone (e.g. CDT) and then
                //   parse according to predefined Intergraph 911 format
                try
                {
                    time = timeFormat.parse(timeString + "T");
                }
                catch (ParseException e)
                {
                    AVLDriver.log.warn("Exception parsing date-time string ", timeString + "T");
                    continue;
                }

                if (time != null)
                    julianTime = time.getTime() / 1000.0;
                
                // skip tokens[1]
                mdtID = tokens[2];
                unitID = tokens[3];
                vehID = tokens[4];

                lat = Double.parseDouble(tokens[5]);
                lon = Double.parseDouble(tokens[6]);

                // skip tokens[7 - 15]
                status = tokens[16];
                if (tokens.length > 17) // event id is not always present
                    eventID = tokens[17];
                else
                    eventID = "NONE";

                gotMsg = true;
            }
        }
        catch (IOException e)
        {
            if (sendData)
                AVLDriver.log.error("Unable to parse AVL message", e);
            return;
        }
        
        // create new FOI if needed
        parentSensor.addFoi(julianTime, vehID);

        // create and populate datablock
        DataBlock dataBlock;
        if (latestRecord == null)
            dataBlock = dataStruct.createDataBlock();
        else
            dataBlock = latestRecord.renew();

        dataBlock.setDoubleValue(0, julianTime);
        dataBlock.setStringValue(1, mdtID);
        dataBlock.setStringValue(2, unitID);
        dataBlock.setStringValue(3, vehID);
        dataBlock.setDoubleValue(4, lat);
        dataBlock.setDoubleValue(5, lon);
        dataBlock.setStringValue(6, status);
        dataBlock.setStringValue(7, eventID);

        // update latest record and send event
        latestRecord = dataBlock;
        latestRecords.put(vehID, latestRecord);
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, vehID, AVLOutput.this, dataBlock));
    }


    protected void start(ICommProvider<?> commProvider)
    {
        if (sendData)
            return;
        sendData = true;

        // connect to data stream
        try
        {
            msgReader = new BufferedReader(new InputStreamReader(commProvider.getInputStream()));
            AVLDriver.log.info("Connected to AVL data stream");
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error while initializing communications ", e);
        }

        // start main measurement thread
        Thread t = new Thread(new Runnable()
        {
            public void run()
            {
                while (sendData)
                    pollAndSendMeasurement();
            }
        });
        t.start();
    }


    protected synchronized void stop()
    {
        sendData = false;

        if (msgReader != null)
        {
            try { msgReader.close(); }
            catch (IOException e) { }
            msgReader = null;
        }
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
        return 1200.0; //why 20 minutes?
    }


    @Override
    public Collection<String> getEntityIDs()
    {
        return parentSensor.getEntityIDs();
    }


    @Override
    public Map<String, DataBlock> getLatestRecords()
    {
        return Collections.unmodifiableMap(latestRecords);
    }


    @Override
    public DataBlock getLatestRecord(String entityID)
    {
        return latestRecords.get(entityID);
    }

}
