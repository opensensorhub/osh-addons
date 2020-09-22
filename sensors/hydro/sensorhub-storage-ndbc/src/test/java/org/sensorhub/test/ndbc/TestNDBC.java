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
    	config.stationIds.add("46258");
    	config.parameters.add(BuoyParam.SEA_WATER_TEMPERATURE);
    	storage.init(config);
    	storage.start();
    }
}
