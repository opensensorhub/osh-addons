/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.android;

import java.io.ByteArrayOutputStream;
import java.nio.ByteOrder;
import net.opengis.swe.v20.BinaryBlock;
import net.opengis.swe.v20.BinaryComponent;
import net.opengis.swe.v20.BinaryEncoding;
import net.opengis.swe.v20.ByteEncoding;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Time;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.AbstractDataBlock;
import org.vast.data.DataBlockMixed;
import org.vast.data.SWEFactory;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.SystemClock;
import android.view.SurfaceHolder;


/**
 * <p>
 * Implementation of data interface for Android cameras using legacy Camera API
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since June 11, 2015
 */
@SuppressWarnings("deprecation")
public class AndroidCameraOutput extends AbstractSensorOutput<AndroidSensorsDriver> implements IAndroidOutput, Camera.PreviewCallback
{
    // keep logger name short because in LogCat it's max 23 chars
    static final Logger log = LoggerFactory.getLogger(AndroidCameraOutput.class.getSimpleName());
    protected static final String TIME_REF = "http://www.opengis.net/def/trs/BIPM/0/UTC";
    
    int cameraId;
    Camera camera;
    int imgHeight, imgWidth, frameRate;
    byte[] imgBuf1, imgBuf2;
    YuvImage yuvImg1, yuvImg2;
    Rect imgArea;
    ByteArrayOutputStream jpegBuf = new ByteArrayOutputStream();
    SurfaceHolder previewSurfaceHolder;
    
    String name;
    DataComponent dataStruct;
    BinaryEncoding dataEncoding;
    int samplingPeriod;
    long systemTimeOffset = -1L;
    
    
    protected AndroidCameraOutput(AndroidSensorsDriver parentModule, int cameraId, SurfaceHolder previewSurfaceHolder)
    {
        super(parentModule);
        this.cameraId = cameraId;
        this.name = "camera" + cameraId + "_data";
        this.previewSurfaceHolder = previewSurfaceHolder;
    }
    
    
    @Override
    public String getName()
    {
        return name;
    }
    
    
    @Override
    public void init() throws SensorException
    {
        try
        {
            // TODO get closest values from camera characteristics
            imgWidth = 800;
            imgHeight = 600;
            frameRate = 1;
            
            // open camera and set parameters
            camera = Camera.open(cameraId);
            Parameters camParams = camera.getParameters();
            camParams.setPreviewSize(imgWidth, imgHeight);
            camParams.setPreviewFormat(ImageFormat.NV21);
            camera.setParameters(camParams);
            
            // setup buffers and callback
            imgArea = new Rect(0, 0, imgWidth, imgHeight);
            int bufSize = imgWidth*imgHeight*ImageFormat.getBitsPerPixel(ImageFormat.NV21)/8;
            imgBuf1 = new byte[bufSize];
            yuvImg1 = new YuvImage(imgBuf1, ImageFormat.NV21, imgWidth, imgHeight, null);
            imgBuf2 = new byte[bufSize];
            yuvImg2 = new YuvImage(imgBuf2, ImageFormat.NV21, imgWidth, imgHeight, null);
            camera.addCallbackBuffer(imgBuf1);
            camera.addCallbackBuffer(imgBuf2);
            camera.setPreviewCallbackWithBuffer(this);
            camera.setDisplayOrientation(90);
                        
            // create SWE Common data structure            
            SWEFactory fac = new SWEFactory();            
            dataStruct = fac.newDataRecord(2);
            dataStruct.setName(getName());
            
            Time time = fac.newTime();
            time.getUom().setHref(Time.ISO_TIME_UNIT);
            time.setDefinition(SWEConstants.DEF_SAMPLING_TIME);
            time.setReferenceFrame(TIME_REF);
            dataStruct.addComponent("time", time);
                    
            DataArray img = fac.newDataArray(imgHeight);
            img.setDefinition("http://sensorml.com/ont/swe/property/VideoFrame");
            dataStruct.addComponent("videoFrame", img);
            
            DataArray imgRow = fac.newDataArray(imgWidth);
            img.addComponent("row", imgRow);
            
            DataRecord imgPixel = fac.newDataRecord(3);
            imgPixel.addComponent("red", fac.newCount(DataType.BYTE));
            imgPixel.addComponent("green", fac.newCount(DataType.BYTE));
            imgPixel.addComponent("blue", fac.newCount(DataType.BYTE));
            imgRow.addComponent("pixel", imgPixel);
            
            // SWE Common encoding
            dataEncoding = fac.newBinaryEncoding();
            dataEncoding.setByteEncoding(ByteEncoding.RAW);
            dataEncoding.setByteOrder(ByteOrder.BIG_ENDIAN);
            BinaryComponent timeEnc = fac.newBinaryComponent();
            timeEnc.setRef("/" + time.getName());
            timeEnc.setCdmDataType(DataType.DOUBLE);
            dataEncoding.addMemberAsComponent(timeEnc);
            //BinaryBlock compressedBlock = fac.newBinaryBlock();
            //compressedBlock.setRef("/" + img.getName());
            //compressedBlock.setCompression("H264");
            BinaryBlock compressedBlock = fac.newBinaryBlock();
            compressedBlock.setRef("/" + img.getName());
            compressedBlock.setCompression("JPEG");
            dataEncoding.addMemberAsBlock(compressedBlock);
            
            // resolve encoding so compressed blocks can be properly generated
            SWEHelper.assignBinaryEncoding(dataStruct, dataEncoding);
            
            // start streaming video
            if (previewSurfaceHolder != null)
                camera.setPreviewDisplay(previewSurfaceHolder);
            camera.startPreview();
        }
        catch (Exception e)
        {
            throw new SensorException("Cannot access camera " + cameraId, e);
        }
    }
    
    
    @Override
    public void onPreviewFrame(byte[] data, Camera camera)
    {
        long timeStamp = SystemClock.elapsedRealtimeNanos();
        
        // select current buffer
        YuvImage yuvImg = (data == imgBuf1) ? yuvImg1 : yuvImg2;
        
        // compress as JPEG
        jpegBuf.reset();
        yuvImg.compressToJpeg(imgArea, 90, jpegBuf);
        
        // release buffer for next frame
        camera.addCallbackBuffer(data);
        
        // generate new data record
        DataBlock newRecord;
        if (latestRecord == null)
            newRecord = dataStruct.createDataBlock();
        else
            newRecord = latestRecord.renew();
        
        // set time stamp
        double samplingTime = getJulianTimeStamp(timeStamp);
        newRecord.setDoubleValue(0, samplingTime);
        
        // set encoded data
        AbstractDataBlock frameData = ((DataBlockMixed)newRecord).getUnderlyingObject()[1];
        frameData.setUnderlyingObject(jpegBuf.toByteArray());
        
        // send event
        latestRecord = newRecord;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, AndroidCameraOutput.this, latestRecord));          
    }
    
    
    @Override
    public void stop()
    {
        if (camera != null)
        {
            camera.release();
            camera = null;
        }
    }


    @Override
    public double getAverageSamplingPeriod()
    {
        return 1./ (double)frameRate;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return dataStruct;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return dataEncoding;
    }

    
    @Override
    public DataBlock getLatestRecord()
    {
        return latestRecord;
    }
    
    
    @Override
    public long getLatestRecordTime()
    {
        return latestRecordTime;
    }
    
    
    protected final double getJulianTimeStamp(long sensorTimeStampNanos)
    {
        long sensorTimeMillis = sensorTimeStampNanos / 1000000;
        
        if (systemTimeOffset < 0)
            systemTimeOffset = System.currentTimeMillis() - sensorTimeMillis;
            
        return (systemTimeOffset + sensorTimeMillis) / 1000.;
    }
}
