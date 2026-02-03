package com.botts.impl.sensor.universalcontroller;


import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.data.DataChoiceImpl;
import org.vast.swe.SWEHelper;

public class UniversalControllerControl extends AbstractSensorControl<UniversalControllerSensor> {
    DataChoice commandData;

    protected UniversalControllerControl(UniversalControllerSensor parentSensor) {
        super("updateIndex", parentSensor);
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandData;
    }

    @Override
    protected boolean execCommand(DataBlock cmdData) throws CommandException {
        boolean testCmd = false;

        DataChoice commandMsg = commandData.copy();
        commandMsg.setData(cmdData);

        DataComponent component = ((DataChoiceImpl) commandMsg).getSelectedItem();
        String itemID = component.getName();
        DataBlock data = component.getData();
        int itemValue = data.getIntValue();

        try {

            if(itemID.equals("primaryControlStreamIndex")) {
                parentSensor.output.setPrimaryControlStreamIndex(itemValue);
            } else if (itemID.equals("primaryControllerIndex")) {
                parentSensor.output.setPrimaryControllerIndex(itemValue);
            }
            testCmd = true;

        } catch (Exception e) {
            getLogger().error("failed to send command: " + e.getMessage());
        }
        return testCmd;
    }

    protected void init() {

        SWEHelper sweFactory = new SWEHelper();

        commandData = sweFactory.createChoice()
                .name("updateIndex")
                .label("Index Updater")
                .description("Command to update primary controller/control stream index")
                .updatable(true)
                .build();

        commandData.setName("updateIndex");

        commandData.addItem("primaryControlStreamIndex", sweFactory.createCount()
                .label("Primary Control Stream Index")
                .description("Index of the primary control stream")
                .definition(SWEHelper.getPropertyUri("PrimaryControlStreamIndex"))
                .value(0)
                .build());

        commandData.addItem("primaryControllerIndex", sweFactory.createCount()
                .label("Primary Controller Index")
                .description("Index of the primary controller in use")
                .definition(SWEHelper.getPropertyUri("PrimaryControllerIndex"))
                .value(0)
                .build());
    }
}