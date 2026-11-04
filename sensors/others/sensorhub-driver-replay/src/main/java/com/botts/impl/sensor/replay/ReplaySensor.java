/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.replay;

import com.botts.impl.sensor.replay.config.ReplayConfig;
import com.botts.impl.sensor.replay.output.ReplayOutput;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.DataStreamInfo;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.datastore.obs.IDataStreamStore;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.api.feature.FeatureId;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.system.ISystemWithDesc;
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

        if (config.moduleId != null && !config.moduleId.isEmpty() && config.uniqueId != null && !config.uniqueId.isEmpty()) {

            if (getParentHub().getModuleRegistry().getModuleById(config.moduleId) instanceof IObsSystemDatabase systemDriverDatabase) {

                if (((IModule<?>) systemDriverDatabase).getCurrentState() == ModuleEvent.ModuleState.STARTED) {

                    describeSystem(systemDriverDatabase);

                } else {

                    getParentHub().getModuleRegistry().waitForModule(config.moduleId, ModuleEvent.ModuleState.STARTED).thenAccept(iModule -> {

                        describeSystem(systemDriverDatabase);

                    });
                }

                initialized.set(true);
            }

        } else {

            getLogger().warn("No datastore or system id configured");

            throw new SensorHubException("No datastore or system id configured");
        }
    }

    private void describeSystem(IObsSystemDatabase systemDriverDatabase) {

        IDataStreamStore federatedStreamStore = getParentHub().getDatabaseRegistry().getFederatedDatabase().getDataStreamStore();

        for (ISystemWithDesc systemWithDesc : systemDriverDatabase.getSystemDescStore().values()) {

            if (systemWithDesc.getUniqueIdentifier().equalsIgnoreCase(config.uniqueId)) {

                generateUniqueID(systemWithDesc.getUniqueIdentifier(), ":replay");

                generateXmlID(systemWithDesc.getUniqueIdentifier(), "replay");

                uniqueID = systemWithDesc.getUniqueIdentifier() + ":replay";

                DataStreamFilter filter = new DataStreamFilter.Builder().withSystems(new SystemFilter.Builder().withUniqueIDs(systemWithDesc.getUniqueIdentifier()).build()).build();

                systemDriverDatabase.getDataStreamStore().selectEntries(filter).forEach(entry -> {

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

                        // Need to get datastream info from federated database as system driver database datastream
                        // info does not return an implementation that contains the time stamp information necessary to
                        // compute replay parameters if not specified in config
                        IDataStreamInfo federatedDataStreamInfo = federatedStreamStore.get(dataStreamKey);

                        TimeExtent timeRange = federatedDataStreamInfo.getPhenomenonTimeRange();

                        if (timeRange == null) {

                            timeRange = federatedDataStreamInfo.getValidTime();
                        }

                        Instant startTime;
                        Instant endTime;

                        if (config.startTime == null) {

                            startTime = timeRange.begin();

                        } else {

                            startTime = config.startTime.toInstant();
                        }

                        if (config.endTime == null) {

                            endTime = timeRange.end();;

                        } else {

                            endTime = config.endTime.toInstant();
                        }

                        ReplayOutput replayOutput = new ReplayOutput(this, dataStreamKey, replayDataStreamInfo, systemDriverDatabase, startTime, endTime, config.batchSize);

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
    }
}