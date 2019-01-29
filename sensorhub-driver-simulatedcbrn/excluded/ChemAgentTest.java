package org.sensorhub.test.impl.sensor.simulatedcbrn;

import datasimulation.ChemAgent;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by Ianpa on 5/10/2017.
 */
public class ChemAgentTest
{
    ChemAgent testAgent = new ChemAgent("VX");

    @Test
    public void getThreat() throws Exception
    {
        testAgent.setThreatLevel(0);
        assertNotNull(testAgent.getThreat());
        System.out.println(testAgent.getThreat());

        testAgent.setThreatLevel(5);
        assertNotNull(testAgent.getThreat());
        System.out.println(testAgent.getThreat());

        testAgent.setThreatLevel(10);
        assertNotNull(testAgent.getThreat());
        System.out.println(testAgent.getThreat());

        for(int i = 0; i < 1000; i++)
        {
            testAgent.update();
            assertNotNull(testAgent.getThreat());
            System.out.println(testAgent.getThreat());
        }
    }

    @Test
    public void getThreatLevel() throws Exception
    {
        testAgent.setThreatLevel(0);
        assertNotNull(testAgent.getThreatLevel());
        System.out.println(testAgent.getThreatLevel());

        testAgent.setThreatLevel(5);
        assertNotNull(testAgent.getThreatLevel());
        System.out.println(testAgent.getThreatLevel());

        testAgent.setThreatLevel(10);
        assertNotNull(testAgent.getThreatLevel());
        System.out.println(testAgent.getThreatLevel());
    }

    @Test
    public void update() throws Exception {
    }

    @Test
    public void getBars() throws Exception {
    }

    @Test
    public void getAgentID() throws Exception {
    }

    @Test
    public void getAgentClass() throws Exception {
    }

}