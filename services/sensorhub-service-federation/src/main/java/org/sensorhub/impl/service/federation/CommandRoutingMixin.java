package org.sensorhub.impl.service.federation;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.sensorhub.impl.service.federation.oshconnect.APIHelper;
import org.sensorhub.impl.service.federation.oshconnect.APIResourceTypes;
import org.sensorhub.impl.service.federation.oshconnect.ApiResponse;
import org.sensorhub.impl.service.federation.oshconnect.ContentTypes;
import org.sensorhub.impl.service.federation.oshconnect.ControlStream;
import org.sensorhub.impl.service.federation.oshconnect.System;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.sensorhub.impl.service.federation.BrokerLogging.log;

/**
 * Port of broker.command_routing.CommandRoutingMixin.
 */
public interface CommandRoutingMixin extends BrokerContext
{
    default void subscribeToCommanderControlstream(ControlStream commanderCs, System remoteSys, ControlStream remoteCs)
    {
        String csId = commanderCs.getId() != null ? commanderCs.getId() : "unknown";
        String remoteCsId = remoteCs.getId() != null ? remoteCs.getId() : "unknown";

        try
        {
            // PULL = subscribe to the command topic; broker reads commands sent
            // by clients and forwards them.
            commanderCs.setConnectionMode(org.sensorhub.impl.service.federation.oshconnect.StreamableModes.PULL);
            commanderCs.initialize();

            String topic = commanderCs.getTopic();
            if (topic != null && topic.startsWith("/"))
            {
                String stripped = topic;
                while (stripped.startsWith("/"))
                    stripped = stripped.substring(1);
                commanderCs.setTopic(stripped);
                log.debug("[CONTROLSTREAM] fixed topic from {} to {}", topic, stripped);
            }

            commanderCs.start();

            Thread cmdThread = new Thread(() -> commandForwardingLoop(commanderCs, remoteSys, remoteCs));
            cmdThread.setDaemon(true);
            getWorkerThreads().add(cmdThread);
            cmdThread.start();
            log.debug("Started command forwarding: {} -> {}", csId, remoteCsId);
        }
        catch (Exception e)
        {
            log.error("[CONTROLSTREAM] failed to subscribe to commander control stream {}: {}", csId, e.toString());
        }
    }

    /**
     * Continuously poll the commander control stream's inbound deque and forward
     * commands to the remote.
     */
    default void commandForwardingLoop(ControlStream commanderCs, System remoteSys, ControlStream remoteCs)
    {
        String cmdCsId = commanderCs.getId() != null ? commanderCs.getId() : "unknown";

        while (true)
        {
            if (Thread.currentThread().isInterrupted())
                return;
            try
            {
                java.util.Deque<byte[]> inbound = commanderCs.getInboundDeque();

                if (!inbound.isEmpty())
                {
                    byte[] commandPayload = inbound.pollFirst();
                    log.info("Command received on {}", cmdCsId);
                    log.debug("Command payload: {}", new String(commandPayload, StandardCharsets.UTF_8));

                    forwardCommandToRemote(commandPayload, remoteSys, remoteCs);
                }
                else
                {
                    Thread.sleep(500); // small sleep to avoid busy-waiting
                }
            }
            catch (InterruptedException ie)
            {
                Thread.currentThread().interrupt();
                return;
            }
            catch (Exception e)
            {
                log.error("[COMMAND-FORWARD] error in loop for {}: {}", cmdCsId, e.toString());
                try
                {
                    Thread.sleep(1000); // back off on error
                }
                catch (InterruptedException ie)
                {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    default void forwardCommandToRemote(Object commandPayload, System remoteSys, ControlStream remoteCs)
    {
        String remoteCsId = remoteCs.getId() != null ? remoteCs.getId() : "unknown";

        if (commandPayload instanceof byte[])
            commandPayload = new String((byte[]) commandPayload, StandardCharsets.UTF_8);

        JsonObject commandDict;
        if (commandPayload instanceof String)
        {
            try
            {
                commandDict = JsonParser.parseString((String) commandPayload).getAsJsonObject();
            }
            catch (Exception e)
            {
                log.error("[CMD-FORWARD] failed to parse command JSON: {}", e.toString());
                return;
            }
        }
        else if (commandPayload instanceof JsonObject)
        {
            commandDict = (JsonObject) commandPayload;
        }
        else
        {
            log.error("[CMD-FORWARD] unsupported payload type {}",
                    commandPayload == null ? "null" : commandPayload.getClass().getSimpleName());
            return;
        }

        // Strip commander-side identifiers; remote will assign its own.
        JsonObject body = new JsonObject();
        for (Map.Entry<String, com.google.gson.JsonElement> e : commandDict.entrySet())
        {
            if (!e.getKey().equals("id") && !e.getKey().equals("controlstream@id"))
                body.add(e.getKey(), e.getValue());
        }

        try
        {
            APIHelper api = remoteCs.getParentNode().getApiHelper();
            ApiResponse res = api.createResource(APIResourceTypes.COMMAND, body.toString(), remoteCsId,
                    Map.of("Content-Type", ContentTypes.JSON.value()));
            if (res.ok())
                log.info("Command forwarded to {}: HTTP {}", remoteCsId, res.statusCode());
            else
                log.error("Remote rejected command for {}: HTTP {} — {}", remoteCsId, res.statusCode(), res.text());
        }
        catch (Exception e)
        {
            log.error("Failed to POST command to {}: {}", remoteCsId, e.toString());
        }
    }

    /**
     * Route a command from a commander control stream to the corresponding remote.
     */
    default void routeCommand(String commanderCsId, JsonObject command)
    {
        if (!getCsMap().containsKey(commanderCsId))
        {
            log.warn("No mapping for commander_cs_id={}", commanderCsId);
            return;
        }
        Map.Entry<System, ControlStream> entry = getCsMap().get(commanderCsId);
        forwardCommandToRemote(command, entry.getKey(), entry.getValue());
    }
}
