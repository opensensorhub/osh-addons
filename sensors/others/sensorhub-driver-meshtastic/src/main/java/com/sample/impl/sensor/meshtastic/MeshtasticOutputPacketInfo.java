/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.sample.impl.sensor.meshtastic;

import com.geeksville.mesh.MeshProtos;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import java.time.Instant;
import java.util.ArrayList;

/**
 * Output specification and provider for {@link MeshtasticSensor}.
 */
public class MeshtasticOutputPacketInfo extends AbstractSensorOutput<MeshtasticSensor> {
    private static final int MAX_NUM_TIMING_SAMPLES = 10;

    private final ArrayList<Double> intervalHistogram = new ArrayList<>(MAX_NUM_TIMING_SAMPLES);
    private final Object histogramLock = new Object();
    private final Object processingLock = new Object();
    public DataRecord packetRecord;
    public DataEncoding dataEncoding;

    public int packetRecordSize;

    // Packet Variables
    String packetId;
    String packetTo;
    String packetFrom;
    Instant packetTime;
    boolean packetHasData;
    String packetPortnum = "None";
    int packetPortNumValue = 0;
    int packetRxRssi;
    int packetHopLimit;
    int packethopStart;
    String packetRelayNode;

    /**
     * Creates a new output for the sensor driver.
     *
     * @param parentMeshtasticSensor Sensor driver providing this output.
     */
    MeshtasticOutputPacketInfo(MeshtasticSensor parentMeshtasticSensor, String packetName) {
        super(packetName, parentMeshtasticSensor);
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering, and data types.
     */
    void doInit(String packetName, String packetLabel, String packetDescrition) {
        // Get an instance of SWE Factory suitable to build components
        GeoPosHelper geoFac = new GeoPosHelper();
        SWEHelper sweFactory = new SWEHelper();
        // Create the data record description

        packetRecord = geoFac.createRecord()
                .name(packetName)
                .label(packetLabel)
                .description(packetDescrition)
                .addField("sampleTime", geoFac.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("Time of data collection")
                        .definition("SampleTime")
                )
                .addField("packetID", sweFactory.createText()
                        .label("ID")
                        .description("the id of a meshtastic packet")
                        .definition(SWEHelper.getPropertyUri("PacketID")))
                .addField("packetTo", sweFactory.createText()
                        .label("To")
                        .description("node meshtastic packet is being sent to")
                        .definition(SWEHelper.getPropertyUri("PacketTo")))
                .addField("packetFrom", sweFactory.createText()
                        .label("From")
                        .description("node meshtastic packet is being sent from")
                        .definition(SWEHelper.getPropertyUri("PacketFrom")))
                .addField("packetTime", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Packet Rx Time")
                        .description("the time the packet was sent from the meshtastic node")
                        .definition(SWEHelper.getPropertyUri("PacketTime")))
                .addField("packetHasData", sweFactory.createBoolean()
                        .label("Does Packet have data")
                        .description("Does the packet provided actually contain data")
                        .definition(SWEHelper.getPropertyUri("PacketHasData")))
                .addField("portnum", sweFactory.createText()
                        .label("Portnum")
                        .description("What is the PortNum of the packet")
                        .definition(SWEHelper.getPropertyUri("Portnum")))
                .addField("portnumVal", sweFactory.createQuantity()
                        .label("Portnum Value")
                        .description("The value of the portnum provided")
                        .definition(SWEHelper.getPropertyUri("PortnumVal")))
                .addField("packetRxRssi", sweFactory.createQuantity()
                        .label("Rx RSSI")
                        .description("Received Signal Strength Indicator")
                        .definition(SWEHelper.getPropertyUri("PacketRxRssi")))
                .addField("packetHopLimit", sweFactory.createQuantity()
                        .label("Hop Limit")
                        .description("Hop limit of a meshtastic packet")
                        .definition(SWEHelper.getPropertyUri("PacketHopLimit")))
                .addField("packetHopStart", sweFactory.createQuantity()
                        .label("Hop Start")
                        .description("Hop Start of a meshtastic packet")
                        .definition(SWEHelper.getPropertyUri("PacketHopStart")))
                .addField("packetRelayNode", sweFactory.createText()
                        .label("Relay Node")
                        .description("What is the RelayNode of the packet")
                        .definition(SWEHelper.getPropertyUri("PacketRelayNode")))
                .build();

        packetRecordSize = packetRecord.getNumFields()-1;

        dataEncoding = geoFac.newTextEncoding(",", "\n");
    }


    @Override
    public DataComponent getRecordDescription() {
        return packetRecord;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {
        synchronized (histogramLock) {
            double sum = 0;
            for (double sample : intervalHistogram)
                sum += sample;

            return sum / intervalHistogram.size();
        }
    }

    public void setPacketData(MeshProtos.MeshPacket packetData){
        packetId = convert32IntToString(packetData.getId());
        packetTo = convert32IntToString(packetData.getTo());
        packetFrom = convert32IntToString(packetData.getFrom());
        packetTime = convert32IntToInstant(packetData.getRxTime());
        packetHasData = packetData.hasDecoded();
        if(packetHasData){
            packetPortnum = packetData.getDecoded().getPortnum().name();
            packetPortNumValue = packetData.getDecoded().getPortnumValue();
        }
        packetRxRssi = packetData.getRxRssi();
        packetHopLimit = packetData.getHopLimit();
        packethopStart = packetData.getHopStart();
        packetRelayNode = convert32IntToString(packetData.getRelayNode());
    }

    public void populatePacketDataStructure(DataBlock dataBlock){
        // Populate Parent Class Packet Data
        dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000d);
        dataBlock.setStringValue(1, packetId);
        dataBlock.setStringValue(2, packetTo);
        dataBlock.setStringValue(3, packetFrom);
        dataBlock.setTimeStamp(4, packetTime);
        dataBlock.setBooleanValue(5, packetHasData);
        dataBlock.setStringValue(6, packetPortnum);
        dataBlock.setIntValue(7, packetPortNumValue);
        dataBlock.setDoubleValue(8, packetRxRssi);
        dataBlock.setDoubleValue(9, packetHopLimit);
        dataBlock.setDoubleValue(10, packethopStart);
        dataBlock.setStringValue(11, packetRelayNode);
    }


    public String convert32IntToString(int data){
        long unsigned_num = Integer.toUnsignedLong(data);
        return String.format("!%08x", unsigned_num);
    }
    public Instant convert32IntToInstant(int data){
        long unsigned_num = Integer.toUnsignedLong(data);
        return Instant.ofEpochSecond(unsigned_num);
    }
}
