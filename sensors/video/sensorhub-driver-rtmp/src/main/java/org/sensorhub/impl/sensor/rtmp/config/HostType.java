package org.sensorhub.impl.sensor.rtmp.config;

public enum HostType {
    UNSPECIFIED("0.0.0.0"),
    LOCALHOST("localhost"),
    DOCKER_INTERNAL("host.docker.internal")/*,
    OVERRIDE("")*/;

    public final String host;

    HostType(String host) {
        this.host = host;
    }
}
