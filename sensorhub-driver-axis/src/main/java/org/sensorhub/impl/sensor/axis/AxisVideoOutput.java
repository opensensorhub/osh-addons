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
import java.util.List;
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
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.common.BasicEventHandler;
import org.vast.data.CountImpl;
import org.vast.data.DataArrayImpl;
import org.vast.data.DataBlockMixed;
import org.vast.data.DataRecordImpl;
import org.vast.data.TimeImpl;
import org.vast.sweCommon.SWEConstants;


public class AxisVideoOutput implements ISensorDataInterface
{
	AxisCameraDriver driver;
	IEventHandler eventHandler;
    DataComponent videoDataStruct;
	DataBlock latestRecord;
	boolean reconnect;
	boolean streaming;
	
	
	public AxisVideoOutput(AxisCameraDriver driver)
    {
    	this.driver = driver;
        this.eventHandler = new BasicEventHandler();    	
    }
	
	
	protected void init()
    {
		try
		{
			// get image size from camera HTTP interface
			int[] imgSize = getImageSize();
			
			// build output structure
			videoDataStruct = new DataRecordImpl(2);
			
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
		String ipAddress = driver.getConfiguration().ipAddress;
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
			String ipAddress = driver.getConfiguration().ipAddress;
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
	//					        ImageTypeSpecifier imageType = reader.getRawImageType(0);
						        
						        BufferedImage rgbImage = reader.read(0);
						        long timestamp = AXISJpegHeaderReader.getTimestamp(data);
						        dataBlock.setDoubleValue(0, timestamp/1000.);
						        byte[] byteData = ((DataBufferByte)rgbImage.getRaster().getDataBuffer()).getData();
						        ((DataBlockMixed)dataBlock).getUnderlyingObject()[1].setUnderlyingObject(byteData);
								
						        latestRecord = dataBlock;
								long time = System.currentTimeMillis();
								eventHandler.publishEvent(new SensorDataEvent(AxisVideoOutput.this, time, videoDataStruct, latestRecord));
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	@Override
	public void registerListener(IEventListener listener)
	{
		eventHandler.registerListener(listener);
	}

	@Override
	public void unregisterListener(IEventListener listener)
	{
		eventHandler.unregisterListener(listener);
	}

	@Override
	public ISensorModule<?> getSensorInterface()
	{
		return driver;
	}

	@Override
	public boolean isEnabled()
	{
		return true;
	}

	@Override
	public boolean isStorageSupported()
	{
		return false;
	}

	@Override
	public boolean isPushSupported()
	{
		return true;
	}

	@Override
	public double getAverageSamplingPeriod()
	{
		return 1/30.;
	}

	@Override
	public DataComponent getRecordDescription() throws SensorException
	{
		return videoDataStruct;
	}

	@Override
	public DataEncoding getRecommendedEncoding() throws SensorException
	{
		return null;
	}

	@Override
	public DataBlock getLatestRecord() throws SensorException
	{
		return latestRecord;
	}
	
	// storage is unsupported so the methods below are kept simple

	@Override
	public int getStorageCapacity() throws SensorException
	{
		return 0;
	}

	@Override
	public int getNumberOfAvailableRecords() throws SensorException
	{
		return 0;
	}

	@Override
	public List<DataBlock> getLatestRecords(int maxRecords, boolean clear) throws SensorException
	{
		return null;
	}

	@Override
	public List<DataBlock> getAllRecords(boolean clear) throws SensorException
	{
		return null;
	}

	@Override
	public int clearAllRecords() throws SensorException
	{
		return 0;
	}

}
