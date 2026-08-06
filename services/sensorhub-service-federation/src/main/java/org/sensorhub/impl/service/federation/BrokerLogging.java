package org.sensorhub.impl.service.federation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Port of broker.logging_config: the shared "broker" logger and the
 * DEBUG_VERBOSE flag.
 */
public final class BrokerLogging
{
    public static final Logger log = LoggerFactory.getLogger("broker");

    public static final boolean DEBUG_VERBOSE = false;

    private BrokerLogging()
    {
    }
}
