/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2026 Sensia Software LLC. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.consys.mqtt;

import static org.junit.Assert.*;
import static org.sensorhub.impl.service.consys.mqtt.ConSysTopicValidator.ResourceType.*;
import org.junit.Test;
import org.sensorhub.impl.service.consys.mqtt.ConSysTopicValidator.ResourceType;


/**
 * Unit tests for {@link ConSysTopicValidator}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Exact topics (no wildcards) for every resource type</li>
 *   <li>Single-level wildcard ({@code +}) at every valid ID position</li>
 *   <li>Multi-level wildcard ({@code #}) at valid ID positions</li>
 *   <li>Mixed — concrete resource IDs with wildcards in other slots</li>
 *   <li>Rejection of wildcards in keyword positions (e.g. {@code +/datastreams/+})</li>
 *   <li>Rejection of unknown or malformed paths</li>
 *   <li>{@link ConSysTopicValidator#hasWildcard(String)}</li>
 * </ul>
 * </p>
 */
public class TestConSysTopicValidator
{
    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void assertMatches(ResourceType expected, String path)
    {
        var result = ConSysTopicValidator.matchEventTopic(path);
        assertTrue("Expected " + expected + " for path '" + path + "' but got empty", result.isPresent());
        assertEquals("Wrong ResourceType for path '" + path + "'", expected, result.get());
    }

    private static void assertNoMatch(String path)
    {
        var result = ConSysTopicValidator.matchEventTopic(path);
        assertFalse("Expected no match for path '" + path + "' but got " + result.orElse(null), result.isPresent());
    }


    // =========================================================================
    // Exact topics (no wildcards) — concrete IDs in every slot
    // =========================================================================

    @Test
    public void testExact_system()
    {
        assertMatches(SYSTEM, "systems/abc123");
    }

    @Test
    public void testExact_subsystem()
    {
        assertMatches(SYSTEM, "systems/abc123/subsystems/def456");
    }

    @Test
    public void testExact_datastream()
    {
        assertMatches(DATASTREAM, "systems/abc123/datastreams/ds001");
    }

    @Test
    public void testExact_controlstream()
    {
        assertMatches(CONTROLSTREAM, "systems/abc123/controlstreams/cs001");
    }

    @Test
    public void testExact_observation()
    {
        assertMatches(OBSERVATION, "systems/abc123/datastreams/ds001/observations/obs001");
    }

    @Test
    public void testExact_command()
    {
        assertMatches(COMMAND, "systems/abc123/controlstreams/cs001/commands/cmd001");
    }

    @Test
    public void testExact_commandStatus()
    {
        assertMatches(COMMAND, "systems/abc123/controlstreams/cs001/commands/cmd001/status/st001");
    }

    @Test
    public void testExact_commandResult()
    {
        assertMatches(COMMAND, "systems/abc123/controlstreams/cs001/commands/cmd001/result/res001");
    }

    @Test
    public void testExact_systemDeployment()
    {
        assertMatches(SYSTEM, "systems/abc123/deployments/dep001");
    }

    @Test
    public void testExact_deployment()
    {
        assertMatches(DEPLOYMENT, "deployments/dep001");
    }

    @Test
    public void testExact_subdeployment()
    {
        assertMatches(DEPLOYMENT, "deployments/dep001/subdeployments/sub001");
    }

    @Test
    public void testExact_procedure()
    {
        assertMatches(PROCEDURE, "procedures/proc001");
    }

    @Test
    public void testExact_property()
    {
        assertMatches(PROPERTY, "properties/prop001");
    }

    // =========================================================================
    // Single-level wildcard (+) at ID positions
    // =========================================================================

    @Test
    public void testPlus_allSystems()
    {
        // {nodeId}/systems/+ — "all top-level systems"
        assertMatches(SYSTEM, "systems/+");
    }

    @Test
    public void testPlus_allSubsystemsOfAllSystems()
    {
        assertMatches(SYSTEM, "systems/+/subsystems/+");
    }

    @Test
    public void testPlus_allDatastreamsOfAllSystems()
    {
        assertMatches(DATASTREAM, "systems/+/datastreams/+");
    }

    @Test
    public void testPlus_allDatastreamsOfOneSystem()
    {
        assertMatches(DATASTREAM, "systems/abc123/datastreams/+");
    }

    @Test
    public void testPlus_allControlstreamsOfAllSystems()
    {
        assertMatches(CONTROLSTREAM, "systems/+/controlstreams/+");
    }

    @Test
    public void testPlus_observationsFromOneDatastream()
    {
        assertMatches(OBSERVATION, "systems/abc123/datastreams/ds001/observations/+");
    }

    @Test
    public void testPlus_observationsFromAllDatastreamsOfOneSystem()
    {
        // {nodeId}/systems/134/datastreams/+/observations/+ per spec
        assertMatches(OBSERVATION, "systems/abc123/datastreams/+/observations/+");
    }

    @Test
    public void testPlus_observationsFromAllDatastreamsOfAllSystems()
    {
        assertMatches(OBSERVATION, "systems/+/datastreams/+/observations/+");
    }

    @Test
    public void testPlus_mixedConcreteIdAndWildcard()
    {
        // concrete system ID + wildcard datastream — valid per spec
        assertMatches(DATASTREAM, "systems/03ie1mkrr9r0/datastreams/+");
    }

    @Test
    public void testPlus_mixedConcreteAndWildcard_observations()
    {
        // the failing case from the issue report
        assertMatches(OBSERVATION, "systems/03ie1mkrr9r0/datastreams/+/observations/+");
    }

    @Test
    public void testPlus_allProcedures()
    {
        assertMatches(PROCEDURE, "procedures/+");
    }

    @Test
    public void testPlus_allProperties()
    {
        assertMatches(PROPERTY, "properties/+");
    }

    @Test
    public void testPlus_allDeployments()
    {
        assertMatches(DEPLOYMENT, "deployments/+");
    }

    // =========================================================================
    // Multi-level wildcard (#) at ID positions
    // =========================================================================

    @Test
    public void testHash_everythingUnderOneSystem()
    {
        // {nodeId}/systems/489/# — "everything under a system"
        assertMatches(SYSTEM, "systems/abc123/#");
    }

    @Test
    public void testHash_everythingUnderAllSystems()
    {
        assertMatches(SYSTEM, "systems/+/#");
    }

    @Test
    public void testHash_allSubsystemsAtAllLevels()
    {
        // {nodeId}/systems/+/subsystems/# per spec
        assertMatches(SYSTEM, "systems/+/subsystems/#");
    }

    @Test
    public void testHash_everythingUnderDatastreams()
    {
        // # at the datastream ID position — covers datastream + all obs beneath
        assertMatches(DATASTREAM, "systems/abc123/datastreams/#");
    }

    @Test
    public void testHash_everythingUnderAllDatastreams()
    {
        assertMatches(DATASTREAM, "systems/+/datastreams/#");
    }

    @Test
    public void testHash_obsUnderOneDatastream()
    {
        assertMatches(OBSERVATION, "systems/abc123/datastreams/ds001/observations/#");
    }

    // =========================================================================
    // Invalid: wildcard in keyword (literal) position
    // =========================================================================

    @Test
    public void testInvalid_plusInKeywordPosition_systems()
    {
        // + where "systems" keyword must be
        assertNoMatch("+/abc123");
    }

    @Test
    public void testInvalid_plusInKeywordPosition_datastreams()
    {
        // systems/{id}/+/{id} — wildcard where "datastreams" must be
        assertNoMatch("systems/abc123/+/ds001");
    }

    @Test
    public void testInvalid_plusInKeywordPosition_observations()
    {
        // systems/{id}/datastreams/{id}/+/{id} — wildcard where "observations" must be
        assertNoMatch("systems/abc123/datastreams/ds001/+/obs001");
    }

    @Test
    public void testInvalid_hashInKeywordPosition()
    {
        // # where "systems" must be
        assertNoMatch("#");
    }

    @Test
    public void testInvalid_hashNotAtEnd()
    {
        // # must be the final segment
        assertNoMatch("systems/#/datastreams/+");
    }

    // =========================================================================
    // Invalid: unknown or malformed paths
    // =========================================================================

    @Test
    public void testInvalid_empty()
    {
        assertNoMatch("");
    }

    @Test
    public void testInvalid_unknownTopLevelKeyword()
    {
        assertNoMatch("sensors/abc123");
    }

    @Test
    public void testInvalid_tooManySegments()
    {
        // No pattern has 7 segments like this
        assertNoMatch("systems/abc123/datastreams/ds001/observations/obs001/extra");
    }

    @Test
    public void testInvalid_missingIdSegment()
    {
        // "systems" alone — no ID segment after it
        assertNoMatch("systems");
    }

    @Test
    public void testInvalid_nodeIdNotStripped()
    {
        // matchEventTopic expects prefix already stripped — raw topic should not match
        assertNoMatch("oshex/systems/abc123");
    }

    // =========================================================================
    // hasWildcard
    // =========================================================================

    @Test
    public void testHasWildcard_noWildcard()
    {
        assertFalse(ConSysTopicValidator.hasWildcard("systems/abc123/datastreams/ds001"));
    }

    @Test
    public void testHasWildcard_singleLevel()
    {
        assertTrue(ConSysTopicValidator.hasWildcard("systems/+/datastreams/+"));
    }

    @Test
    public void testHasWildcard_multiLevel()
    {
        assertTrue(ConSysTopicValidator.hasWildcard("systems/abc123/#"));
    }

    @Test
    public void testHasWildcard_both()
    {
        assertTrue(ConSysTopicValidator.hasWildcard("systems/+/datastreams/#"));
    }

    @Test
    public void testHasWildcard_empty()
    {
        assertFalse(ConSysTopicValidator.hasWildcard(""));
    }
}