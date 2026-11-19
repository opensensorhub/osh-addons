/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.process.video;

import org.bytedeco.javacpp.BytePointer;
import org.vast.data.DataBlockByte;


/**
 * <p>
 * DataBlockByte extension carrying a native JavaCPP pointer for direct
 * use in subsequent native code (e.g. FFMPEG decoder -> OpenCV algo)
 * </p>
 *
 * @author Alex Robin
 * @since Jun 7, 2021
 */
public class DataBlockByteNative extends DataBlockByte
{
    private static final long serialVersionUID = -9198205679143401216L;
    
    BytePointer pointer;
    
    
    public DataBlockByteNative(BytePointer pointer, int length)
    {
        var frameData = new byte[length];
        pointer.get(frameData);
        setUnderlyingObject(frameData);
    }
    
    
    public BytePointer getNativePointer()
    {
        return this.pointer;
    }
}
