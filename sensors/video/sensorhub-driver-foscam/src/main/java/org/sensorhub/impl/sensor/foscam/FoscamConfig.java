/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Botts Innovative Research Inc.. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.foscam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.Required;
import org.sensorhub.api.sensor.PositionConfig;
import org.sensorhub.api.sensor.PositionConfig.EulerOrientation;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.comm.HTTPConfig;
import org.sensorhub.impl.comm.RobustIPConnectionConfig;
import org.sensorhub.impl.sensor.foscam.ptz.FoscamPTZconfig;
import org.sensorhub.impl.sensor.foscam.ptz.FoscamPTZpreset;
import org.sensorhub.impl.sensor.foscam.ptz.FoscamPTZrelMove;
import org.sensorhub.impl.sensor.rtpcam.RTSPConfig;
import org.sensorhub.impl.sensor.videocam.BasicVideoConfig;
import org.sensorhub.impl.sensor.videocam.VideoResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Configuration parameters for Foscam camera
 * </p>
 * 
 * @author Lee Butler <labutler10@gmail.com>
 * @since September 2016
 */
public class FoscamConfig extends SensorConfig {
	private static final Logger logger = LoggerFactory.getLogger(FoscamConfig.class);

	@Required
	@DisplayInfo(label = "Camera ID", desc = "Camera ID to be appended to UID prefix")
	public String cameraID;

	@DisplayInfo(label = "HTTP", desc = "HTTP configuration")
	public HTTPConfig http = new HTTPConfig();

	@DisplayInfo(label = "RTP/RTSP", desc = "RTP/RTSP configuration (Remote host is obtained from HTTP configuration)")
	public RTSPConfig rtsp = new RTSPConfig();

	@DisplayInfo(label = "Connection Options")
	public RobustIPConnectionConfig connection = new RobustIPConnectionConfig();

	@DisplayInfo(label = "Video", desc = "Video settings")
	public VideoConfig video = new VideoConfig();

	@DisplayInfo(desc = "Camera geographic position")
	public PositionConfig position = new PositionConfig();

	@DisplayInfo(label = "PTZ", desc = "Pan-Tilt-Zoom configuration")
	public FoscamPTZconfig ptz = new FoscamPTZconfig();

	public int ptzSpeedVal = 2; // Set PTZ Speed (integer value in range 0-4,
								// 0=fast, 4=slow)

	public class VideoConfig extends BasicVideoConfig {
		@DisplayInfo(desc = "Resolution of video frames in pixels")
		public ResolutionEnum resolution;

		public VideoResolution getResolution() {
			return resolution;
		}
	}

	public enum ResolutionEnum implements VideoResolution {
		D1("D1", 704, 480), HD_720P("HD", 1280, 720), HD_1080P("Full HD", 1920, 1080);

		private String text;
		private int width, height;

		private ResolutionEnum(String text, int width, int height) {
			this.text = text;
			this.width = width;
			this.height = height;
		}

		public int getWidth() {
			return width;
		};

		public int getHeight() {
			return height;
		};

		public String toString() {
			return text + " (" + width + "x" + height + ")";
		}
	};

	public FoscamConfig() throws IOException {
		// default params for Foscam
		video.resolution = ResolutionEnum.HD_1080P;
		video.frameRate = 30;

		http.remotePort = 88;

	}

	public void init() throws IOException {
		/****************** Add PTZ Presets ******************/
		FoscamPTZpreset home = new FoscamPTZpreset();
		home.name = "Reset";
		home.cgi = "ptzReset";
		ptz.presets.add(home);

		FoscamPTZpreset point0 = new FoscamPTZpreset();
		FoscamPTZpreset point1 = new FoscamPTZpreset();
		FoscamPTZpreset point2 = new FoscamPTZpreset();
		FoscamPTZpreset point3 = new FoscamPTZpreset();
		FoscamPTZpreset point4 = new FoscamPTZpreset();
		FoscamPTZpreset point5 = new FoscamPTZpreset();
		FoscamPTZpreset point6 = new FoscamPTZpreset();
		FoscamPTZpreset point7 = new FoscamPTZpreset();
		FoscamPTZpreset point8 = new FoscamPTZpreset();
		FoscamPTZpreset point9 = new FoscamPTZpreset();
		FoscamPTZpreset point10 = new FoscamPTZpreset();
		FoscamPTZpreset point11 = new FoscamPTZpreset();
		FoscamPTZpreset point12 = new FoscamPTZpreset();
		FoscamPTZpreset point13 = new FoscamPTZpreset();
		FoscamPTZpreset point14 = new FoscamPTZpreset();
		FoscamPTZpreset point15 = new FoscamPTZpreset();
		/****************************************************/

		/***************************************
		 * Set PTZ speed
		 **************************************/
		if (!((ptzSpeedVal >= 0) && (ptzSpeedVal <= 4))) {
			logger.info("Invalid PTZ speed setting...setting to default speed");
			ptzSpeedVal = 2;
		}
		URL optionsURLsetSpeed = new URL("http://" + http.remoteHost + ":" + Integer.toString(http.remotePort)
				+ "/cgi-bin/CGIProxy.fcgi?cmd=setPTZSpeed&speed=" + Integer.toString(ptzSpeedVal) + "&usr=" + http.user
				+ "&pwd=" + http.password);
		InputStream isSetSpeed = optionsURLsetSpeed.openStream();
		BufferedReader readerSetSpeed = null;
		readerSetSpeed = new BufferedReader(new InputStreamReader(isSetSpeed));
		String lineSetSpeed;
		while ((lineSetSpeed = readerSetSpeed.readLine()) != null) {
			String[] tokenSetSpeed = lineSetSpeed.split("<|\\>");
			if (tokenSetSpeed[1].trim().equals("result"))
				if (!tokenSetSpeed[2].trim().equals("0"))
					logger.info("Setting PTZ Speed was Unsuccessful");
				else
					logger.info("Setting PTZ Speed was Successful");
		}
		isSetSpeed.close();
		/********************************************************************************************/

		/*************** Get current PTZ Speed **************/
		int ptzSpeed = getPTZspeed();
		logger.info("Current PTZ speed = " + ptzSpeed);
		/****************************************************/

		/**************** Set Speed Factor ******************/
		double speedFactor = 0.00;
		switch (ptzSpeed) {
		case 0:
			speedFactor = 0.25;
			break;
		case 1:
			speedFactor = 0.50;
			break;
		case 2:
			speedFactor = 1.00;
			break;
		case 3:
			speedFactor = 1.50;
			break;
		case 4:
			speedFactor = 2.00;
			break;
		}
		/****************************************************/

		/************** Add Relative Movements **************/
		FoscamPTZrelMove Up = new FoscamPTZrelMove();
		Up.name = "Up";
		Up.cgi = "ptzMoveUp";
		Up.moveTime = (int) (1000 * speedFactor); // time (in ms) to move
		ptz.relMoves.add(Up);

		FoscamPTZrelMove Down = new FoscamPTZrelMove();
		Down.name = "Down";
		Down.cgi = "ptzMoveDown";
		Down.moveTime = (int) (1000 * speedFactor); // time (in ms) to move
		ptz.relMoves.add(Down);

		FoscamPTZrelMove Left = new FoscamPTZrelMove();
		Left.name = "Left";
		Left.cgi = "ptzMoveLeft";
		Left.moveTime = (int) (2000 * speedFactor); // time (in ms) to move
		ptz.relMoves.add(Left);

		FoscamPTZrelMove Right = new FoscamPTZrelMove();
		Right.name = "Right";
		Right.cgi = "ptzMoveRight";
		Right.moveTime = (int) (2000 * speedFactor); // time (in ms) to move
		ptz.relMoves.add(Right);

		FoscamPTZrelMove TopLeft = new FoscamPTZrelMove();
		TopLeft.name = "TopLeft";
		TopLeft.cgi = "ptzMoveTopLeft";
		TopLeft.moveTime = (int) (1500 * speedFactor); // time (in ms) to move
		ptz.relMoves.add(TopLeft);

		FoscamPTZrelMove TopRight = new FoscamPTZrelMove();
		TopRight.name = "TopRight";
		TopRight.cgi = "ptzMoveTopRight";
		TopRight.moveTime = (int) (1500 * speedFactor); // time (in ms) to move
		ptz.relMoves.add(TopRight);

		FoscamPTZrelMove BottomLeft = new FoscamPTZrelMove();
		BottomLeft.name = "BottomLeft";
		BottomLeft.cgi = "ptzMoveBottomLeft";
		BottomLeft.moveTime = (int) (1500 * speedFactor); // time (in ms) to
															// move
		ptz.relMoves.add(BottomLeft);

		FoscamPTZrelMove BottomRight = new FoscamPTZrelMove();
		BottomRight.name = "BottomRight";
		BottomRight.cgi = "ptzMoveBottomRight";
		BottomRight.moveTime = (int) (1500 * speedFactor); // time (in ms) to
															// move
		ptz.relMoves.add(BottomRight);

		/****************************************************/

		/**************** Get current list of PTZ presets ****************/
		String[] ptzPresetList = getPresets();
		/*****************************************************************/

		/******************* Populate PTZ Preset List ********************/
		if (!ptzPresetList[0].isEmpty()) {
			point0.name = ptzPresetList[0];
			ptz.presets.add(point0);
		}

		if (!ptzPresetList[1].isEmpty()) {
			point1.name = ptzPresetList[1];
			ptz.presets.add(point1);
		}

		if (!ptzPresetList[2].isEmpty()) {
			point2.name = ptzPresetList[2];
			ptz.presets.add(point2);
		}

		if (!ptzPresetList[3].isEmpty()) {
			point3.name = ptzPresetList[3];
			ptz.presets.add(point3);
		}

		if (!ptzPresetList[4].isEmpty()) {
			point4.name = ptzPresetList[4];
			ptz.presets.add(point4);
		}

		if (!ptzPresetList[5].isEmpty()) {
			point5.name = ptzPresetList[5];
			ptz.presets.add(point5);
		}

		if (!ptzPresetList[6].isEmpty()) {
			point6.name = ptzPresetList[6];
			ptz.presets.add(point6);
		}

		if (!ptzPresetList[7].isEmpty()) {
			point7.name = ptzPresetList[7];
			ptz.presets.add(point7);
		}

		if (!ptzPresetList[8].isEmpty()) {
			point8.name = ptzPresetList[8];
			ptz.presets.add(point8);
		}

		if (!ptzPresetList[9].isEmpty()) {
			point9.name = ptzPresetList[9];
			ptz.presets.add(point9);
		}

		if (!ptzPresetList[10].isEmpty()) {
			point10.name = ptzPresetList[10];
			ptz.presets.add(point10);
		}

		if (!ptzPresetList[11].isEmpty()) {
			point11.name = ptzPresetList[11];
			ptz.presets.add(point11);
		}

		if (!ptzPresetList[12].isEmpty()) {
			point12.name = ptzPresetList[12];
			ptz.presets.add(point12);
		}

		if (!ptzPresetList[13].isEmpty()) {
			point13.name = ptzPresetList[13];
			ptz.presets.add(point13);
		}

		if (!ptzPresetList[14].isEmpty()) {
			point14.name = ptzPresetList[14];
			ptz.presets.add(point14);
		}

		if (!ptzPresetList[15].isEmpty()) {
			point15.name = ptzPresetList[15];
			ptz.presets.add(point15);
		}
		/*****************************************************************/
	}

	/************ Method to get and return current PTZ speed *************/
	public int getPTZspeed() throws IOException {
		URL optionsURLspeed = new URL("http://" + http.remoteHost + ":" + Integer.toString(http.remotePort)
				+ "/cgi-bin/CGIProxy.fcgi?cmd=getPTZSpeed&usr=" + http.user + "&pwd=" + http.password);
		InputStream isSpeed = optionsURLspeed.openStream();
		BufferedReader readerSpeed = null;
		readerSpeed = new BufferedReader(new InputStreamReader(isSpeed));
		String lineSpeed;
		int currentPTZspeed = 0;
		while ((lineSpeed = readerSpeed.readLine()) != null) {
			String[] tokenSpeed = lineSpeed.split("<|\\>");
			if (tokenSpeed[1].trim().equals("speed"))
				currentPTZspeed = Integer.parseInt(tokenSpeed[2].trim());
		}
		isSpeed.close();
		return currentPTZspeed;
	}

	/********************************************************************/

	/********** Method to get and return current PTZ presets ************/
	public String[] getPresets() throws IOException {
		URL optionsURLpreset = new URL("http://" + http.remoteHost + ":" + Integer.toString(http.remotePort)
				+ "/cgi-bin/CGIProxy.fcgi?cmd=getPTZPresetPointList&usr=" + http.user + "&pwd=" + http.password);
		InputStream isPreset = optionsURLpreset.openStream();
		BufferedReader readerPreset = null;
		readerPreset = new BufferedReader(new InputStreamReader(isPreset));
		String linePreset;
		String[] presets = new String[16];
		int cntPreset = 0;
		while ((linePreset = readerPreset.readLine()) != null) {
			String[] tokenPreset = linePreset.split("<|\\>");
			if (tokenPreset[1].trim().contains("point")) {
				if (!tokenPreset[2].trim().isEmpty())
					presets[cntPreset] = tokenPreset[2];
				else
					presets[cntPreset] = "";
				cntPreset++;
			}
		}
		isPreset.close();
		return presets;
	}

	/********************************************************************/

	@Override
	public LLALocation getLocation() {
		return position.location;
	}

	@Override
	public EulerOrientation getOrientation() {
		return position.orientation;
	}
}