/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.csapi.mqtt;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import org.junit.Test;
import org.sensorhub.impl.service.csapi.mqtt.BatchWindow.Batch;


public class TestBatchWindow
{
    long now = 10_000;
    boolean rejectSchedule;
    final List<Batch> published = new ArrayList<>();
    final List<Runnable> scheduled = new ArrayList<>();
    final List<Long> delays = new ArrayList<>();
    BatchWindow window = newWindow(1000);


    BatchWindow newWindow(long windowMs)
    {
        return new BatchWindow(windowMs, () -> now,
            (task, delay) -> {
                if (rejectSchedule)
                    throw new RejectedExecutionException();
                scheduled.add(task);
                delays.add(delay);
            },
            (start, end, count) -> published.add(new Batch(start, end, count)));
    }


    void runScheduled()
    {
        var tasks = new ArrayList<>(scheduled);
        scheduled.clear();
        tasks.forEach(Runnable::run);
    }


    @Test
    public void testFirstEventAtOnce()
    {
        window.record(1, 500);
        assertEquals(List.of(new Batch(500, 500, 1)), published);
        assertTrue(scheduled.isEmpty());
    }


    @Test
    public void testAfterQuietWindowAtOnce()
    {
        window.record(1, 1);
        now += 1000;
        window.record(1, 2);
        now += 5000;
        window.record(2, 3);
        assertEquals(List.of(new Batch(1, 1, 1), new Batch(2, 2, 1), new Batch(3, 3, 2)), published);
        assertTrue(scheduled.isEmpty());
    }


    @Test
    public void testNextEventsAtEndOfWindow()
    {
        window.record(1, 100);
        for (int i = 1; i < 30; i++)
        {
            now += 33;
            window.record(1, 100 + i);
        }

        // only the first event is out; one flush is scheduled at the end of the window
        assertEquals(1, published.size());
        assertEquals(List.of(1000L - 33), delays);

        now = 11_000;
        runScheduled();
        assertEquals(new Batch(101, 129, 29), published.get(1));
    }


    @Test
    public void testSecondWindowCycle()
    {
        window.record(1, 1);
        now += 10;
        window.record(1, 2);
        now = 11_000;
        runScheduled();
        assertEquals(2, published.size());

        // a record in the next window waits for its end, not for a full window
        now += 400;
        window.record(5, 3);
        assertEquals(2, published.size());
        assertEquals(Long.valueOf(600), delays.get(1));
        now += 600;
        runScheduled();
        assertEquals(new Batch(3, 3, 5), published.get(2));
    }


    @Test
    public void testQuietWindowPublishesNothing()
    {
        window.record(1, 1);
        window.flush();
        now += 5000;
        window.flush();
        assertEquals(1, published.size());
    }


    @Test
    public void testTimeRangeInOrder()
    {
        window.record(1, 100);
        now += 10;
        window.record(2, 70);
        now += 10;
        window.record(1, 200);
        runScheduled();
        assertEquals(new Batch(70, 200, 3), published.get(1));
    }


    @Test
    public void testScheduleRejected()
    {
        window.record(1, 1);
        rejectSchedule = true;
        now += 10;
        window.record(1, 2);
        assertEquals(1, published.size());

        // the window is not stuck: the next record after the window publishes
        rejectSchedule = false;
        now += 1000;
        window.record(1, 3);
        assertEquals(new Batch(2, 3, 2), published.get(1));
    }


    @Test
    public void testCloseDropsPending()
    {
        window.record(1, 1);
        now += 10;
        window.record(1, 2);
        window.close();
        runScheduled();
        now += 5000;
        window.record(1, 3);
        assertEquals(1, published.size());
    }


    @Test
    public void testZeroWindow()
    {
        window = newWindow(0);
        window.record(1, 1);
        window.record(3, 2);
        assertEquals(List.of(new Batch(1, 1, 1), new Batch(2, 2, 3)), published);
        assertTrue(scheduled.isEmpty());
    }
}
