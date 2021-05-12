/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.misb.stanag4609.klv;

import static org.junit.Assert.assertEquals;
import org.junit.Test;


public class ImapBTest {
    
    /*
     * Test IMAPB conversion using examples from MISB ST 903.5 spec
     */
    @Test
    public void testImapb() {
        
        double dval;
        ImapB imapb;
        
        imapb = new ImapB(0, 180, 2);
        dval = imapb.intToDouble(new byte[] {0x06, 0x40});
        assertEquals(12.5, dval, 1e-8);
                
        imapb = new ImapB(-19.2, 19.2, 3);
        dval = imapb.intToDouble(new byte[] {0x3A, 0x66, 0x67});
        assertEquals(10.0, dval, 1e-8);
        
        imapb = new ImapB(-19.2, 19.2, 3);
        dval = imapb.intToDouble(new byte[] {0x3E, 0x66, 0x67});
        assertEquals(12.0, dval, 1e-8);
        
        imapb = new ImapB(-900., 19000, 2);
        dval = imapb.intToDouble(new byte[] {0x2A, (byte)0x94});
        assertEquals(10000.0, dval, 1e-8);
    }
}
