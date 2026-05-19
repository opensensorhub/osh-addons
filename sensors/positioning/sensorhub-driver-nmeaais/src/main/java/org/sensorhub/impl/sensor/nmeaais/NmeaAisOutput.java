package org.sensorhub.impl.sensor.nmeaais;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.impl.sensor.VarRateSensorOutput;
import org.vast.swe.SWEBuilders;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import java.util.Objects;

public class NmeaAisOutput extends VarRateSensorOutput<NmeaAisDriver> {
    private static final double INITIAL_SAMPLING_PERIOD = 1.0;

    protected DataRecord aisRecord;
    protected DataEncoding dataEncoding;
    protected int aisRecordSize;

    // AIS Message Variables
    String sentenceType;
    int fragmentTotal;
    int fragmentNumber;
    String sequentialId;
    String channel;
    double channelFreq;
    String rawPayload;
    int fillBits;
    String checkSum;

    NmeaAisOutput(String outputName, NmeaAisDriver nmeaAisDriver) {
        super(outputName, nmeaAisDriver, INITIAL_SAMPLING_PERIOD);
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering, and data types.
     */
    void doInit(String outputName, String outputLabel, String outputDescription, String outputDefinition) {
        // Get an instance of SWE Factory suitable to build components
        GeoPosHelper geoFac = new GeoPosHelper();
        SWEHelper sweFactory = new SWEHelper();
        // Create the data record description

        // Create the data record description
        SWEBuilders.DataRecordBuilder recordBuilder = sweFactory.createRecord()
                .name(outputName)
                .label(outputLabel)
                .description(outputDescription)
                .definition(outputDefinition)
                .addField("nmeaAisMsg", sweFactory.createRecord()
                        .label("NMEA AIS Message Info")
                        .description("Data Record for the general NMEA AIS message")
                        .definition(SWEHelper.getPropertyUri("NmeaAisMsg"))
                        .addField("sampleTime", geoFac.createTime()
                                .asSamplingTimeIsoUTC()
                                .label("Sample Time")
                                .description("Time of data collection")
                                .definition("SampleTime"))
                        .addField("sentenceType", sweFactory.createText()
                                .label("Sentence Type")
                                .description("AIS VHF Data-link Message")
                                .definition(SWEHelper.getPropertyUri("SentenceType")))
                        .addField("fragmentCount", sweFactory.createQuantity()
                                .label("Fragment Count")
                                .description("Total number of fragments in this message")
                                .definition(SWEHelper.getPropertyUri("FragmentCount")))
                        .addField("fragmentNumber", sweFactory.createQuantity()
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
                        .addField("rawPayload", sweFactory.createText()
                                .label("Raw PayLoad")
                                .description("Encoded AIS payload")
                                .definition(SWEHelper.getPropertyUri("RawPayload")))
                        .addField("fillBits", sweFactory.createQuantity()
                                .label("Fill Bits")
                                .description("Padding bits added to final payload")
                                .definition(SWEHelper.getPropertyUri("FillBits")))
                        .addField("checkSum", sweFactory.createText()
                                .label("Check Sum")
                                .description("NMEA checksum (hex)")
                                .definition(SWEHelper.getPropertyUri("CheckSum")))
                );

        aisRecord = recordBuilder.build();

        aisRecordSize = aisRecord.getNumFields()-1;

        dataEncoding = geoFac.newTextEncoding(",", "\n");
    }


    @Override
    public DataComponent getRecordDescription() {
        return aisRecord;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }


    public void setAisMsgData(String nmeaAisMsg){
        // Step 1: Parse NMEA fields of NMEA AIS Sentence (ex. nmea = !AIVDM,1,1,,A,15Muan<000qm2=2CavBWSCL20@2?,0*6A)
        String[] nmea = nmeaAisMsg.split(",");
        sentenceType = nmea[0];
        fragmentTotal = Integer.parseInt(nmea[1]);
        fragmentNumber = Integer.parseInt(nmea[2]);
        sequentialId = nmea[3];
        channel = nmea[4];
        channelFreq = Objects.equals(channel, "A") ? 161.975 :162.025;
        rawPayload  = nmea[5];

        // Split up last field to get checksum and fill bits
        String[] lastField = nmea[6].split("\\*"); // last field of a nmea sentence is a [fill bits and checksum]
        fillBits = Integer.parseInt(lastField[0].trim());
        checkSum = lastField[1].trim();
    }

    public void populateNmeaAisDataStructure(DataBlock dataBlock){
        // Populate Parent Class Packet Data
        dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000d);
        dataBlock.setStringValue(1, sentenceType);
        dataBlock.setIntValue(2, fragmentTotal);
        dataBlock.setIntValue(3, fragmentNumber);
        dataBlock.setStringValue(4, sequentialId);
        dataBlock.setStringValue(5, channel);
        dataBlock.setStringValue(6, rawPayload);
        dataBlock.setIntValue(7, fillBits);
        dataBlock.setStringValue(8, checkSum);
    }

}
