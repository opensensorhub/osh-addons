/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research, LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.replay;

import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.DataStreamInfo;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.api.feature.FeatureId;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.LoggerFactory;
import org.vast.util.TimeExtent;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sensor driver providing sensor description, output registration, initialization
 * and shutdown of driver and outputs.
 *
 * @author Nicolas Garay
 * @since 3/19/2025
 */
public class ReplaySensor extends AbstractSensorModule<ReplayConfig> {

    Map<String, ReplayOutput> outputsMap = new HashMap<>();

    AtomicBoolean initialized = new AtomicBoolean(false);

    @Override
    public void doInit() throws SensorHubException {

        super.doInit();

        logger = LoggerFactory.getLogger(ReplaySensor.class);

        initialize();
    }

    @Override
    public void doStart() throws SensorHubException {

        super.doStart();

        if (!initialized.get()) {
            initialize();
        }

        outputsMap.values().forEach(ReplayOutput::doStart);
    }

    @Override
    public void doStop() throws SensorHubException {

        super.doStop();

        outputsMap.values().forEach(ReplayOutput::doStop);

        initialized.set(false);
    }

    @Override
    public boolean isConnected() {

        return true;
    }

    private void initialize() throws SensorHubException {
        ReplayConfig config = getConfiguration();

        if (config.uniqueId == null || config.uniqueId.isEmpty()) {
            getLogger().warn("No system id configured");
            throw new SensorHubException("No datastore or system id configured");
        }

        IObsSystemDatabase federatedDatabase = getParentHub().getDatabaseRegistry().getFederatedDatabase();
        describeSystem(federatedDatabase);
        initialized.set(true);
    }

    private void describeSystem(IObsSystemDatabase federatedDatabase) {

        generateUniqueID(config.uniqueId, ":replay");
        generateXmlID(config.uniqueId, "replay");

        DataStreamFilter filter = new DataStreamFilter.Builder()
                .withSystems(new SystemFilter.Builder()
                        .withUniqueIDs(config.uniqueId)
                        .build())
                .build();

        federatedDatabase.getDataStreamStore().selectEntries(filter).forEach(entry -> {
            IDataStreamInfo dataStreamInfo = entry.getValue();
            DataStreamKey dataStreamKey = entry.getKey();
            DataComponent dataComponent = dataStreamInfo.getRecordStructure();

            IDataStreamInfo replayDataStreamInfo = new DataStreamInfo.Builder()
                    .withName(dataComponent.getName())
                    .withSystem(new FeatureId(BigId.NONE, this.uniqueID))
                    .withRecordDescription(dataComponent)
                    .withRecordEncoding(dataStreamInfo.getRecordEncoding())
                    .build();

            if (!outputsMap.containsKey(dataComponent.getName())) {

                TimeExtent timeRange = dataStreamInfo.getPhenomenonTimeRange();

                if (timeRange == null) {

                    timeRange = dataStreamInfo.getValidTime();
                }

                Instant startTime = (config.startTime != null) ? config.startTime.toInstant() : timeRange.begin();
                Instant endTime = (config.startTime != null) ? config.endTime.toInstant() : timeRange.end();

                ReplayOutput replayOutput = new ReplayOutput(this, dataStreamKey, replayDataStreamInfo, federatedDatabase, startTime, endTime, config.batchSize);
                replayOutput.doInit();

                outputsMap.put(dataComponent.getName(), replayOutput);
                addOutput(replayOutput, false);

            } else {
                ReplayOutput replayOutput = outputsMap.get(dataComponent.getName());
                replayOutput.doInit();
                addOutput(replayOutput, false);
            }
        });
    }
}