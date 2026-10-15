package org.sensorhub.impl.service.federation.events;

import java.time.Instant;

/**
 * Port of events.Event.
 */
public class Event
{
    public Instant timestamp;
    public DefaultEventTypes type;
    public String topic;
    public Object data;
    public Object producer;

    public Event(Instant timestamp, DefaultEventTypes type, String topic, Object data, Object producer)
    {
        this.timestamp = timestamp;
        this.type = type;
        this.topic = topic;
        this.data = data;
        this.producer = producer;
    }

    public static Event blankEvent()
    {
        return new Event(Instant.now(), DefaultEventTypes.NEW_OBSERVATION, "", null, null);
    }
}
