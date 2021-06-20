/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.process.opencv;

import net.opengis.swe.v20.Category;
import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Text;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_core.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.vast.data.DataBlockByte;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEHelper;


/**
 * <p>
 * Face detection using Haar Cascade classifier
 * </p>
 *
 * @author Alex Robin
 * @date Jun 1, 2021
 */
public class FaceDetection extends ExecutableProcessImpl
{
	public static final OSHProcessInfo INFO = new OSHProcessInfo("opencv:FaceDetection", "Face Detection Algorithm", null, FaceDetection.class);
    
	Count inputWidth;
    Count inputHeight;
	DataArray imgIn;
	Count numFaces;
    DataArray bboxList;
    Category modeParam;
    Text configFileParam;
    
    enum ModeEnum {CONTINUOUS, ONE_SHOT}
    
    ModeEnum mode;
    CascadeClassifier face_cascade;
    RectVector detectedObjects = new RectVector();
    
    
    public FaceDetection()
    {
    	super(INFO);
        var swe = new CVHelper();
    	
    	// inputs
        inputData.add("rgbFrame", swe.createRecord()
            .label("Video Frame")
            .addField("time", swe.createTime()
                .asSamplingTimeIsoUTC()
                .label("Frame Timestamp")
                .build())
            .addField("width", inputWidth = swe.createCount()
                .id("IN_WIDTH")
                .label("Input Frame Width")
                .build())
            .addField("height", inputHeight = swe.createCount()
                .id("IN_HEIGHT")
                .label("Input Frame Height")
                .build())
            .addField("img", imgIn = swe.newRgbImage(
                inputWidth,
                inputHeight,
                DataType.BYTE))
            .build());
        
        // parameters
        paramData.add("detectionMode", modeParam = swe.createCategory()
            .definition(SWEHelper.getPropertyUri("ModeID"))
            .addAllowedValues(ModeEnum.class)
            .build());
        
        paramData.add("configFile", configFileParam = swe.createText()
            .definition(SWEHelper.getPropertyUri("Path"))
            .label("Classifier Config File")
            .description("Path of XML file containing the Haar cascade configuration (OpenCV format)")
            .build());
        
        // outputs
        outputData.add("detectedFaces", swe.createRecord()
            .label("Detected Faces")
            .addField("numFaces", numFaces = swe.createCount()
                .id("NUM_FACES")
                .build())
            .addField("bboxList", bboxList = swe.createBboxList(numFaces)
                .build())
            .build());
    }

    
    @Override
    public void init() throws ProcessException
    {
        super.init();
        
        // read mode param
        try
        {
            var val = modeParam.getData().getStringValue();
            if (val != null)
                mode = ModeEnum.valueOf(val);
            else
                mode = ModeEnum.CONTINUOUS; // default
        }
        catch (IllegalArgumentException e)
        {
            reportError("Unsupported mode. Must be one of " + Arrays.toString(ModeEnum.values()));
        }
        
        // read config file
        var configFile = configFileParam.getData().getStringValue();
        if (configFile == null || !Files.isReadable(Path.of(configFile)))
            reportError("Missing or inaccessible config file: " + configFile);
            
        this.face_cascade = new CascadeClassifier(configFile);
    }
    

    @Override
    public void execute() throws ProcessException
    {
        var rows = imgIn.getComponentCount();
        var cols = ((DataArray)imgIn.getElementType()).getComponentCount();
        var imgData = imgIn.getData();
        
        // convert input image data to OpenCV Mat object
        Mat mat;
        if (imgData instanceof DataBlockByte)
        {
            var imgBytes = ((DataBlockByte)imgData).getUnderlyingObject();
            
            mat = new Mat(rows, cols, CV_8UC(3), new BytePointer(imgBytes));
            // TODO reuse same native array
        }
        else
            throw new IllegalArgumentException("Only DataBlockByte supported as input");
        
        /*else if (imgData instanceof DataBlockByteBuffer)
        {
            // optimized version of the above if datablock contains a direct byte buffer
            // e.g. coming from FFMPEG decoder. This avoids copying the buffer twice between
            // Java and native code!
        }*/
        
        //Mat gray = new Mat(rows, cols, CV_8UC1);
        //cvtColor(mat, gray, COLOR_RGB2GRAY);
        
        detectedObjects.clear();
        face_cascade.detectMultiScale(mat, detectedObjects);
        //gray.deallocate();
        
        long numberOfFaces = detectedObjects.size();
        numFaces.getData().setIntValue((int)numberOfFaces);        
        bboxList.updateSize();
        var bboxData = bboxList.getData();
        
        int idx = 0;
        for (int i = 0; i < numberOfFaces; i++) {
            Rect rect = detectedObjects.get(i);
            //System.out.format("Face detected @ %d,%d, size = %dx%d\n", rect.x(), rect.y(), rect.width(), rect.height());
            bboxData.setIntValue(idx++, rect.x());
            bboxData.setIntValue(idx++, rect.y());
            bboxData.setIntValue(idx++, rect.width());
            bboxData.setIntValue(idx++, rect.height());
        }
        
        // in one_shot mode, stop after first successful detection
        if (numberOfFaces > 0 && mode == ModeEnum.ONE_SHOT)
            started = false;
    }
    
    
    @Override
    public void dispose()
    {
        super.dispose();
        
        if (detectedObjects != null) {
            detectedObjects.deallocate();
    }
}
}