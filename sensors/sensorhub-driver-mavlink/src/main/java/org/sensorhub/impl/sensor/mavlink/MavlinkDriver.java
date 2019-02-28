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
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.mavlink.MavlinkConfig.MsgTypes;
import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Parser;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.common.msg_command_ack;
import com.MAVLink.common.msg_command_long;
import com.MAVLink.common.msg_heartbeat;
import com.MAVLink.common.msg_param_set;
import com.MAVLink.common.msg_position_target_global_int;
import com.MAVLink.common.msg_set_mode;
import com.MAVLink.enums.MAV_CMD;
import com.MAVLink.enums.MAV_MODE_FLAG;
import com.MAVLink.enums.MAV_PARAM_TYPE;


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
    protected static final String BODY_FRAME = "BODY_FRAME";
    protected static final String GIMBAL_FRAME = "GIMBAL_FRAME";
    protected static final long MAX_MSG_PERIOD = 10000L;
    
    ICommProvider<?> commProvider;
    Timer watchDogTimer;
    volatile boolean started;
    boolean connected;
    InputStream msgIn;
    OutputStream cmdOut;
    Parser mavlinkParser;
    
    long lastMsgTime = 0;
    
    
    enum CopterModes
    {
        STABILIZE,
        ACRO,
        ALT_HOLD,
        AUTO,
        GUIDED,
        LOITER,
        RTL,
        CIRCLE,
        POSITION,
        LAND,
        OF_LOITER,
        DRIFT,
        SPORT,
        FLIP,
        AUTOTUNE,
        POSHOLD
    }
    
    
    @Override
    public void init() throws SensorHubException
    {
        // generate identifiers
        generateUniqueID("urn:osh:sensor:mavlink:", config.vehicleID);
        generateXmlID("MAVLINK_SYSTEM_", config.vehicleID);
        
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
        
        if (config.activeMessages.contains(MsgTypes.BATTERY_STATUS))
        {
            BatteryStatusOutput dataInterface = new BatteryStatusOutput(this);
            addOutput(dataInterface, true);
            dataInterface.init();
        }
        
        // create control inputs depending on selected commands
        // only add the control input objects if some commands were enabled
        MavlinkNavControl navControl = new MavlinkNavControl(this);
        navControl.init();
        if (navControl.commandData.getNumItems() > 0)
            addControlInput(navControl);
        
        MavlinkCameraControl camControl = new MavlinkCameraControl(this);
        camControl.init();
        if (camControl.commandData.getNumItems() > 0)
            addControlInput(camControl);
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
            cmdOut = new BufferedOutputStream(commProvider.getOutputStream());
            
            // send heartbeat
            sendHeartbeat();
        }
        catch (IOException e)
        {
            throw new SensorException("Error while initializing communications ", e);
        }
        
        // set ardupilot parameters
        try
        {         
            setTelemetryRates();
            setGeofenceParams();
            setDefaultNavParams();
            
            if (!config.activeCommands.isEmpty())
            {
                getLogger().info("Switching to GUIDED mode");
                setMode(CopterModes.GUIDED.ordinal());
            }
        }
        catch (Exception e)
        {
            throw new SensorException("Error while setting UAV parameters ", e);
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
    
    
    private void sendHeartbeat()
    {
        try
        {
            msg_heartbeat hb = new msg_heartbeat();
            hb.type = 6;
            hb.autopilot = 8;
            sendCommand(hb.pack());
        }
        catch (IOException e)
        {
        }
    }
    
    
    private void setTelemetryRates() throws IOException
    {
        getLogger().info("Setting Telemetry Update Rate");
        setParam("SR1_RAW_SENS", 0);
        //setParam("SR1_EXT_STAT", 0);
        setParam("SR1_RC_CHAN", 0);
        setParam("SR1_RAW_CTRL", 0);
        setParam("SR1_POSITION", 10);
        setParam("SR1_EXTRA1", 10);
        //setParam("SR1_EXTRA2", 0);
        setParam("SR1_EXTRA3", 10);
    }
    
    
    private void setDefaultNavParams() throws IOException
    {
        getLogger().info("Setting Navigation Parameters");
        setParam("WPNAV_RADIUS", 100);
        setParam("CIRCLE_RADIUS", 2000);
        setParam("CIRCLE_RATE", 5);
    }
    
    
    private void setGeofenceParams() throws IOException
    {
        getLogger().info("Setting Geofencing Parameters");
        setParam("FENCE_TYPE", 3);
        setParam("FENCE_ALT_MAX", config.maxAltitude);
        setParam("FENCE_RADIUS", config.maxTravelDistance);
        setParam("FENCE_MARGIN", 2f);
        setParam("FENCE_ENABLE", 1);
    }
    
    
    protected void setParam(String name, float value) throws IOException
    {
        msg_param_set setParam = new msg_param_set();
        setParam.target_system = 1;
        setParam.target_component = 1;
        setParam.param_type = MAV_PARAM_TYPE.MAV_PARAM_TYPE_REAL32;
        setParam.setParam_Id(name);
        setParam.param_value = value;
        sendCommand(setParam.pack());
    }
    
    
    protected void setMode(int mode) throws IOException
    {
        // this command typeis not implemented by ArduCopter although the docs say it is...
        /*msg_command_long cmd = new msg_command_long();
        cmd.target_system = 1;
        cmd.target_component = 1;
        cmd.command = MAV_CMD.MAV_CMD_DO_SET_MODE;
        cmd.param1 = (float)mode;
        sendCommand(cmd.pack()); */
        
        msg_set_mode cmd = new msg_set_mode();
        cmd.target_system = 1;
        cmd.base_mode = MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED;
        cmd.custom_mode = mode;
        sendCommand(cmd.pack());
    }
    
    
    protected void armMotors() throws IOException
    {
        msg_command_long cmd = new msg_command_long();
        cmd.target_system = 1;
        cmd.target_component = 1;
        cmd.command = MAV_CMD.MAV_CMD_COMPONENT_ARM_DISARM; // working but need gps fix and other prearm checks
        cmd.param1 = 1;
        sendCommand(cmd.pack());
    }
    
    
    protected void sendCommand(MAVLinkPacket pkt) throws IOException
    {
        synchronized (cmdOut)
        {
            pkt.compid = 0;
            pkt.generateCRC();
            byte[] cmdData = pkt.encodePacket();
            cmdOut.write(cmdData);
            cmdOut.flush();
            getLogger().trace("MAVLink command sent: " + pkt.msgid);
        }
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
                                notifyConnectionStatus(false, "MAVLink system");
                            }
                        }
                        
                        // send heartbeat
                        sendHeartbeat();
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
                notifyConnectionStatus(true, "MAVLink system");
            }
            
            // unpack and log message
            MAVLinkMessage msg = packet.unpack();
            if (msg instanceof msg_command_ack || msg instanceof msg_position_target_global_int)
                getLogger().info("Received {}", msg);
            else
                getLogger().trace("Received message {} ({}) from {}:{}", msg, msg.getClass().getName(), msg.sysid, msg.compid);
            
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
