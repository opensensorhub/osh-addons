/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research, LLC. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.process.gpmf;

import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.sensorml.v20.AggregateProcess;
import net.opengis.swe.v20.DataComponent;
import org.junit.Test;
import org.vast.data.DataIterator;
import org.vast.sensorML.SMLHelper;
import org.vast.sensorML.SMLUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Checks the sample chain against the process it drives, so that a change to either output structure
 * does not quietly leave the chain declaring something the process no longer publishes.
 */
public class GpmfProcessChainTest {
    private static final String CHAIN_RESOURCE = "processchain-gpmf.xml";

    @Test
    public void chainDeclaresTheOutputsTheProcessPublishes() throws Exception {
        AggregateProcess chain = readChain();
        GpmfProcess process = new GpmfProcess();
        process.init();

        for (String name : new String[]{GpmfProcess.TELEMETRY_OUTPUT_NAME, GpmfProcess.GPS_OUTPUT_NAME}) {
            var declared = chain.getOutputList().get(name);
            assertNotNull("chain does not declare the '" + name + "' output", declared);

            assertStructureMatches(
                    process.getOutputList().getComponent(name),
                    SMLHelper.getIOComponent(declared));
        }
    }

    @Test
    public void chainWiresTheSourceToTheProcessPorts() throws Exception {
        AggregateProcess chain = readChain();

        List<String> links = new ArrayList<>();
        chain.getConnectionList().forEach(c -> links.add(c.getSource() + " -> " + c.getDestination()));

        // the camera driver's byte array feeds the packet input
        assertTrue(links.toString(), links.contains(
                "components/source0/outputs/data/data -> components/gpmfDecoder/inputs/gpmfPacket"));

        // and both decoder outputs are republished by the chain
        assertTrue(links.toString(), links.contains(
                "components/gpmfDecoder/outputs/gpmf -> outputs/gpmf"));
        assertTrue(links.toString(), links.contains(
                "components/gpmfDecoder/outputs/gps -> outputs/gps"));
    }

    @Test
    public void chainReferencesTheRightProcessesAndCamera() throws Exception {
        AggregateProcess chain = readChain();

        AbstractProcess source = (AbstractProcess) chain.getComponentList().get("source0");
        assertEquals("urn:osh:process:datasource:stream", source.getTypeOf().getHref());
        assertEquals("urn:osh:sensor:georobotix:ffmpeg:video001",
                source.getConfiguration().getSetValueList().get(0).getValue());

        AbstractProcess decoder = (AbstractProcess) chain.getComponentList().get("gpmfDecoder");
        assertEquals(GpmfProcess.INFO.getUri(), decoder.getTypeOf().getHref());
    }

    private static AggregateProcess readChain() throws Exception {
        try (InputStream is = GpmfProcessChainTest.class.getResourceAsStream(CHAIN_RESOURCE)) {
            assertNotNull("chain file not found on the test classpath at " + CHAIN_RESOURCE, is);
            return (AggregateProcess) new SMLUtils(SMLUtils.V2_0).readProcess(is);
        }
    }

    /**
     * Compares two record structures field by field, which is what a chain connection needs to agree on.
     */
    private static void assertStructureMatches(DataComponent expected, DataComponent actual) {
        DataIterator expectedIt = new DataIterator(expected);
        DataIterator actualIt = new DataIterator(actual);

        while (expectedIt.hasNext()) {
            DataComponent expectedComp = expectedIt.next();
            assertTrue("chain is missing component '" + expectedComp.getName() + "'", actualIt.hasNext());

            DataComponent actualComp = actualIt.next();
            assertEquals(expectedComp.getName(), actualComp.getName());
            assertEquals("type of '" + expectedComp.getName() + "'",
                    expectedComp.getClass(), actualComp.getClass());
            assertEquals("definition of '" + expectedComp.getName() + "'",
                    expectedComp.getDefinition(), actualComp.getDefinition());
        }

        assertFalse("chain declares components the process does not publish", actualIt.hasNext());
    }
}