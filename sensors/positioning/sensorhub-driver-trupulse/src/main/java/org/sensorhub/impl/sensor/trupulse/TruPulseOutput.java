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

package org.sensorhub.impl.sensor.trupulse;

import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.data.DataEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;


public class TruPulseOutput extends AbstractSensorOutput<TruPulseSensor>
{
    private static String OUTPUT_NAME = "rangeData";
    private static String MSG_PREFIX = "$PLTIT";
    private static String MSG_TYPE_HV = "HV";
    private static String MSG_TYPE_HT = "HT";
    private static String MSG_TYPE_ML = "ML";
    private static double FEET_TO_METERS = 0.304800610;
    private static double YARDS_TO_METERS = 0.9144;
    
    DataComponent lrfData;
    DataEncoding dataEncoding;
    Thread readThread;
    BufferedReader msgReader;
    boolean sendData;
    
    
    public TruPulseOutput(TruPulseSensor parentSensor)
    {
        super(OUTPUT_NAME, parentSensor);
    }


    public void init()
    {
        SWEHelper swe = new SWEHelper();
        
        // build SWE Common record structure
        lrfData = swe.createRecord()
            .addSamplingTimeIsoUTC("time")        
            .addField("horizDistance", swe.createQuantity()
                .definition(SWEHelper.getQudtUri("Distance"))
                .label("Horizontal Distance")
                .description("Horizontal distance to target")
                .uomCode("m"))
            .addField("slopeDistance", swe.createQuantity()
                .definition(SWEHelper.getQudtUri("Distance"))
                .label("LineOfSight Distance")
                .description("Line-of-sight distance to target")
                .uomCode("m"))
            .addField("azimuth", swe.createQuantity()
                .definition(SWEHelper.getPropertyUri("AzimuthAngle"))
                .refFrame(SWEConstants.REF_FRAME_NED)
                .axisId("z")
                .label("Azimuth Angle")
                .description("Azimuth/Heading angle of line-of-sight measured from true north")
                .uomCode("deg"))
            .addField("inclination", swe.createQuantity()
                .definition(SWEHelper.getPropertyUri("ElevationAngle"))
                .refFrame(SWEConstants.REF_FRAME_NED)
                .axisId("y")
                .label("Elevation Angle")
                .description("Inclination/Elevation of line-of-sight from the horizontal plane")
                .uomCode("deg"))
            .addField("height", swe.createQuantity()
                    .definition(SWEHelper.getQudtUri("Height"))
                    .label("Height of Target")
                    .description("Height of the target")
                    .uomCode("m"))
            .build();
     
        // also generate encoding definition as text block
        dataEncoding = swe.newTextEncoding(",", "\n");
    }
    

    /* TODO: only using HV message; add support for HT and ML */
    private void pollAndSendMeasurement()
    {
        double hd = Double.NaN;
        double ht = Double.NaN;
        double incl = Double.NaN;
        double az = Double.NaN;
        double sd = Double.NaN;

        try
        {
            long msgTime = 0;
            boolean gotMsg = false;
            while (!gotMsg)
            {
                String line = msgReader.readLine();
                msgTime = System.currentTimeMillis();
                String val, unit;

                // parse the data string
                TruPulseSensor.log.debug("Message received: {}", line);
                String[] tokens = line.split(",");

                val = tokens[0];
                if (!val.equals(MSG_PREFIX))
                {
                    TruPulseSensor.log.warn("Message initial token does NOT equal expected string {}", MSG_PREFIX);
                    continue;
                }

                // check for desired message type HV
                val = tokens[1];
                if (val.equals(MSG_TYPE_HV) || val.equals(MSG_TYPE_ML))
                {

                    // get horizontal distance measure and check units (convert if not meters)
                    val = tokens[2];
                    unit = tokens[3];
                    if (val.length() > 0 && unit.length() > 0)
                    {
                        hd = Double.parseDouble(val);
                        hd = convert(hd, unit);
                    }

                    // get azimuth angle measure (units should be degrees)
                    val = tokens[4];
                    unit = tokens[5];
                    if (val.length() > 0 && unit.length() > 0)
                        az = Double.parseDouble(val);

                    // get inclination angle measure (units should be degrees)
                    val = tokens[6];
                    unit = tokens[7];
                    if (val.length() > 0 && unit.length() > 0)
                        incl = Double.parseDouble(val);

                    // get slope distance measure and check units (should be meters)
                    val = tokens[8];
                    unit = tokens[9];
                    if (val.length() > 0 && unit.length() > 0)
                    {
                        sd = Double.parseDouble(val);
                        sd = convert(sd, unit);
                    }
                    gotMsg = true;
                }

                if(val.equals(MSG_TYPE_HT)){

                    // Height
                    val = tokens[2]; //HT value
                    unit = tokens[3]; //HT units Feet or Meters

                    if(val.length() > 0 && unit.length() > 0){
                        ht = Double.parseDouble(val);
                        ht = convert(ht, unit);
                    }
                    gotMsg = true;

                }
            }

            // create and populate datablock
            DataBlock dataBlock;
            if (latestRecord == null)
                dataBlock = lrfData.createDataBlock();
            else
                dataBlock = latestRecord.renew();

            dataBlock.setDoubleValue(0, msgTime / 1000.);
            dataBlock.setDoubleValue(1, hd);
            dataBlock.setDoubleValue(2, sd);
            dataBlock.setDoubleValue(3, az);
            dataBlock.setDoubleValue(4, incl);
            dataBlock.setDoubleValue(5, ht);

            // update latest record and send event
            latestRecord = dataBlock;
            latestRecordTime = System.currentTimeMillis();
            eventHandler.publish(new DataEvent(latestRecordTime, TruPulseOutput.this, dataBlock));
        }
        catch(IOException e)
        {
            if (sendData)
                TruPulseSensor.log.error("Unable to parse TruPulse message", e);
        }
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


    protected void start(ICommProvider<?> commProvider)
    {
        if (sendData)
            return;
        
        sendData = true;
        
        // connect to data stream
        try
        {
            msgReader = new BufferedReader(new InputStreamReader(commProvider.getInputStream()));
            TruPulseSensor.log.info("Connected to TruPulse data stream");
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error while initializing communications ", e);
        }
        
        // start main measurement thread
        readThread = new Thread(new Runnable()
        {
            public void run()
            {
                while (sendData)
                {
                    pollAndSendMeasurement();
                }
            }
        });
        readThread.start();
    }


    protected void stop()
    {
        sendData = false;
        
        if (readThread != null)
        {
            readThread.interrupt();
            readThread = null;
            msgReader = null;
        }
    }


    @Override
    public double getAverageSamplingPeriod()
    {
        return Double.NaN;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return lrfData;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return dataEncoding;
    }
}
