package org.sensorhub.impl.sensor.krakensdr.controls;

import com.google.gson.JsonObject;
import net.opengis.swe.v20.*;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.sensorhub.impl.sensor.krakensdr.KrakenSdrConstants;
import org.sensorhub.impl.sensor.krakensdr.KrakenSdrDriver;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;

public class KrakenSdrControlReceiver extends AbstractSensorControl<KrakenSdrDriver> {
    private DataRecord commandDataStruct;
    private static final String CENTER_FREQ = "centerFreq";
    private static final String GAIN = "uniformGain";

    // CONSTRUCTOR
    public KrakenSdrControlReceiver(KrakenSdrDriver krakenSdrDriver) {
        super("receiverControl", krakenSdrDriver);
    }

    // INITIALIZE CONTROL
    public void doInit(){
        SWEHelper fac = new SWEHelper();
        // The Master Control Data Structure is a Choice of individual controls for the KrakenSDR
        commandDataStruct = fac.createRecord()
                .updatable(true)
                .name("rfReceiverControl")
                .label("RF Receiver Configuration Control")
                .description("Data Record for the RF Receiver Configuration")
                .definition(SWEHelper.getPropertyUri("RfReceiverControl"))
                .addField(CENTER_FREQ, fac.createQuantity()
                        .uomCode("MHz")
                        .label("Center Frequency")
                        .description("The transmission frequency of the event in MegaHertz")
                        .definition(SWEConstants.QUDT_URI_PREFIX+"Frequency")
                )
                .addField(GAIN, fac.createCategory()
                        .label("Receiver Gain (dB)")
                        .description("Input the Receiver Gain in dB")
                        .definition(SWEHelper.getPropertyUri("UniformGain"))
                        .addAllowedValues("0", "0.9", "1.4", "2.7", "3.7", "7.7", "8.7", "12.5", "14.4", "15.7", "16.6", "19.7", "20.7", "22.9", "25.4", "28.0", "29.7", "32.8", "33.8", "36.4", "37.2", "38.6", "40.2", "42.1", "43.4", "43.9", "44.5", "48.0", "49.6")
                        .value("19.7")
                )
                .build();
    }

    @Override
    protected boolean execCommand(DataBlock cmdData) throws CommandException {

        // RETRIEVE INPUTS FROM ADMIN PANEL CONTROL
        DataRecord commandData = commandDataStruct.copy();
        commandData.setData(cmdData);

        JsonObject data = new JsonObject();

        // Retrieve values from OSH Controls and add to data object
        Quantity oshFrequency = (Quantity) commandData.getField(CENTER_FREQ);
        double oshFrequencyValue = oshFrequency.getValue();
        if (oshFrequencyValue != 0.0) {
            data.addProperty(KrakenSdrConstants.CENTER_FREQ, oshFrequencyValue);
        }

        Category oshGain = (Category) commandData.getField(GAIN);
        String oshGainValue = oshGain.getValue();
        if (oshGainValue != null) {
            data.addProperty(KrakenSdrConstants.GAIN, Double.parseDouble(oshGainValue));
        }

        if (data.isEmpty()) {
            return true;
        }

        parentSensor.updateKrakenSettings(data);
        return true;
    }


    @Override
    public DataComponent getCommandDescription() {
        return commandDataStruct;
    }





}
