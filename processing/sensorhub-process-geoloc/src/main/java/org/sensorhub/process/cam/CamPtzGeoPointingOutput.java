/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.process.cam;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.IDataProducerModule;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.sensor.ISensorControlInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.impl.SensorHub;
import org.vast.data.TextEncodingImpl;
import org.vast.ows.OWSException;
import org.vast.ows.sps.DescribeTaskingRequest;
import org.vast.ows.sps.DescribeTaskingResponse;
import org.vast.ows.sps.SPSUtils;
import org.vast.ows.sps.SubmitRequest;
import org.vast.swe.SWEData;
import org.vast.swe.SWEHelper;


/**
 * <p>
 * Camera PTZ commmand Output
 * </p>
 *
 * <p>Copyright (c) 2015 Sensia Software LLC</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Aug 15, 2015
 */
public class CamPtzGeoPointingOutput implements IStreamingDataInterface
{
    CamPtzGeoPointingProcess parentProcess;
    IEventHandler eventHandler;
    DataComponent outputDef;
    DataEncoding outputEncoding;
    long latestRecordTime = Long.MIN_VALUE;
    DataBlock latestRecord;
    double samplingPeriod = 10.0;
    
    // command stuff for temporary HACK
    boolean useSps;
    SPSUtils utils = new SPSUtils();
    DescribeTaskingResponse dtResp;
    ISensorControlInterface controlInput;
    int commandID;
    DataBlock commandData;
    

    public CamPtzGeoPointingOutput(CamPtzGeoPointingProcess parentProcess)
    {
        this.parentProcess = parentProcess;
        
        // create output structure
        SWEHelper fac = new SWEHelper();
        DataRecord rec = fac.newDataRecord();
        rec.setName(getName());
        rec.addField("time", fac.newTimeStampIsoUTC());
        rec.addField("pan", fac.newQuantity(SWEHelper.getPropertyUri("Pan"), "Pan", null, "deg", DataType.FLOAT));
        rec.addField("tilt", fac.newQuantity(SWEHelper.getPropertyUri("Tilt"), "Tilt", null, "deg", DataType.FLOAT));
        rec.addField("zoom", fac.newCount(SWEHelper.getPropertyUri("ZoomFactor"), "Zoom Factor", null, DataType.SHORT));
        this.outputDef = rec;        
        this.outputEncoding = fac.newTextEncoding();
        
        // obtain an event handler for this output
        String moduleID = parentProcess.getLocalID();
        String topic = getName();
        this.eventHandler = SensorHub.getInstance().getEventBus().registerProducer(moduleID, topic);
    }
    
    
    protected void start() throws SensorHubException
    {
        if (parentProcess.getConfiguration().camSpsEndpointUrl != null)
            initSps();
        else
            initSensorControl();
    }
    
    
    private void initSps() throws SensorHubException
    {
        try
        {
            // connect to SPS server and retrieve tasking parameters
            DescribeTaskingRequest dtReq = new DescribeTaskingRequest();
            dtReq.setVersion("2.0");
            dtReq.setPostServer(parentProcess.getConfiguration().camSpsEndpointUrl);
            dtReq.setProcedureID(parentProcess.getConfiguration().camSensorUID);
            utils.writeXMLQuery(System.out, dtReq);
            dtResp = utils.sendRequest(dtReq, false);
            useSps = true;
        }
        catch (OWSException e)
        {
            throw new SensorHubException("Error while retrieving tasking message definition from SPS", e);
        }
    }
    
    
    private void initSensorControl() throws SensorHubException
    {
        String sensorUID = parentProcess.getConfiguration().camSensorUID;
        
        try
        {
            ISensorModule<?> camSensor = SensorHub.getInstance().getSensorManager().findSensor(sensorUID);
            if (camSensor == null)
                throw new SensorHubException("Cannot find sensor " + sensorUID);
            controlInput = camSensor.getCommandInputs().get("ptzControl");
            if (controlInput == null)
                throw new SensorHubException("No PTZ control interface available");
        }
        catch (Exception e)
        {
            throw new SensorHubException("Cannot connect to PTZ camera " + sensorUID, e);
        }
    }
    
    
    protected void sendPtz(double time, double pan, double tilt, double zoom)
    {
        // create and populate datablock
        DataBlock dataBlock;
        if (latestRecord != null)
        {
            dataBlock = latestRecord.renew();
            samplingPeriod = time - latestRecord.getDoubleValue(0);
        }
        else
            dataBlock = outputDef.createDataBlock();
        
        dataBlock.setDoubleValue(0, time);
        dataBlock.setDoubleValue(1, pan);
        dataBlock.setDoubleValue(2, tilt);
        dataBlock.setDoubleValue(3, zoom);
                    
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new DataEvent(latestRecordTime, this, dataBlock));
        
        // HACK to also send the command to camera directly
        if (useSps)
            sendToSps(pan, tilt, zoom);
        else
            sendToSensor(pan, tilt, zoom);
    }
    
    
    private void sendToSensor(double pan, double tilt, double zoom)
    {
        if (commandData == null)
        {
            commandData = controlInput.getCommandDescription().createDataBlock();
            commandID = ((DataChoice)controlInput.getCommandDescription()).getComponentIndex("ptzPos");
        }
        
        try
        {
            commandData.setIntValue(0, commandID);
            commandData.setDoubleValue(1, pan);
            commandData.setDoubleValue(2, tilt);
            commandData.setDoubleValue(3, zoom);
            controlInput.execCommand(commandData);
        }
        catch (Exception e)
        {
            CamPtzGeoPointingProcess.log.error("Error while sending command to PTZ camera", e);
        }
    }
    
    
    private void sendToSps(double pan, double tilt, double zoom)
    {
        try
        {
            SubmitRequest subReq;
            
            SWEData taskParams = new SWEData();
            taskParams.setElementType(dtResp.getTaskingParameters());
            taskParams.setEncoding(new TextEncodingImpl());
            DataChoice ptzParams = (DataChoice)dtResp.getTaskingParameters().copy();
            taskParams.clearData();
            
            // generate pan command
            ptzParams.renewDataBlock();
            ptzParams.setSelectedItem("pan");
            ptzParams.getComponent("pan").getData().setDoubleValue(pan);
            taskParams.addData(ptzParams.getData());
            
            // send request
            subReq = new SubmitRequest();
            subReq.setVersion("2.0");
            subReq.setPostServer(parentProcess.getConfiguration().camSpsEndpointUrl);
            subReq.setProcedureID(parentProcess.getConfiguration().camSensorUID);
            subReq.setParameters(taskParams);
            utils.writeXMLQuery(System.out, subReq);
            utils.sendRequest(subReq, false);
            
            Thread.sleep(500L);
            
            // generate tilt command
            taskParams.clearData();
            ptzParams.renewDataBlock();
            ptzParams.setSelectedItem("tilt");
            ptzParams.getComponent("tilt").getData().setDoubleValue(Math.max(0.0, tilt));
            taskParams.addData(ptzParams.getData());
            
            // send request
            subReq = new SubmitRequest();
            subReq.setVersion("2.0");
            subReq.setPostServer(parentProcess.getConfiguration().camSpsEndpointUrl);
            subReq.setProcedureID(parentProcess.getConfiguration().camSensorUID);
            subReq.setParameters(taskParams);
            utils.writeXMLQuery(System.out, subReq);
            utils.sendRequest(subReq, false);
        }
        catch (Exception e)
        {
            CamPtzGeoPointingProcess.log.error("Error while sending tasking request to SPS", e);
        }
    }


    @Override
    public IDataProducerModule<?> getParentModule()
    {
        return parentProcess;
    }


    @Override
    public String getName()
    {
        return "targetLocation";
    }


    @Override
    public boolean isEnabled()
    {
        return true;
    }
    
    
    @Override
    public DataComponent getRecordDescription()
    {
        return outputDef;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return outputEncoding;
    }


    @Override
    public DataBlock getLatestRecord()
    {
        return latestRecord;
    }


    @Override
    public long getLatestRecordTime()
    {
        return latestRecordTime;
    }


    @Override
    public double getAverageSamplingPeriod()
    {
        return samplingPeriod;
    }
    
    
    @Override
    public void registerListener(IEventListener listener)
    {
        eventHandler.registerListener(listener);
    }


    @Override
    public void unregisterListener(IEventListener listener)
    {
        eventHandler.unregisterListener(listener);
    }
}
