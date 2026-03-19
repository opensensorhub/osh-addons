/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
Copyright (C) 2025 Botts Innovative Research. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.comm.mavsdk;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;

import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.comm.ICommConfig;
import org.sensorhub.api.comm.IDeviceInfo;
import org.sensorhub.api.comm.ICommNetwork;
import org.sensorhub.api.comm.IDeviceScanCallback;
import org.sensorhub.api.comm.IDeviceScanner;
import org.sensorhub.api.comm.INetworkInfo;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IDataProducerModule;
import org.sensorhub.impl.comm.UDPConfig;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.sensor.SensorSystem;
import org.sensorhub.impl.sensor.SensorSystemConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import io.mavsdk.telemetry.Telemetry;
//import io.mavsdk.action.Action;
//import io.mavsdk.camera.Camera;
import io.mavsdk.core.Core;

/**
 * <p>
 * Network implementation for MAVLink networks
 * provided by the MAVSDK Library
 * </p>
 *
 * @author Alex Robin
 * @since Feb 10, 2016
 */
public class MavLinkCommNetwork extends AbstractModule<MavLinkNetworkConfig> implements ICommNetwork<MavLinkNetworkConfig>
{
    private static final Logger log = LoggerFactory.getLogger(MavLinkCommNetwork.class);
    
    NetworkInterface netInterface;
    MavLinkScanner scanner;

    class MavLinkScanner implements IDeviceScanner
    {
        volatile boolean scanning;

        @Override
        public void startScan(IDeviceScanCallback callback)
        {
            startScan(callback, null);
        }

//        //Potential fix for UI bug where subsystem doesn't show up. Normally will have to
//        //hit "Refresh" in the browser
//        private AbstractModule<?> registerSubmodule(ModuleConfig config) throws SensorHubException {
//            var newMember = new SensorSystemConfig.SystemMember();
//            newMember.config = config;
//            var newSubmodule = (AbstractModule<?>) addSubsystem(newMember);
//
//            // Wait for loaded module, then notify listeners of config changed. QOL for admin UI
//            threadPool.execute(() -> {
//                try {
//                    newSubmodule.waitForState(ModuleEvent.ModuleState.LOADED, 10000);
//                } catch (SensorHubException e) {
//                    throw new RuntimeException(e);
//                }
//                eventHandler.publish(new ModuleEvent(this, ModuleEvent.Type.CONFIG_CHANGED));
//            });
//
//            return newSubmodule;
//        }

        private void registerModule() {

            ISensorHub hub = getParentHub();

            ModuleRegistry registry = hub.getModuleRegistry();

            // Getting list of available module providers

            // Creating config and loading a new module

            SensorSystemConfig sensorSystemConfig = new SensorSystemConfig();
            sensorSystemConfig.id = UUID.randomUUID().toString();
            sensorSystemConfig.name = "MAVLink Connected Systems";
            sensorSystemConfig.autoStart = true;
            sensorSystemConfig.moduleClass = SensorSystem.class.getCanonicalName();
            sensorSystemConfig.uniqueID = UUID.randomUUID().toString();

            SensorSystem sensorSystem = null;
            try {
                sensorSystem = (SensorSystem) registry.loadModule(sensorSystemConfig);
            } catch (SensorHubException e) {
                throw new RuntimeException(e);
            }

            UnmannedConfig unmannedConfig = new UnmannedConfig();
            unmannedConfig.id = UUID.randomUUID().toString();
            unmannedConfig.name = "Unmanned System";
            unmannedConfig.autoStart = true;
            unmannedConfig.moduleClass = UnmannedSystem.class.getCanonicalName();
            unmannedConfig.serialNumber = UUID.randomUUID().toString();

            try {

                SensorSystemConfig.SystemMember member = new SensorSystemConfig.SystemMember();
                member.config = unmannedConfig;
                sensorSystem.addSubsystem(member);
                var members = sensorSystem.getMembers();
                for (IDataProducerModule<?> m: members.values()) {
                    if ( !m.isInitialized() ) {
                       m.init();
                    }

                    if ( !m.isStarted() ) {
                        m.start();
                    }
                }

            } catch (SensorHubException e) {
                throw new RuntimeException(e);
            }
        }

        private void receiveDrone( final IDeviceScanCallback callback ) {

            System.out.println("Listening for drone connection...");

            io.mavsdk.System drone = new io.mavsdk.System();
            drone.getCore().getConnectionState()
                .filter(Core.ConnectionState::getIsConnected)
                .firstElement()
                .subscribe(state -> {
                    System.out.println("Drone connection detected.");

                    drone.getInfo().getProduct().subscribe(
                            info -> {

                                // create device info
                                IDeviceInfo devInfo = new IDeviceInfo() {

                                    @Override
                                    public String getName()
                                    {
                                        var id = info.getVendorId();
                                        return "UAS System " + id;
                                    }

                                    @Override
                                    public String getType()
                                    {
                                        return info.getProductName();
                                    }

                                    @Override
                                    public String getAddress()
                                    {
                                        return "127.0.0.1";
                                    }

                                    @Override
                                    public String getSignalLevel()
                                    {
                                        return null;
                                    }

                                    @Override
                                    public ICommConfig getCommConfig()
                                    {
                                        final ICommConfig commConfig;
                                        UDPConfig tcpConfig = new UDPConfig();
                                        tcpConfig.remoteHost = "127.0.0.1";
                                        tcpConfig.remotePort = 14550;
                                        commConfig = tcpConfig;

                                        return commConfig;
                                    }
                                };


                                callback.onDeviceFound(devInfo);

                                registerModule();
                            });

                });
        }

        @Override
        public synchronized void startScan(final IDeviceScanCallback callback, String idRegex)
        {
            this.scanning = true;

            receiveDrone(callback);
        }
        

        @Override
        public synchronized void stopScan()
        {
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
    protected void doInit() throws SensorHubException
    {
        super.doInit();
        
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
                    if (bytes != null)
                    {
                        for (byte b: bytes)
                            buf.append(String.format("%02X", b)).append(':');
                        mac = buf.substring(0, buf.length()-1);
                    }
                    // If interface is loopback, VPN, or other non-physical interface,
                    // it won't have physical address in some operating systems
                    else
                        mac = "NONE";

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
                            return MavLinkCommNetwork.this.getNetworkType(netInt);
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
    protected void doStart() throws SensorHubException
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
    }


    @Override
    protected void doStop() throws SensorHubException
    {
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
            scanner = new MavLinkScanner();
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
