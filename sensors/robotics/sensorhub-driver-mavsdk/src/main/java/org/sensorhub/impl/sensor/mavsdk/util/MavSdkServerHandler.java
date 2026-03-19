package org.sensorhub.impl.comm.mavsdk.util;

import org.sensorhub.impl.comm.mavsdk.UnmannedConfig;

import java.io.IOException;
import java.io.InputStream;
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
      //mavsdk_server_macos_arm64 tcpin://127.0.0.1:50051 serial:///dev/tty.usbserial-0001:57600
      if ( ! "".equals(mavsdkServerString)) {

          Path exePath = extractExecutable(mavsdkServerString);

          /**
           * Each configuration (WIFI, SIM, SERIAL, EMPTY) will kickstart
           * the mavsdk_server binary with different parameter options. Some will use the UDP address/port
           * some will use the serial interface.
           */
          ProcessBuilder pb = switch( config.connectionType ) {
              case WIFI, SIM -> new ProcessBuilder(
                      exePath.toString(),   // program
                      "tcpin://" + config.SDKAddress + ":" + String.valueOf(config.SDKPort),
                      "udpin://" + config.UDPListenAddr + ":" + String.valueOf(config.UDPListenPort));
              case SERIAL -> new ProcessBuilder(
                      exePath.toString(),   // program
                      "tcpin://" + config.SDKAddress + ":" + String.valueOf(config.SDKPort),
                      config.SerialPort);
              case EMPTY ->  new ProcessBuilder(exePath.toString());
          };

          pb.inheritIO(); // forward output

          try {

              //first kill any pre-existing mavsdk_server processes around
              try {
                  if ( platform.os().equals("macOS") || platform.os().equals("Linux") ) {
                      Process p = Runtime.getRuntime().exec("pkill -f " + mavsdkServerString);
                      p.waitFor();
                  } else if ( platform.os().equals("Windows") ) {
                      Process p = Runtime.getRuntime()
                              .exec("taskkill /IM " + mavsdkServerString + " /F");
                      p.waitFor();
                  }
              } catch (InterruptedException e) {
                  throw new RuntimeException(e);
              }

              process = pb.start();
              final String mStr = mavsdkServerString;

              // Wait until the MAVSDK server process is accepting connections
              Instant start = Instant.now();
              boolean connected = false;
              while (Duration.between(start, Instant.now()).getSeconds() < 10) { // 10s timeout
                  try (Socket s = new Socket(config.SDKAddress, config.SDKPort)) {
                      connected = true;
                      break;
                  } catch (IOException ignored) {}
                  Thread.sleep(200); // retry every 200ms
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
