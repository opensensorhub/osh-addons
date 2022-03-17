package org.sensorhub.impl.sensor.uas.common;

import org.sensorhub.api.data.IDataProducer;

/**
 * Common interface implemented by the two sensors in this package. Used in code that needs to work with both.
 */
public interface ITimeSynchronizedUasDataProducer extends IDataProducer {
	public void setStreamSyncTime(SyncTime syncTime);
	public SyncTime getSyncTime();
	public String getUasFoiUID();
	public String getImagedFoiUID();
}
