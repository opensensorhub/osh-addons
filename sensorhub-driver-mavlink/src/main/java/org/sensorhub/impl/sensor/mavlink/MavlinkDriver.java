/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.mavlink;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import org.sensorhub.api.comm.CommConfig;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.ISensorControlInterface;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.SensorEvent;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.mavlink.MavlinkConfig.CmdTypes;
import org.sensorhub.impl.sensor.mavlink.MavlinkConfig.MsgTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Parser;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.common.msg_heartbeat;


/**
 * <p>
 * Driver implementation for MAVLink enabled systems.<br/>
 * Only a few messages and commands are supported for now but this can easily
 * be extended.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Dec 12, 2015
 */
public class MavlinkDriver extends AbstractSensorModule<MavlinkConfig>
{
    static final Logger log = LoggerFactory.getLogger(MavlinkDriver.class);
    
    protected static final String BODY_FRAME = "BODY_FRAME";
    protected static final String GIMBAL_FRAME = "GIMBAL_FRAME";
    protected static final long MAX_MSG_PERIOD = 10000L;
    
    ICommProvider<? super CommConfig> commProvider;
    Timer watchDogTimer;
    volatile boolean started;
    boolean connected;
    InputStream msgIn;
    OutputStream cmdOut;
    Parser mavlinkParser;
    
    long lastMsgTime = 0;
    
    
    @Override
    public void init(MavlinkConfig config) throws SensorHubException
    {
        super.init(config);
        
        // create outputs depending on selected sentences
        if (config.activeMessages.contains(MsgTypes.GLOBAL_POSITION))
        {
            GlobalPositionOutput dataInterface = new GlobalPositionOutput(this);
            addOutput(dataInterface, false);
            dataInterface.init();
        }
        
        if (config.activeMessages.contains(MsgTypes.ATTITUDE))
        {
            AttitudeEulerOutput dataInterface = new AttitudeEulerOutput(this);
            addOutput(dataInterface, false);
            dataInterface.init();
        }
        
        if (config.activeMessages.contains(MsgTypes.ATTITUDE_QUATERNION))
        {
            AttitudeQuatOutput dataInterface = new AttitudeQuatOutput(this);
            addOutput(dataInterface, false);
            dataInterface.init();
        }
        
        if (config.activeMessages.contains(MsgTypes.GIMBAL_REPORT))
        {
            GimbalEulerOutput dataInterface = new GimbalEulerOutput(this);
            addOutput(dataInterface, false);
            dataInterface.init();
        }
        
        // create control inputs depending on selected commands
        if (config.activeCommands.contains(CmdTypes.MOUNT_CONTROL))
        {
            ISensorControlInterface controlInterface = null;
            this.addControlInput(controlInterface);
        }
    }
    
    
    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescription)
        {
            super.updateSensorDescription();
            
            if (AbstractSensorModule.DEFAULT_ID.equals(sensorDescription.getId()))
                sensorDescription.setId("MAVLINK_SYSTEM");
            
            if (config.vehicleID != null)
                sensorDescription.setUniqueIdentifier("urn:osh:sensor:mavlink:" + config.vehicleID);
        }
    }


    @Override
    public synchronized void start() throws SensorHubException
    {
        if (started)
            return;        
                
        // init comm provider
        if (commProvider == null)
        {
            // we need to recreate comm provider here because it can be changed by UI
            try
            {
                if (config.commSettings == null)
                    throw new SensorHubException("No communication settings specified");
                
                commProvider = config.commSettings.getProvider();
                commProvider.start();
            }
            catch (Exception e)
            {
                commProvider = null;
                throw e;
            }
        }
        
        // connect to data stream
        try
        {
            mavlinkParser = new Parser();
            msgIn = new BufferedInputStream(commProvider.getInputStream());
            cmdOut = commProvider.getOutputStream();
            
            // send heartbeat
            msg_heartbeat hb = new msg_heartbeat();
            sendCommand(hb.pack());
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error while initializing communications ", e);
        }
        
        // start main measurement thread
        Thread t = new Thread(new Runnable()
        {
            public void run()
            {
                while (started)
                    handleNextMessage();
            }
        });
        
        started = true;
        t.start();
        
        // start connection watchdog
        startWatchDogTimer();
    }
    
    
    private void startWatchDogTimer()
    {
        watchDogTimer = new java.util.Timer();
        watchDogTimer.schedule( 
                new java.util.TimerTask() {
                    public void run() {
                        long now = System.currentTimeMillis();
                        if (now - lastMsgTime > MAX_MSG_PERIOD)
                        {
                            if (connected)
                            {
                                connected = false;
                                SensorEvent e = new SensorEvent(now, MavlinkDriver.this, SensorEvent.Type.DISCONNECTED);
                                log.info("Disconnected from remote MAVLink system");
                                eventHandler.publishEvent(e);
                            }
                        }
                        
                        // send heartbeat
                        try
                        {
                            msg_heartbeat hb = new msg_heartbeat();
                            sendCommand(hb.pack());
                        }
                        catch (IOException e)
                        {
                        }
                    }
                }, 
                0L, Math.min(MAX_MSG_PERIOD, 1000L) 
        );
    }
    
    
    private void handleNextMessage()
    {
        try
        {
            // read next message
            MAVLinkPacket packet = null;
            while (started && packet == null)
            {
                int c = msgIn.read();
                packet = mavlinkParser.mavlink_parse_char(c);
            }
            
            // if null, it's EOF
            if (packet == null)
                return;
            
            // time tag message receipt
            lastMsgTime = System.currentTimeMillis();
            
            // send connection event
            if (!connected)
            {
                connected = true;
                SensorEvent e = new SensorEvent(lastMsgTime, MavlinkDriver.this, SensorEvent.Type.CONNECTED);
                log.info("Connected to remote MAVLink system");
                eventHandler.publishEvent(e);
            }
            
            // unpack and log message
            MAVLinkMessage msg = packet.unpack();
            log.trace("Received message {} from {}:{}", msg, msg.sysid, msg.compid);
            
            // special case for system time message
            /*if (msg instanceof msg_system_time)
            {
                long unixTime = ((msg_system_time)msg).time_unix_usec;
                log.info("Unix time = " + new DateTimeFormat().formatIso(unixTime/1e6, 0));
            }*/
            
            // let each registered output handle this message
            for (ISensorDataInterface output: this.getAllOutputs().values())
            {
                MavlinkOutput nmeaOut = (MavlinkOutput)output;
                nmeaOut.handleMessage(lastMsgTime, msg);
            }
        }
        catch (IOException e)
        {
            if (started)
                throw new RuntimeException("Error while parsing MAVLink message", e);
        }
    }
    
    
    protected double getUtcTimeFromBootMillis(long timeFromBootMs)
    {
        // just use receiving time stamp for now
        // TODO use sender time stamp for better relative timing accuracy
        return ((double)lastMsgTime) / 1000.;
    }
    
    
    protected void sendCommand(MAVLinkPacket pkt) throws IOException
    {
        pkt.generateCRC();
        byte[] cmdData = pkt.encodePacket();
        cmdOut.write(cmdData);
        cmdOut.flush();
        log.trace("MAVLink command sent: " + pkt.msgid);
    }


    @Override
    public synchronized void stop() throws SensorHubException
    {
        started = false;
        
        if (msgIn != null)
        {
            try { msgIn.close(); }
            catch (IOException e) { }
            msgIn = null;
        }
        
        if (commProvider != null)
        {
            commProvider.stop();
            commProvider = null;
        }
    }


    @Override
    public void cleanup() throws SensorHubException
    {

    }
    
    
    @Override
    public boolean isConnected()
    {
        return connected;
    }

}
