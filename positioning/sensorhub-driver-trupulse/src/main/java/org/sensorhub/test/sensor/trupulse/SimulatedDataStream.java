/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.sensor.trupulse;

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
import org.sensorhub.api.comm.CommProviderConfig;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.module.AbstractModule;


public class SimulatedDataStream extends AbstractModule<CommProviderConfig<?>> implements ICommProvider<CommProviderConfig<?>>
{
    PipedInputStream is;
    Writer writer;    
    Timer timer;
    int counter = 0;
    
    
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
            float range = 100f;
            float az = (float)((counter%4)*90);
            float inc = (float)((counter/4)*10);
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
        counter = 0;
        
        // start main measurement generation thread
        TimerTask task = new TimerTask() {
            public void run()
            {
                sendMeasurement();
                counter++;
            }            
        };
        
        timer.scheduleAtFixedRate(task, 0, 5000L);
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
