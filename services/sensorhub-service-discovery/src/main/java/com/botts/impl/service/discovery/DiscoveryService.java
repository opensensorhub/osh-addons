/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2022 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.service.discovery;

import com.botts.impl.service.discovery.engine.RulesEngine;
import com.botts.impl.service.discovery.engine.facts.DataStreamFact;
import com.botts.impl.service.discovery.engine.rules.RuleManager;
import com.botts.impl.service.discovery.engine.rules.Rules;
import com.botts.impl.service.discovery.engine.visualizations.VisualizationMapper;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.DataStreamAddedEvent;
import org.sensorhub.api.data.DataStreamEvent;
import org.sensorhub.api.data.DataStreamRemovedEvent;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.datastore.obs.IDataStreamStore;
import org.sensorhub.api.event.EventUtils;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.service.IHttpServer;
import org.sensorhub.api.service.IServiceModule;
import org.sensorhub.api.system.SystemEvent;
import org.sensorhub.impl.service.AbstractHttpServiceModule;
import org.sensorhub.utils.NamedThreadFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A service allowing discovery of pertinent data streams given a sets of rules defined in
 * txt, csv, or json file.
 *
 * @author Nick Garay
 * @since Jan. 7, 2022
 */
public class DiscoveryService extends AbstractHttpServiceModule<DiscoveryServiceConfig> implements IServiceModule<DiscoveryServiceConfig>, IEventListener {

    /**
     * Handle to the servlet
     */
    private DiscoveryServlet servlet;

    /**
     * Flag indicating if the service has been initialized
     */
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * Subscription used by the service to be notified in changes to the available data streams
     */
    private Flow.Subscription dataStreamSubscription = null;
    
    /**
     * Subscription used by the service to be notified in changes to modules
     */
    private Flow.Subscription moduleEventSubscription = null;

    /**
     * A thread pool controlling the maximum number of concurrent requests processed by the service
     */
    private ExecutorService threadPool;

    /**
     * For connecting defined visualizations to rules
     */
    private VisualizationMapper visualizationMapper;
    @Override
    protected void doInit() throws SensorHubException {

        super.doInit();

        threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),
                new NamedThreadFactory("DiscoveryPool"));
    
        // Subscribe to system events, and then look for specifically data stream events
        // subscription cancellation occurs in doStop
        getParentHub().getEventBus().newSubscription(SystemEvent.class)
                .withTopicID(EventUtils.getSystemRegistryTopicID())
                .subscribe(new Flow.Subscriber<>() {
                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                    
                        dataStreamSubscription = subscription;
                        dataStreamSubscription.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onNext(SystemEvent systemEvent) {

                        logger.debug("SystemEvent received");

                        if (systemEvent instanceof DataStreamEvent) {

                            DataStreamEvent dataStreamEvent = (DataStreamEvent) systemEvent;

                            logger.debug("SystemEvent is DataStreamEvent");

                            IDataStreamStore dataStreamStore = getParentHub().getDatabaseRegistry().getFederatedDatabase().getDataStreamStore();

                            Map.Entry<DataStreamKey, IDataStreamInfo> dataStreamEntry = dataStreamStore.getLatestVersionEntry(dataStreamEvent.getSystemUID(), dataStreamEvent.getOutputName());

                            IDataStreamInfo dataStreamInfo = dataStreamEntry.getValue();
                            String systemId = getParentHub().getIdEncoders().getSystemIdEncoder().encodeID(dataStreamInfo.getSystemID().getInternalID());
                            String dataStreamId = getParentHub().getIdEncoders().getDataStreamIdEncoder().encodeID(dataStreamEntry.getKey().getInternalID());

                            if (dataStreamEvent instanceof DataStreamAddedEvent) {

                                logger.debug("Adding new fact to knowledge base");
                                RulesEngine.getInstance().addFact(new DataStreamFact(systemId, dataStreamId, dataStreamInfo));

                            } else if (dataStreamEvent instanceof DataStreamRemovedEvent) {

                                logger.debug("Removing fact from knowledge base");
                                RulesEngine.getInstance().removeFact(systemId, dataStreamId);
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {

                        logger.error("Error handling data stream subscriptions: {}", throwable.getMessage());
                    }

                    @Override
                    public void onComplete() {

                    }
                });

        loadRules();

        initializeFacts();
        VisualizationMapper visualizationMapper = VisualizationMapper.getInstance();
        visualizationMapper.setFile(config.visRulesFilePath);
//        visualizationMapper.setEncoder(idEncoder);
        visualizationMapper.setParentHub(this.getParentHub());
        // set federatedDB for visualization mapper
        visualizationMapper.setFederatedDB(getParentHub().getDatabaseRegistry().getFederatedDatabase().getDataStreamStore());
        visualizationMapper.populateVisRelations();

        initialized.set(true);
    }

    @Override
    public void setConfiguration(DiscoveryServiceConfig config) {

        super.setConfiguration(config);

        this.securityHandler = new DiscoveryServiceSecurity(this, config.security.enableAccessControl);
    }

    @Override
    public void doStart() throws SensorHubException {

        if (!initialized.get()) {

            init();
        }

        // deploy servlet
        servlet = new DiscoveryServlet(this, (DiscoveryServiceSecurity) this.securityHandler);

        deploy();

        setState(ModuleEvent.ModuleState.STARTED);
    }

    @Override
    public void doStop() {

        if (dataStreamSubscription != null) {

            dataStreamSubscription.cancel();
        }

        // undeploy servlet
        undeploy();

        if (servlet != null) {

            servlet.stop();
        }

        servlet = null;

        setState(ModuleEvent.ModuleState.STOPPED);

        initialized.set(false);

        RulesEngine.getInstance().reset();
    }

    @Override
    public void cleanup() throws SensorHubException {

        super.cleanup();

        threadPool.shutdown();

        // stop listening to http server events
        IHttpServer<?> httpServer = getParentHub().getModuleRegistry().getModuleByType(IHttpServer.class);

        if (httpServer != null) {

            httpServer.unregisterListener(this);
        }

        // unregister security handler
        if (securityHandler != null) {

            securityHandler.unregister();
        }
    }

    /**
     * Deploys the servlet within the server.
     *
     * @throws SensorHubException if an instance of the HttpServer is not started
     */
    private void deploy() throws SensorHubException {

        // deploy servlet to HTTP server
        httpServer.deployServlet(servlet, config.endPoint + "/*");

        httpServer.addServletSecurity(config.endPoint + "/*", config.security.requireAuth);
    }

    /**
     * Gracefully cleans up the servlet
     */
    private void undeploy() {

        // return silently if HTTP server missing on stop
        if (httpServer != null && httpServer.isStarted()) {

            httpServer.undeployServlet(servlet);
        }
    }

    /**
     * Loads the rules to be used by the internal rules based engine
     *
     * @throws SensorHubException if rules could not be loaded or are missing
     */
    private void loadRules() throws SensorHubException {

        File configRules = new File(getConfiguration().rulesFilePath);

        if (!configRules.exists()) {

            throw new SensorHubException("Rules must be specified for the service");
        }

        try {

            Rules rules = new Rules();

            RuleManager.loadRules(configRules, rules);

            RulesEngine.getInstance().setRules(rules);

        } catch (IOException e) {

            throw new SensorHubException("Error reading rules file: " + configRules.getAbsolutePath(), e);
        }
    }

    /**
     * Initializes the knowledge the rules based engine will operate on
     */
    private void initializeFacts() {

        IDataStreamStore dataStreamStore = getParentHub().getDatabaseRegistry().getFederatedDatabase().getDataStreamStore();

        for (Map.Entry<DataStreamKey, IDataStreamInfo> dataStreamEntry : dataStreamStore.entrySet()) {

            IDataStreamInfo dataStreamInfo = dataStreamEntry.getValue();
            String systemId = getParentHub().getIdEncoders().getSystemIdEncoder().encodeID(dataStreamInfo.getSystemID().getInternalID());
            String dataStreamId = getParentHub().getIdEncoders().getDataStreamIdEncoder().encodeID(dataStreamEntry.getKey().getInternalID());
            RulesEngine.getInstance().addFact(new DataStreamFact(systemId, dataStreamId, dataStreamInfo));
        }
    }

    /**
     * Retrieves the thread pool managed by this service
     *
     * @return the thread pool managed by this service
     */
    ExecutorService getThreadPool() {

        return threadPool;
    }
}
