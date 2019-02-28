package org.sensorhub.impl.sensor.vaisala;

import java.io.IOException;

import net.opengis.sensorml.v20.IdentifierList;
import net.opengis.sensorml.v20.Term;
import net.opengis.swe.v20.DataComponent;

import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.vaisala.VaisalaWeatherConfig;
import org.sensorhub.impl.sensor.vaisala.VaisalaWeatherCompositeOutput;
import org.vast.sensorML.SMLFactory;
import org.vast.swe.SWEHelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class VaisalaWeatherSensor extends AbstractSensorModule<VaisalaWeatherConfig>
{ 
    ICommProvider<?> commProvider;
    DataComponent weatherData;
    BufferedReader dataIn;
    BufferedWriter dataOut;
    VaisalaWeatherCompositeOutput compOut;
    VaisalaWeatherWindOutput windOut;
    VaisalaWeatherPrecipOutput precipOut;
    VaisalaWeatherPTUOutput ptuOut;
    VaisalaWeatherSupervisorOutput supOut;
    String modelNumber;
    String serialNumber = null;
    String inputLine = null;
    String deviceAddress = null;
    String[] checkAddr = null;
    String[] inputTemp = null;
    String compSupMesSettings = null;
    String indSupMesSettings = null;
    String compWindMesSettings = null;
    String indWindMesSettings = null;
    String compPTUMesSettings = null;
    String indPTUMesSettings = null;
    String compPrecipMesSettings = null;
    String indPrecipMesSettings = null;
    String[] checkMesSettings = null;
    int cnt;
    volatile boolean started;
    public final static char CR = (char) 0x0D;
    public final static char LF = (char) 0x0A;
    public final static String CRLF = "" + CR + LF;
    
    /******************** Settings Messages **************************/
    private String commsSettingsInit = "M=P,T=0,C=2,I=0,B=19200";
    private String commsSettingsAutoASCII = "M=A,I=1";
    /*****************************************************************/
    
    /******************* Editable Sensor Settings ********************/
    private String supervisorSettings1 = "R=0000000000100000";
    private String supervisorSettings2 = "I=15,S=N,H=N";
    private String windSettings1 = "R=0000000011111100";
    private String windSettings2 = "I=1,A=12,U=S,D=0,N=W,F=2";
    private String ptuSettings1 = "R=0000000011110000";
    private String ptuSettings2 = "I=60,P=I,T=F";
    private String precipSettings1 = "R=0000000010110111";
    private String precipSettings2 = "I=60,U=I,S=I,M=T,Z=A";
    /*****************************************************************/
    
    public VaisalaWeatherSensor()
    {        
    }
    
    
    @Override
    public void init() throws SensorHubException
    {
        super.init();
        
        // create data interfaces
        compOut = new VaisalaWeatherCompositeOutput(this);
        addOutput(compOut, false);
        
        windOut = new VaisalaWeatherWindOutput(this);
        addOutput(windOut, false);
        
        ptuOut = new VaisalaWeatherPTUOutput(this);
        addOutput(ptuOut, false);
        
        precipOut = new VaisalaWeatherPrecipOutput(this);
        addOutput(precipOut, false);

        supOut = new VaisalaWeatherSupervisorOutput(this);
        addOutput(supOut, false);
        
        //System.out.println("Initializing...");
        
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
                dataOut = new BufferedWriter(new OutputStreamWriter(commProvider.getOutputStream()));
                getLogger().info("Connected to Vaisala data stream");

                
                /************************* Get Device Address *************************/
                //System.out.println(CRLF + "Getting Device Address...");
                dataOut.write("?" + CRLF);
                dataOut.flush();
                try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                
                // get line and split to check its length
                inputLine = dataIn.readLine();
                checkAddr = inputLine.split(",");
                
                // if input line length is other than 1,
                // it must be an automatic data message.
                // so keep getting lines until length = 1
                while (!(checkAddr.length == 1 && checkAddr[0].length() == 1))
                {
                	inputLine = dataIn.readLine();
                	checkAddr = inputLine.split(",");
                }
                
                // save dataAddress to use in commands
                deviceAddress = inputLine;
                System.out.println(CRLF + "Device Address = " + deviceAddress);
                inputLine = null;
                /***********************************************************************/
                
                
                /***************** Configure Comm Protocol to ASCII Poll ***************/
                //System.out.println(CRLF + "Configuring Comm Protocol...");
                dataOut.write(deviceAddress + "XU," + commsSettingsInit + CRLF);
                dataOut.flush();
                try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                
                // get input lines until pipe is clear, indicating polling mode is on 
                inputLine = dataIn.readLine();
                
                // need dataIn.ready() = false to ensure polling mode is on 
                while(dataIn.ready())
                {
                	inputLine = dataIn.readLine();
                }
                //System.out.println("Changed Comm Settings: " + inputLine);
                // should be in polling mode at this point
                inputLine = null;
                /***********************************************************************/
                
                /**************************** Get Model Number *************************/
                //System.out.println(CRLF + "Getting Model Number...");
                dataOut.write(deviceAddress + "XU" + CRLF);
                dataOut.flush();
                try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                inputLine = dataIn.readLine();
                String[] split = inputLine.split(",");
                modelNumber = split[11].replaceAll("N=", "");
                System.out.println("Model Number: " + modelNumber);
                inputLine = null;
                /***********************************************************************/
                
                /******************** Configure Supervisor Settings ********************/
                //System.out.println(CRLF + "Configuring Supervisor Settings...");
                dataOut.write(deviceAddress + "SU," + supervisorSettings1 + CRLF);
                dataOut.flush();
                try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                inputLine = dataIn.readLine();
                //System.out.println("Sup Message Settings: " + inputLine);
                
                checkMesSettings = inputLine.split(",");
                compSupMesSettings = checkMesSettings[1];
                indSupMesSettings = compSupMesSettings.substring(2, 10);
                
                //System.out.println("Ind Sup Message Settings: " + indSupMesSettings);
                
                if (indSupMesSettings.length() != 8 || !indSupMesSettings.replaceAll("[01]", "").isEmpty())
                	System.err.println("Unrecognized Supervisor Message Setting");
                
                compSupMesSettings = compSupMesSettings.substring(compSupMesSettings.length() - 8);
                //System.out.println("Comp Sup Message Settings: " + compSupMesSettings);
                
                if (compSupMesSettings.length() != 8 || !compSupMesSettings.replaceAll("[01]", "").isEmpty())
                	System.err.println("Unrecognized Supervisor Message Setting");
                checkMesSettings = null;
                
                inputLine = null;
                dataOut.write(deviceAddress + "SU," + supervisorSettings2 + CRLF);
                dataOut.flush();
                try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                inputLine = dataIn.readLine();
                inputLine = null;
                /***********************************************************************/
                
                //System.out.println(CRLF + "Configuring Wind Settings...");
                /************************ Configure Wind Settings **********************/
                dataOut.write(deviceAddress + "WU," + windSettings1 + CRLF);
                dataOut.flush();
                try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                inputLine = dataIn.readLine();
                //System.out.println("Wind Message Settings: " + inputLine);
                
                checkMesSettings = inputLine.split(",");
                compWindMesSettings = checkMesSettings[1];
                indWindMesSettings = compWindMesSettings.substring(2, 10);
                
                //System.out.println("Ind Wind Message Settings: " + indWindMesSettings);
                
                if (indWindMesSettings.length() != 8 || !indWindMesSettings.replaceAll("[01]", "").isEmpty())
                	System.err.println("Unrecognized Wind Message Setting");
                
                compWindMesSettings = compWindMesSettings.substring(compWindMesSettings.length() - 8);
                //System.out.println("Comp Wind Message Settings: " + compWindMesSettings);
                
                if (compSupMesSettings.length() != 8 || !compSupMesSettings.replaceAll("[01]", "").isEmpty())
                	System.err.println("Unrecognized Wind Message Setting");
                checkMesSettings = null;
                
                inputLine = null;
                dataOut.write(deviceAddress + "WU," + windSettings2 + CRLF);
                dataOut.flush();
                try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                inputLine = dataIn.readLine();
                inputLine = null;
                /************************************************************************/
                
                /************************ Configure PTU Settings ************************/
                //System.out.println(CRLF + "Configuring PTU Settings...");
                dataOut.write(deviceAddress + "TU," + ptuSettings1 + CRLF);
                dataOut.flush();
                try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                inputLine = dataIn.readLine();
                //System.out.println("PTU Message Settings: " + inputLine);
                
                
                checkMesSettings = inputLine.split(",");
                compPTUMesSettings = checkMesSettings[1];
                indPTUMesSettings = compPTUMesSettings.substring(2, 10);
                
                //System.out.println("Ind PTU Message Settings: " + indPTUMesSettings);
                
                if (indPTUMesSettings.length() != 8 || !indPTUMesSettings.replaceAll("[01]", "").isEmpty())
                	System.err.println("Unrecognized Wind Message Setting");
                
                compPTUMesSettings = compPTUMesSettings.substring(compPTUMesSettings.length() - 8);
                //System.out.println("Comp PTU Message Settings: " + compPTUMesSettings);
                
                if (compPTUMesSettings.length() != 8 || !compPTUMesSettings.replaceAll("[01]", "").isEmpty())
                	System.err.println("Unrecognized PTU Message Setting");
                checkMesSettings = null;
                
                
                inputLine = null;
                dataOut.write(deviceAddress + "TU," + ptuSettings2 + CRLF);
                dataOut.flush();
                try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                inputLine = dataIn.readLine();
                inputLine = null;
                /***********************************************************************/
                
                /************************ Configure Precip Settings ********************/
                //System.out.println(CRLF + "Configuring Precip Settings...");
                dataOut.write(deviceAddress + "RU," + precipSettings1 + CRLF);
                dataOut.flush();
                try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                inputLine = dataIn.readLine();
                //System.out.println("Precip Message Settings: " + inputLine);
                
                
                checkMesSettings = inputLine.split(",");
                compPrecipMesSettings = checkMesSettings[1];
                indPrecipMesSettings = compPrecipMesSettings.substring(2, 10);
                
                //System.out.println("Ind Precip Message Settings: " + indPrecipMesSettings);
                
                if (indPrecipMesSettings.length() != 8 || !indPrecipMesSettings.replaceAll("[01]", "").isEmpty())
                	System.err.println("Unrecognized Precip Message Setting");
                
                compPrecipMesSettings = compPrecipMesSettings.substring(compPrecipMesSettings.length() - 8);
                //System.out.println("Comp Precip Message Settings: " + compPrecipMesSettings);
                
                if (compPrecipMesSettings.length() != 8 || !compPrecipMesSettings.replaceAll("[01]", "").isEmpty())
                	System.err.println("Unrecognized Precip Message Setting");
                checkMesSettings = null;
                
                
                inputLine = null;
                
                dataOut.write(deviceAddress + "RU," + precipSettings2 + CRLF);
                dataOut.flush();
                try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                inputLine = dataIn.readLine();
                inputLine = null;
                /***********************************************************************/
                
                /***************** Configure Comm Protocol to Auto ASCII ***************/
                //System.out.println(CRLF + "Configuring Comm Protocol Settings...");
                dataOut.write(deviceAddress + "XU," + commsSettingsAutoASCII + CRLF);
                dataOut.flush();
                try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                inputLine = dataIn.readLine();
                //System.out.println("Changed Comm Settings: " + inputLine);
                inputLine = null;
                /***********************************************************************/
            }
            catch (IOException e)
            {
                throw new RuntimeException("Error while initializing communications ", e);
            }
        }
        
        // generate identifiers: use serial number from config or first characters of local ID
        serialNumber = config.serialNumber;
        if (serialNumber == null)
        {
            int endIndex = Math.min(config.id.length(), 8);
            serialNumber = config.id.substring(0, endIndex);
        }
        // add unique ID based on serial number
        this.uniqueID = "urn:vaisala:" + modelNumber + ":" + serialNumber;
        this.xmlID = "VAISALA_" + modelNumber + "_" + serialNumber.toUpperCase();
        
        // execute initializations in each output class
        compOut.init(compSupMesSettings,compWindMesSettings,compPTUMesSettings,compPrecipMesSettings);
        windOut.init(indWindMesSettings);
        ptuOut.init(indPTUMesSettings);
        precipOut.init(indPrecipMesSettings);
        supOut.init(indSupMesSettings);
        //System.out.println("...Done Initializing");
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
                sensorDescription.setDescription("Vaisala Weather Transmitter " + modelNumber);
          
            IdentifierList identifierList = smlFac.newIdentifierList();
            sensorDescription.addIdentification(identifierList);

            Term term;            
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("Manufacturer"));
            term.setLabel("Manufacturer Name");
            term.setValue("Vaisala");
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
            term.setValue("Vaisala " + modelNumber + " Weather Transmitter #" + serialNumber);
            identifierList.addIdentifier2(term);

            // Short Name
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("ShortName"));
            term.setLabel("Short Name");
            term.setValue("Vaisala " + modelNumber);
            identifierList.addIdentifier2(term);
        }
        System.out.println("Done Updating Sensor Description");
    }


    private void getMeasurement()
    {	
    	String inputLine = null;
    	try {
    		
    		/******** Get Input from Serial and Split String ************/
    		//System.out.println(CRLF + "Got Measurement!");
            inputLine = dataIn.readLine();
            //System.out.println("Message: " + inputLine);
            inputTemp = inputLine.split(",");
            //System.out.println("Message Type: " + inputTemp[0].substring(1));
            
            // send message to appropriate output class to be processed
            switch (inputTemp[0].substring(1))
            {
            case "R0":
            	compOut.ParseAndSendCompMeasurement(inputLine);
            	break;
            case "R1":
            	windOut.ParseAndSendWindMeasurement(inputLine);
            	break;
            case "R2":
            	ptuOut.ParseAndSendPTUMeasurement(inputLine);
            	break;
            case "R3":
            	precipOut.ParseAndSendPrecipMeasurement(inputLine);
            	break;
            case "R5":
            	supOut.ParseAndSendSupMeasurement(inputLine);
            	break;
            default:
            	break;
            }
            /***********************************************************/
		}
    	catch (Exception e)
    	{
			e.printStackTrace();
		}
    }
    
    @Override
    public void start() throws SensorHubException
    {
    	// start main measurement thread
      Thread t = new Thread(new Runnable()
      {
          public void run()
          {
              while (started)
              {
            	  //System.out.println(CRLF + "Getting Measurement...");
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
