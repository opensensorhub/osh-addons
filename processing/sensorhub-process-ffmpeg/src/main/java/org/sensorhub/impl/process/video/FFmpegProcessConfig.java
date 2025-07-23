package org.sensorhub.impl.process.video;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.processing.ProcessConfig;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.IProcessExec;

public abstract class FFmpegProcessConfig extends ProcessConfig {

    @DisplayInfo.Required
    @DisplayInfo(label = "Video Process ID", desc = "Serial number or unique identifier for video processor.")
    public String processId = "video_process001";

    @DisplayInfo.Required
    @DisplayInfo.FieldType(DisplayInfo.FieldType.Type.MODULE_ID)
    @DisplayInfo(label="Video Source")
    public String videoUID;

    // SET THIS TO THE CORRESPONDING EXECUTABLE PROCESS IN ANY SUB CLASSES
    protected Class<? extends IProcessExec> execProcess = null;

    public FFmpegProcessConfig() {}

}
