/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.consys.mqtt;

import java.util.function.LongSupplier;


/**
 * <p>
 * Counts the observations of one datastream and publishes at most one
 * batch for each time window.
 * </p><p>
 * After a quiet window, the first observations are published at once.
 * Observations that come less than one window after a publication are
 * counted, and published as one batch at the end of that window. A window
 * with no new observations publishes nothing.
 * </p>
 */
final class BatchWindow
{
    /** Receives one batch: the times of its first and last observation event, and the count */
    interface Publisher
    {
        void publish(long startMs, long endMs, long count);
    }

    /** Runs a task after a delay in milliseconds */
    interface Scheduler
    {
        /** @throws java.util.concurrent.RejectedExecutionException if the task cannot run */
        void schedule(Runnable task, long delayMs);
    }

    record Batch(long startMs, long endMs, long count) {}

    final long windowMs;
    final LongSupplier clockMs; // monotonic clock, for the window timing only
    final Scheduler scheduler;
    final Publisher publisher;

    // guarded by this
    private boolean published;
    private long lastPublishMs;
    private boolean flushScheduled;
    private boolean closed;
    private long count;
    private long startMs, endMs;


    /**
     * @param windowMs the window length. 0 publishes each record at once.
     * @param clockMs a monotonic clock in milliseconds
     */
    BatchWindow(long windowMs, LongSupplier clockMs, Scheduler scheduler, Publisher publisher)
    {
        this.windowMs = Math.max(0, windowMs);
        this.clockMs = clockMs;
        this.scheduler = scheduler;
        this.publisher = publisher;
    }


    /**
     * Count {@code n} new observations
     * @param timeMs the time when the observations became visible
     */
    void record(int n, long timeMs)
    {
        if (n <= 0)
            return;

        Batch batch = null;
        synchronized (this)
        {
            if (closed)
                return;

            if (count == 0 || timeMs < startMs)
                startMs = timeMs;
            if (count == 0 || timeMs > endMs)
                endMs = timeMs;
            count += n;

            if (flushScheduled)
                return;

            var now = clockMs.getAsLong();
            var elapsed = now - lastPublishMs;
            if (!published || elapsed >= windowMs)
            {
                batch = take(now);
            }
            else
            {
                try
                {
                    scheduler.schedule(this::flush, windowMs - elapsed);
                    flushScheduled = true;
                }
                catch (RuntimeException e)
                {
                    // the publisher is stopping; the next record tries again
                }
            }
        }

        if (batch != null)
            publisher.publish(batch.startMs, batch.endMs, batch.count);
    }


    /** Publish the observations counted since the last publication, if any */
    void flush()
    {
        Batch batch;
        synchronized (this)
        {
            flushScheduled = false;
            if (closed)
                return;
            batch = take(clockMs.getAsLong());
        }

        if (batch != null)
            publisher.publish(batch.startMs, batch.endMs, batch.count);
    }


    /** Stop publishing. Observations not yet published are dropped. */
    synchronized void close()
    {
        closed = true;
    }


    private Batch take(long now)
    {
        if (count == 0)
            return null;
        var batch = new Batch(startMs, endMs, count);
        count = 0;
        published = true;
        lastPublishMs = now;
        return batch;
    }
}
