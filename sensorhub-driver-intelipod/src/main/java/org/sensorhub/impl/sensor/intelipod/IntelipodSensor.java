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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.vast.sensorML.SMLHelper;
import org.vast.swe.SWEHelper;


/**
 * <p>
 * Driver implementation supporting the Laser Technology TruPulse 360 Laser Rangefinder.
 * The TruPulse 360 includes GeoSpatial Orientation ("azimuth"), as well as inclination
 * and direct distance. When combined with a sensor that measures GPS location of the 
 * TruPulse sensor, one can calculate the geospatial position of the target.
 * </p>
 *
 * @author Mike Botts <mike.botts@botts-inc.com>
 * @since June 8, 2015
 */
public class IntelipodSensor extends AbstractSensorModule<IntelipodConfig>
{
    //static final Logger log = LoggerFactory.getLogger(IntelipodSensor.class);
    
    ICommProvider<?> commProvider;
    IntelipodOutput intelipodOut;
    BufferedReader dataIn;
    BufferedWriter dataOut;
    BufferedWriter writer;
    BufferedReader input;
    OutputStream output;
    String inputLine = null;
    String serialNumber = null;
    boolean started = false;
    
    
    public IntelipodSensor()
    {        
    }
    
    
    @Override
    public void init() throws SensorHubException
    {
    	//System.out.println("Initializing...");
        super.init();
        
        // init main data interface
        intelipodOut = new IntelipodOutput(this);
        addOutput(intelipodOut, false);
        
        // init comm provider
        if (commProvider == null)
        {
            // we need to recreate comm provider here because it can be changed by UI
            if (config.commSettings == null)
                throw new SensorHubException("No communication settings specified");
            commProvider = config.commSettings.getProvider();
            commProvider.start();
            
            // connect to comm data streams
            try
            {
            	dataIn = new BufferedReader(new InputStreamReader(commProvider.getInputStream()));
                //dataOut = new BufferedWriter(new OutputStreamWriter(commProvider.getOutputStream()));
                getLogger().info("Connected to Intelipod data stream");
                
            }
            catch (IOException e)
            {
                throw new RuntimeException("Error while initializing communications ", e);
            }
        }

        // generate identifiers: use serial number from config or first characters of local ID
        generateUniqueID("urn:osh:intelipod:", config.serialNumber);
        generateXmlID("INTELIPOD_", config.serialNumber);
        
        intelipodOut.init();
    }


    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
            super.updateSensorDescription();
            SMLHelper helper = new SMLHelper(sensorDescription);
            
            // set identifiers in SensorML
            if (!sensorDescription.isSetDescription())
                sensorDescription.setDescription("Venti Intelipod Sensor");
                        
            helper.addIdentifier("Manufacturer Name", SWEHelper.getPropertyUri("Manufacturer"), "Venti");
            
            if (config.serialNumber != null)
            {
                helper.addIdentifier("Short Name", SWEHelper.getPropertyUri("ShortName"), "Intelipod " + config.serialNumber);
                helper.addIdentifier("Serial Number", SWEHelper.getPropertyUri("SerialNumber"), config.serialNumber);
            }
        }
    }
    
    
    @Override
    public void start() throws SensorHubException
    {
    	//System.out.println("Starting...");

    	// start main measurement thread
        Thread t = new Thread(new Runnable()
        {
            public void run()
            {
                while (started)
                {
                    getMeasurement();
                }
                
                dataIn = null;
            }
        });
        
        started = true;
        t.start();
    }
    

    @Override
    public void stop() throws SensorHubException
    {
    	//close();
    	started = false;
        
        if (dataIn != null)
        {
            try { dataIn.close(); }
            catch (IOException e) { }
        }
        
        if (commProvider != null)
        {
        	try {
  				commProvider.stop();
  			} catch (SensorHubException e) {
  				// TODO Auto-generated catch block
  				e.printStackTrace();
  			}
        	commProvider = null;
        }
    }
    
    
    private void getMeasurement()
    {	
    	String inputLine = null;
    	try
    	{
            inputLine = dataIn.readLine();
            //System.out.println("Message: " + inputLine);
            intelipodOut.postMeasurement(inputLine);
		}
    	catch (Exception e)
    	{
			e.printStackTrace();
		}
    }
    

    @Override
    public void cleanup() throws SensorHubException
    {
    	
    }
    
    
    @Override
    public boolean isConnected()
    {
        return true;
    }
}