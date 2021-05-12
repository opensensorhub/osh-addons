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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Decodes MISB-TS STANAG 4609 ST0601.16 UAS Metadata
 *
 * @author Nick Garay
 * @since Oct. 5, 2020
 */
public class SetDecoder implements DataBufferListener, Runnable {

    private static final Logger logger = LoggerFactory.getLogger(SetDecoder.class);

    private final AtomicBoolean stopProcessing = new AtomicBoolean(false);

    private final BlockingQueue<DataBufferRecord> dataBufferQueue = new LinkedBlockingDeque<>();

    private final List<DecodedSetListener> listeners = new ArrayList<>();

    Thread worker;

    /**
     * Constructor
     */
    public SetDecoder() {

        worker = new Thread(this, this.getClass().getSimpleName());
    }

    /**
     * Begins processing data for output
     */
    public void start() {

        logger.info("Starting worker thread: {}", worker.getName());

        worker.start();
    }

    @Override
    public void onDataBuffer(DataBufferRecord record) {

        try {

            dataBufferQueue.put(record);

        } catch (InterruptedException e) {

            logger.error("Error in worker thread: {} due to exception: {}", Thread.currentThread().getName(), e.toString());
        }
    }

    /**
     * Terminates processing data for output
     */
    public void stop() {

        listeners.clear();

        stopProcessing.set(true);
    }

    /**
     * Check to validate data processing is still running
     *
     * @return true if worker thread is active, false otherwise
     */
    public boolean isAlive() {

        return worker.isAlive();
    }

    @Override
    public void run() {

        try {

            while (!stopProcessing.get()) {

                DataBufferRecord record = dataBufferQueue.take();

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

        } catch (InterruptedException e) {

            logger.error("Error in worker thread: {} due to exception: {}", Thread.currentThread().getName(), e.toString());

        } catch (Exception e) {

            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            logger.error("Error in worker thread: {} due to exception: {}", Thread.currentThread().getName(), stringWriter.toString());

        } finally {

            logger.debug("Terminating worker thread: {}", worker.getName());
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
