/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.hivemq;

import static org.junit.Assert.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;


public class TestMqttServerConfig
{
    static String defaultConfig() throws IOException
    {
        try (var in = MqttServer.class.getResourceAsStream("config.xml"))
        {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }


    static Path write(String text) throws IOException
    {
        var file = Files.createTempFile("hivemq-config", ".xml");
        file.toFile().deleteOnExit();
        Files.writeString(file, text, StandardCharsets.UTF_8);
        return file;
    }


    /** The earlier default: the current one with wildcards disabled */
    static String earlierDefault() throws IOException
    {
        var text = defaultConfig();
        var old = text.replaceFirst("(<wildcard-subscriptions>\\s*<enabled>)true", "$1false");
        assertNotEquals(text, old);
        return old;
    }


    @Test
    public void testDefaultEnablesWildcards() throws IOException
    {
        var file = write(defaultConfig());
        assertFalse(MqttServer.wildcardsDisabled(file));
        assertFalse(MqttServer.isOldDefaultConfig(file));
    }


    @Test
    public void testEarlierDefaultIsReplaced() throws IOException
    {
        var old = earlierDefault();
        assertTrue(MqttServer.isOldDefaultConfig(write(old)));

        // line endings and indentation do not matter
        assertTrue(MqttServer.isOldDefaultConfig(write(old.replace("\n", "\r\n").replace("    ", "\t"))));

        // the first default had no persistence element
        var first = old.replaceFirst("\\s*<persistence>\\s*<mode>in-memory</mode>\\s*</persistence>", "");
        assertNotEquals(old, first);
        assertTrue(MqttServer.isOldDefaultConfig(write(first)));
    }


    @Test
    public void testChangedConfigIsKept() throws IOException
    {
        // a user changed the port: keep the file, warn about wildcards
        var file = write(earlierDefault().replace("<port>1883</port>", "<port>1884</port>"));
        assertFalse(MqttServer.isOldDefaultConfig(file));
        assertTrue(MqttServer.wildcardsDisabled(file));
    }
}
