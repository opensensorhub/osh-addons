/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.sample.impl.sensor.MeshtasticMQTT;

import com.geeksville.mesh.MeshProtos;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
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

    public int packet_record_size;

    // Packet Variables
    String packet_id;
    String packet_to;
    String packet_from;
    Instant packet_time;
    boolean packet_hasData;
    String packet_portnum = "None";
    int packet_portnum_value = 0;
    int packet_RxRssi;
    int packet_HopLimit;
    int packet_hopStart;
    String packet_RelayNode;

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
                        .description("Time of data collection"))
                .addField("packet_id", sweFactory.createText()
                        .label("ID")
                        .description("the id of a meshtastic packet")
                        .definition(SWEHelper.getPropertyUri("packet_id")))
                .addField("packet_to", sweFactory.createText()
                        .label("To")
                        .description("node meshtastic packet is being sent to")
                        .definition(SWEHelper.getPropertyUri("packet_to")))
                .addField("packet_from", sweFactory.createText()
                        .label("From")
                        .description("node meshtastic packet is being sent from")
                        .definition(SWEHelper.getPropertyUri("packet_from")))
                .addField("packet_time", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Packet Rx Time")
                        .description("the time the packet was sent from the meshtastic node")
                        .definition(SWEHelper.getPropertyUri("packet_time")))
                .addField("packet_hasData", sweFactory.createBoolean()
                        .label("Does Packet have data")
                        .description("Does the packet provided actually contain data")
                        .definition(SWEHelper.getPropertyUri("packet_hasData")))
                .addField("portnum", sweFactory.createText()
                        .label("Portnum")
                        .description("What is the PortNum of the packet")
                        .definition(SWEHelper.getPropertyUri("portnum")))
                .addField("portnum_val", sweFactory.createQuantity()
                        .label("Portnum Value")
                        .description("The value of the portnum provided")
                        .definition(SWEHelper.getPropertyUri("portnum_val")))
                .addField("packet_RxRssi", sweFactory.createQuantity()
                        .label("Rx RSSI")
                        .description("Received Signal Strength Indicator")
                        .definition(SWEHelper.getPropertyUri("packet_RxRssi")))
                .addField("packet_HopLimit", sweFactory.createQuantity()
                        .label("Hop Limit")
                        .description("Hop limit of a meshtastic packet")
                        .definition(SWEHelper.getPropertyUri("packet_HopLimit")))
                .addField("packet_hopStart", sweFactory.createQuantity()
                        .label("Hop Start")
                        .description("Hop Start of a meshtastic packet")
                        .definition(SWEHelper.getPropertyUri("packet_hopStart")))
                .addField("packet_RelayNode", sweFactory.createText()
                        .label("Relay Node")
                        .description("What is the RelayNode of the packet")
                        .definition(SWEHelper.getPropertyUri("packet_RelayNode")))
                .build();

        packet_record_size = packetRecord.getNumFields()-1;

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

    public void setPacketData(MeshProtos.MeshPacket packet_data){
        packet_id = convert_32int_to_string(packet_data.getId());
        packet_to = convert_32int_to_string(packet_data.getTo());
        packet_from = convert_32int_to_string(packet_data.getFrom());
        packet_time = convert_32int_to_Instant(packet_data.getRxTime());
        packet_hasData = packet_data.hasDecoded();
        if(packet_hasData){
            packet_portnum = packet_data.getDecoded().getPortnum().name();
            packet_portnum_value = packet_data.getDecoded().getPortnumValue();
        }
        packet_RxRssi = packet_data.getRxRssi();
        packet_HopLimit = packet_data.getHopLimit();
        packet_hopStart = packet_data.getHopStart();
        packet_RelayNode = convert_32int_to_string(packet_data.getRelayNode());
    }

    public void populatePacketDataStructure(DataBlock dataBlock){
        // Populate Parent Class Packet Data
        dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000d);
        dataBlock.setStringValue(1, packet_id);
        dataBlock.setStringValue(2, packet_to);
        dataBlock.setStringValue(3, packet_from);
        dataBlock.setTimeStamp(4, packet_time);
        dataBlock.setBooleanValue(5, packet_hasData);
        dataBlock.setStringValue(6, packet_portnum);
        dataBlock.setIntValue(7, packet_portnum_value);
        dataBlock.setDoubleValue(8, packet_RxRssi);
        dataBlock.setDoubleValue(9, packet_HopLimit);
        dataBlock.setDoubleValue(10, packet_hopStart);
        dataBlock.setStringValue(11, packet_RelayNode);
    }


    public String convert_32int_to_string(int data){
        long unsigned_num = Integer.toUnsignedLong(data);
        return String.format("!%08x", unsigned_num);
    }
    public Instant convert_32int_to_Instant(int data){
        long unsigned_num = Integer.toUnsignedLong(data);
        return Instant.ofEpochSecond(unsigned_num);
    }
}
