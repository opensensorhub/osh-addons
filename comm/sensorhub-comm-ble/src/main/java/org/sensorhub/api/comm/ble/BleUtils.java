/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.comm.ble;

import java.nio.ByteBuffer;
import java.util.UUID;


public class BleUtils
{
    public static final String BLE_UUID_SUFFIX = "-0000-1000-8000-00805f9b34fb";
    
    
    public static UUID getUUID(int shortUUID)
    {
        String uuidPrefix = String.format("%08X", shortUUID);
        return UUID.fromString(uuidPrefix + BLE_UUID_SUFFIX);
    }
    
    
    /** 
     * Reads a decimal value as a IEEE-11073 32-bits float
     * This is necessary because the default float format in java is IEEE-754
     * @param data byte buffer to read the 4 bytes from
     * @return java float value decoded from the given bytes
     **/
    public static float readHealthFloat32(ByteBuffer data)
    {
        byte b0 = data.get();
        byte b1 = data.get();
        byte b2 = data.get();
        byte b3 = data.get();
        int mantissa = unsignedToSigned((b0 & 0xFF) + ((b1 & 0xFF) << 8) + ((b2 & 0xFF) << 16), 24);
        return (float)(mantissa * Math.pow(10, b3));
    }
    
    
    /**
     * Reads a decimal value as a IEEE-11073 16-bits float
     * @param data byte buffer to read the 2 bytes from
     * @return java float value decoded from the given bytes
     */
    public static float readHealthFloat16(ByteBuffer data)
    {
        byte b0 = data.get();
        byte b1 = data.get();
        int mantissa = unsignedToSigned((b0 & 0xFF) + ((b1 & 0x0F) << 8), 12);
        int exponent = unsignedToSigned((b1 & 0xFF) >> 4, 4);
        return (float)(mantissa * Math.pow(10, exponent));
    }


    private static int unsignedToSigned(int unsigned, int size)
    {
        if ((unsigned & (1 << size - 1)) != 0)
            unsigned = -1 * ((1 << size - 1) - (unsigned & ((1 << size - 1) - 1)));
        return unsigned;
    }
}
