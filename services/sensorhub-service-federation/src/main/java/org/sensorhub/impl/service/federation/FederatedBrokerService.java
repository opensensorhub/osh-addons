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

    @Override
    protected void doStart() throws SensorHubException
    {
        if (config == null)
            throw new SensorHubException("Federated broker configuration is missing");

        broker = new OSHDataBroker();

        // main.py: broker.load_env_file(...); broker.discover_all(); then idle.
        workerThread = new Thread(() ->
        {
            broker.loadFromConfig(config);
            broker.discoverAll();
        }, "federation-broker");
        workerThread.setDaemon(true);
        workerThread.start();

        reportStatus("Federated broker service started");
    }

    @Override
    protected void doStop() throws SensorHubException
    {
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
