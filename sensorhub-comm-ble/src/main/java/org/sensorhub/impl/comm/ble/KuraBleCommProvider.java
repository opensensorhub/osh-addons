/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.comm.ble;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.comm.RS232Config;
import org.sensorhub.impl.module.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Communication provider for serial ports
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since July 2, 2015
 */
public class KuraBleCommProvider extends AbstractModule<RS232Config> implements ICommProvider<RS232Config>
{
    static final Logger log = LoggerFactory.getLogger(KuraBleCommProvider.class);
    
    InputStream is;
    OutputStream os;
    
    
    public KuraBleCommProvider() 
    {
    }
    
    
    @Override
    public void start() throws SensorHubException
    {
        
    }
    
    
    @Override
    public InputStream getInputStream() throws IOException
    {
        return is;
    }


    @Override
    public OutputStream getOutputStream() throws IOException
    {
        return os;
    }    


    @Override
    public void stop() throws SensorHubException
    {

        
        is = null;
        os = null;
    }


    @Override
    public void cleanup() throws SensorHubException
    {        
    }
}
