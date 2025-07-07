package org.sensorhub.impl.sensor.notecardGPS;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.Text;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.swe.SWEHelper;

public class notecardMSGControl extends AbstractSensorControl<notecardGPSSensor> {

    private DataRecord commandDataStruct;

    // CONSTRUCTOR
    public notecardMSGControl( notecardGPSSensor notecardGPSSensor) {
        super("Blues Notecard Message Control", notecardGPSSensor);
    }


    // INITIALIZE
    public void doInit() {

        SWEHelper fac = new SWEHelper();

        // Create data structure to format typical 'Note' message for notehub.io
        commandDataStruct = fac.createRecord()
                .name(getName())
                .updatable(true)
                .definition(SWEHelper.getPropertyUri("TrackControls"))
                .label("Send Notehub Message")
                .description("Send a note to Notehub.io Cloud")
                    .addField("noteTitle", fac.createText()
                            .label("Note File Title")
                            .description("Input the name of the file that will be displayed in notehub.io"))
                    .addField("noteMessage", fac.createText()
                            .label("Note Message")
                            .description("This is a note message"))
                    .build();

    }

    @Override
    protected boolean execCommand(DataBlock cmdData) throws CommandException
    {
        DataRecord commandData = commandDataStruct.copy();
        commandData.setData(cmdData);

        Text noteTitle = (Text) commandData.getField("noteTitle");
        Text noteMessage = (Text) commandData.getField("noteMessage");

        String hubNoteReq = String.format(
                "{\"req\": \"note.add\", \"file\": \"%s.qo\", \"body\": {\"message\": \"%s\"}, \"sync\": true}",
                noteTitle.getValue(),
                noteMessage.getValue()
        );

        parentSensor.Transaction(hubNoteReq);

        return true;
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandDataStruct;
    }

}
