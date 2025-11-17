package com.sensorhub.impl.sensor.meshtastic;

import com.geeksville.mesh.MeshProtos;
import com.geeksville.mesh.Portnums;
import com.google.protobuf.ByteString;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.swe.SWEHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MeshtasticControlTextMessage extends AbstractSensorControl<MeshtasticSensor> {

    protected static final String NAME = "textMessageControl";
    protected final DataComponent commandDescription;

    public MeshtasticControlTextMessage(MeshtasticSensor parentSensor){
        super(NAME, parentSensor);

        SWEHelper fac = new SWEHelper();

        commandDescription = fac.createRecord()
                .name(NAME)
                .label("Text Message")
                .description("Control for sending Text Messages over meshtastic")
                .definition(SWEHelper.getPropertyUri("TextMessageControl"))
                .addField("message", fac.createText()
                        .label("Message")
                        .description("Type the message you want to send over the meshtastic network")
                        .definition(SWEHelper.getPropertyUri("Message"))
                )
                .addField("channel", fac.createQuantity()
                        .label("Channel")
                        .description("Channel, default is 0")
                        .definition(SWEHelper.getPropertyUri("Channel"))
                )
                .addField("destination", fac.createText()
                        .label("Destination")
                        .description("Type the Node Number of the Meshtastic Node if you want to send a direct message to that Node. Type 'broadcast' if you want to broadcast your message to all nodes present")
                        .definition(SWEHelper.getPropertyUri("Destination"))
                )
                .build();
    }

    private MeshProtos.ToRadio createMeshtasticMessage(DataBlock cmdData){
        String message = cmdData.getStringValue(0);
        int channel = cmdData.getIntValue(1);
        String destination = cmdData.getStringValue(2).toLowerCase();

        // CREATE A CHECK FOR DESTINATION. USERS ON ADMIN CAN BROADCAST CHANNELS BY TYPING "Broadcast" or
        // USERS CAN SEND TO DIRECT NODE USING NODE NUMBER
        int destinationID;
        if(destination.equals("broadcast")){
            destinationID = 0xFFFFFFFF;
        }else{
            destinationID = (int) (Long.parseLong(destination) & 0xFFFFFFFFL );

        }

        // CREATE A MESHTASTIC PROTO PACKET TO BE SENT AS TEXT MESSAGE
        MeshProtos.MeshPacket packet = MeshProtos.MeshPacket.newBuilder()
                .setDecoded(MeshProtos.Data.newBuilder()
                        .setPortnum(Portnums.PortNum.internalGetValueMap().findValueByNumber(Portnums.PortNum.TEXT_MESSAGE_APP_VALUE))
                        .setPayload(ByteString.copyFrom(message, StandardCharsets.UTF_8))
                        .build()
                )
                .setChannel(channel)
                .setTo(destinationID) // Primary Channel
                .setWantAck(!destination.equals("broadcast")) // Acknowledge direct messages to display history
                .build();

        return MeshProtos.ToRadio.newBuilder()
                .setPacket(packet)
                .build();

    }

    @Override
    protected boolean execCommand(DataBlock cmdData) throws CommandException {
        var msg = createMeshtasticMessage(cmdData);

        try {
            parentSensor.sendMessage(msg);
            return true;
        } catch (IOException e) {
            getLogger().error("Failed to send message", e);
            return false;
        }
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandDescription;
    }



}
