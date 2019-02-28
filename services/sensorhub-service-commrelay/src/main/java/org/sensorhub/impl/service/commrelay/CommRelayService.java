/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.commrelay;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.module.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Comm Relay Service implementation simply forwarding data from incoming input
 * stream to outgoing outputstream, and from outgoing inputstream to incoming
 * outputstream (for commands).
 * </p>
 *
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Feb 16, 2016
 */
public class CommRelayService extends AbstractModule<CommRelayConfig>
{
    private static final Logger log = LoggerFactory.getLogger(CommRelayService.class);
    private static final int EOF = -1;
    
    ICommProvider<?> incoming;
    ICommProvider<?> outgoing;
    boolean started;
    
    
    class TransferThread extends Thread
    {
        InputStream input;
        OutputStream output;
        
        TransferThread(InputStream input, OutputStream output)
        {
            this.input = input;
            this.output = output;
        }
        
        @Override
        public void run()
        {
            byte[] buffer = new byte[config.bufferSize];
            
            try
            {
                if (input != null && output != null)
                {
                    int n = 0;
                    while (started && (n = input.read(buffer)) != EOF)
                        output.write(buffer, 0, n);
                }
            }
            catch (IOException e)
            {
                if (started)
                    log.error("Error while transfering byte stream", e);
            }
        }            
    };
        
    
    public CommRelayService()
    {
    }    


    @Override
    public void start() throws SensorHubException
    {
        // start incoming provider
        if (incoming == null)
        {
            try
            {
                if (config.incomingCommSettings == null)
                    throw new SensorHubException("No incoming communication settings specified");
                
                incoming = config.incomingCommSettings.getProvider();
                incoming.start();
            }
            catch (Exception e)
            {
                incoming = null;
                throw e;
            }
        }
        
        // start ontgoing provider
        if (outgoing == null)
        {
            try
            {
                if (config.outgoingCommSettings == null)
                    throw new SensorHubException("No outgoing communication settings specified");
                
                outgoing = config.outgoingCommSettings.getProvider();
                outgoing.start();
            }
            catch (Exception e)
            {
                outgoing = null;
                throw e;
            }
        }
        
        // start transfer thread
        try
        {
            TransferThread t1 = new TransferThread(incoming.getInputStream(), outgoing.getOutputStream());
            TransferThread t2 = new TransferThread(outgoing.getInputStream(), incoming.getOutputStream());       
            started = true;
            t1.start();
            t2.start();
        }
        catch (IOException e)
        {
            throw new SensorHubException("Error while accessing input and output streams");
        }
    }
    

    @Override
    public void stop() throws SensorHubException
    {
        started = false;
        
        if (incoming != null)
        {
            incoming.stop();
            incoming = null;
        }
        
        if (outgoing != null)
        {
            outgoing.stop();
            outgoing = null;
        }
    }
    

    @Override
    public void cleanup() throws SensorHubException
    {
        
    }

}
