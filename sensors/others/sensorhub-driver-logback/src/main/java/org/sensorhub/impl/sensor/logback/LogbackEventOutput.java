/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research, LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.VarRateSensorOutput;
import org.vast.data.TextEncodingImpl;
import org.vast.swe.SWEHelper;

public class LogbackEventOutput extends VarRateSensorOutput<LogbackDriver> {

    public static final String NAME = "loggingEventOutput";
    public static final String LABEL = "Logging Event Output";
    public static final String DESCRIPTION = "Logging event from logback appender";

    private final DataComponent recordDescription;
    private final DataEncoding recordEncoding;

    protected LogbackEventOutput(LogbackDriver parent) {
        super(NAME, parent, 1.0f);

        var fac = new SWEHelper();

        recordDescription = fac.createRecord()
                .name(NAME)
                .label(LABEL)
                .description(DESCRIPTION)
                .addField("timestamp", fac.createTime()
                        .asSamplingTimeIsoUTC())
                .addField("level", fac.createText()
                        .definition(SWEHelper.getPropertyUri("LogbackLevel"))
                        .addAllowedValues(LogbackDriverConfig.LogbackLevel.class))
                .addField("threadName", fac.createText()
                        .definition(SWEHelper.getPropertyUri("ThreadName")))
                .addField("loggerName", fac.createText()
                        .definition(SWEHelper.getPropertyUri("LoggerName")))
                .addField("message", fac.createText()
                        .definition(SWEHelper.getPropertyUri("LogMessage")))
                .addField("stackTrace", fac.createText()
                        .definition(SWEHelper.getPropertyUri("StackTrace")))
                .build();

        recordEncoding = new TextEncodingImpl();
    }

    public void publishObservation(ILoggingEvent event) {
        DataBlock dataBlock = (latestRecord == null) ? recordDescription.createDataBlock() : latestRecord.renew();
        int i = 0;
        dataBlock.setDoubleValue(i++, event.getTimeStamp() / 1000.0);
        dataBlock.setStringValue(i++, event.getLevel().toString());
        dataBlock.setStringValue(i++, event.getThreadName());
        dataBlock.setStringValue(i++, event.getLoggerName());
        dataBlock.setStringValue(i++, event.getFormattedMessage());

        // Get full stack trace if available
        String stackTrace = "";
        try {
            IThrowableProxy throwableProxy = event.getThrowableProxy();
            if (throwableProxy != null) {
                stackTrace = ThrowableProxyUtil.asString(throwableProxy);
            }
        } catch (Exception e) {
            stackTrace = "Error getting stack trace: " + e.getMessage();
        }
        dataBlock.setStringValue(i, stackTrace);

        latestRecord = dataBlock;
        latestRecordTime = event.getTimeStamp();
        updateSamplingPeriod(latestRecordTime);
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));
    }

    @Override
    public DataComponent getRecordDescription() {
        return recordDescription;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return recordEncoding;
    }

}
