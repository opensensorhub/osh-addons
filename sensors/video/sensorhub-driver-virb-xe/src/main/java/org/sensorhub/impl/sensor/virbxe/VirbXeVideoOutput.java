/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
Developer are Copyright (C) 2016 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.virbxe;

import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.rtpcam.RTPVideoOutput;
import org.sensorhub.impl.sensor.videocam.BasicVideoConfig;
import org.sensorhub.impl.sensor.videocam.VideoResolution;


/**
 * <p>
 * Implementation of video output interface for Virb XE cameras using
 * RTSP/RTP protocol
 * </p>
 *
 * @author Alex Robin
 * @since March 2016
 */
public class VirbXeVideoOutput extends RTPVideoOutput<VirbXeDriver>
{
    volatile long lastFrameTime;
    Timer watchdog;
    VirbVideoConfig fixedVideoConfig = new VirbVideoConfig();
    
    
    // fixed config for Virb
    static class VirbVideoConfig extends BasicVideoConfig
    {
        public VideoResolution getResolution()
        {
            return new VideoResolution()
            {
                public int getWidth() { return 704; }
                public int getHeight() { return 418; }
            };
        }
    }
    
    
	protected VirbXeVideoOutput(VirbXeDriver driver)
	{
		super(driver);
	}
	

	public void init() throws SensorException
	{
	    VideoResolution res = fixedVideoConfig.getResolution();
	    super.init(res.getWidth(), res.getHeight());
	}
	
	
    public void start() throws SensorException
    {
        VirbXeConfig config = parentSensor.getConfiguration();
        super.start(fixedVideoConfig, config.rtsp, config.connection.connectTimeout);
        
        // start watchdog thread to detect disconnections
        final long maxFramePeriod = 1000;
        lastFrameTime = Long.MAX_VALUE;
        TimerTask checkFrameTask = new TimerTask()
        {
            @Override
            public void run()
            {
                if (lastFrameTime < System.currentTimeMillis() - maxFramePeriod)
                {
                    parentSensor.getLogger().warn("No frame received in more than {}ms. Reconnecting...", maxFramePeriod);
                    parentSensor.connection.reconnect();
                    cancel();
                }
            }
        };
        
        watchdog = new Timer();
        watchdog.scheduleAtFixedRate(checkFrameTask, 0L, 10000L);
    }


    @Override
    public void onFrame(long timeStamp, int seqNum, ByteBuffer frameBuf, boolean packetLost)
    {
        super.onFrame(timeStamp, seqNum, frameBuf, packetLost);
        lastFrameTime = System.currentTimeMillis();
    }


    @Override
    public void stop()
    {
        super.stop();
        watchdog.cancel();
    }
}
