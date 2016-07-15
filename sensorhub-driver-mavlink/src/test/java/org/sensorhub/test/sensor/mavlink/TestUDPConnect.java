/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.sensor.mavlink;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Parser;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.ardupilotmega.*;
import com.MAVLink.common.*;
import com.MAVLink.enums.*;

public class TestUDPConnect
{
    static DatagramSocket socket;
    
    
    public static void main(String[] args) throws Exception
    {
        socket = new DatagramSocket(14550);
        //socket.setReuseAddress(true);
        
        final byte[] receiveData = new byte[300];
        final DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        final Parser mavlinkParser = new Parser();
        
        // start telemetry thread
        Thread t = new Thread()
        {
            public void run()
            {
                while (true)
                {
                    try
                    {
                        receivePacket.setLength(300);
                        socket.receive(receivePacket);
                        int length = receivePacket.getLength();
                        /*System.out.println(length + " bytes received");
                        for (int i=0; i<length; i++)
                            System.out.print(String.format("%02x", receiveData[i] & 0xFF));
                        System.out.println();*/
                        //System.out.println(receivePacket.getAddress() + ":" + receivePacket.getPort());
                        
                        MAVLinkPacket packet = null;
                        for (int i=0; i<length; i++)
                            packet = mavlinkParser.mavlink_parse_char(receiveData[i] & 0xFF);                
                        
                        if (packet != null)
                        {
                            MAVLinkMessage msg = packet.unpack();
                            
                            //if (msg instanceof msg_command_ack)
                                //System.out.println(receivePacket.getAddress() + ": " + msg);
                            System.out.println(msg + " (" + msg.getClass() + ")");
                            System.out.println(receivePacket.getAddress() + ":" + receivePacket.getPort());

//                            if (msg instanceof msg_gimbal_report)
//                                System.out.println(((msg_gimbal_report) msg).joint_el*180/Math.PI);
                            
//                            if (msg instanceof msg_global_position_int)
//                                System.out.println(msg);
                            
//                            if (msg instanceof msg_attitude)
//                                System.out.println(msg);
//                            
//                            if (msg instanceof msg_param_value)
//                                System.out.println(((msg_param_value) msg).getParam_Id() + ": " + ((msg_param_value) msg).param_value);
                            
                            //System.out.println("sysid = " + msg.sysid);
                            //System.out.println(msg.compid);
                            //if (msg instanceof msg_statustext)
                            //    System.err.println(((msg_statustext)msg).getText());
                        }
                    }
                    catch (Exception e)
                    {
                        System.err.println(e.getMessage());
                    }
                }
            }
        };
        t.start();
        
        System.err.println("Disabling pre-arm checks");
        msg_param_set setParam = new msg_param_set();
        setParam.target_system = 1;
        setParam.target_component = 1;
        setParam.setParam_Id("ARMING_CHECK");
        setParam.param_type = MAV_PARAM_TYPE.MAV_PARAM_TYPE_REAL32;
        setParam.param_value = 0;
        sendCommand(setParam.pack());        
        Thread.sleep(1000);        
        
        System.err.println("Arming system");
        msg_command_long cmd = new msg_command_long();
        cmd.target_system = 1;
        cmd.target_component = 1;
        cmd.command = MAV_CMD.MAV_CMD_COMPONENT_ARM_DISARM; // working but need gps fix and other prearm checks
        cmd.param1 = 1;
        sendCommand(cmd.pack());        
        Thread.sleep(5000);
        
        /*
        // set telemetry update rate
        System.err.println("Changing telemetry rate");
        setParam = new msg_param_set();
        setParam.target_system = 1;
        setParam.target_component = 1;
        setParam.param_type = MAV_PARAM_TYPE.MAV_PARAM_TYPE_REAL32;
        
        setParam.setParam_Id("SR1_RAW_SENS");
        setParam.param_value = 0;
        sendCommand(setParam.pack());
        
        setParam.setParam_Id("SR1_EXT_STAT");
        setParam.param_value = 0;
        sendCommand(setParam.pack());
        
        setParam.setParam_Id("SR1_RC_CHAN");
        setParam.param_value = 0;
        sendCommand(setParam.pack());
        
        setParam.setParam_Id("SR1_RAW_CTRL");
        setParam.param_value = 0;
        sendCommand(setParam.pack());
        
        setParam.setParam_Id("SR1_EXTRA2");
        setParam.param_value = 0;
        sendCommand(setParam.pack());
        
        setParam.setParam_Id("SR1_EXTRA3");
        setParam.param_value = 10;
        sendCommand(setParam.pack());
        
        setParam.setParam_Id("SR1_POSITION");
        setParam.param_value = 10;
        sendCommand(setParam.pack());
        
        setParam.setParam_Id("SR1_EXTRA1");
        setParam.param_value = 10;
        sendCommand(setParam.pack());
        
        
        
        Thread.sleep(1000);
        
        System.err.println("Gimbal Config");
        msg_mount_configure confGimbal = new msg_mount_configure();
        confGimbal.target_system = 1;
        confGimbal.target_component = 0;
        confGimbal.mount_mode = MAV_MOUNT_MODE.MAV_MOUNT_MODE_MAVLINK_TARGETING;
        //confGimbal.mount_mode = MAV_MOUNT_MODE.MAV_MOUNT_MODE_RC_TARGETING;
        //confGimbal.mount_mode = MAV_MOUNT_MODE.MAV_MOUNT_MODE_RETRACT;
        confGimbal.stab_pitch = 1;
        confGimbal.stab_roll = 1;
        confGimbal.stab_yaw = 1;
        sendCommand(confGimbal.pack());        
        Thread.sleep(2000);
        
//        System.err.println("Start Camera");
//        msg_mount_control setGimbal = new msg_mount_control();
//        setGimbal.target_system = 1;
//        setGimbal.target_component = 0;
//        setGimbal.input_a = 0;
//        setGimbal.input_c = 0;
//        sendCommand(setGimbal.pack());        
//        Thread.sleep(1000);
        
        System.err.println("Gimbal Control");
        msg_mount_control setGimbal = new msg_mount_control();
        setGimbal.target_system = 1;
        setGimbal.target_component = 0;
        setGimbal.input_a = -1000;
        setGimbal.input_c = 0;
        sendCommand(setGimbal.pack());        
        Thread.sleep(1000);*/
    }
    
    
    static void sendCommand(MAVLinkPacket pkt) throws IOException
    {
        pkt.generateCRC();
        byte[] cmdData = pkt.encodePacket();
        DatagramPacket dgm = new DatagramPacket(cmdData, cmdData.length, InetAddress.getByName("10.1.1.10"), 14560);
        socket.send(dgm);
    }

}
