package org.sensorhub.impl.sensor.notecardGPS.config;
import org.sensorhub.api.config.DisplayInfo;


/**
 Configuration settings for the i2c Connection.
 Specifically, allows for selection of outputs to provide
 @author Bill Brown
 @since June 9, 2025
 */

public class noteCardConfig {
    @DisplayInfo.Required
    @DisplayInfo(label="NoteHub Product UID", desc="Provide the Product UID associated with your NoteHub.io Project")
    public String NHproductUID = "com.botts-inc.bill.brown:gps";

    @DisplayInfo.Required
    @DisplayInfo(label="GPS Sample Rate (seconds)", desc="Provide the sample rate at which the GPS is expected to gather readings")
    public int gpsSampleRate = 10;

    @DisplayInfo(label = "Sync with Notehub.io", desc="Do you want data from this notecard to sync with NoteHub.io?")
    public boolean isNoteHubSync = true;

    @DisplayInfo(label = "Reset sensor upon initialization", desc="By checking this box, the sensor will reset upon initialization. " +
            "The sensor will temporarily lose connection but module will reestablish connection")
    public boolean isResetSensor = true;
}
