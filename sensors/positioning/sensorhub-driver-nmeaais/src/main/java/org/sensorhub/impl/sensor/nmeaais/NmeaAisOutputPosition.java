package org.sensorhub.impl.sensor.nmeaais;

import net.opengis.swe.v20.DataBlock;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

public class NmeaAisOutputPosition extends NmeaAisOutput implements NmeaAisOutputInterface {
    private static final String OUTPUT_NAME = "nmeaAisOutputPosition";
    private static final String OUTPUT_LABEL = "Position Report";
    private static final String OUTPUT_DESCRIPTION = "Class A AIS Position Report";
    private static final String OUTPUT_DEFINITION = SWEHelper.getPropertyUri("NmeaAisOutputPosition");

    private final Object processingLock = new Object();

    NmeaAisOutputPosition(NmeaAisDriver nmeaAisDriver) {
        super(OUTPUT_NAME, nmeaAisDriver);
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering, and data types.
     */
    void doInit() {
        // INITIALIZE THE PACKET PARENT CLASS
        super.doInit(OUTPUT_NAME, OUTPUT_LABEL, OUTPUT_DESCRIPTION, OUTPUT_DEFINITION);

        // Get an instance of SWE Factory suitable to build components
        GeoPosHelper geoFac = new GeoPosHelper();
        SWEHelper sweFactory = new SWEHelper();

        aisRecord.addField("payloadInfo", sweFactory.createRecord()
                .label("Decoded Payload")
                .description("Decoded Payload")
                .definition(SWEHelper.getPropertyUri("Payload"))
                .addField("messageId", sweFactory.createQuantity()
                        .label("Message Id")
                        .description("Identifier for this message 1, 2 or 3")
                        .definition(SWEHelper.getPropertyUri("MessageId")))
                .addField("repeat", sweFactory.createQuantity()
                        .label("Repeat Indicator")
                        .description("Used by the repeater to indicate how many times a message has been repeated. See Section 4.6.1, Annex 2; 0-3; 0 = default; 3 = do not repeat any more.")
                        .definition(SWEHelper.getPropertyUri("repeat")))
                .addField("mmsi", sweFactory.createText()
                        .label("MMSI Number")
                        .description("MMSI Number")
                        .definition(SWEHelper.getPropertyUri("Mmsi")))
                .addField("navStatus", sweFactory.createQuantity()
                        .label("Navigational Status")
                        .description(
                                "0 = under way using engine, 1 = at anchor, 2 = not under command, 3 = restricted maneuverability, 4 = constrained by her draught, 5 = moored, 6 = aground, 7 = engaged in fishing, 8 = under way sailing, 9 = reserved for future amendment of navigational status for ships carrying DG, HS, or MP, or IMO hazard or pollutant category C, high speed craft (HSC), 10 = reserved for future amendment of navigational status for ships carrying dangerous goods (DG), harmful substances (HS) or marine pollutants (MP), or IMO hazard or pollutant category A, wing in ground (WIG); 11 = power-driven vessel towing astern (regional use); 12 = power-driven vessel pushing ahead or towing alongside (regional use);\n" +
                                "13 = reserved for future use,\n" +
                                "14 = AIS-SART (active), MOB-AIS, EPIRB-AIS\n" +
                                "15 = undefined = default (also used by AIS-SART, MOB-AIS and EPIRB-AIS under test)"
                        )
                        .definition(SWEHelper.getPropertyUri("NavStatus")))
                .addField("rot", sweFactory.createQuantity()
                        .label("Rate of Turn")
                        .description(
                                "0 to +126 = turning right at up to 708 deg per min or higher\n" +
                                "0 to -126 = turning left at up to 708 deg per min or higher Values between 0 and 708 deg per min coded by ROTAIS = 4.733 SQRT(ROTsensor) degrees per min\n" +
                                "where ROTsensor is the Rate of Turn as input by an external Rate of Turn Indicator (TI). ROTAIS is rounded to the nearest integer value.\n" +
                                "+127 = turning right at more than 5 deg per 30 s (No TI available)\n" +
                                "-127 = turning left at more than 5 deg per 30 s (No TI available)\n" +
                                "-128 (80 hex) indicates no turn information available (default).\n" +
                                "ROT data should not be derived from COG information."
                        )
                        .definition(SWEHelper.getPropertyUri("Rot")))
                .addField("positionAccuracy", sweFactory.createQuantity()
                        .label("Position Accuracy")
                        .description(
                                "1 = high (<= 10 m)\n" +
                                "0 = low (> 10 m)\n" +
                                "0 = default"
                        )
                        .definition(SWEHelper.getPropertyUri("PositionAccuracy")))
                .addField("location",geoFac.createLocationVectorLatLon()
                        .label("Location"))
                .addField("cog",sweFactory.createQuantity()
                        .label("COG")
                        .description("Course over ground in 1/10 = (0-3599). 3600 (E10h) = not available = default. 3 601-4 095 should not be used")
                        .definition(SWEHelper.getPropertyUri("Cog")))
                .addField("heading",sweFactory.createQuantity()
                        .label("True Heading")
                        .uom("deg")
                        .description("Degrees (0-359) (511 indicates not available = default)")
                        .definition(SWEHelper.getPropertyUri("heading")))
                .addField("timeStamp",sweFactory.createTime()
                        .label("Time Stamp")
                        .description("UTC second when the report was generated by the electronic position system (EPFS) (0-59, or 60 if time stamp is not available, which should also be the default value, or 61 if positioning system is in manual input mode, or 62 if electronic position fixing system operates in estimated (dead reckoning) mode, or 63 if the positioning system is inoperative)")
                        .definition(SWEHelper.getPropertyUri("TimeStamp")))
                .addField("smi",sweFactory.createQuantity()
                        .label("Special Maneuvre Indicator")
                        .description("0 = not available = default\n" +
                                "1 = not engaged in special maneuver\n" +
                                "2 = engaged in special maneuver\n" +
                                "(i.e.: regional passing arrangement on Inland Waterway)")
                        .definition(SWEHelper.getPropertyUri("Smi")))
                .addField("raim",sweFactory.createQuantity()
                        .label("RAIM-flag")
                        .description("Receiver autonomous integrity monitoring (RAIM) flag of electronic position fixing device; 0 = RAIM not in use = default; 1 = RAIM in use. See Table")
                        .definition(SWEHelper.getPropertyUri("Raim")))
                .addField("commState",sweFactory.createQuantity()
                        .label("Communication State")
                        .description("visit https://www.navcen.uscg.gov/ais-class-a-reports#CommState")
                        .definition(SWEHelper.getPropertyUri("CommState")))
                .addField("bits",sweFactory.createQuantity()
                        .label("Number of Bits")
                        .description("Number of Bits")
                        .definition(SWEHelper.getPropertyUri("bits")))
                .build()
        );

        dataEncoding = geoFac.newTextEncoding(",", "\n");
    }

    /**
     * Sets the data for the output and publishes it.
     */
    @Override
    public void setData(String nmeaAisMsg, String[] positionData) {
        synchronized (processingLock) {
            // Set PacketInfo in Parent Class
            setAisMsgData(nmeaAisMsg);

//            try {
                DataBlock dataBlock = latestRecord == null ? aisRecord.createDataBlock() : latestRecord.renew();

                // POPULATE PACKET FIELDS
                populateNmeaAisDataStructure(dataBlock);

//                // POPULATE POSITION FIELDS
//                dataBlock.setDoubleValue(aisRecordSize + 1, lon);
//                dataBlock.setDoubleValue(aisRecordSize + 2, lat);
//                dataBlock.setDoubleValue(aisRecordSize + 3, alt);
//
//                // CREATE FOI UID
//                String foiUID = parentSensor.addFoi(packetFrom);
//                // Publish the data block
//                latestRecord = dataBlock;
//                latestRecordTime = System.currentTimeMillis();
//                updateSamplingPeriod(latestRecordTime);
//                eventHandler.publish(new DataEvent(latestRecordTime, MeshtasticOutputPosition.this, foiUID, dataBlock));


//            } catch (InvalidProtocolBufferException e) {
//                parentSensor.getLogger().error("Failed to parse Position payload: {}", e.getMessage());
//            }

        }
    }

}
