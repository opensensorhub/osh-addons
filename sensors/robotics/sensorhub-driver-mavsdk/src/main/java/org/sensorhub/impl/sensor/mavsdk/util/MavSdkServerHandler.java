/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.mavsdk.util;

import org.sensorhub.impl.sensor.mavsdk.UnmannedConfig;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;

public class MavSdkServerHandler {

    private Process process = null;

    public void stop() {
        if ( null != process && process.isAlive() ) {
            process.destroy();
        }
    }

    public boolean start( UnmannedConfig config ) {

        //Start mavsdk_server executable for the appropriate platform
        //We have already downloaded the native binaries
        PlatformId.Platform platform = PlatformId.get();

        String mavsdkServerString = "";

        if ( platform.os().equals("macOS") ) {
            if ( platform.arch().equals("ARM64")) {

                mavsdkServerString = "mavsdk_server_macos_arm64";

            } else if ( platform.arch().equals("x86_64")) {

                mavsdkServerString = "mavsdk_server_macos_x64";

            }
        } else if ( platform.os().equals("Windows")) {
            if ( platform.arch().equals("ARM64")) {

                mavsdkServerString = "mavsdk_server_win_arm64.exe";

            } else if ( platform.arch().equals("x86_64")) {

                mavsdkServerString = "mavsdk_server_win_x64.exe";

            }
        } else if ( platform.os().equals("Linux")) {
            if ( platform.arch().equals("ARM64")) {

                mavsdkServerString = "mavsdk_server_linux-arm64-musl";

            } else {

                mavsdkServerString = "mavsdk_server_musl_x86_64";
            }
        }

        //example:
        //mavsdk_server_macos_arm64 -p 50051 serial:///dev/tty.usbserial-0001:57600
        if ( ! "".equals(mavsdkServerString)) {

            Path exePath = extractExecutable(mavsdkServerString);

            /**
             * Each configuration (NETWORK, SERIAL, EMPTY) will kickstart
             * the mavsdk_server binary with different parameter options. Some will use the UDP address/port
             * some will use the serial interface.
             */
            ProcessBuilder pb = switch( config.connectionType ) {
                case NETWORK -> new ProcessBuilder(
                        exePath.toString(),   // program
                        "-p", String.valueOf(config.SDKPort),
                        "udpin://" + config.UDPListenAddr + ":" + String.valueOf(config.UDPListenPort));
                case SERIAL -> new ProcessBuilder(
                        exePath.toString(),   // program
                        "tcpin://" + config.SDKAddress + ":" + String.valueOf(config.SDKPort),
                        config.SerialPort);
                case EMPTY ->  new ProcessBuilder(exePath.toString());
            };

            pb.inheritIO(); // forward output

            try {

                PortOwnership ownership = checkPortOwnership(platform, mavsdkServerString, config.SDKPort);

                switch ( ownership ) {
                    case OWNED_BY_MAVSDK -> {
                        // The correct mavsdk_server is already running on this port — reuse it.
                        System.out.println(mavsdkServerString
                                + " is already running on port " + config.SDKPort
                                + "; reusing existing instance.");
                        return true;
                    }
                    case OWNED_BY_OTHER -> {
                        // Something else is on our port — we cannot start safely.
                        System.out.println("MAVSDK_SERVER: port " + config.SDKPort
                                + " is already in use by a different process; cannot start "
                                + mavsdkServerString + ".");
                        return false;
                    }
                    case FREE -> {
                        // Port is free — fall through and launch the server normally.
                    }
                }

                process = pb.start();
                final String mStr = mavsdkServerString;

                // Wait until the MAVSDK server process is accepting connections
                Instant start = Instant.now();
                boolean connected = false;
                while (Duration.between(start, Instant.now()).getSeconds() < 10) {
                    try (Socket s = new Socket()) {
                        s.connect(new InetSocketAddress(config.SDKAddress, config.SDKPort), 200);
                        connected = true;
                        break;
                    } catch (IOException e) {
                        System.out.println("Not ready yet: " + e.getMessage()); // remove after debugging
                    }
                    Thread.sleep(200);
                }

                if (!connected) {
                    System.err.println("Timed out connecting to " + config.SDKAddress + ":" + config.SDKPort);
                }

                // Stop the process when the JVM exits
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    System.out.println("Stopping " + mStr + " ...");
                    process.destroy();
                }));

                System.out.println(mStr + " started in background.");

            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            return true;

        } else {
            System.out.println("MAVSDK_SERVER: couldn't start mavsdk_server");
            return false;
        }

    }

    /**
     * Describes who (if anyone) owns the given TCP port.
     */
    private enum PortOwnership {
        /** Port is free — no process is listening. */
        FREE,
        /** Our mavsdk_server binary is already listening on the port. */
        OWNED_BY_MAVSDK,
        /** A different process is listening on the port. */
        OWNED_BY_OTHER
    }

    /**
     * Checks whether the given TCP port is free, owned by mavsdk_server, or owned by
     * something else. No processes are killed; this is purely an observation step.
     */
    private PortOwnership checkPortOwnership( PlatformId.Platform platform,
                                              String mavsdkServerString,
                                              int sdkPort )
            throws IOException, InterruptedException {

        if ( platform.os().equals("macOS") || platform.os().equals("Linux") ) {

            // lsof -ti TCP:<port> -sTCP:LISTEN returns the PID(s) listening on that port
            Process lsof = Runtime.getRuntime().exec(
                    new String[]{ "lsof", "-ti", "TCP:" + sdkPort, "-sTCP:LISTEN" });
            String pidOutput = new String(lsof.getInputStream().readAllBytes()).trim();
            lsof.waitFor();

            if ( pidOutput.isEmpty() ) {
                return PortOwnership.FREE;
            }

            for ( String pidStr : pidOutput.split("\\s+") ) {
                int pid;
                try {
                    pid = Integer.parseInt(pidStr.trim());
                } catch ( NumberFormatException e ) {
                    continue;
                }

                // Confirm whether the PID belongs to our mavsdk_server binary
                Process ps = Runtime.getRuntime().exec(
                        new String[]{ "ps", "-p", String.valueOf(pid), "-o", "comm=" });
                String comm = new String(ps.getInputStream().readAllBytes()).trim();
                ps.waitFor();

                if ( comm.contains(mavsdkServerString) || mavsdkServerString.contains(comm) ) {
                    return PortOwnership.OWNED_BY_MAVSDK;
                }
            }

            return PortOwnership.OWNED_BY_OTHER;

        } else if ( platform.os().equals("Windows") ) {

            // netstat -ano lists proto, local addr, foreign addr, state, PID
            Process netstat = Runtime.getRuntime().exec(new String[]{ "netstat", "-ano" });
            String netstatOutput = new String(netstat.getInputStream().readAllBytes());
            netstat.waitFor();

            String portSuffix = ":" + sdkPort;
            String matchedPid = null;

            for ( String line : netstatOutput.split("\\r?\\n") ) {
                if ( line.contains(portSuffix) && line.toUpperCase().contains("LISTENING") ) {
                    String[] parts = line.trim().split("\\s+");
                    if ( parts.length >= 5 ) {
                        matchedPid = parts[parts.length - 1];
                        break;
                    }
                }
            }

            if ( matchedPid == null ) {
                return PortOwnership.FREE;
            }

            // Verify whether the PID belongs to our mavsdk_server binary
            Process tasklist = Runtime.getRuntime().exec(
                    new String[]{ "tasklist", "/FI", "PID eq " + matchedPid, "/FO", "CSV", "/NH" });
            String taskOutput = new String(tasklist.getInputStream().readAllBytes()).trim();
            tasklist.waitFor();

            if ( taskOutput.toLowerCase().contains(mavsdkServerString.toLowerCase()) ) {
                return PortOwnership.OWNED_BY_MAVSDK;
            }

            return PortOwnership.OWNED_BY_OTHER;
        }

        // Unrecognized platform — assume free and let the launch attempt proceed
        return PortOwnership.FREE;
    }

    public static Path extractExecutable(String filename) {
        try {
            String resourcePath = "natives/" + filename;

            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "mavsdkcache");
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
            }

            Path targetFile = tempDir.resolve(filename);

            if (Files.exists(targetFile)) {
                return targetFile;
            }

            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            try (InputStream in = classLoader.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    throw new IOException("Could not find resource " + resourcePath);
                }

                Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
                targetFile.toFile().setExecutable(true);
            }

            return targetFile;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
