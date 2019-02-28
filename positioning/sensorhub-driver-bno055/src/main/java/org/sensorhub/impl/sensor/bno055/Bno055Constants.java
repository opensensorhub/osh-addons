/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.bno055;


public class Bno055Constants
{
    public final static byte START_BYTE = (byte)0xAA;
    public final static byte ACK_BYTE = (byte)0xBB;
    public final static byte ERR_BYTE = (byte)0xEE;
    
    public final static byte DATA_WRITE = (byte)0x00;
    public final static byte DATA_READ = (byte)0x01;
    
    public final static byte OPERATION_MODE_ADDR = 0X3D;
    public final static byte POWER_MODE_ADDR = 0X3E;
    public final static byte SYS_TRIGGER_ADDR = 0X3F;
    
    public final static byte OPERATION_MODE_CONFIG = 0x00;
    public final static byte OPERATION_MODE_ACCONLY = 0X01;
    public final static byte OPERATION_MODE_MAGONLY = 0X02;
    public final static byte OPERATION_MODE_GYRONLY = 0X03;
    public final static byte OPERATION_MODE_ACCMAG = 0X04;
    public final static byte OPERATION_MODE_ACCGYRO = 0X05;
    public final static byte OPERATION_MODE_MAGGYRO = 0X06;
    public final static byte OPERATION_MODE_AMG = 0X07;
    public final static byte OPERATION_MODE_IMUPLUS = 0X08;
    public final static byte OPERATION_MODE_COMPASS = 0X09;
    public final static byte OPERATION_MODE_M4G = 0X0A;
    public final static byte OPERATION_MODE_NDOF_FMC_OFF = 0X0B;
    public final static byte OPERATION_MODE_NDOF = 0X0C;    
    
    public final static byte POWER_MODE_NORMAL = 0X00;
    public final static byte POWER_MODE_LOWPOWER = 0X01;
    public final static byte POWER_MODE_SUSPEND = 0X02;
    
    public final static byte QUAT_DATA_W_LSB_ADDR = 0x20;
    public final static byte QUAT_SIZE = 0x08;
    
    public final static byte CALIB_STAT_ADDR = 0x35;
    public final static byte CALIB_ADDR = 0x55;
    public final static byte CALIB_SIZE = 22;
}
