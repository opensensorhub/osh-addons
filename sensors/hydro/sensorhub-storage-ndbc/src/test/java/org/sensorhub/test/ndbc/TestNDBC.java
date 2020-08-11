package org.sensorhub.test.ndbc;

import org.junit.Test;
import org.sensorhub.impl.ndbc.BuoyParam;
import org.sensorhub.impl.ndbc.NDBCArchive;
import org.sensorhub.impl.ndbc.NDBCConfig;

public class TestNDBC
{
    @Test
    public void test() throws Exception
    {
    	NDBCArchive storage = new NDBCArchive();
    	NDBCConfig config = new NDBCConfig();
    	config.stationIds.add("0Y2W3");
    	config.parameters.add(BuoyParam.AIR_PRESSURE_AT_SEA_LEVEL);
    	storage.init(config);
    	storage.start();
    }
}
