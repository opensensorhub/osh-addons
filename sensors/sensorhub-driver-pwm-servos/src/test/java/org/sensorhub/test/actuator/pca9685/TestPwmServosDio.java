/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.actuator.pca9685;

import java.io.IOException;
import java.nio.ByteBuffer;
import jdk.dio.DeviceConfig;
import jdk.dio.DeviceManager;
import jdk.dio.i2cbus.I2CDevice;
import jdk.dio.i2cbus.I2CDeviceConfig;


public class TestPwmServosDio
{
    static int __MODE1              = 0x00;
    static int __MODE2              = 0x01;
    static int __SUBADR1            = 0x02;
    static int __SUBADR2            = 0x03;
    static int __SUBADR3            = 0x04;
    static int __PRESCALE           = 0xFE;
    static int __LED0_ON_L          = 0x06;
    static int __LED0_ON_H          = 0x07;
    static int __LED0_OFF_L         = 0x08;
    static int __LED0_OFF_H         = 0x09;
    static int __ALL_LED_ON_L       = 0xFA;
    static int __ALL_LED_ON_H       = 0xFB;
    static int __ALL_LED_OFF_L      = 0xFC;
    static int __ALL_LED_OFF_H      = 0xFD;

    // Bits
    static byte __RESTART           = (byte)0x80;
    static byte __SLEEP             = (byte)0x10;
    static byte __ALLCALL           = (byte)0x01;
    static byte __INVRT             = (byte)0x10;
    static byte __OUTDRV            = (byte)0x04;
    
    
    static ByteBuffer buf = ByteBuffer.allocate(10);
    static I2CDevice i2c;
    
    
    public static void main(String[] args) throws Exception
    {
        I2CDeviceConfig i2cConf = new I2CDeviceConfig(
                1,
                0x40,
                DeviceConfig.DEFAULT,
                DeviceConfig.DEFAULT);
        
        // open I2C device
        i2c = DeviceManager.<I2CDevice>open(i2cConf);        
        reset();
        
        setPWMFreq(50);
        /*while (true)
        {
            setPWM(0, (short)0, (short)200);
            Thread.sleep(1000L);
            setPWM(0, (short)0, (short)500);
            Thread.sleep(1000L);
        }*/
        
        short off = (short)200;
        while (true)
        {
            setPWM(0, (short)0, off);
            setPWM(3, (short)0, off);
            Thread.sleep(200L);
            off += 20;
            if (off > 500)
                off = 200;
        }
    }
    
    
    static void reset() throws IOException, InterruptedException
    {
        System.out.println("Resetting PCA9685 MODE1 (without SLEEP) and MODE2");
        setAllPWM((short)0, (short)0);
        writeByte(__MODE2, __OUTDRV);
        writeByte(__MODE1, __ALLCALL);
        Thread.sleep(5);
        
        byte mode1 = readByte(__MODE1);
        mode1 = (byte)(mode1 & ~__SLEEP); // wake up (reset sleep)
        writeByte(__MODE1, mode1);
        Thread.sleep(5);
    }
    
    
    // Sets the PWM frequency
    static void setPWMFreq(float freq) throws IOException, InterruptedException
    {
        System.out.format("Setting PWM frequency to %f Hz\n", freq);
        
        double prescaleval = 25000000.0;   // 25MHz
        prescaleval /= 4096.0;             // 12-bit
        prescaleval /= freq;
        prescaleval -= 1.0;
        int prescale = (int)Math.floor(prescaleval + 0.5);
        System.out.format("Final pre-scale: %d\n", prescale);

        byte oldmode = readByte(__MODE1);
        byte newmode = (byte)((oldmode & 0x7F) | __SLEEP);
        writeByte(__MODE1, newmode); // go to sleep
        writeByte(__PRESCALE, (byte)prescale);
        writeByte(__MODE1, oldmode);
        Thread.sleep(5);
        writeByte(__MODE1, (byte)(oldmode | __RESTART));
        System.out.println("Restarted PCA9685");
    }
    
    
    // Sets a single PWM channel
    static void setPWM(int channel, short on, short off) throws IOException
    {
        writeByte(__LED0_ON_L+4*channel, (byte)(on & 0xFF));
        writeByte(__LED0_ON_H+4*channel, (byte)(on >> 8));
        writeByte(__LED0_OFF_L+4*channel, (byte)(off & 0xFF));
        writeByte(__LED0_OFF_H+4*channel, (byte)(off >> 8));
    }
    
    
    // Sets all PWM channels
    static void setAllPWM(short on, short off) throws IOException
    {
        writeByte(__ALL_LED_ON_L, (byte)(on & 0xFF));
        writeByte(__ALL_LED_ON_H, (byte)(on >> 8));
        writeByte(__ALL_LED_OFF_L, (byte)(off & 0xFF));
        writeByte(__ALL_LED_OFF_H, (byte)(off >> 8));
    }
    
    
    static byte readByte(int reg) throws IOException
    {
        buf.clear();
        i2c.read(reg, 1, buf);
        buf.flip();
        return buf.get();
    }
    
    
    static void writeByte(int reg, byte b) throws IOException
    {
        buf.clear();
        buf.put(b);
        buf.flip();
        i2c.write(reg, 1, buf);
    }

}
