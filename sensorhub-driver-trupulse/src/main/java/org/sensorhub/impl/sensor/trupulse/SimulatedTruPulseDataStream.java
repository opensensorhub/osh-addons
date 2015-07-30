/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2014 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.trupulse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Writer;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.comm.TCPConfig;
import org.sensorhub.impl.module.AbstractModule;


public class SimulatedTruPulseDataStream extends AbstractModule<TCPConfig> implements ICommProvider<TCPConfig>
{
    PipedInputStream is;
    Writer writer;
    
    Timer timer;
    
    
    @Override
    public InputStream getInputStream() throws IOException
    {
        return is;
    }
    

    @Override
    public OutputStream getOutputStream() throws IOException
    {
        return null;
    }
    
    
    private void sendMeasurement()
    {
        try
        {
            float range = 100.0f;
            float az = (float)(Math.random()*360.);
            float inc = (float)(Math.random()*5.+5.);
            String msg = String.format(Locale.US, "$PLTIT,HV,%.2f,M,%.2f,D,%.2f,D,%.2f,M,*FF\n", range, az, inc, range);
            writer.write(msg);
            writer.flush();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }


    @Override
    public void start() throws SensorHubException
    {
        // create piped is / os pair
        try
        {
            PipedOutputStream os = new PipedOutputStream();
            writer = new OutputStreamWriter(os);
            is = new PipedInputStream();
            is.connect(os);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        
        // start simulated datastream thread
        if (timer != null)
            return;
        timer = new Timer();
        
        // start main measurement generation thread
        TimerTask task = new TimerTask() {
            public void run()
            {
                sendMeasurement();
            }            
        };
        
        timer.scheduleAtFixedRate(task, 0, 1000L);
    }


    @Override
    public void stop() throws SensorHubException
    {
        if (is != null)
        {
            try { is.close(); }
            catch (IOException e) { }
            is = null;
        }
        
        if (timer != null)
        {
            timer.cancel();
            timer = null;
        }
    }


    @Override
    public void cleanup() throws SensorHubException
    {        
    }

}
