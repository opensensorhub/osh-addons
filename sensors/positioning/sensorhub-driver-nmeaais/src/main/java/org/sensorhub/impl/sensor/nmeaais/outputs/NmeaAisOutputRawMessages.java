/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.nmeaais.outputs;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.VarRateSensorOutput;
import org.sensorhub.impl.sensor.nmeaais.NmeaAisDriver;
import org.vast.swe.SWEBuilders;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import java.util.Objects;

public class NmeaAisOutputRawMessages extends VarRateSensorOutput<NmeaAisDriver> {
    private DataRecord aisReportRecord;
    private DataEncoding dataEncoding;

    private final Object processingLock = new Object();

    public NmeaAisOutputRawMessages(NmeaAisDriver nmeaAisDriver) {
        super("nmeaAisOutputRawMessages", nmeaAisDriver, 1.);
    }

    /**
     * Initializes the data structure for the output.
     *
     * Flat index map:
     *   0 = sampleTime     1 = sentenceType    2 = fragmentCount
     *   3 = fragmentNumber  4 = sequentialId    5 = channel
     *   6 = frequency       7 = rawPayload      8 = fillBits
     *   9 = checkSum
     */
    public void doInit() {
        SWEHelper sweFactory = new SWEHelper();
        GeoPosHelper geoFac = new GeoPosHelper();

        SWEBuilders.DataRecordBuilder recordBuilder = sweFactory.createRecord()
                .name("nmeaAisOutputRawMessages")
                .label("NMEA AIS Messages")
                .description("Raw NMEA AIS message envelope for all received AIS sentences")
                .definition(SWEHelper.getPropertyUri("NmeaAisOutputRawMessages"))
                .addField("sampleTime", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("Time data was received")
                        .definition("SampleTime"))
                .addField("sentenceType", sweFactory.createText()
                        .label("Sentence Type")
                        .description("AIS VHF Data-link Message")
                        .definition(SWEHelper.getPropertyUri("SentenceType")))
                .addField("fragmentCount", sweFactory.createCount()
                        .label("Fragment Count")
                        .description("Total number of fragments in this message")
                        .definition(SWEHelper.getPropertyUri("FragmentCount")))
                .addField("fragmentNumber", sweFactory.createCount()
                        .label("Fragment Number")
                        .description("Number of fragment per message")
                        .definition(SWEHelper.getPropertyUri("FragmentNumber")))
                .addField("sequentialId", sweFactory.createText()
                        .label("Sequential Id")
                        .description("Id to link multipart messages")
                        .definition(SWEHelper.getPropertyUri("SequentialId")))
                .addField("channel", sweFactory.createText()
                        .label("AIS Channel")
                        .description("AIS channel used: A (161.975 MHz) or B (162.025 MHz)")
                        .definition(SWEHelper.getPropertyUri("Channel")))
                .addField("frequency", sweFactory.createQuantity()
                        .label("AIS Channel Frequency")
                        .description("AIS channel frequency in MHz")
                        .uom("MHz")
                        .definition(SWEHelper.getPropertyUri("Frequency")))
                .addField("rawPayload", sweFactory.createText()
                        .label("Raw Payload")
                        .description("Encoded AIS payload")
                        .definition(SWEHelper.getPropertyUri("RawPayload")))
                .addField("fillBits", sweFactory.createCount()
                        .label("Fill Bits")
                        .description("Padding bits added to final payload")
                        .definition(SWEHelper.getPropertyUri("FillBits")))
                .addField("checkSum", sweFactory.createText()
                        .label("Check Sum")
                        .description("NMEA checksum (hex)")
                        .definition(SWEHelper.getPropertyUri("CheckSum")));

        aisReportRecord = recordBuilder.build();

        dataEncoding = geoFac.newTextEncoding(",", "\n");
    }

    public void setData(String nmeaAisMsg) {
        synchronized (processingLock) {
            String[] nmea = nmeaAisMsg.split(",");
            String sentenceType = nmea[0];
            int fragmentCount   = Integer.parseInt(nmea[1]);
            int fragmentNumber  = Integer.parseInt(nmea[2]);
            String sequentialId = nmea[3];
            String channel      = nmea[4];
            double channelFreq  = Objects.equals(channel, "A") ? 161.975 : 162.025;
            String rawPayload   = nmea[5];
            String[] lastField  = nmea[6].split("\\*");
            int fillBits        = Integer.parseInt(lastField[0].trim());
            String checkSum     = lastField[1].trim();

            DataBlock dataBlock = latestRecord == null ? aisReportRecord.createDataBlock() : latestRecord.renew();
            dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000d);
            dataBlock.setStringValue(1, sentenceType);
            dataBlock.setIntValue(2, fragmentCount);
            dataBlock.setIntValue(3, fragmentNumber);
            dataBlock.setStringValue(4, sequentialId);
            dataBlock.setStringValue(5, channel);
            dataBlock.setDoubleValue(6, channelFreq);
            dataBlock.setStringValue(7, rawPayload);
            dataBlock.setIntValue(8, fillBits);
            dataBlock.setStringValue(9, checkSum);

            latestRecord = dataBlock;
            latestRecordTime = System.currentTimeMillis();
            updateSamplingPeriod(latestRecordTime);
            eventHandler.publish(new DataEvent(latestRecordTime, NmeaAisOutputRawMessages.this, dataBlock));
        }
    }

    @Override
    public DataComponent getRecordDescription() {
        return aisReportRecord;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }
}
