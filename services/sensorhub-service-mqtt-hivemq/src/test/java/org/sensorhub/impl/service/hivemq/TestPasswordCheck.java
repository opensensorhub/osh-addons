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
import org.junit.Test;


public class TestPasswordCheck
{
    static final String MD5_OF_EMPTY = "MD5:d41d8cd98f00b204e9800998ecf8427e";
    static final String MD5_OF_SECRET = "MD5:5ebe2294ecd0e0f08eab7690d2a6ee69";


    static boolean check(String storedPwd, String pwd)
    {
        return OshAuthenticator.passwordMatches(storedPwd, pwd != null ? pwd.toCharArray() : null);
    }


    @Test
    public void testPasswordAccepted()
    {
        assertTrue(check("secret", "secret"));
        assertTrue(check(MD5_OF_SECRET, "secret"));
    }


    @Test
    public void testWrongPasswordRefused()
    {
        assertFalse(check("secret", "other"));
        assertFalse(check("secret", ""));
        assertFalse(check("secret", null));
    }


    @Test
    public void testEmptyStoredPasswordRefused()
    {
        // a CONNECT without a password has an empty password here
        assertFalse(check("", ""));
        assertFalse(check("", null));
        assertFalse(check("", "secret"));
    }


    @Test
    public void testNullStoredPasswordRefused()
    {
        assertFalse(check(null, ""));
        assertFalse(check(null, null));
        assertFalse(check(null, "secret"));
    }


    @Test
    public void testHashOfEmptyPasswordRefused()
    {
        assertFalse(check(MD5_OF_EMPTY, ""));
    }
}
