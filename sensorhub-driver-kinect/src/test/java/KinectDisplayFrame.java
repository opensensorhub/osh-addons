/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.
Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2019 Botts Innovative Research, Inc. All Rights Reserved. 

******************************* END LICENSE BLOCK ***************************/
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.JFrame;

import org.sensorhub.impl.sensor.kinect.KinectConfig;
import org.sensorhub.impl.sensor.kinect.KinectConfig.Mode;
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

	private boolean isJPEG = false;

	private double scaleFactor = 1.0;

	public KinectDisplayFrame() {
	}

	public void initialize(String title, KinectConfig config, Mode mode, boolean isJPEG) {

		if ((config.pointCloudScaleFactor > 0) && (config.pointCloudScaleFactor <= 1.0)) {

			scaleFactor = config.pointCloudScaleFactor;
		}

		initialize(title, config.frameWidth, config.frameHeight, mode, isJPEG);
	}

	public void initialize(String title, int width, int height, KinectConfig.Mode mode, boolean isJPEG) {

		this.mode = mode;
		this.width = width;
		this.height = height;
		this.isJPEG = isJPEG;

		setSize(width, height);
		setTitle(STR_TITLE + title);
		setVisible(true);
	}

	public void drawFrame(DataBlock data) {

		if (KinectConfig.Mode.DEPTH == mode) {

			img = new BufferedImage((int) (width * scaleFactor), (int) (height * scaleFactor),
					BufferedImage.TYPE_BYTE_GRAY);

			DataBlock frameBlock = ((DataBlockMixed) data).getUnderlyingObject()[1];

			double[] frameData = (double[]) frameBlock.getUnderlyingObject();

			byte[] depthData = new byte[frameData.length];

			for (int idx = 0; idx < depthData.length; ++idx) {

				depthData[idx] = (byte) (frameData[idx] * (2047 / 255));

				short dataValue = (short) (((1 / frameData[idx]) - 3.3309495161) / -0.0030711016);

				depthData[idx] = (byte) ((dataValue >> 8) & 255);
			}

			byte[] destArray = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();

			System.arraycopy(depthData, 0, destArray, 0, frameData.length);

			getContentPane().getGraphics().drawImage(img, 0, 0, width, height, null);

		} else if (KinectConfig.Mode.IR == mode) {

			if (isJPEG) {

				img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

				byte[] frameData = (byte[]) ((DataBlockMixed) data).getUnderlyingObject()[1].getUnderlyingObject();

				// uncompress JPEG data
				try {
					InputStream imageStream = new ByteArrayInputStream(frameData);
					ImageInputStream input = ImageIO.createImageInputStream(imageStream);
					Iterator<ImageReader> readers = ImageIO.getImageReadersByMIMEType("image/jpeg");
					ImageReader reader = readers.next();
					reader.setInput(input);
					BufferedImage image = reader.read(0);
					getContentPane().getGraphics().drawImage(image, 0, 0, null);

				} catch (IOException e1) {

					throw new RuntimeException(e1);
				}

			} else {

				img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

				byte[] frameData = (byte[]) ((DataBlockMixed) data).getUnderlyingObject()[1].getUnderlyingObject();

				byte[] destArray = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();

				System.arraycopy(frameData, 0, destArray, 0, frameData.length);

				getContentPane().getGraphics().drawImage(img, 0, 0, width, height, null);
			}

		} else {

			if (isJPEG) {

				img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);

				byte[] frameData = (byte[]) ((DataBlockMixed) data).getUnderlyingObject()[1].getUnderlyingObject();

				// uncompress JPEG data
				try {
					InputStream imageStream = new ByteArrayInputStream(frameData);
					ImageInputStream input = ImageIO.createImageInputStream(imageStream);
					Iterator<ImageReader> readers = ImageIO.getImageReadersByMIMEType("image/jpeg");
					ImageReader reader = readers.next();
					reader.setInput(input);
					BufferedImage image = reader.read(0);
					getContentPane().getGraphics().drawImage(image, 0, 0, null);

				} catch (IOException e1) {

					throw new RuntimeException(e1);
				}

			} else {

				img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);

				byte[] frameData = (byte[]) ((DataBlockMixed) data).getUnderlyingObject()[1].getUnderlyingObject();

				byte[] destArray = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();

				System.arraycopy(frameData, 0, destArray, 0, frameData.length);

				getContentPane().getGraphics().drawImage(img, 0, 0, width, height, null);
			}
		}
	}
}
