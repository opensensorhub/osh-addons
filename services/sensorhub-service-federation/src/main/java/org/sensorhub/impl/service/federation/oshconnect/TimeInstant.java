package org.sensorhub.impl.service.federation.oshconnect;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Port of the slice of oshconnect.timemanagement.TimeInstant the broker uses:
 * {@code now_as_time_instant()} and {@code get_iso_time()}.
 */
public class TimeInstant
{
    // Matches TimeUtils format '%Y-%m-%dT%H:%M:%S.%fZ' (microsecond precision, trailing Z)
    private static final DateTimeFormatter ISO_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'").withZone(ZoneOffset.UTC);

    private final Instant instant;

    private TimeInstant(Instant instant)
    {
        this.instant = instant;
    }

    public static TimeInstant nowAsTimeInstant()
    {
        return new TimeInstant(Instant.now());
    }

    public String getIsoTime()
    {
        return ISO_FORMAT.format(instant);
    }
}
