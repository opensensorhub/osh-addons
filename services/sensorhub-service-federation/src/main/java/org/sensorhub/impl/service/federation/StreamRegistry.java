package org.sensorhub.impl.service.federation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Port of broker.registry.StreamRegistry — lock-guarded bookkeeping of active
 * pumps for the reconcile loop. Pure state; thread stop/join is orchestrated by
 * {@link ReconcileMixin}, never under this lock.
 */
public class StreamRegistry
{
    private final Map<String, PumpEntry> entries = new HashMap<>();
    private int cycle = 0;

    public synchronized int nextCycle()
    {
        return ++cycle;
    }

    public synchronized void add(PumpEntry entry)
    {
        entries.put(entry.key, entry);
    }

    public synchronized PumpEntry get(String key)
    {
        return entries.get(key);
    }

    public synchronized Set<String> keys()
    {
        return new HashSet<>(entries.keySet());
    }

    public synchronized List<PumpEntry> snapshot()
    {
        return new ArrayList<>(entries.values());
    }

    public synchronized void markSeen(String key, int cycle)
    {
        PumpEntry e = entries.get(key);
        if (e != null)
        {
            e.lastSeenCycle = cycle;
            e.absentCycles = 0;
        }
    }

    /** Increment and return the entry's consecutive-absent counter. */
    public synchronized int markAbsent(String key)
    {
        PumpEntry e = entries.get(key);
        if (e == null)
            return 0;
        return ++e.absentCycles;
    }

    public synchronized PumpEntry remove(String key)
    {
        return entries.remove(key);
    }

    public synchronized int size()
    {
        return entries.size();
    }
}
