package org.sensorhub.impl.sensor.rtmp.config;

import org.sensorhub.api.config.DisplayInfo;

public class ConnectionConfig {

    /*
    @DisplayInfo.Required
    @DisplayInfo(label = "Generate Random Stream Key", desc = "Enable to generate and append a random hex string to the path. " +
            "Recommended for security. Only enable on first init, otherwise path will include multiple keys. ")
    public boolean generateRandomStreamKey = true;

     */

    @DisplayInfo.Required
    @DisplayInfo(label = "Host", desc = "Domain listening for an RTMP connection request. Unspecified should work " +
    "for most cases.")
    public HostType host = HostType.UNSPECIFIED;

    @DisplayInfo.Required
    @DisplayInfo(label = "Port", desc = "Port listening for an RTMP connection request.")
    @DisplayInfo.ValueRange(min = 1, max = 65535)
    public int port = 1935;

    /*
    @DisplayInfo(label = "Path", desc = "(Optional) Path to listen for an RTMP connection request. I.e. everything in the URL " +
    "after the port.")
    public String path = "";

     */
}