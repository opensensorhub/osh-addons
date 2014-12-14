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

package org.sensorhub.impl.sensor.axis;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.media.Buffer;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Time;
import net.sf.jipcam.axis.media.protocol.http.MjpegStream;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.common.BasicEventHandler;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.data.CountImpl;
import org.vast.data.DataArrayImpl;
import org.vast.data.DataBlockMixed;
import org.vast.data.DataRecordImpl;
import org.vast.data.TimeImpl;
import org.vast.swe.SWEConstants;


public class AxisVideoOutput extends AbstractSensorOutput<AxisCameraDriver>
{
	DataComponent videoDataStruct;
	DataBlock latestRecord;
	boolean reconnect;
	boolean streaming;
	
	
	public AxisVideoOutput(AxisCameraDriver driver)
    {
    	super(driver);
        this.eventHandler = new BasicEventHandler();    	
    }
	
	
	@Override
    public String getName()
    {
        return "videoOutput";
    }
    
    
    protected void init()
    {
		try
		{
			// get image size from camera HTTP interface
			int[] imgSize = getImageSize();
			
			// build output structure
			videoDataStruct = new DataRecordImpl(2);
			videoDataStruct.setName(getName());
			
			Time time = new TimeImpl();
			time.getUom().setHref(Time.ISO_TIME_UNIT);
			time.setDefinition(SWEConstants.DEF_SAMPLING_TIME);
			videoDataStruct.addComponent("time", time);
					
			DataArray img = new DataArrayImpl(imgSize[1]);
			img.setDefinition("http://sensorml.com/ont/swe/property/VideoFrame");
			videoDataStruct.addComponent("videoFrame", img);
			
			DataArray imgRow = new DataArrayImpl(imgSize[0]);
			img.addComponent("row", imgRow);
			
			DataRecord imgPixel = new DataRecordImpl(3);
			imgPixel.addComponent("red", new CountImpl(DataType.BYTE));
			imgPixel.addComponent("green", new CountImpl(DataType.BYTE));
			imgPixel.addComponent("blue", new CountImpl(DataType.BYTE));
			imgRow.addComponent("pixel", imgPixel);
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
	
	
	protected int[] getImageSize() throws IOException
	{
		String ipAddress = parentSensor.getConfiguration().ipAddress;
		URL getImgSizeUrl = new URL("http://" + ipAddress + "/axis-cgi/view/imagesize.cgi?camera=1");
		BufferedReader reader = new BufferedReader(new InputStreamReader(getImgSizeUrl.openStream()));
		
		int imgSize[] = new int[2];
		String line;
		while ((line = reader.readLine()) != null)
		{
			// split line and parse each possible property
			String[] tokens = line.split("=");
			if (tokens[0].trim().equalsIgnoreCase("image width"))
				imgSize[0] = Integer.parseInt(tokens[1].trim());
			else if (tokens[0].trim().equalsIgnoreCase("image height"))
				imgSize[1] = Integer.parseInt(tokens[1].trim());
		}
		
		// index 0 is width, index 1 is height
		return imgSize;
	}
	
	
	protected void startStream()
	{
		try
		{
			String ipAddress = parentSensor.getConfiguration().ipAddress;
			final URL videoUrl = new URL("http://" + ipAddress + "/mjpg/video.mjpg");
			reconnect = true;
			
			Thread t = new Thread(new Runnable()
	    	{
				@Override
				public void run()
				{
					while (reconnect)
					{
						// send http query
						try
						{
							InputStream is = new BufferedInputStream(videoUrl.openStream());
							MjpegStream stream = new MjpegStream(is, null);
							streaming = true;
							
							while (streaming)
							{
								DataBlock dataBlock = videoDataStruct.createDataBlock();
								
								Buffer buf = new Buffer();
						        buf.setData(new byte[]{});
						        
						        stream.read(buf);
						        
						        byte[] data = (byte[]) buf.getData();
						        InputStream imageStream = new ByteArrayInputStream( data );
						        
						        ImageInputStream input = ImageIO.createImageInputStream(imageStream); 
						        Iterator<ImageReader> readers = ImageIO.getImageReadersByMIMEType("image/jpeg");
						        ImageReader reader = readers.next();
						        reader.setInput(input);
						        //int width = reader.getWidth(0);
						        //int height = reader.getHeight(0);
						        
						        // The ImageTypeSpecifier object gives you access to more info such as 
						        // bands, color model, etc.
						        //ImageTypeSpecifier imageType = reader.getRawImageType(0);
						        
						        BufferedImage rgbImage = reader.read(0);
						        double timestamp = AXISJpegHeaderReader.getTimestamp(data) / 1000.;
						        dataBlock.setDoubleValue(0, timestamp/1000.);
						        byte[] byteData = ((DataBufferByte)rgbImage.getRaster().getDataBuffer()).getData();
						        ((DataBlockMixed)dataBlock).getUnderlyingObject()[1].setUnderlyingObject(byteData);
								
						        latestRecord = dataBlock;
								eventHandler.publishEvent(new SensorDataEvent(timestamp, AxisVideoOutput.this, latestRecord));
							}
							
							// wait 1s before trying to reconnect
							Thread.sleep(1000);
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					};
	 			}    		
	    	});
			
			t.start();
		}
    	catch (Exception e)
		{
			e.printStackTrace();
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
		return videoDataStruct;
	}

	@Override
	public DataEncoding getRecommendedEncoding()
	{
		return null;
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
