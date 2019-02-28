/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.comm.zeroconf;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceTypeListener;
import org.sensorhub.api.comm.ICommConfig;
import org.sensorhub.api.comm.IDeviceInfo;
import org.sensorhub.api.comm.ICommNetwork;
import org.sensorhub.api.comm.IDeviceScanCallback;
import org.sensorhub.api.comm.IDeviceScanner;
import org.sensorhub.api.comm.INetworkInfo;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.comm.TCPConfig;
import org.sensorhub.impl.comm.UDPConfig;
import org.sensorhub.impl.module.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Network implementation for IP based networks, with support for ZeroConf
 * provided by the jmDNS library.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 10, 2016
 */
public class IpCommNetwork extends AbstractModule<IpNetworkConfig> implements ICommNetwork<IpNetworkConfig>
{
    private static final Logger log = LoggerFactory.getLogger(IpCommNetwork.class); 
    
    NetworkInterface netInterface;
    ZeroConfDeviceScanner scanner;
    JmDNS jmdns;
    
        
    class ZeroConfDeviceScanner implements IDeviceScanner
    {
        volatile boolean scanning;
        ServiceTypeListener sTypeListener;
        Map<String, ServiceListener> srvListeners = new HashMap<String, ServiceListener>();        
        
        @Override
        public void startScan(IDeviceScanCallback callback)
        {
            startScan(callback, null);
        }

        @Override
        public synchronized void startScan(final IDeviceScanCallback callback, String idRegex)
        {
            this.scanning = true;
            
            try
            {
                jmdns.addServiceTypeListener(sTypeListener = new ServiceTypeListener() {
                    @Override
                    public void serviceTypeAdded(ServiceEvent ev)
                    {
                        log.debug("Service Type Added: " + ev.getType());                
                        ServiceListener srvListener = new ServiceListener()
                        {
                            @Override
                            public void serviceAdded(ServiceEvent ev)
                            {
                                log.debug("Service Added: " + ev.getName() + "." + ev.getType());
                                
                                ServiceInfo svcInfo = jmdns.getServiceInfo(ev.getType(), ev.getName(), 1);
                                if (svcInfo != null)
                                    notifyServiceInfo(svcInfo, callback);
                            }

                            @Override
                            public void serviceRemoved(ServiceEvent ev)
                            {                                
                            }

                            @Override
                            public void serviceResolved(final ServiceEvent ev)
                            {
                                log.debug("Service Resolved: " + ev.getName() +
                                        ", Type=" + ev.getType() + 
                                        ", Address=" + ev.getInfo().getInetAddresses()[0],
                                        ", Port=" + ev.getInfo().getPort());
                                
                                notifyServiceInfo(ev.getInfo(), callback);
                            }                            
                        };
                        
                        srvListeners.put(ev.getType(), srvListener);
                        jmdns.addServiceListener(ev.getType(), srvListener);
                    }

                    @Override
                    public void subTypeForServiceTypeAdded(ServiceEvent event)
                    {   
                    }
                });
            }
            catch (IOException e)
            {
                callback.onScanError(e);
            }     
        }
        
        protected void notifyServiceInfo(final ServiceInfo svcInfo, IDeviceScanCallback callback)
        {
            // use IPV6 only if no IPV4 is found
            InetAddress ipAdd = null;
            Inet4Address[] ipv4List = svcInfo.getInet4Addresses();
            InetAddress[] ipList = svcInfo.getInetAddresses();
            if (ipv4List.length > 0)
                ipAdd = ipv4List[0];
            else if (ipList.length > 0)
                ipAdd = ipList[0];
            final String ip = (ipAdd != null) ? ipAdd.getHostAddress() : "NONE";
            
            // remove last dot from server name
            String server = svcInfo.getServer();
            if (server.endsWith("."))
                server = server.substring(0, server.length()-1);
            final String hostName = server;
            
            final String type = svcInfo.getType();
            int port = svcInfo.getPort();
            
            // build comm config
            final ICommConfig commConfig;
            if (type.contains("_tcp."))
            {
                TCPConfig tcpConfig = new TCPConfig();
                tcpConfig.remoteHost = ip;
                tcpConfig.remotePort = port;
                commConfig = tcpConfig;
            }
            else if (type.contains("_udp."))
            {
                UDPConfig tcpConfig = new UDPConfig();
                tcpConfig.remoteHost = ip;
                tcpConfig.remotePort = port;
                commConfig = tcpConfig;
            }
            else
                commConfig = null;
            
            // create device info
            IDeviceInfo devInfo = new IDeviceInfo() {

                @Override
                public String getName()
                {
                    return hostName;
                }

                @Override
                public String getType()
                {
                    return type;
                }

                @Override
                public String getAddress()
                {
                    return ip;
                }

                @Override
                public String getSignalLevel()
                {
                    return null;
                }

                @Override
                public ICommConfig getCommConfig()
                {
                    return commConfig;
                }                                    
            };
            
            callback.onDeviceFound(devInfo);
        }

        @Override
        public synchronized void stopScan()
        {
            if (sTypeListener != null)
                jmdns.removeServiceTypeListener(sTypeListener);
            
            for (Entry<String, ServiceListener> entry: srvListeners.entrySet())
                jmdns.removeServiceListener(entry.getKey(), entry.getValue());
            
            this.scanning = false;
            log.debug("Scan stopped");
        }

        @Override
        public boolean isScanning()
        {
            return scanning;
        }       
    }


    @Override
    public void init() throws SensorHubException
    {
        super.init();
        
        // use first interface by default if none is specified
        if (config.networkInterface == null)
        {
            try
            {
                Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
                if (nets.hasMoreElements())
                    config.networkInterface = nets.nextElement().getName();
            }
            catch (SocketException e)
            {
            }
        }
    }


    @Override
    public Collection<INetworkInfo> getAvailableNetworks()
    {
        ArrayList<INetworkInfo> ipNetworks = new ArrayList<INetworkInfo>();
        
        try
        {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            for (final NetworkInterface netInt : Collections.list(nets))
            {
                if (!netInt.isLoopback() && netInt.isUp())
                {
                    // MAC address
                    final String mac;
                    StringBuilder buf = new StringBuilder();
                    byte[] bytes = netInt.getHardwareAddress();
                    for (byte b: bytes)
                        buf.append(String.format("%02X", b)).append(':');
                    mac = buf.substring(0, buf.length()-1);
                    
                    // IP address
                    Enumeration<InetAddress> ipList = netInt.getInetAddresses();
                    InetAddress ipAdd = getDefaultInetAddress(ipList);
                    final String ip = (ipAdd != null) ? ipAdd.getHostAddress() : "NONE";
                
                    INetworkInfo netInfo = new INetworkInfo() {

                        @Override
                        public String getInterfaceName()
                        {
                            return netInt.getName();
                        }

                        @Override
                        public NetworkType getNetworkType()
                        {
                            return IpCommNetwork.this.getNetworkType(netInt);
                        }

                        @Override
                        public String getHardwareAddress()
                        {
                            return mac; 
                        }

                        @Override
                        public String getLogicalAddress()
                        {
                            return ip;
                        }
                        
                    };
                    
                    ipNetworks.add(netInfo);
                }
            }
        }
        catch (SocketException e)
        {
            throw new RuntimeException("Error while scanning available network interfaces");
        }
        
        return ipNetworks;
    }


    @Override
    public void start() throws SensorHubException
    {
        // bind to selected network interface
        try
        {            
            // try to find it by name
            netInterface = NetworkInterface.getByName(config.networkInterface);
            
            // else try to find it by IP address
            if (netInterface == null)
                netInterface = NetworkInterface.getByInetAddress(InetAddress.getByName(config.networkInterface));
            
            if (netInterface == null)
                throw new SensorHubException("Cannot find local network interface " + config.networkInterface);
        }
        catch (SocketException | UnknownHostException e)
        {
            throw new SensorHubException("Error while looking up network interface " + config.networkInterface, e);
        }
        
        
        try
        {
            Enumeration<InetAddress> ipList = netInterface.getInetAddresses();
            jmdns = JmDNS.create(getDefaultInetAddress(ipList));
        }
        catch (IOException e)
        {
            throw new SensorHubException("Error while starting ZeroConf service", e);
        }
    }


    @Override
    public void stop() throws SensorHubException
    {
        try
        {
            if (jmdns != null)
                jmdns.close();
            jmdns = null;
        }
        catch (IOException e)
        {
        }        
    }


    @Override
    public String getInterfaceName()
    {
        return netInterface.getName();
    }


    @Override
    public NetworkType getNetworkType()
    {
        if (netInterface == null)
            return NetworkType.IP;
        else
            return getNetworkType(netInterface);
    }
    
    
    protected NetworkType getNetworkType(NetworkInterface netInt)
    {
        String name = netInt.getName();
        
        if (name.startsWith("eth") || name.startsWith("en"))
            return NetworkType.ETHERNET;
        else if (name.startsWith("wlan") || name.startsWith("wl"))
            return NetworkType.WIFI;
        
        return NetworkType.IP;
    }
    
    
    @Override
    public boolean isOfType(NetworkType type)
    {
        if (type == NetworkType.IP)
            return true;
        
        return (type == getNetworkType());
    }
    
    
    @Override
    public IDeviceScanner getDeviceScanner()
    {
        if (scanner == null)
            scanner = new ZeroConfDeviceScanner();
        return scanner;
    }


    @Override
    public void cleanup() throws SensorHubException
    {
       
    }
    
    
    /*
     * Tries to get the first IPv4 address;
     * If none is found, defaults to the first IP address
     */
    protected InetAddress getDefaultInetAddress(Enumeration<InetAddress> ipEnum)
    {
        InetAddress defaultIp = null;
        
        while (ipEnum.hasMoreElements())
        {
            InetAddress ip = ipEnum.nextElement();
            if (ip instanceof Inet4Address)
                return ip;
            else if (defaultIp == null)
                defaultIp = ip;
        }
    
        return defaultIp;
    }
}
