/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.trek1000;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.trek1000.Triangulation.Vec3d;


/**
 * <p>
 * Implementation of DecaWave's Trek1000 sensor.
 * </p>
 * 
 * @author Joshua Wolfe <developer.wolfe@gmail.com>
 * @since March 24, 2017
 */
public class Trek1000Sensor extends AbstractSensorModule<Trek1000Config>
{
    ICommProvider<?> commProvider;
    BufferedReader reader;
    volatile boolean started;

    RangeOutput rangeOutput;
    LocalPosOutput localPosOutput;
    //GeoPosOutput geoPosOutput;
    
    Triangulation trilatAlgo = new Triangulation();
    Vec3d[] anchorLocations = new Vec3d[4];
    int[] ranges = new int[4]; // in mm
    Vec3d solution = new Vec3d(0.0, 0.0, 0.0);
    

    public Trek1000Sensor()
    {
    }


    @Override
    public void init() throws SensorHubException
    {
        super.init();

        // generate identifiers
        generateUniqueID("urn:osh:sensor:trek1000:", config.serialNumber);
        generateXmlID("TREK1000_", config.serialNumber);
        
        // init anchor positions
        if (config.anchorLocations == null || config.anchorLocations.size() < 3)
            throw new SensorException("At least 3 anchor locations must be specified");            
            
        for (int i=0; i<3; i++) 
        {
            LLALocation configLocation = config.anchorLocations.get(i);
            anchorLocations[i] = new Vec3d(configLocation.lon,
                                           configLocation.lat,
                                           configLocation.alt);
        }

        // init main data interfaces
        rangeOutput = new RangeOutput(this);
        addOutput(rangeOutput, false);
        rangeOutput.init();
        
        localPosOutput = new LocalPosOutput(this);
        addOutput(localPosOutput, false);
        localPosOutput.init();
        
        /*geoPosOutput = new GeoPosOutput(this);
        addOutput(geoPosOutput, false);
        geoPosOutput.init();*/
    }


    @Override
    public void start() throws SensorException
    {
        if (started)
            return;
        
        // init comm provider
        if (commProvider == null)
        {
            // we need to recreate comm provider here because it can be changed by UI
            try
            {
                if (config.commSettings == null)
                    throw new SensorException("No communication settings specified");
                
                commProvider = config.commSettings.getProvider();
                commProvider.start();
            }
            catch (Exception e)
            {
                commProvider = null;
                throw new SensorException("Cannot start comm provider", e);
            }
        }
        
        // connect to data stream
        try
        {
            reader = new BufferedReader(new InputStreamReader(commProvider.getInputStream(), StandardCharsets.US_ASCII));
            getLogger().info("Connected to TREK1000 data stream");
        }
        catch (IOException e)
        {
            throw new SensorException("Error while initializing communications ", e);
        }
        
        // start main measurement thread
        Thread t = new Thread(new Runnable()
        {
            public void run()
            {
                while (started)
                {
                    pollAndSendMeasurement();
                }
                
                reader = null;
            }
        });
        
        started = true;
        t.start();
    }
    
    
    private void pollAndSendMeasurement()
    {
        String msg = null;
        
        try
        {
            // read next message
            msg = reader.readLine();
            long msgTime = System.currentTimeMillis();
            
            // if null, it's EOF
            if (msg == null)
                return;            
            
            getLogger().trace("Received message: {}", msg);
            
            // parse message
            String[] parts = msg.trim().split(" ");
            String msgType = parts[0];            
            if (msgType.trim().equals("mc"))
            {
                // send range data
                for (int i=0; i<4; i++)
                {
                    int range = Integer.parseInt(parts[2+i].trim(), 16);
                    rangeOutput.sendData(msgTime, "A"+i, "T0", range*0.001);
                    ranges[i] = range;
                }
                
                // send xyz pos
                trilatAlgo.getLocation(solution, 0, anchorLocations, ranges);
                localPosOutput.sendData(msgTime, "T0", solution.x, solution.y, solution.z);
                
                // send geo pos
                
            }
        }
        catch (EOFException e)
        {
            // do nothing
            // this happens when reader is closed in stop() method
            started = false;
        }
        catch (Exception e)
        {
            getLogger().error("Cannot parse TREK1000 message: " + msg, e);
        }
    }
    

    @Override
    public void stop() throws SensorHubException
    {
        started = false;
        
        if (reader != null)
        {
            try { reader.close(); }
            catch (IOException e) { }
        }
        
        if (commProvider != null)
        {
            commProvider.stop();
            commProvider = null;
        }
    }


    @Override
    public boolean isConnected()
    {
        return (commProvider != null);
    }
}
