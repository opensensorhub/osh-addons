/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
Developer are Copyright (C) 2016 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.foscam;

import java.nio.ByteOrder;
import java.util.Collection;
import net.opengis.swe.v20.AllowedTokens;
import net.opengis.swe.v20.AllowedValues;
import net.opengis.swe.v20.BinaryBlock;
import net.opengis.swe.v20.BinaryComponent;
import net.opengis.swe.v20.BinaryEncoding;
import net.opengis.swe.v20.ByteEncoding;
import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataStream;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Text;
import net.opengis.swe.v20.Time;
import org.vast.cdm.common.CDMException;
import org.vast.swe.SWEHelper;


/**
 * <p>
 * Implementation of a helper class to support Foscam video cameras with or without 
 * Pan-Tilt-Zoom (PTZ) control
 * </p>
 *
 * @author Lee Butler <labutler10@gmail.com>
 * @since September 2016
 */
public class FoscamHelper extends SWEHelper
{
    public static final String DEF_VIDEOFRAME = getPropertyUri("VideoFrame");
    
    // PTZ tasking commands
    public static final String TASKING_PTZPRESET = "preset";
    public static final String TASKING_PTZREL = "relMove";


    // system info	
    String cameraBrand = " ";
    String cameraModel = " ";
    String serialNumber = " ";


    public DataChoice getPtzTaskParameters(String name, Collection<String> relMoveNames, Collection<String> presetNames)
    {
        DataChoice commandData = this.newDataChoice();
        commandData.setName(name);
        
        // PTZ Preset Positions
        Text preset = newText();
        preset.setDefinition(getPropertyUri("CameraPresetPositionName"));
        preset.setLabel("Preset Camera Position");
        AllowedTokens presetTokens = newAllowedTokens();
        for (String position : presetNames)
            presetTokens.addValue(position);
        preset.setConstraint(presetTokens);
        commandData.addItem(TASKING_PTZPRESET, preset);
        
        // PTZ Relative Movements
        Text relMove = newText();
        relMove.setDefinition(getPropertyUri("CameraRelativeMovementName"));
        relMove.setLabel("Camera Relative Movements");
        AllowedTokens relMoveTokens = newAllowedTokens();
        for (String position2 : relMoveNames)
        	relMoveTokens.addValue(position2);
        relMove.setConstraint(relMoveTokens);
        commandData.addItem(TASKING_PTZREL, relMove);
        
        return commandData;
    }
    
    
    public DataRecord newVideoFrameRGB(String name, int width, int height)
    {
        Time timeStamp = newTimeStampIsoUTC();        
        DataArray imgArr = newRgbImage(width, height, DataType.BYTE);
        imgArr.setName("img");
        
        DataRecord dataStruct = wrapWithTimeStamp(timeStamp, imgArr);
        dataStruct.setName(name);
        dataStruct.setDefinition(DEF_VIDEOFRAME);
        
        return dataStruct;
    }
    
    
    public DataStream newVideoOutputRGB(String name, int width, int height)
    {
        DataRecord dataStruct = newVideoFrameRGB(name, width, height);
        BinaryEncoding dataEnc = SWEHelper.getDefaultBinaryEncoding(dataStruct);        
        return newDataStream(dataStruct, dataEnc);
    }
    
    
    public DataStream newVideoOutputCODEC(String name, int width, int height, String codec)
    {
        DataRecord dataStruct = newVideoFrameRGB(name, width, height);
        
        // MJPEG encoding
        BinaryEncoding dataEnc = newBinaryEncoding();
        dataEnc.setByteEncoding(ByteEncoding.RAW);
        dataEnc.setByteOrder(ByteOrder.BIG_ENDIAN);
        
        BinaryComponent timeEnc = newBinaryComponent();
        timeEnc.setRef("/" + dataStruct.getComponent(0).getName());
        timeEnc.setCdmDataType(DataType.DOUBLE);
        dataEnc.addMemberAsComponent(timeEnc);
        
        BinaryBlock compressedBlock = newBinaryBlock();
        compressedBlock.setRef("/" + dataStruct.getComponent(1).getName());
        compressedBlock.setCompression(codec);
        dataEnc.addMemberAsBlock(compressedBlock);
        
        try
        {
            SWEHelper.assignBinaryEncoding(dataStruct, dataEnc);
        }
        catch (CDMException e)
        {
            throw new RuntimeException("Invalid binary encoding configuration", e);
        };
        
        return newDataStream(dataStruct, dataEnc);
    }
    
    
    public DataStream newVideoOutputMJPEG(String name, int width, int height)
    {
        return newVideoOutputCODEC(name, width, height, "JPEG");
    }
    
    
    public DataStream newVideoOutputH264(String name, int width, int height)
    {
        return newVideoOutputCODEC(name, width, height, "H264");
    }
}
