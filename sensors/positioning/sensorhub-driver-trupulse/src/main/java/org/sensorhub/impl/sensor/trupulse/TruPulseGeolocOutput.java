/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2019 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.trupulse;

import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.event.Event;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.processing.DataStreamSource;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.process.geoloc.LosToTarget;
import org.vast.process.DataConnection;
import org.vast.process.ProcessException;
import org.vast.swe.SWEHelper;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;


/**
 * <p>
 * Output class to expose target geoloc process output on sensor object
 * </p>
 *
 * @author Alex Robin
 * @since Apr 10, 2019
 */
public class TruPulseGeolocOutput extends AbstractSensorOutput<TruPulseSensor> implements IEventListener
{
    private static String OUTPUT_NAME = "targetLoc";
    
    DataStreamSource locationSource;
    
    LosToTarget targetGeolocProcess;
    DataComponent observerLocInput;
    DataComponent losDirInput;
    DataComponent targetDistInput;
    DataComponent targetLocOutput;
    DataComponent targetLocData; // output descriptor
    
    
    TruPulseGeolocOutput(TruPulseWithGeolocSensor parentSensor)
    {
        super(OUTPUT_NAME, parentSensor);
        
        var config = (TruPulseWithGeolocConfig)parentSensor.getConfiguration();
        
        try
        {
            // setup los to target process
            targetGeolocProcess = new LosToTarget();
            observerLocInput = targetGeolocProcess.getObserverLocationInput();
            losDirInput = targetGeolocProcess.getLosDirectionInput();
            targetDistInput = targetGeolocProcess.getTargetDistanceInput();
            targetLocOutput = targetGeolocProcess.getTargetLocationOutput();
            targetGeolocProcess.init();
            
            // setup stream source process to collect data from location datastream
            locationSource = new DataStreamSource();
            locationSource.setParentHub(parentSensor.getParentHub());
            locationSource.init();
            var params = locationSource.getParameterList();
            params.getComponent(DataStreamSource.SYSTEM_UID_PARAM).getData().setStringValue(config.locationSourceUID);
            params.getComponent(DataStreamSource.OUTPUT_NAME_PARAM).getData().setStringValue(config.locationOutputName);
            locationSource.notifyParamChange();
            
            //TODO find location vector in source output
            var locationData = locationSource.getOutputList().getComponent(0).getComponent(1);
        
            // create connection
            var cxn = new DataConnection() {
                @Override
                public void publishData() throws InterruptedException {
                    super.publishData();
                    synchronized (targetGeolocProcess) {
                        transferData(false);
                    }
                }
            };
            locationSource.connect(locationData, cxn);
            targetGeolocProcess.connect(observerLocInput, cxn);
            
            // init output info
            SWEHelper swe = new SWEHelper();
            targetLocData = swe.createRecord()
                .addField("time", swe.createTime().asPhenomenonTimeIsoUTC())
                .addField("location", targetLocOutput.copy())
                .build();
        }
        catch (ProcessException e)
        {
            throw new IllegalStateException("Error initializing processing components");
        }
    }


    @Override
    public void handleEvent(Event e)
    {
        if (e instanceof DataEvent)
        {
            var lrfData = ((DataEvent) e).getRecords()[0];
            
            synchronized (targetGeolocProcess)
            {
                try
                {
                    targetDistInput.getData().setDoubleValue(lrfData.getDoubleValue(2)); // range
                    losDirInput.getData().setDoubleValue(0, lrfData.getDoubleValue(3)); // azimuth
                    losDirInput.getData().setDoubleValue(1, lrfData.getDoubleValue(4)); // elevation
                    targetGeolocProcess.execute();
                    
                    // create or renew datablock
                    DataBlock dataBlock;
                    if (latestRecord == null)
                        dataBlock = targetLocData.createDataBlock();
                    else
                        dataBlock = latestRecord.renew();
                    
                    // copy timestamp and computed target location to output
                    dataBlock.setDoubleValue(0, lrfData.getDoubleValue(0)); // timestamp
                    dataBlock.setDoubleValue(1, targetLocOutput.getData().getDoubleValue(0)); // lat
                    dataBlock.setDoubleValue(2, targetLocOutput.getData().getDoubleValue(1)); // lon
                    dataBlock.setDoubleValue(3, targetLocOutput.getData().getDoubleValue(2)); // alt
                    
                    // update latest record and send event
                    latestRecord = dataBlock;
                    latestRecordTime = System.currentTimeMillis();
                    eventHandler.publish(new DataEvent(latestRecordTime, TruPulseGeolocOutput.this, dataBlock));
                }
                catch (ProcessException e1)
                {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        }
        
    }
    
    
    protected void start() throws SensorException
    {
        try {
            locationSource.start(e -> {
                getLogger().error("Error receiving own location data", e);
            });
        }
        catch (ProcessException e) {
          throw new SensorException("Error starting location source listening process", e);
        }
        parentSensor.rangeOutput.registerListener(this);
    }
    
    
    protected void stop()
    {
        parentSensor.rangeOutput.unregisterListener(this);
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return targetLocData;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return parentSensor.rangeOutput.getRecommendedEncoding();
    }


    @Override
    public double getAverageSamplingPeriod()
    {
        return parentSensor.rangeOutput.getAverageSamplingPeriod();
    }

}
