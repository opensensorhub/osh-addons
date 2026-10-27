package org.sensorhub.impl.service.federation.events;

import java.time.Instant;

/**
 * Port of events.EventBuilder.
 */
public class EventBuilder
{
    private Event event;

    public EventBuilder()
    {
        this.event = Event.blankEvent();
    }

    public EventBuilder withType(DefaultEventTypes eventType)
    {
        this.event.type = eventType;
        return this;
    }

    public EventBuilder withTopic(String topic)
    {
        this.event.topic = topic;
        return this;
    }

    public EventBuilder withData(Object data)
    {
        this.event.data = data;
        return this;
    }

    public EventBuilder withProducer(Object producer)
    {
        this.event.producer = producer;
        return this;
    }

    public EventBuilder withTimestamp(Instant timestamp)
    {
        this.event.timestamp = timestamp;
        return this;
    }

    public Event build()
    {
        Event built = new Event(event.timestamp, event.type, event.topic, event.data, event.producer);
        reset();
        return built;
    }

    public void reset()
    {
        this.event = Event.blankEvent();
    }

    public static String createTopic(DefaultEventTypes baseTopic, String resourceId)
    {
        if (resourceId != null)
            return baseTopic.value() + "/" + resourceId;
        return baseTopic.value();
    }
}
