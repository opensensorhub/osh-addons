/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research, LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.LoggerFactory;
import org.vast.util.Asserts;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.*;

public class LogbackDriver extends AbstractSensorModule<LogbackDriverConfig> {

    private static int appenderCounter = 0;
    private LogbackEventOutput output;
    private final Set<LoggerContext> attachedContexts = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final java.util.Map<LoggerContext, Appender<ILoggingEvent>> contextAppenders = new ConcurrentHashMap<>();
    private ScheduledExecutorService contextScanner;

    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();

        Asserts.checkNotNullOrBlank(config.uniqueId, "Must configure or use default UID for this driver");

        // Generate identifiers
        generateUniqueID("urn:osh:sensor:logback:", config.uniqueId);
        generateXmlID("LOGBACK_SENSOR_",  config.uniqueId);

        // Create log output
        output = new LogbackEventOutput(this);
        addOutput(output, false);

        // Detach and stop all existing appenders
        for (java.util.Map.Entry<LoggerContext, Appender<ILoggingEvent>> entry : contextAppenders.entrySet()) {
            try {
                LoggerContext context = entry.getKey();
                Appender<ILoggingEvent> appender = entry.getValue();
                Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);

                appender.stop();
                rootLogger.detachAppender(appender.getName());
            } catch (Exception e) {
                System.err.println("LogbackDriver: Error removing old appender: " + e.getMessage());
            }
        }

        contextAppenders.clear();
        attachedContexts.clear();

        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        attachToContext(rootLogger.getLoggerContext());
        scanForModuleContexts();
    }

    @Override
    protected void doStart() {
        // Attach to default context first
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        attachToContext(rootLogger.getLoggerContext());

        // Immediately scan for existing module contexts
        scanForModuleContexts();

        // Start background scanner to find new module contexts as they're created
        contextScanner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "LogbackDriver-ContextScanner");
            t.setDaemon(true);
            return t;
        });

        // Scan routinely for new logback contexts
        contextScanner.scheduleAtFixedRate(() -> {
            try {
                scanForModuleContexts();
            } catch (Exception e) {
                System.err.println("LogbackDriver: Error scanning for logger contexts: " + e.getMessage());
            }
        }, 5, 5, TimeUnit.SECONDS);

        // Listen for new modules being added
        try {
            if (getParentHub() != null) {
                getParentHub().getEventBus()
                        .newSubscription()
                        .withTopicID(ModuleRegistry.EVENT_GROUP_ID)
                        .subscribe(event -> {
                    if (event instanceof ModuleEvent) {
                        ModuleEvent moduleEvent = (ModuleEvent) event;
                        if (moduleEvent.getType() == ModuleEvent.Type.LOADED ||
                            moduleEvent.getType() == ModuleEvent.Type.STATE_CHANGED) {
                            scanForModuleContexts();
                        }
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("LogbackDriver: Could not register module event listener: " + e.getMessage());
        }
    }

    @Override
    protected void doStop() {
        // Stop context scanner
        if (contextScanner != null) {
            contextScanner.shutdown();
            try {
                contextScanner.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                contextScanner.shutdownNow();
            }
        }

        // Stop and detach all appenders from all contexts
        for (java.util.Map.Entry<LoggerContext, Appender<ILoggingEvent>> entry : contextAppenders.entrySet()) {
            try {
                LoggerContext context = entry.getKey();
                Appender<ILoggingEvent> appender = entry.getValue();

                Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);

                // Stop appender
                appender.stop();

                // Detach from root logger
                if (appender.getName() != null) {
                    rootLogger.detachAppender(appender.getName());
                }
            } catch (Exception e) {
                System.err.println("LogbackDriver: Error stopping appender: " + e.getMessage());
            }
        }

        attachedContexts.clear();
        contextAppenders.clear();
    }

    protected void attachToContext(LoggerContext context) {
        if (context == null || attachedContexts.contains(context))
            return;

        try {
            Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);

            // Remove any existing SensorHub appenders from this context
            java.util.Iterator<Appender<ILoggingEvent>> iter = rootLogger.iteratorForAppenders();
            while (iter.hasNext()) {
                Appender<ILoggingEvent> existing = iter.next();
                if (existing instanceof SensorHubAppender ||
                        (existing.getName() != null && existing.getName().startsWith("SensorHubAppender-"))) {
                    rootLogger.detachAppender(existing);
                }
            }

            // Each context needs its own appender instance
            Appender<ILoggingEvent> appender = new SensorHubAppender(output, Level.toLevel(config.level.name()));
            appender.setName("SensorHubAppender-" + getLocalID() + "-" + (appenderCounter++));
            appender.setContext(context);
            appender.start();

            // Attach to root logger of this context
            rootLogger.addAppender(appender);
            attachedContexts.add(context);
            contextAppenders.put(context, appender);

            if (!context.getName().equals("default"))
                System.out.println("LogbackDriver: Attached to module logger context '" + context.getName() + "'");
        } catch (Exception e) {
            System.err.println("LogbackDriver: Error attaching to context " + context.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Need to scan for new contexts since ModuleUtils.createModuleLogger() creates a new LoggerContext for each module
     */
    private void scanForModuleContexts() {
        try {
            if (getParentHub() == null)
                return;

            var moduleRegistry = getParentHub().getModuleRegistry();
            if (moduleRegistry == null)
                return;

            for (IModule<?> module : moduleRegistry.getLoadedModules()) {
                if (module == null)
                    continue;

                // check module logger for logback context
                org.slf4j.Logger moduleLogger = module.getLogger();
                if (moduleLogger instanceof Logger) {
                    Logger logbackLogger = (Logger) moduleLogger;
                    LoggerContext context = logbackLogger.getLoggerContext();

                    // Attach to this module's context
                    if (context != null)
                        attachToContext(context);
                }
            }
        } catch (Exception e) {
            System.err.println("LogbackDriver: Error scanning modules: " + e.getMessage());
        }
    }

    @Override
    public boolean isConnected() {
        return !contextAppenders.isEmpty();
    }

}
