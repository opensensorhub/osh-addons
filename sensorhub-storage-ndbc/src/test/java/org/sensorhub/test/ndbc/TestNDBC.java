package org.sensorhub.test.ndbc;

import org.junit.Test;
import org.sensorhub.impl.ndbc.NDBCArchive;

public class TestNDBC
{
    @Test
    public void test() throws Exception
    {
    	NDBCArchive storage = new NDBCArchive();
    	storage.start();
    }
}
