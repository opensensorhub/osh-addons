import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import javax.swing.JFrame;

import org.sensorhub.impl.sensor.kinect.KinectConfig;
import org.vast.data.DataBlockList;
import org.vast.data.DataBlockMixed;

import net.opengis.swe.v20.DataBlock;

public class KinectDisplayFrame extends JFrame {

	/**
	 * Computed serial version UID
	 */
	private static final long serialVersionUID = -1617450219539878272L;

	private static final String STR_TITLE = new String("Kinect Test - ");

	private KinectConfig.Mode mode = KinectConfig.Mode.DEPTH;

	private BufferedImage img = null;

	private int width = 0;

	private int height = 0;

	public KinectDisplayFrame() {
	}

	public void initialize(String title, int width, int height, KinectConfig.Mode mode) {

		this.mode = mode;
		this.width = width;
		this.height = height;

		setSize(width, height);
		setTitle(STR_TITLE + title);
		setVisible(true);
	}

	public void drawFrame(DataBlock data) {

		if (KinectConfig.Mode.DEPTH == mode) {

			System.out.println("drawFrame ====> DEPTH");

			img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

	        DataBlock frameBlock = ((DataBlockMixed)data).getUnderlyingObject()[1];

			double[] frameData = (double[]) frameBlock.getUnderlyingObject();
			
			byte[] depthData = new byte[frameData.length];
			
			for (int idx = 0; idx < depthData.length; ++idx) {
				
				depthData[idx] = (byte)(frameData[idx] * (2047 / 255));
				
				short dataValue = (short) (((1/frameData[idx]) - 3.3309495161) / -0.0030711016);
								
				depthData[idx] = (byte) ((dataValue >> 8) & 255);
			}

			byte[] destArray = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();

			System.arraycopy(depthData, 0, destArray, 0, frameData.length);

			System.out.println(frameData.length + " " + depthData.length + " " + destArray.length);
			
			getContentPane().getGraphics().drawImage(img, 0, 0, width, height, null);
			
		} else if (KinectConfig.Mode.IR == mode) {

			img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

			DataBlock frameBlock = ((DataBlockList) data).getUnderlyingObject().get(0);

			byte[] frameData = (byte[]) frameBlock.getUnderlyingObject();

			byte[] destArray = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();

			System.arraycopy(frameData, 0, destArray, 0, frameData.length);

			getContentPane().getGraphics().drawImage(img, 0, 0, width, height, null);

		} else {
			
			img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);

			DataBlock frameBlock = ((DataBlockList) data).getUnderlyingObject().get(0);

			byte[] frameData = (byte[]) frameBlock.getUnderlyingObject();

			byte[] destArray = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();

			System.arraycopy(frameData, 0, destArray, 0, frameData.length);

			getContentPane().getGraphics().drawImage(img, 0, 0, width, height, null);
		}
	}
}
