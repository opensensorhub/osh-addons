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

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.Supplier;
import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Text;
import net.opengis.swe.v20.Time;
import static org.opencv.core.CvType.*;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;
import org.opencv.core.Mat;
import org.opencv.core.Rect2d;
import org.opencv.tracking.*;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.vast.data.DataBlockByte;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEHelper;


/**
 * <p>
 * Implementation of object tracker based on OpenCV.
 * Tracking is reinitialized every time a new set of bboxes is provided as
 * parameter.
 * </p>
 *
 * @author Alex Robin
 * @date Jun 1, 2021
 */
public class ObjectTrackingOld extends ExecutableProcessImpl
{
	public static final OSHProcessInfo INFO = new OSHProcessInfo("opencv:ObjectTracking", "Video Object Tracking", null, ObjectTrackingOld.class);
    
	enum TrackerAlgoEnum {BOOSTING, MIL, KCF, CSRT, MEDIAN_FLOW, TLD, MOSSE}
	
	Time inputTimeStamp;
	Count inputWidth;
    Count inputHeight;
	DataArray imgIn;
	Time outputTimeStamp;
    Count numInputBboxes;
    DataArray bboxesIn;
	Count numOutputBboxes;
    DataArray bboxesOut;
    Text algorithm;
    
    Supplier<legacy_Tracker> trackerSupplier;
    legacy_Tracker tracker;
    boolean trackerInitialized;    
    
    
    public ObjectTrackingOld()
    {
    	super(INFO);
        var swe = new CVHelper();
    	
    	// inputs
        inputData.add("rgbFrame", swe.createRecord()
            .label("Video Frame")
            .addField("time", inputTimeStamp = swe.createTime()
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
        
        // outputs
        outputData.add("trackedObjects", swe.createRecord()
            .label("Tracked Objects")
            .addField("time", outputTimeStamp = swe.createTime()
                .asSamplingTimeIsoUTC()
                .label("Frame Timestamp")
                .build())
            .addField("numObjs", numOutputBboxes = swe.createCount()
                .id("NUM_OBJS")
                .build())
            .addField("bboxList", bboxesOut = swe.createBboxList(numOutputBboxes)
                .build())
            .build());
        
        // parameters
        paramData.add("algorithm", algorithm = swe.createText()
            .definition(SWEHelper.getPropertyUri("Algorithm"))
            .label("Algorithm")
            .addAllowedValues(TrackerAlgoEnum.class)
            .build());
        
        paramData.add("objectRois", swe.createRecord()
            .label("Object Bboxes")
            .description("Rectangular image areas containing objects to be tracked")
            .addField("numRois", numInputBboxes = swe.createCount()
                .id("NUM_ROIS")
                .build())
            .addField("bboxList", bboxesIn = swe.createBboxList(numInputBboxes)
                .build())
            .build());
    }

    
    @Override
    public void init() throws ProcessException
    {
        super.init();
        Loader.load(opencv_java.class);
        trackerInitialized = false;
        
        // read algorithm type
        try
        {
            var algo = TrackerAlgoEnum.valueOf(algorithm.getData().getStringValue());
            
            switch (algo)
            {
                case BOOSTING:
                    trackerSupplier = () -> legacy_TrackerBoosting.create();
                    break;
                    
                case MIL:
                    trackerSupplier = () -> legacy_TrackerMIL.create();
                    break;
                    
                case KCF:
                    trackerSupplier = () -> legacy_TrackerKCF.create();
                    break;
                    
                case CSRT:
                    trackerSupplier = () -> legacy_TrackerCSRT.create();
                    break;
                    
                case MEDIAN_FLOW:
                    trackerSupplier = () -> legacy_TrackerMedianFlow.create();
                    break;
                    
                case TLD:
                    trackerSupplier = () -> legacy_TrackerTLD.create();
                    break;
                    
                case MOSSE:
                    trackerSupplier = () -> legacy_TrackerMOSSE.create();
                    break;
                    
                default:
                    throw new IllegalArgumentException();
            }
        }
        catch (IllegalArgumentException e)
        {
            reportError("Unsupported algorithm. Must be one of " + Arrays.toString(TrackerAlgoEnum.values()));
        }
    }
    

    @Override
    public void execute() throws ProcessException
    {
        Mat mat;
        var imgData = imgIn.getData();
        var timeStamp = inputTimeStamp.getData().getDoubleValue();
        
        if (imgData instanceof DataBlockByte)
        {
            var imgBytes = ((DataBlockByte)imgData).getUnderlyingObject();
            var rows = imgIn.getComponentCount();
            var cols = ((DataArray)imgIn.getElementType()).getComponentCount();
            var bbuf = ByteBuffer.allocateDirect(imgBytes.length);
            bbuf.put(imgBytes);
            bbuf.flip();
            mat = new Mat(rows, cols, CV_8UC(3), bbuf);//ByteBuffer.wrap(imgBytes));// BytePointer(imgBytes));
        }
        else
            throw new IllegalArgumentException("Only DataBlockByte supported as input");
        
        /*else if (imgData instanceof DataBlockByteBuffer)
        {
            // optimized version of the above if datablock contains a direct byte buffer
            // e.g. coming from FFMPEG decoder. This avoids copying the buffer twice between
            // Java and native code!
        }*/
        
        Rect2d rect = null;
        if (numInputBboxes.hasData() && numInputBboxes.getData().getIntValue() > 0)
        {
            var bbox = bboxesIn.getComponent(0).getData();
            rect = new Rect2d(
                bbox.getIntValue(0),
                bbox.getIntValue(1),
                bbox.getIntValue(2),
                bbox.getIntValue(3));
            
            tracker = trackerSupplier.get();
            tracker.init(mat, rect);
            trackerInitialized = true;

            getLogger().info("Tracker initialized with BBOX: x={}, y={}, w={}, h={}",
                rect.x, rect.y, rect.width, rect.height);
            
            // reset param
            bboxesIn.clearData();
        }
        else if (trackerInitialized)
        {
            rect = new Rect2d();
            tracker.update(mat, rect);
        }
        
        // output bbox
        outputTimeStamp.getData().setDoubleValue(timeStamp);
        if (rect != null)
        {   
            numOutputBboxes.getData().setIntValue(1);
            bboxesOut.updateSize();
            var bboxData = bboxesOut.getData();
            
            int idx = 0;
            bboxData.setIntValue(idx++, (int)rect.x);
            bboxData.setIntValue(idx++, (int)rect.y);
            bboxData.setIntValue(idx++, (int)rect.width);
            bboxData.setIntValue(idx++, (int)rect.height);
        }
        else
        {
            numOutputBboxes.getData().setIntValue(0);
            bboxesOut.updateSize();
        }
    } 
}