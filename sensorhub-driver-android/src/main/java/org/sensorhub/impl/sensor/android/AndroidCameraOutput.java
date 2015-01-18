/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Sensia Software LLC. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.android;

import java.nio.ByteOrder;
import java.util.ArrayList;
import net.opengis.swe.v20.BinaryBlock;
import net.opengis.swe.v20.BinaryEncoding;
import net.opengis.swe.v20.ByteEncoding;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Time;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.SWEFactory;
import org.vast.swe.SWEConstants;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Builder;
import android.hardware.camera2.TotalCaptureResult;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;


/**
 * <p>
 * Implementation of data interface for Android cameras
 * </p>
 *
 * <p>Copyright (c) 2015</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Jan 18, 2015
 */
public class AndroidCameraOutput extends AbstractSensorOutput<AndroidSensorsDriver> implements IAndroidOutput
{
    // keep logger name short because in LogCat it's max 23 chars
    private static final Logger log = LoggerFactory.getLogger(AndroidCameraOutput.class.getSimpleName());
    
    CameraManager camManager;
    String cameraId;
    CameraDevice camera;
    MediaCodec codec;
    HandlerThread backgroundThread;
    Handler backgroundHandler;
    String name;
    boolean enabled;
    DataBlock latestRecord;
    DataComponent dataStruct;
    BinaryEncoding dataEncoding;
    int samplingPeriod;
    long systemTimeOffset = -1L;
    
    
    protected AndroidCameraOutput(AndroidSensorsDriver parentModule, CameraManager camManager, String cameraId)
    {
        super(parentModule);
        this.camManager = camManager;
        this.cameraId = cameraId;
        this.name = "camera" + cameraId + "_data";
    }
    
    
    @Override
    public String getName()
    {
        return name;
    }
    
    
    @Override
    public void init() throws SensorException
    {
        final Object camLock = new Object();
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler (backgroundThread.getLooper());
        
        try
        {
            // launch camera video recording
            camManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera)
                {
                    log.debug("Camera " + camera.getId() + " opened");
                    AndroidCameraOutput.this.camera = camera;
                    synchronized(camLock) { camLock.notify(); }
                }

                @Override
                public void onDisconnected(CameraDevice camera)
                {                    
                }

                @Override
                public void onError(CameraDevice camera, int error)
                {
                    log.error("Failed to open camera " + camera.getId() + " with error code " + error);
                    camLock.notify();                    
                }
                
            }, backgroundHandler);
            
            // wait for camera to be opened
            synchronized (camLock) { camLock.wait(); }            
            
            if (camera == null)
                throw new IllegalStateException();
            
            startCaptureSession(camera);     
            
            // SWE Common data structure
            int imgHeight = 480;
            int imgWidth = 640;
            SWEFactory fac = new SWEFactory();            
            dataStruct = fac.newDataRecord(2);
            dataStruct.setName(getName());
            
            Time time = fac.newTime();
            time.getUom().setHref(Time.ISO_TIME_UNIT);
            time.setDefinition(SWEConstants.DEF_SAMPLING_TIME);
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
            BinaryBlock h264block = fac.newBinaryBlock();
            h264block.setRef(dataStruct.getName());
            h264block.setCompression("H264");
            dataEncoding.addMemberAsBlock(h264block);
        }
        catch (SensorException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new SensorException("Cannot access camera " + cameraId, e);
        }
    }
    
    
    protected void startCaptureSession(final CameraDevice camera) throws Exception
    {
        ArrayList<Surface> outputs = new ArrayList<Surface>(1);
        
        // prepare H264 encoder
        /*try
        {
            codec = MediaCodec.createEncoderByType("video/avc"); //video/mp4v-es
            MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", 640, 480);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 125000);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);
            codec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            outputs.add(codec.createInputSurface()); // use input surface for direct connection to cam capture API
            codec.start();
            log.debug("MediaCodec initialized");
        }
        catch (Exception e)
        {
            throw new SensorException("Error while initializing codec " + codec.getName(), e);
        }*/
        outputs.add(new Surface(new SurfaceTexture(10)));
        
        // create capture session to codec buffer
        camera.createCaptureSession(outputs, new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(CameraCaptureSession session)
            {
                try
                {
                    Builder builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                    CaptureRequest captureRequest = builder.build();
                    log.debug("Capture request created");
                    
                    session.setRepeatingRequest(captureRequest, new CameraCaptureSession.CaptureCallback()
                    {
                        @Override
                        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result)
                        {
                            log.debug("Image " + result.getFrameNumber() + " captured");
                        }
                        
                        @Override
                        public void onCaptureFailed (CameraCaptureSession session, CaptureRequest request, CaptureFailure failure)
                        {
                            log.error("Video capture failed, error=" + failure.getReason());
                        }
                    }, backgroundHandler);
                }
                catch (Exception e)
                {
                    // TODO: handle exception
                }               
            }

            @Override
            public void onConfigureFailed(CameraCaptureSession session)
            {
                log.error("Could not configure capture session");          
            }            
        }, backgroundHandler);

    }
    
    
    @Override
    public void stop()
    {
        if (backgroundThread != null)
            backgroundThread.quitSafely();
        
        if (camera != null)
            camera.close();
        
        if (codec != null)
        {
            codec.stop();
            codec.release();
        }
    }


    @Override
    public double getAverageSamplingPeriod()
    {
        return 1/30.;
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
    public DataBlock getLatestRecord() throws SensorException
    {
        return latestRecord;
    }
    
    
    @Override
    public double getLatestRecordTime()
    {
        if (latestRecord != null)
            return latestRecord.getDoubleValue(0);
        
        return Double.NaN;
    }    
}
