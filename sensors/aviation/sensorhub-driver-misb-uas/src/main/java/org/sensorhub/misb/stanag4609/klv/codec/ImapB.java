/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.misb.stanag4609.klv.codec;

import java.math.BigInteger;

/**
 * Immutable class implementing the IMAPB algorithm defined in MISB ST 1201.
 * The same instance can be reused to decode multiple values
 */
public class ImapB {
    
    static double LOG2_BASE = Math.log(2);
    double min;
    double max;
    double sR;
    double zOff;        
    
    /**
     * Construct the IMAPB function to be applied
     * @param min min parameter value
     * @param max max parameter value
     * @param len known uint length
     */
    public ImapB(double min, double max, int len) {
        
        var a = this.min = min;
        var b = this.max = max;
        var bPow = Math.ceil(log2(b-a));
        var dPow = 8*len-1;
        this.sR = Math.pow(2, bPow-dPow);
        this.zOff = (a < 0 && b > 0.0) ? a/sR - Math.floor(a/sR) : 0.0;
    }        
    
    private double log2(double val) {
        return Math.log(val) / LOG2_BASE;
    }        
    
    /**
     * Converts the unsigned integer value passed as byte[] to floating point representation
     * @param data unsigned integer data
     * @return double value of parameter after mapping
     */
    public double intToDouble(byte[] data) {
        
        var y = new BigInteger(1, data).doubleValue(); // int value is always positive
        return sR * (y - zOff) + min;
    }

    public static byte[] doubleToImapB(ImapB func, double value) {
        // Reverse of: sR * (y - zOff) + min
        double y = (value - func.min) / func.sR + func.zOff;

        long ui = Math.round(y);

        // Determine byte length from ST1201 rules
        int len = (int) Math.ceil((Math.log(func.max - func.min) / Math.log(2) + 1) / 8.0);
        byte[] out = new byte[len];

        for (int i = len - 1; i >= 0; i--) {
            out[i] = (byte) (ui & 0xFF);
            ui >>= 8;
        }

        return out;
    }
}