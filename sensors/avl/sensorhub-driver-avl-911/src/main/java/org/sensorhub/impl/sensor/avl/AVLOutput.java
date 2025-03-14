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
import java.util.Date;
import java.util.GregorianCalendar;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;


public class AVLOutput extends AbstractSensorOutput<AVLDriver>
{
    GregorianCalendar cal;
    DataRecord dataStruct;
    DataEncoding dataEncoding;
    BufferedReader msgReader;
    boolean sendData;
    SimpleDateFormat timeFormat;


    public AVLOutput(AVLDriver parentSensor)
    {
        super("avlData", parentSensor);

        // Intergraph 911 System format (20140329002208CD) + add T (before parsing)
        timeFormat = new SimpleDateFormat("yyyyMMddHHmmssz");

        //this.cal = new GregorianCalendar();
        //cal.setTimeZone(TimeZone.getTimeZone("UTC"));
    }


    protected void init()
    {
        GeoPosHelper fac = new GeoPosHelper();

        // SWE Common data structure
        dataStruct = fac.createRecord()
            .name(getName())
            .addSamplingTimeIsoUTC("time")
            .addField("mdt-id", fac.createText()
                .definition(SWEHelper.getPropertyUri("MDT-ID"))
                .label("MDT-ID")
                .description("Mobile Data Terminal ID"))
            .addField("unit-id", fac.createText()
                .definition(SWEHelper.getPropertyUri("Unit-ID"))
                .label("Unit ID")
                .description("Mobile Unit ID"))
            .addField("veh-id", fac.createText()
                .definition(SWEHelper.getPropertyUri("Vehicle-ID"))
                .label("Vehicle ID")
                .description("Mobile Vehicle Identification"))
            .addField("location", fac.createLocationVectorLatLon()
                .definition(SWEConstants.DEF_SENSOR_LOC)
                .label("Vehicle Location"))
            .addField("status", fac.createCategory()
                .definition(SWEHelper.getPropertyUri("Vehicle-Status"))
                .label("Unit Status")
                .description("Unit-Vehicle Status")
                .addAllowedValues("AQ", "ER", "AR", "OS", "AK"))
            .addField("event-id", fac.createText()
                .definition(SWEHelper.getPropertyUri("Event-ID"))
                .label("Event ID")
                .description("ID assigned to the emergency event"))
            .build();

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
                log.debug("Message received: {}", line);

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
                    getLogger().warn("Exception parsing date-time string ", timeString + "T");
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
                getLogger().error("Unable to parse AVL message", e);
            return;
        }
        
        // create new FOI if needed
        var foiUID = parentSensor.addFoi(julianTime, vehID);

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
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publish(new DataEvent(latestRecordTime, AVLOutput.this, foiUID, dataBlock));
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
            parentSensor.getLogger().info("Connected to AVL data stream");
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

}
