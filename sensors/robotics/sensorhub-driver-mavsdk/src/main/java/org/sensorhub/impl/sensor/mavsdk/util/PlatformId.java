package org.sensorhub.impl.sensor.mavsdk.util;

public final class PlatformId {

    public record Platform(String os, String arch) {}

    public static Platform get() {
        String osStringLower = System.getProperty("os.name").toLowerCase();

        String os = osStringLower.contains("mac") ? "macOS"
                : osStringLower.contains("win") ? "Windows"
                : (osStringLower.contains("nux") || osStringLower.contains("nix")) ? "Linux"
                : "UnknownOS";

        String archStringLower = System.getProperty("os.arch").toLowerCase();

        String arch = archStringLower.contains("aarch64") || archStringLower.contains("arm64") ? "ARM64"
                : (archStringLower.contains("x86_64") || archStringLower.contains("amd64")) ? "x86_64"
                : "UnknownArch";

        return new Platform(os, arch);
    }
}
