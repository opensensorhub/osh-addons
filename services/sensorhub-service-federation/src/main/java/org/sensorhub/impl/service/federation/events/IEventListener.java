package org.sensorhub.impl.service.federation.events;

import java.util.List;

/**
 * Port of events.IEventListener. Listeners may subscribe to specific topics
 * and/or certain event types.
 */
public interface IEventListener
{
    default List<String> topics()
    {
        return List.of();
    }

    default List<DefaultEventTypes> types()
    {
        return List.of();
    }

    void handleEvents(Event event);
}
