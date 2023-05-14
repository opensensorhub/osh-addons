/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.uas.outputs;

import java.util.HashMap;
import java.util.Map;

import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.uas.UasSensorBase;
import org.sensorhub.impl.sensor.uas.common.SyncTime;
import org.sensorhub.impl.sensor.uas.config.UasConfig;
import org.sensorhub.impl.sensor.uas.klv.DecodedSetListener;
import org.sensorhub.impl.sensor.uas.klv.UasDataLinkSet;
import org.sensorhub.misb.stanag4609.tags.Tag;
import org.sensorhub.misb.stanag4609.tags.TagRegistry;
import org.sensorhub.misb.stanag4609.tags.TagSet;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;

/**
 * Base class for sensor outputs of the two sensors in this module. Each concrete subclass will represents one or more
 * of the data elements in MISB-TS STANAG 4609 ST0601.16 UAS Metadata.
 *
 * @param <UasConfigType> A type parameter that allows us to use this output on both types of sensors that are defined
 *   in this module.
 *
 * @author Nick Garay
 * @since Oct. 6, 2020
 */
public abstract class UasOutput<UasConfigType extends UasConfig> extends AbstractSensorOutput<UasSensorBase<UasConfigType>> implements DecodedSetListener {

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;

    protected static final int MAX_NUM_TIMING_SAMPLES = 10;
    protected int setCount = 0;
    protected final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    protected final Object histogramLock = new Object();

    protected long lastSetTimeMillis = 0;
    
    /* precision time stamp tag */
    protected Tag precisionTimeStampTag = TagRegistry.getInstance().getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x02);
    

    /**
     * Constructor
     *
     * @param name         Name assigned to the output
     * @param parentSensor Sensor driver providing this output
     */
    public UasOutput(String name, UasSensorBase<UasConfigType> parentSensor) {

        super(name, parentSensor);
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering,
     * and data types.
     */
    public abstract void init();

    /**
     * Sets the data block fields in accordance to the Uas Local Set tag and corresponding object
     * translating the object to the correct form for output.
     *
     * @param dataBlock   The data block onto which the output values are being mapped
     * @param localSet    The local set that the tag is a member of   
     * @param localSetTag The local set tag id according to which the value is decoded and mapped
     * @param value       The raw value
     */
    protected abstract void setData(DataBlock dataBlock, TagSet localSet, int localSetTag, Object value);

    /**
     * Publishes the populated SWE Common Data on the event bus
     *
     * @param dataBlock The SWE Common data block to publish
     */
    protected abstract void publish(DataBlock dataBlock);

    /**
     * Check to validate data processing is still running
     *
     * @return true if worker thread is active, false otherwise
     */
    public boolean isAlive() {

        return true;
    }

    @Override
    public DataComponent getRecordDescription() {

        return dataStruct;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {

        return dataEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {

        long accumulator = 0;

        synchronized (histogramLock) {

            for (int idx = 0; idx < MAX_NUM_TIMING_SAMPLES; ++idx) {

                accumulator += timingHistogram[idx];
            }
        }

        return accumulator / (double) MAX_NUM_TIMING_SAMPLES;
    }

    @Override
    public void onSetDecoded(SyncTime syncTime, HashMap<Tag, Object> valuesMap) {

        // set to current time in loop mode to simulate a real-time stream
        if (parentSensor.getConfiguration().connection.loop)
        {
            var timeStampValue = valuesMap.get(precisionTimeStampTag);
            if (timeStampValue != null)
            {
                var precisionTimeStamp = (double)timeStampValue;
                
                // compute simulated timestamp
                var simTime = parentSensor.getSimulatedTime();
                var newPrecisionTimeStamp = simTime.getSimlatedTimeStamp(precisionTimeStamp);
                valuesMap.put(precisionTimeStampTag, newPrecisionTimeStamp);
                
                // also shift synctime so that video timestamps are computed correctly
                syncTime = new SyncTime(newPrecisionTimeStamp, syncTime.getPresentationTimeStamp());
            }
        }
        
        parentSensor.setStreamSyncTime(syncTime);

        DataBlock dataBlock;

        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {

            dataBlock = latestRecord.renew();
        }

        synchronized (histogramLock) {

            int setIndex = setCount % MAX_NUM_TIMING_SAMPLES;

            // Get a sampling time for latest set based on previous set sampling time
            timingHistogram[setIndex] = System.currentTimeMillis() - lastSetTimeMillis;

            // Set latest sampling time to now
            lastSetTimeMillis = timingHistogram[setIndex];
        }

        ++setCount;

        for (Map.Entry<Tag, Object> entry : valuesMap.entrySet()) {

            Tag tag = entry.getKey();
            int localSetTag = tag.getLocalSetTag();
            Object value = entry.getValue();
            setData(dataBlock, tag.getMemberOf(), localSetTag, value);
        }

        latestRecord = dataBlock;

        latestRecordTime = System.currentTimeMillis();

        publish(dataBlock);
    }
}
