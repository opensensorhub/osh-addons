package org.sensorhub.impl.service.sta.ingest;

import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.command.*;
import org.sensorhub.api.event.IEventListener;

import java.util.concurrent.CompletableFuture;

public class STAProxyControl implements IStreamingControlInterface {
    @Override
    public ICommandReceiver getParentProducer() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public DataComponent getCommandDescription() {
        return null;
    }

    @Override
    public void validateCommand(ICommandData command) throws CommandException {

    }

    @Override
    public CompletableFuture<ICommandStatus> submitCommand(ICommandData command) {
        return null;
    }

    @Override
    public void registerListener(IEventListener listener) {

    }

    @Override
    public void unregisterListener(IEventListener listener) {

    }
}
