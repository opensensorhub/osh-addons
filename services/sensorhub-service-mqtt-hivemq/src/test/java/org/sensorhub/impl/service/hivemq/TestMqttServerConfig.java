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
        assertFalse(MqttServer.wildcardsDisabled(write(defaultConfig())));
    }


    @Test
    public void testEarlierDefaultDisablesWildcards() throws IOException
    {
        // the config of a node from an earlier version: the server keeps it and logs a warning
        var old = earlierDefault();
        assertTrue(MqttServer.wildcardsDisabled(write(old)));

        // line endings and indentation do not matter
        assertTrue(MqttServer.wildcardsDisabled(write(old.replace("\n", "\r\n").replace("    ", "\t"))));
    }


    @Test
    public void testConfigWithoutWildcardElement() throws IOException
    {
        // no element: no warning
        var text = defaultConfig().replaceFirst("(?s)<wildcard-subscriptions>.*?</wildcard-subscriptions>", "");
        assertNotEquals(defaultConfig(), text);
        assertFalse(MqttServer.wildcardsDisabled(write(text)));
    }
}
