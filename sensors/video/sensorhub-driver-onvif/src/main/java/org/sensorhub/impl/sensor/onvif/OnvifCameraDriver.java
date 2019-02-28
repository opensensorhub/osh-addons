/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.onvif;

import java.net.ConnectException;
import java.util.List;

import javax.xml.soap.SOAPException;

import net.opengis.sensorml.v20.IdentifierList;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.rtpcam.RTPVideoOutput;
import org.onvif.ver10.schema.H264Configuration;
import org.onvif.ver10.schema.Mpeg4Configuration;
import org.onvif.ver10.schema.Profile;
import org.sensorhub.api.common.SensorHubException;
import org.vast.sensorML.SMLFactory;

import de.onvif.soap.OnvifDevice;

/**
 * <p>
 * Implementation of sensor interface for generic Cameras using IP
 * protocol
 * </p>
 * 
 * @author Joshua Wolfe <developer.wolfe@gmail.com>
 * @since June 13, 2017
 */

public class OnvifCameraDriver extends AbstractSensorModule <OnvifCameraConfig>
{
    RTPVideoOutput <OnvifCameraDriver> h264VideoOutput;
    OnvifVideoOutput mpeg4VideoOutput;
    OnvifVideoControl videoControlInterface;
    OnvifPtzOutput ptzPosOutput;
    OnvifPtzControl ptzControlInterface;

    String hostIp;
    Integer port;
    String user;
    String password;
    String path;
    Integer timeout;

    OnvifDevice camera;
    Profile profile;
    
    String serialNumber;
    String modelNumber;
    String shortName;
    String longName;
    String imageSize;
    
    public OnvifCameraDriver() {
    }

    @Override
    public void setConfiguration(final OnvifCameraConfig config) {
        super.setConfiguration(config);
        
        hostIp = config.hostIp;
        port = config.port;
        user = config.user;
        password = config.password;
        path = config.path;
        timeout = config.timeout;
    };

    @Override
    public void init() throws SensorHubException {
        // reset internal state in case init() was already called
        super.init();
        
        if (hostIp == null) {
			throw new SensorHubException("No host IP address provided in config");
        }

        try {
			if (user == null || password == null) {
				camera = new OnvifDevice(hostIp);
			} else {
				camera = new OnvifDevice(hostIp, user, password);
			}

			camera.getSoap().setLogging(true);
		} catch (ConnectException e) {
			throw new SensorHubException("Exception occured when connecting to camera");
		} catch (SOAPException e) {
			throw new SensorHubException("Exception occured when calling XML Web service over SOAP");
		} catch (Exception e) {
			throw new SensorHubException(e.toString());
		}
        
        List<Profile> profiles = camera.getDevices().getProfiles();
        
        if (profiles == null || profiles.isEmpty()) {
			throw new SensorHubException("Camera does not have any profiles to use");
        }
        
        for (Profile p: profiles) {
			String token = p.getToken();
			if (camera.getPtz().isAbsoluteMoveSupported(token) &&
				camera.getPtz().isRelativeMoveSupported(token) &&
				camera.getPtz().isPtzOperationsSupported(token)) {
				profile = p;
				break;
			}
		}

		if (profile == null) {
			throw new SensorHubException("Camera does not have any profiles capable of PTZ");
        }
        
        serialNumber = camera.getDevices().getDeviceInformation().getSerialNumber().trim();
        modelNumber = camera.getDevices().getDeviceInformation().getModel().trim();
        shortName = camera.getDevices().getDeviceInformation().getManufacturer().trim();
        longName = shortName + "_" + modelNumber + "_" + serialNumber;
        
        // generate identifiers
        generateUniqueID("urn:onvif:cam:", serialNumber);
        generateXmlID("ONVIF_CAM_", serialNumber);
        
        // create I/O objects
        String videoOutName = "video";
        int videoOutNum = 1;
        
        H264Configuration h264Config = profile.getVideoEncoderConfiguration().getH264();
        Mpeg4Configuration mpeg4Config = profile.getVideoEncoderConfiguration().getMPEG4();
        
        if (config.enableH264 && h264Config == null) {
			throw new SensorException("Cannot connect to H264 stream - H264 not supported");
        } else if(config.enableMPEG4 && mpeg4Config == null) {
			throw new SensorException("Cannot connect to MPEG4 stream - MPEG4 not supported");
        }
        
        // add H264 video output
        if (config.enableH264) {
			String outputName = videoOutName + videoOutNum++;
			h264VideoOutput = new RTPVideoOutput<OnvifCameraDriver>(this, outputName);
			h264VideoOutput.init(profile.getVideoSourceConfiguration().getBounds().getWidth(), 
								profile.getVideoSourceConfiguration().getBounds().getHeight());
			// TODO: check the difference between the above and what's below
			//profile.getVideoEncoderConfiguration().getResolution().getWidth();
			//profile.getVideoEncoderConfiguration().getResolution().getHeight();
			addOutput(h264VideoOutput, false);
        }

        // add MPEG4 video output
        if (config.enableMPEG4) {
			String outputName = videoOutName + videoOutNum++;
			mpeg4VideoOutput = new OnvifVideoOutput(this, outputName);
			mpeg4VideoOutput.init();
			addOutput(h264VideoOutput, false);
        }

        // add PTZ output
        ptzPosOutput = new OnvifPtzOutput(this);
        addOutput(ptzPosOutput, false);
        ptzPosOutput.init();

        // add PTZ controller
        ptzControlInterface = new OnvifPtzControl(this);
        addControlInput(ptzControlInterface);
        ptzControlInterface.init();
    }

    @Override
    public void start() throws SensorHubException {
		// Validate connection to camera
		if (camera == null)
			throw new SensorHubException("Exception occured when connecting to camera");

		// start video output
		if (mpeg4VideoOutput != null)
			mpeg4VideoOutput.start();

		// start PTZ output
		if (ptzPosOutput != null && ptzControlInterface != null) {
			ptzPosOutput .start();
			ptzControlInterface.start();
		}
	}

    @Override
    protected void updateSensorDescription() {
        synchronized(sensorDescLock) {
            // parent class reads SensorML from config if provided
            // and then sets unique ID, outputs and control inputs
            super.updateSensorDescription();

            SMLFactory smlFac = new SMLFactory();

            if (!sensorDescription.isSetDescription())
                sensorDescription.setDescription("ONVIF Video Camera");

            IdentifierList identifierList = smlFac.newIdentifierList();
            sensorDescription.addIdentification(identifierList);
        }
    }

    @Override
    public boolean isConnected() {
        if (camera == null)
            return false;

        return true;
    }

    @Override
    public void stop() {}

    @Override
    public void cleanup() {}

    protected String getHostUrl() {
        return hostIp;
    }
}
