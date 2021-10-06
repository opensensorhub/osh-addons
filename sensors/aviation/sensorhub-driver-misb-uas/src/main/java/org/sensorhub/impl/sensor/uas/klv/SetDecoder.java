/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.uas.klv;

import org.sensorhub.impl.sensor.uas.common.SyncTime;
import org.sensorhub.misb.stanag4609.comm.DataBufferListener;
import org.sensorhub.misb.stanag4609.comm.DataBufferRecord;
import org.sensorhub.misb.stanag4609.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.util.Asserts;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Decodes MISB-TS STANAG 4609 ST0601.16 UAS Metadata
 *
 * @author Nick Garay
 * @since Oct. 5, 2020
 */
public class SetDecoder implements DataBufferListener {

    private static final Logger logger = LoggerFactory.getLogger(SetDecoder.class);
    
    private final List<DecodedSetListener> listeners = new ArrayList<>();

    private Executor executor;
    
    /**
     * Constructor
     */
    public SetDecoder() {
        
    }
    
    public void setExecutor(Executor executor) {
        this.executor = Asserts.checkNotNull(executor, Executor.class);
    }

    @Override
    public void onDataBuffer(DataBufferRecord record) {

        executor.execute(() -> {
            try {
                processBuffer(record);
            } catch (Throwable e) {
                logger.error("Error while decoding MISB Local Set", e);
            }
        });
    }

    public void processBuffer(DataBufferRecord record) {

        byte[] dataBuffer = record.getDataBuffer();

        // Read the set
        UasDataLinkSet dataLinkSet = new UasDataLinkSet(dataBuffer.length, dataBuffer);

        List<String> acceptedDesignators = new ArrayList<>();
        acceptedDesignators.add(UasDataLinkSet.UAS_LOCAL_SET.getDesignator());

        // If it is a valid set
        if (dataLinkSet.validateChecksum() && dataLinkSet.validateDesignator(acceptedDesignators)) {

            HashMap<Tag, Object> valuesMap = dataLinkSet.decode();

            SyncTime syncTime = new SyncTime(dataLinkSet.getPrecisionTimeStamp(), record.getPresentationTimestamp());

            synchronized (listeners) {

                for (DecodedSetListener listener : listeners) {

                    listener.onSetDecoded(syncTime, valuesMap);
                }
            }
        }
    }

    /**
     * Adds a listener to callback when new KLV data is decoded
     *
     * @param listener the listener to register for decoded KLV data
     */
    public void addListener(DecodedSetListener listener) {

        if (!listeners.contains(listener)) {

            listeners.add(listener);
        }
    }

    /**
     * Removes a listener
     *
     * @param listener the listener to remove, owner is no longer interested in listening
     *                 for decode KLV data
     */
    public void removeListener(DecodedSetListener listener) {

        synchronized (listeners) {

            if (listeners.contains(listener)) {

                listeners.remove(listener);
            }
        }
    }
}
