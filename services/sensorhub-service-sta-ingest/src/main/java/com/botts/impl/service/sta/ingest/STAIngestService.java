/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2024 Botts Innovative Research, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package com.botts.impl.service.sta.ingest;

import com.google.common.base.Strings;
import de.fraunhofer.iosb.ilt.sta.MqttException;
import de.fraunhofer.iosb.ilt.sta.service.MqttConfig;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.event.IEventBus;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.system.SystemDatabaseTransactionHandler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;


/**
 * <p>
 *     Service to ingest SensorThings API resources as OSH-readable resources.
 * </p>
 *
 * @author Alex Almanza
 * @since Dec 9, 2024
 */
public class STAIngestService extends AbstractModule<STAIngestConfig>
{
    IEventBus eventBus;
    SystemDatabaseTransactionHandler transactionHandler;
    IObsSystemDatabase writeDb;

    @Override
    public void setConfiguration(STAIngestConfig config)
    {
        super.setConfiguration(config);
    }

    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();

        // Get obs database to store ingested data
        if (!Strings.isNullOrEmpty(config.databaseID))
        {
            writeDb = (IObsSystemDatabase) getParentHub().getModuleRegistry().getModuleById(config.databaseID);
            if (writeDb != null && !writeDb.isOpen())
                writeDb = null;
        }
        else
            writeDb = getParentHub().getSystemDriverRegistry().getSystemStateDatabase();

        eventBus = getParentHub().getEventBus();
        transactionHandler = new SystemDatabaseTransactionHandler(eventBus, writeDb);
        // TODO: Move stuff here
    }

    @Override
    protected void doStart() throws SensorHubException
    {
        // TODO: Ingest Datastreams, Things, Sensors, Observations, ObservedProperties. Store in writeDb

        reportStatus("Starting ingestion of " + config.staBaseResourcePathList.size() + " URL(s)...");

        try {
            startThreads();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        setState(ModuleState.STARTED);
    }

    private void startThreads() throws MalformedURLException {
        for (String urlString : config.staBaseResourcePathList)
        {
                URL url = new URL(urlString);
                Thread ingestThread = new Thread(() -> {
                    try {
                        MqttConfig mqttConfig = config.mqttUri == null ? null : new MqttConfig(config.mqttUri);
                        STAIngestor ingestor = new STAIngestor(url, mqttConfig, Objects.equals(writeDb, getParentHub().getSystemDriverRegistry().getSystemStateDatabase()), transactionHandler);
                        ingestor.ingest();
                    } catch (MalformedURLException | MqttException e) {
                        getLogger().error(e.getMessage(), e);
                    }
                });
                ingestThread.start();
        }
    }

    @Override
    protected void doStop()
    {
        setState(ModuleState.STOPPED);
    }

}
