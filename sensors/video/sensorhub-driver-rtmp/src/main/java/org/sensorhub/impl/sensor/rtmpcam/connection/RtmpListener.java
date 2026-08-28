package org.sensorhub.impl.sensor.rtmpcam.connection;

import org.sensorhub.impl.sensor.ffmpeg.outputs.AudioOutput;
import org.sensorhub.impl.sensor.ffmpeg.outputs.VideoOutput;
import org.sensorhub.impl.sensor.rtmpcam.config.ConnectionConfig;
import org.sensorhub.impl.sensor.rtmpcam.event.RtmpConnectEvent;
import org.sensorhub.impl.sensor.rtmpcam.event.RtmpDisconnectEvent;
import org.sensorhub.impl.sensor.rtmpcam.event.RtmpReconnectEvent;
import org.sensorhub.impl.sensor.rtmpcam.event.RtmpStreamEvent;

/**
 * Extend this class and register it with {@link RtmpListenerManager} to
 * receive encoded packets from a matched RTMP stream.
 */
public interface RtmpListener {

    public ConnectionConfig config();

    /** Called once after negotiation succeeds and this listener is selected. */
    public void onConnected(RtmpConnectEvent event);

    /**
     * Called once when a new stream is connected. Implementations MUST set up the video and audio outputs here.
     * @param event Event containing the stream info.
     */
    public void onStreamConnected(RtmpStreamEvent event);

    /** Called once when the client disconnects or the pipeline faults. */
    public void onDisconnected(RtmpDisconnectEvent event);

    public void onReconnected(RtmpReconnectEvent event);

    public VideoOutput<?> getVideoOutput();

    public AudioOutput<?> getAudioOutput();

    public boolean doStreamProcessing();
}