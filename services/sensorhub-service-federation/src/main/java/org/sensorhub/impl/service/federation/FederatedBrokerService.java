package org.sensorhub.impl.service.federation;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.service.IServiceModule;
import org.sensorhub.impl.module.AbstractModule;

/**
 * OSH service entry point for the federation broker.
 *
 * Mirrors the Python broker's main.py: it loads the node environment (here from
 * config rather than broker-env2.json), then runs discovery + mirroring. The
 * load/discover work runs on a background thread because it opens MQTT
 * connections and sleeps for stabilization, which must not block module start.
 */
public class FederatedBrokerService extends AbstractModule<FederatedBrokerConfig>
        implements IServiceModule<FederatedBrokerConfig>
{
    private OSHDataBroker broker;
    private Thread workerThread;
    private Thread statusThread;

    @Override
    protected void doStart() throws SensorHubException
    {
        if (config == null)
            throw new SensorHubException("Federated broker configuration is missing");

        broker = new OSHDataBroker();

        // main.py: broker.load_env_file(...); broker.discover_all(); then idle.
        // The work is wrapped: an exception here (most often the initial MQTT
        // connect) would otherwise kill the thread silently while the module
        // still reported "started", leaving a broker that discovers nothing
        // with no indication of why.
        workerThread = new Thread(() ->
        {
            try
            {
                broker.loadFromConfig(config);
                broker.discoverAll();
            }
            catch (Throwable e)
            {
                getLogger().error("Federation broker startup failed", e);
                reportError("Federation broker startup failed: " + e, e);
            }
        }, "federation-broker");
        workerThread.setDaemon(true);
        workerThread.setUncaughtExceptionHandler((t, e) ->
        {
            getLogger().error("Uncaught error in {}", t.getName(), e);
            reportError("Federation broker thread died: " + e, e);
        });
        workerThread.start();

        // Dedicated status thread: periodically publishes a federation summary
        // to the module status, decoupled from discovery so it reports even
        // while discovery is still running.
        if (config.statusReportIntervalSeconds > 0)
        {
            statusThread = new Thread(this::runStatusLoop, "federation-status");
            statusThread.setDaemon(true);
            statusThread.start();
        }
        else
        {
            reportStatus("Federated broker service started");
        }
    }

    /**
     * Publish a federation status summary on a fixed interval until interrupted.
     * Reads live counts from the broker; safe to run before discovery populates
     * anything (it just reports zeros until mirrors exist).
     */
    private void runStatusLoop()
    {
        long intervalMs = config.statusReportIntervalSeconds * 1000L;
        while (!Thread.currentThread().isInterrupted())
        {
            OSHDataBroker b = broker;
            if (b != null)
            {
                reportStatus(String.format(
                        "Federating: %d commander(s), %d remote(s); %d datastream(s), %d control stream(s) mirrored; %d active worker thread(s)",
                        b.getCommandNodes().size(), b.getRemoteNodes().size(),
                        b.getDsMap().size(), b.getCsMap().size(), b.getWorkerThreads().size()));
            }
            try
            {
                Thread.sleep(intervalMs);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    @Override
    protected void doStop() throws SensorHubException
    {
        if (statusThread != null)
        {
            statusThread.interrupt();
            statusThread = null;
        }
        if (workerThread != null)
        {
            workerThread.interrupt();
            workerThread = null;
        }
        if (broker != null)
        {
            broker.shutdown();
            broker = null;
        }

        reportStatus("Federated broker service stopped");
    }

    public OSHDataBroker getBroker()
    {
        return broker;
    }
}
