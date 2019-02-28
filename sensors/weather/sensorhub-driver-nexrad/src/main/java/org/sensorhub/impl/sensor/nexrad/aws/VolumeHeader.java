package org.sensorhub.impl.sensor.nexrad.aws;

import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * <p>Title: LdmLevel2Header.java</p>
 * <p>Description: </p>
 *
 * @author T
 * @date Mar 9, 2016
 * 
 * 
 */
public class VolumeHeader {
	String archive2filename;
	int daysSince1970;
	int msSinceMidnight;
	String siteId;
	Long timeMs;

	public String getArchive2filename() {
		return archive2filename;
	}
	public void setArchive2filename(String archive2filename) {
		this.archive2filename = archive2filename;
	}
	public String getSiteId() {
		return siteId;
	}
	public void setSiteId(String siteId) {
		this.siteId = siteId;
	}
	public Long getTimeMs() {
		if(timeMs == null) {
			timeMs = TimeUnit.DAYS.toMillis(daysSince1970 - 1) + msSinceMidnight;
		}
		return timeMs;
	}

	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append(archive2filename + "\n");
		b.append(daysSince1970 + "\n");
		b.append(msSinceMidnight + "\n");
		b.append(getTimeMs() + "\n");
		DateTime dt = new DateTime(getTimeMs()).withZone(DateTimeZone.UTC);
		DateTimeFormatter fmt = ISODateTimeFormat.dateTimeNoMillis();
		b.append(fmt.print(dt) + "\n");
		b.append(siteId);
		return b.toString();
	}
}
