package org.sensorhub.impl.service.federation.events;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Port of events.EventHandler. In Python this is a singleton (``__new__``)
 * whose listener collections are class-level attributes; that shared state is
 * reproduced here with static fields, so every {@code new EventHandler()}
 * observes the same listeners (matching the Python semantics).
 */
public class EventHandler
{
    private static final List<IEventListener> listeners = new ArrayList<>();
    private static final List<IEventListener> toAdd = new ArrayList<>();
    private static final List<IEventListener> toRemove = new ArrayList<>();
    private static final Deque<Event> eventQueue = new ArrayDeque<>();
    private static boolean publishLock = false;

    public void registerListener(IEventListener listener)
    {
        if (!listeners.contains(listener))
        {
            if (!publishLock)
                listeners.add(listener);
            else
                toAdd.add(listener);
        }
    }

    public void unregisterListener(IEventListener listener)
    {
        if (!publishLock)
            listeners.remove(listener);
        else
            toRemove.add(listener);
    }

    public void publish(Event evt)
    {
        if (publishLock)
        {
            eventQueue.add(evt);
        }
        else
        {
            publishLock = true;
            try
            {
                for (IEventListener listener : listeners)
                    listener.handleEvents(evt);
            }
            catch (Exception e)
            {
                // TODO: handle a more specific error
                System.out.println("Error publishing event: " + e);
            }
            finally
            {
                publishLock = false;
                commitChanges();
            }
        }
    }

    public void commitChanges()
    {
        commitRemoves();
        commitAdds();

        while (!eventQueue.isEmpty())
            publish(eventQueue.pollFirst());
    }

    public void commitAdds()
    {
        listeners.addAll(toAdd);
        toAdd.clear();
    }

    public void commitRemoves()
    {
        listeners.removeAll(toRemove);
        toRemove.clear();
    }

    public void clearListeners()
    {
        listeners.clear();
        toAdd.clear();
        toRemove.clear();
    }

    public int getNumListeners()
    {
        return listeners.size();
    }
}
