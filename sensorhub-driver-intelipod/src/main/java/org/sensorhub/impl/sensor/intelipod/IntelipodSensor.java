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
import gnu.io.SerialPort;
import net.opengis.sensorml.v20.IdentifierList;
import net.opengis.sensorml.v20.Term;

import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.intelipod.IntelipodOutput;
import org.sensorhub.impl.sensor.intelipod.IntelipodConfig;
import org.vast.sensorML.SMLFactory;
import org.vast.swe.SWEHelper;
import ch.qos.logback.core.Context;


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
    BufferedReader reader;
    BufferedWriter writer;
    BufferedReader input;
    OutputStream output;
    String inputLine = null;
    String modelNumber = null;
    String serialNumber = null;
    SerialPort serialPort;
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
                getLogger().info("Connected to Vaisala data stream");

                
                /************************* Get Device Address *************************/
                //dataOut.flush();
//                try {
//					Thread.sleep(1000);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
                
                // get line and split to check its length
//                inputLine = dataIn.readLine();
//                System.out.println(inputLine);
//                inputLine = null;
                /***********************************************************************/
                
            }
            catch (IOException e)
            {
                throw new RuntimeException("Error while initializing communications ", e);
            }
        }
        
//        CommPortIdentifier portId = null;
//        Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();
//        
//        //First, Find an instance of serial port as set in PORT_NAMES.
//        while (portEnum.hasMoreElements()) {
//            CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
//            System.out.println("portId: " + currPortId);
//            for (String portName : PORT_NAMES) {
//            	System.out.println("portName: " + portName);
//                if (currPortId.getName().equals(portName)) {
//                    portId = currPortId;
//                    break;
//                }
//            }
//        }
//        
//        if (portId == null) {
//            System.out.println("Could not find COM port.");
//            return;
//        }
//        
//        try {
//            // open serial port, and use class name for the appName.
//            serialPort = (SerialPort) portId.open(this.getClass().getName(), TIME_OUT);
//
//            // set port parameters
//            serialPort.setSerialPortParams(DATA_RATE,
//                    SerialPort.DATABITS_8,
//                    SerialPort.STOPBITS_1,
//                    SerialPort.PARITY_NONE);
//
//            // open the streams
//            input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
//            output = serialPort.getOutputStream();
//            
//
//            // add event listeners
//            serialPort.addEventListener(this);
//            serialPort.notifyOnDataAvailable(true);
//        } catch (Exception e) {
//            System.err.println(e.toString());
//        }
        
        
        
        
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
        	System.out.println("Updating Sensor Description...");
        	// set identifiers in SensorML
            SMLFactory smlFac = new SMLFactory();            

            if (!sensorDescription.isSetDescription())
                sensorDescription.setDescription("Venti Intelipod Sensor");
          
            IdentifierList identifierList = smlFac.newIdentifierList();
            sensorDescription.addIdentification(identifierList);

            Term term;            
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("Manufacturer"));
            term.setLabel("Manufacturer Name");
            term.setValue("Venti");
            identifierList.addIdentifier2(term);
            
            if (modelNumber != null)
            {
                term = smlFac.newTerm();
                term.setDefinition(SWEHelper.getPropertyUri("ModelNumber"));
                term.setLabel("Model Number");
                term.setValue(modelNumber);
                identifierList.addIdentifier2(term);
            }
            
            if (serialNumber != null)
            {
                term = smlFac.newTerm();
                term.setDefinition(SWEHelper.getPropertyUri("SerialNumber"));
                term.setLabel("Serial Number");
                term.setValue(serialNumber);
                identifierList.addIdentifier2(term);
            }
            
            // Long Name
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("LongName"));
            term.setLabel("Long Name");
            term.setValue("Intelipod " + modelNumber + " #" + serialNumber);
            identifierList.addIdentifier2(term);

            // Short Name
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("ShortName"));
            term.setLabel("Short Name");
            term.setValue("Intelipod " + modelNumber);
            identifierList.addIdentifier2(term);
        }
        System.out.println("Done Updating Sensor Description");
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
                
                reader = null;
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
        
        if (reader != null)
        {
            try { reader.close(); }
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

    
//    public synchronized void close() {
//        if (serialPort != null) {
//            serialPort.removeEventListener();
//            serialPort.close();
//        }
//    }

    
//	@Override
//	public void serialEvent(SerialPortEvent oEvent) {
//        if (started & oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
//            try
//            {
//                String inputLine = input.readLine();
//                System.out.println(inputLine);
//                
//                // Pick up here - parse inputLine
//            }
//            
//            catch (Exception e)
//            {
//                System.err.println(e.toString());
//            }
//        }
//	}
}