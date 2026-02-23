/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2026 Botts Innovative Research, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.logback;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import net.opengis.swe.v20.DataBlock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.event.IEventListener;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class LogbackDriverTest {

    private static final org.slf4j.Logger testLogger = LoggerFactory.getLogger(LogbackDriverTest.class);

    private LogbackDriver driver;
    private List<DataBlock> receivedEvents;
    private CountDownLatch eventLatch;
    private IEventListener currentListener;

    @Before
    public void setup() {
        receivedEvents = new ArrayList<>();
        currentListener = null;
    }

    @After
    public void cleanup() throws Exception {
        if (driver != null && driver.isStarted()) {
            driver.stop();
        }
    }

    private void setupDriver(LogbackDriverConfig.LogbackLevel level) throws Exception {
        LogbackDriverConfig config = new LogbackDriverConfig();
        config.id = "test-logback-driver";
        config.name = "Test Logback Driver";
        config.level = level;

        driver = new LogbackDriver();
        driver.init(config);
    }

    private void addEventListener(int expectedEvents) {
        receivedEvents.clear();
        eventLatch = new CountDownLatch(expectedEvents);

        IStreamingDataInterface output = driver.getOutputs().get(LogbackEventOutput.NAME);
        assertNotNull("Output should not be null", output);

        // Unregister previous listener if any
        if (currentListener != null) {
            output.unregisterListener(currentListener);
        }

        // Create and register new listener
        currentListener = e -> {
            if (e instanceof DataEvent) {
                DataBlock dataBlock = ((DataEvent) e).getRecords()[0];
                receivedEvents.add(dataBlock);
                eventLatch.countDown();
            }
        };

        output.registerListener(currentListener);
    }

    @Test
    public void testDriverStartsAndStops() throws Exception {
        setupDriver(LogbackDriverConfig.LogbackLevel.INFO);

        assertFalse("Driver should not be started initially", driver.isStarted());

        driver.start();
        assertTrue("Driver should be started", driver.isStarted());

        driver.stop();
        assertFalse("Driver should be stopped", driver.isStarted());
    }

    @Test
    public void testLogEventsArePublished() throws Exception {
        setupDriver(LogbackDriverConfig.LogbackLevel.INFO);
        addEventListener(3);

        driver.start();

        // Generate log events
        testLogger.info("Test info message 1");
        testLogger.info("Test info message 2");
        testLogger.warn("Test warn message");

        // Wait for events to be received
        boolean received = eventLatch.await(2, TimeUnit.SECONDS);
        assertTrue("Should receive all log events within timeout", received);
        assertEquals("Should receive 3 events", 3, receivedEvents.size());

        // Verify first event content
        DataBlock firstEvent = receivedEvents.get(0);
        assertEquals("INFO", firstEvent.getStringValue(1)); // level field
        assertTrue("Logger name should contain test class",
            firstEvent.getStringValue(3).contains("LogbackDriverTest"));
        assertEquals("Test info message 1", firstEvent.getStringValue(4)); // message field
    }

    @Test
    public void testLevelFiltering() throws Exception {
        setupDriver(LogbackDriverConfig.LogbackLevel.WARN);
        addEventListener(2);

        driver.start();

        // Generate events at different levels
        testLogger.debug("Debug message - should be filtered");
        testLogger.info("Info message - should be filtered");
        testLogger.warn("Warn message - should pass");
        testLogger.error("Error message - should pass");

        // Wait for events
        boolean received = eventLatch.await(2, TimeUnit.SECONDS);
        assertTrue("Should receive warn and error events", received);
        assertEquals("Should receive only 2 events (WARN and ERROR)", 2, receivedEvents.size());

        assertEquals("WARN", receivedEvents.get(0).getStringValue(1));
        assertEquals("ERROR", receivedEvents.get(1).getStringValue(1));
    }

    @Test
    public void testRestartWithDifferentLevel() throws Exception {
        // Start with ERROR level
        setupDriver(LogbackDriverConfig.LogbackLevel.ERROR);
        addEventListener(1);

        driver.start();

        testLogger.info("Info message - should be filtered");
        testLogger.error("Error message - should pass");

        boolean received = eventLatch.await(2, TimeUnit.SECONDS);
        assertTrue("Should receive error event", received);
        assertEquals("Should receive 1 event", 1, receivedEvents.size());
        assertEquals("ERROR", receivedEvents.get(0).getStringValue(1));

        // Stop the driver
        driver.stop();
        assertFalse("Driver should be stopped", driver.isStarted());

        // Restart with INFO level
        setupDriver(LogbackDriverConfig.LogbackLevel.INFO);
        addEventListener(2);

        driver.start();
        assertTrue("Driver should be started after restart", driver.isStarted());

        // Generate new log events
        testLogger.info("Info message after restart - should pass");
        testLogger.warn("Warn message after restart - should pass");

        received = eventLatch.await(2, TimeUnit.SECONDS);
        assertTrue("Should receive events after restart", received);
        assertEquals("Should receive 2 events after restart", 2, receivedEvents.size());

        assertEquals("INFO", receivedEvents.get(0).getStringValue(1));
        assertEquals("WARN", receivedEvents.get(1).getStringValue(1));
    }

    @Test
    public void testMultipleEventsReceived() throws Exception {
        setupDriver(LogbackDriverConfig.LogbackLevel.DEBUG);
        addEventListener(10);

        driver.start();

        // Generate multiple events rapidly
        for (int i = 0; i < 10; i++) {
            testLogger.info("Message number " + i);
        }

        boolean received = eventLatch.await(3, TimeUnit.SECONDS);
        assertTrue("Should receive all 10 events", received);
        assertEquals("Should receive 10 events", 10, receivedEvents.size());

        // Verify messages are in order
        for (int i = 0; i < 10; i++) {
            assertEquals("Message number " + i, receivedEvents.get(i).getStringValue(4));
        }
    }

    @Test
    public void testExceptionLogging() throws Exception {
        setupDriver(LogbackDriverConfig.LogbackLevel.ERROR);
        addEventListener(1);

        driver.start();

        // Generate log event with exception
        Exception testException = new RuntimeException("Test exception message");
        testLogger.error("Error with exception", testException);

        boolean received = eventLatch.await(2, TimeUnit.SECONDS);
        assertTrue("Should receive error event", received);
        assertEquals("Should receive 1 event", 1, receivedEvents.size());

        DataBlock event = receivedEvents.get(0);
        assertEquals("ERROR", event.getStringValue(1));
        assertEquals("Error with exception", event.getStringValue(4));

        String stackTrace = event.getStringValue(5);
        assertNotNull("Stack trace should not be null", stackTrace);
        assertTrue("Stack trace should contain exception message",
            stackTrace.contains("Test exception message"));
    }

    @Test
    public void testOutputMetadata() throws Exception {
        setupDriver(LogbackDriverConfig.LogbackLevel.INFO);
        driver.start();

        IStreamingDataInterface output = driver.getOutputs().get(LogbackEventOutput.NAME);
        assertNotNull("Output should exist", output);

        assertNotNull("Record description should not be null", output.getRecordDescription());
        assertNotNull("Recommended encoding should not be null", output.getRecommendedEncoding());

        // Verify record structure
        assertEquals("Record should have 6 fields", 6, output.getRecordDescription().getComponentCount());
    }

    @Test
    public void testUpdateConfigWhileStopped() throws Exception {
        // Initial setup with ERROR level
        setupDriver(LogbackDriverConfig.LogbackLevel.ERROR);
        driver.start();

        testLogger.info("Info message - should be filtered");
        testLogger.error("Error message - should pass");

        Thread.sleep(100); // Give time for logs to driver

        driver.stop();
        assertFalse("Driver should be stopped", driver.isStarted());

        // Update configuration with INFO level
        LogbackDriverConfig newConfig = new LogbackDriverConfig();
        newConfig.id = "test-logback-driver";
        newConfig.name = "Test Logback Driver";
        newConfig.level = LogbackDriverConfig.LogbackLevel.INFO;

        driver.updateConfig(newConfig);

        // Verify config was updated
        assertEquals("Config level should be updated",
            LogbackDriverConfig.LogbackLevel.INFO, driver.getConfiguration().level);
        assertFalse("Driver should still be stopped after update", driver.isStarted());
    }

    @Test
    public void testUpdateConfigWhileStartedAndRestart() throws Exception {
        // Initial setup with ERROR level
        setupDriver(LogbackDriverConfig.LogbackLevel.ERROR);
        addEventListener(1);
        driver.start();

        testLogger.info("Info message 1 - should be filtered");
        testLogger.error("Error message 1 - should pass");

        boolean received = eventLatch.await(2, TimeUnit.SECONDS);
        assertTrue("Should receive error event", received);
        assertEquals("Should receive 1 event (ERROR only)", 1, receivedEvents.size());
        assertEquals("ERROR", receivedEvents.get(0).getStringValue(1));

        // Update configuration to INFO level while started
        LogbackDriverConfig newConfig = new LogbackDriverConfig();
        newConfig.id = "test-logback-driver";
        newConfig.name = "Test Logback Driver Updated";
        newConfig.level = LogbackDriverConfig.LogbackLevel.INFO;

        driver.updateConfig(newConfig);

        // Driver should be restarted automatically
        assertTrue("Driver should be started after updateConfig", driver.isStarted());
        assertEquals("Config level should be updated",
            LogbackDriverConfig.LogbackLevel.INFO, driver.getConfiguration().level);

        // Setup new listener for events after config update
        addEventListener(2);

        // Test that new level is applied
        testLogger.info("Info message 2 - should now pass");
        testLogger.warn("Warn message 2 - should pass");

        received = eventLatch.await(2, TimeUnit.SECONDS);
        assertTrue("Should receive events after config update", received);
        assertEquals("Should receive 2 events (INFO and WARN)", 2, receivedEvents.size());
        assertEquals("INFO", receivedEvents.get(0).getStringValue(1));
        assertEquals("WARN", receivedEvents.get(1).getStringValue(1));
    }

    @Test
    public void testMultipleConfigUpdatesWhileStarted() throws Exception {
        // Initial setup with ERROR level
        setupDriver(LogbackDriverConfig.LogbackLevel.ERROR);
        driver.start();
        assertTrue("Driver should be started initially", driver.isStarted());

        // First update to WARN
        LogbackDriverConfig config1 = new LogbackDriverConfig();
        config1.id = "test-logback-driver";
        config1.name = "Test Logback Driver";
        config1.level = LogbackDriverConfig.LogbackLevel.WARN;

        driver.updateConfig(config1);
        assertTrue("Driver should be started after first update", driver.isStarted());
        assertEquals("Level should be WARN", LogbackDriverConfig.LogbackLevel.WARN,
            driver.getConfiguration().level);

        // Second update to INFO
        LogbackDriverConfig config2 = new LogbackDriverConfig();
        config2.id = "test-logback-driver";
        config2.name = "Test Logback Driver";
        config2.level = LogbackDriverConfig.LogbackLevel.INFO;

        driver.updateConfig(config2);
        assertTrue("Driver should be started after second update", driver.isStarted());
        assertEquals("Level should be INFO", LogbackDriverConfig.LogbackLevel.INFO,
            driver.getConfiguration().level);

        // Third update to DEBUG
        LogbackDriverConfig config3 = new LogbackDriverConfig();
        config3.id = "test-logback-driver";
        config3.name = "Test Logback Driver";
        config3.level = LogbackDriverConfig.LogbackLevel.DEBUG;

        driver.updateConfig(config3);
        assertTrue("Driver should be started after third update", driver.isStarted());
        assertEquals("Level should be DEBUG", LogbackDriverConfig.LogbackLevel.DEBUG,
            driver.getConfiguration().level);

        // Give some time for the appender to be fully attached
        Thread.sleep(100);

        // Set test logger to DEBUG level so debug messages are actually logged
        Logger testLoggerImpl = (Logger) testLogger;
        testLoggerImpl.setLevel(ch.qos.logback.classic.Level.DEBUG);

        // Verify it still works after multiple updates
        addEventListener(1);

        // Give some time for listener to be registered
        Thread.sleep(50);

        testLogger.debug("Debug message after multiple updates");

        boolean received = eventLatch.await(2, TimeUnit.SECONDS);
        assertTrue("Should receive debug event after multiple updates", received);
        assertEquals("DEBUG", receivedEvents.get(0).getStringValue(1));
    }

    @Test
    public void testSetConfigurationWhileStarted() throws Exception {
        // Initial setup
        setupDriver(LogbackDriverConfig.LogbackLevel.ERROR);
        driver.start();
        assertTrue("Driver should be started", driver.isStarted());

        // Use setConfiguration (not updateConfig) to change config
        LogbackDriverConfig newConfig = new LogbackDriverConfig();
        newConfig.id = "test-logback-driver";
        newConfig.name = "Test Logback Driver";
        newConfig.level = LogbackDriverConfig.LogbackLevel.INFO;

        driver.setConfiguration(newConfig);

        // setConfiguration alone shouldn't restart the module
        // But config should be changed
        assertEquals("Config level should be updated",
            LogbackDriverConfig.LogbackLevel.INFO, driver.getConfiguration().level);
    }

    @Test
    public void testSingleUpdateThenListen() throws Exception {
        // This test checks if we can update config ONCE and then listen
        // Unlike testUpdateConfigWhileStartedAndRestart, we don't test before updating
        setupDriver(LogbackDriverConfig.LogbackLevel.ERROR);
        driver.start();

        Thread.sleep(100);

        // Single update to DEBUG
        LogbackDriverConfig newConfig = new LogbackDriverConfig();
        newConfig.id = "test-logback-driver";
        newConfig.name = "Test Logback Driver";
        newConfig.level = LogbackDriverConfig.LogbackLevel.DEBUG;

        driver.updateConfig(newConfig);
        assertTrue("Driver should be started after update", driver.isStarted());

        Thread.sleep(100);

        // Verify output exists in map
        IStreamingDataInterface output = driver.getOutputs().get(LogbackEventOutput.NAME);
        assertNotNull("Output should exist after updateConfig", output);

        // Verify appender is attached
        Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        java.util.Iterator<Appender<ILoggingEvent>> iter = rootLogger.iteratorForAppenders();
        boolean foundAppender = false;
        while (iter.hasNext()) {
            Appender<ILoggingEvent> app = iter.next();
            if (app instanceof SensorHubAppender) {
                foundAppender = true;
                assertTrue("Appender should be started", app.isStarted());
                break;
            }
        }
        assertTrue("SensorHubAppender should be attached to root logger", foundAppender);

        // Set test logger to DEBUG level so debug messages are actually logged
        Logger testLoggerImpl = (Logger) testLogger;
        testLoggerImpl.setLevel(ch.qos.logback.classic.Level.DEBUG);

        // Now add listener and test
        addEventListener(1);
        Thread.sleep(50);

        // First try ERROR to see if it's a level issue
        testLogger.error("Error message after single update");

        boolean received = eventLatch.await(2, TimeUnit.SECONDS);
        if (!received) {
            // If ERROR didn't work, there's a fundamental problem
            fail("Should receive ERROR event (this should always work)");
        }

        // Now try DEBUG
        addEventListener(1);
        testLogger.debug("Debug message after single update");

        received = eventLatch.await(2, TimeUnit.SECONDS);
        assertTrue("Should receive debug event after single update", received);
        assertEquals("DEBUG", receivedEvents.get(0).getStringValue(1));
    }

    @Test
    public void testTwoConfigUpdatesWhileStarted() throws Exception {
        // Initial setup with ERROR level
        setupDriver(LogbackDriverConfig.LogbackLevel.ERROR);
        driver.start();

        Thread.sleep(200); // Let initial start settle

        // First update to INFO
        LogbackDriverConfig config1 = new LogbackDriverConfig();
        config1.id = "test-logback-driver";
        config1.name = "Test Logback Driver";
        config1.level = LogbackDriverConfig.LogbackLevel.INFO;

        driver.updateConfig(config1);
        assertTrue("Driver should be started after first update", driver.isStarted());

        Thread.sleep(200); // Let first update settle

        // Second update to DEBUG
        LogbackDriverConfig config2 = new LogbackDriverConfig();
        config2.id = "test-logback-driver";
        config2.name = "Test Logback Driver";
        config2.level = LogbackDriverConfig.LogbackLevel.DEBUG;

        driver.updateConfig(config2);
        assertTrue("Driver should be started after second update", driver.isStarted());

        Thread.sleep(200); // Let second update settle

        // Set test logger to DEBUG level so debug messages are actually logged
        Logger testLoggerImpl = (Logger) testLogger;
        testLoggerImpl.setLevel(ch.qos.logback.classic.Level.DEBUG);

        // Verify it works after two updates
        addEventListener(1);
        Thread.sleep(100); // Let listener register

        testLogger.debug("Debug message after two updates");

        boolean received = eventLatch.await(2, TimeUnit.SECONDS);
        assertTrue("Should receive debug event after two updates", received);
        assertEquals("DEBUG", receivedEvents.get(0).getStringValue(1));
    }

    @Test
    public void testTimestampCorrectness() throws Exception {
        setupDriver(LogbackDriverConfig.LogbackLevel.INFO);
        addEventListener(1);
        driver.start();

        long beforeLog = System.currentTimeMillis();
        testLogger.info("Test timestamp");
        long afterLog = System.currentTimeMillis();

        boolean received = eventLatch.await(2, TimeUnit.SECONDS);
        assertTrue("Should receive event", received);

        DataBlock event = receivedEvents.get(0);
        double timestampSeconds = event.getDoubleValue(0);
        long timestampMillis = (long)(timestampSeconds * 1000);

        assertTrue("Timestamp should be after beforeLog", timestampMillis >= beforeLog);
        assertTrue("Timestamp should be before afterLog", timestampMillis <= afterLog);

        // Timestamp should be reasonable (not in year 58107!)
        long currentYear = java.time.Year.now().getValue();
        java.time.Instant instant = java.time.Instant.ofEpochMilli(timestampMillis);
        java.time.Year eventYear = java.time.Year.from(instant.atZone(java.time.ZoneId.systemDefault()));
        assertEquals("Year should be current year", currentYear, eventYear.getValue());
    }

    @Test
    public void testExistingLoggersGetAppender() throws Exception {
        setupDriver(LogbackDriverConfig.LogbackLevel.INFO);

        // Create some loggers BEFORE starting the driver
        org.slf4j.Logger customLogger1 = LoggerFactory.getLogger("CustomLogger1");
        org.slf4j.Logger customLogger2 = LoggerFactory.getLogger("CustomLogger2:moduleId");

        driver.start();

        // Verify appender is attached ONLY to root logger
        // Individual loggers inherit via Logback's propagation mechanism (additivity=true by default)
        Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        LoggerContext loggerContext = rootLogger.getLoggerContext();

        Logger logger1 = loggerContext.getLogger("CustomLogger1");
        Logger logger2 = loggerContext.getLogger("CustomLogger2:moduleId");

        // Only root should have the appender directly
        assertFalse("CustomLogger1 should NOT have direct appender (relies on propagation)",
            hasSensorHubAppender(logger1));
        assertFalse("CustomLogger2:moduleId should NOT have direct appender (relies on propagation)",
            hasSensorHubAppender(logger2));
        assertTrue("Root logger should have SensorHubAppender",
            hasSensorHubAppender(rootLogger));
    }

    @Test
    public void testDynamicallyCreatedLoggersGetAppender() throws Exception {
        setupDriver(LogbackDriverConfig.LogbackLevel.INFO);
        driver.start();

        Thread.sleep(100); // Let driver fully start

        // Create a NEW logger after the driver has started
        org.slf4j.Logger dynamicLogger = LoggerFactory.getLogger("DynamicLogger:newModule");

        Thread.sleep(50); // No listener needed - appender only on root

        // Verify root logger has our appender (dynamically created loggers propagate to root)
        Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        LoggerContext loggerContext = rootLogger.getLoggerContext();
        Logger logger = loggerContext.getLogger("DynamicLogger:newModule");

        // Dynamic logger should NOT have direct appender - it propagates to root
        assertFalse("Dynamically created logger should NOT have direct appender (uses propagation)",
            hasSensorHubAppender(logger));
        assertTrue("Root logger should have SensorHubAppender",
            hasSensorHubAppender(rootLogger));
    }

    @Test
    public void testLogsFromDynamicallyCreatedLogger() throws Exception {
        setupDriver(LogbackDriverConfig.LogbackLevel.INFO);
        addEventListener(2);
        driver.start();

        Thread.sleep(100);

        // Create a new logger and log with it
        org.slf4j.Logger dynamicLogger = LoggerFactory.getLogger("DynamicTestLogger:xyz123");

        // Set logger to DEBUG level (test driver is at INFO level)
        ((Logger)dynamicLogger).setLevel(ch.qos.logback.classic.Level.DEBUG);

        Thread.sleep(50);

        dynamicLogger.info("Message from dynamic logger 1");
        dynamicLogger.warn("Message from dynamic logger 2");

        boolean received = eventLatch.await(2, TimeUnit.SECONDS);
        assertTrue("Should receive events from dynamically created logger", received);
        assertEquals("Should receive 2 events", 2, receivedEvents.size());

        assertEquals("INFO", receivedEvents.get(0).getStringValue(1));
        assertTrue("Logger name should be DynamicTestLogger:xyz123",
            receivedEvents.get(0).getStringValue(3).contains("DynamicTestLogger:xyz123"));
        assertEquals("Message from dynamic logger 1", receivedEvents.get(0).getStringValue(4));

        assertEquals("WARN", receivedEvents.get(1).getStringValue(1));
        assertEquals("Message from dynamic logger 2", receivedEvents.get(1).getStringValue(4));
    }

    @Test
    public void testAppenderRemovedFromRootLoggerOnStop() throws Exception {
        setupDriver(LogbackDriverConfig.LogbackLevel.INFO);

        // Create some loggers
        org.slf4j.Logger logger1 = LoggerFactory.getLogger("TestLogger1");
        org.slf4j.Logger logger2 = LoggerFactory.getLogger("TestLogger2:moduleId");

        driver.start();
        Thread.sleep(100);

        // Verify appender is attached to root logger only
        Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

        assertTrue("Root logger should have appender before stop",
            hasSensorHubAppender(rootLogger));

        // Stop the driver
        driver.stop();
        Thread.sleep(100);

        // Verify appender is removed from root logger
        assertFalse("Root logger should not have appender after stop",
            hasSensorHubAppender(rootLogger));
    }

    @Test
    public void testModuleSpecificLoggerCapture() throws Exception {
        setupDriver(LogbackDriverConfig.LogbackLevel.DEBUG);
        addEventListener(3);
        driver.start();

        Thread.sleep(100);

        // Simulate module-specific loggers like SensorHub uses
        org.slf4j.Logger moduleLogger1 = LoggerFactory.getLogger("FakeWeatherSensor:abc123");
        org.slf4j.Logger moduleLogger2 = LoggerFactory.getLogger("ConSysApiService:def456");
        org.slf4j.Logger classLogger = LoggerFactory.getLogger("org.sensorhub.impl.sensor.SomeClass");

        // Set loggers to DEBUG level so debug messages are actually logged
        ((Logger)moduleLogger1).setLevel(ch.qos.logback.classic.Level.DEBUG);
        ((Logger)moduleLogger2).setLevel(ch.qos.logback.classic.Level.DEBUG);
        ((Logger)classLogger).setLevel(ch.qos.logback.classic.Level.DEBUG);

        Thread.sleep(50);

        moduleLogger1.error("Error from fake weather sensor");
        moduleLogger2.info("Info from consys api service");
        classLogger.debug("Debug from some class");

        boolean received = eventLatch.await(2, TimeUnit.SECONDS);
        assertTrue("Should receive all events from module-specific loggers", received);
        assertEquals("Should receive 3 events", 3, receivedEvents.size());

        // Verify all events were captured
        assertEquals("ERROR", receivedEvents.get(0).getStringValue(1));
        assertTrue(receivedEvents.get(0).getStringValue(3).contains("FakeWeatherSensor:abc123"));

        assertEquals("INFO", receivedEvents.get(1).getStringValue(1));
        assertTrue(receivedEvents.get(1).getStringValue(3).contains("ConSysApiService:def456"));

        assertEquals("DEBUG", receivedEvents.get(2).getStringValue(1));
        assertTrue(receivedEvents.get(2).getStringValue(3).contains("org.sensorhub.impl.sensor.SomeClass"));
    }

    @Test
    public void testSeparateLoggerContextCapture() throws Exception {
        setupDriver(LogbackDriverConfig.LogbackLevel.INFO);
        addEventListener(2);
        driver.start();

        Thread.sleep(100);

        // Simulate what ModuleUtils.createModuleLogger() does:
        // Create a SEPARATE LoggerContext for a module logger
        LoggerContext separateContext = new LoggerContext();
        separateContext.setName("TestModule:abc123");

        // Create a logger in this separate context
        Logger moduleLogger = separateContext.getLogger("org.sensorhub.test.TestModule:abc123");

        // Manually attach to this context (simulating what scanForModuleContexts does)
        driver.attachToContext(separateContext);

        Thread.sleep(100);

        // Verify the separate context has our appender
        Logger separateRoot = separateContext.getLogger(Logger.ROOT_LOGGER_NAME);
        assertTrue("Separate context root logger should have SensorHubAppender",
            hasSensorHubAppender(separateRoot));

        // Log from the module logger in the separate context
        moduleLogger.info("Info from separate context logger");
        moduleLogger.error("Error from separate context logger");

        // Wait for events to be captured
        boolean received = eventLatch.await(2, TimeUnit.SECONDS);
        assertTrue("Should receive events from separate context logger", received);
        assertEquals("Should receive 2 events", 2, receivedEvents.size());

        // Verify the events were captured correctly
        assertEquals("INFO", receivedEvents.get(0).getStringValue(1));
        assertTrue("Logger name should contain TestModule:abc123",
            receivedEvents.get(0).getStringValue(3).contains("TestModule:abc123"));
        assertEquals("Info from separate context logger", receivedEvents.get(0).getStringValue(4));

        assertEquals("ERROR", receivedEvents.get(1).getStringValue(1));
        assertEquals("Error from separate context logger", receivedEvents.get(1).getStringValue(4));
    }

    private boolean hasSensorHubAppender(Logger logger) {
        if (logger == null) return false;

        java.util.Iterator<Appender<ILoggingEvent>> iter = logger.iteratorForAppenders();
        while (iter.hasNext()) {
            Appender<ILoggingEvent> appender = iter.next();
            if (appender instanceof SensorHubAppender) {
                return true;
            }
        }
        return false;
    }
}