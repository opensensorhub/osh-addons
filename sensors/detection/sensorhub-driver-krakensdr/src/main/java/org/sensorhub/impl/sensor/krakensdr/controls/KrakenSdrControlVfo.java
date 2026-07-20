package org.sensorhub.impl.sensor.krakensdr.controls;

import com.google.gson.JsonObject;
import net.opengis.swe.v20.*;
import net.opengis.swe.v20.Boolean;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.sensorhub.impl.sensor.krakensdr.KrakenSdrConstants;
import org.sensorhub.impl.sensor.krakensdr.KrakenSdrDriver;
import org.vast.swe.SWEHelper;

public class KrakenSdrControlVfo extends AbstractSensorControl<KrakenSdrDriver> {
    private DataRecord commandDataStruct;
    private static final String VFO_MODE = "vfoMode";
    private static final String SPECTRUM_CALC = "spectrumCalculation";
    private static final String VFO_DEFAULT_SQLCH = "vfoDefaultSquelchMode";
    private static final String ACTIVE_VFOS = "activeVfos";
    private static final String OUTPUT_VFOS = "outputVfos";
    private static final String DSP_DECIMATION = "dspDecimation";
    private static final String OPTIMIZE_SHORT_BURSTS = "optimizeShortBursts";


    // CONSTRUCTOR
    public KrakenSdrControlVfo(KrakenSdrDriver krakenSdrDriver) {
        super("vfoControl", krakenSdrDriver);
    }

    // INITIALIZE CONTROL
    public void doInit(){
        SWEHelper fac = new SWEHelper();
        // The Master Control Data Structure is a Choice of individual controls for the KrakenSDR
        commandDataStruct = fac.createRecord()
                .name("vfoControl")
                .label("VFO Configuration Control")
                .description("Data Record for the VFO Configuration Settings")
                .definition(SWEHelper.getPropertyUri("VfoControl"))
                .addField(SPECTRUM_CALC, fac.createCategory()
                        .label("Spectrum Calculation")
                        .description("Spectrum calculation for the KrakenSDR")
                        .definition(SWEHelper.getPropertyUri("SpectrumCalculation"))
                        .addAllowedValues("Single", "All")
                        .value("Single"))
                .addField(VFO_MODE, fac.createCategory()
                        .label("VFO Mode")
                        .description("By setting this mode to Auto, Kraken will look for highest amplitude in spectrum range")
                        .definition(SWEHelper.getPropertyUri("VfoMode"))
                        .addAllowedValues("Standard", "Auto")
                        .value("Auto"))
                .addField(VFO_DEFAULT_SQLCH, fac.createCategory()
                        .label("VFO Default Squelch Mode")
                        .description("VFO default squelch mode for the KrakenSDR")
                        .definition(SWEHelper.getPropertyUri("VfoDefaultSquelchMode"))
                        .addAllowedValues("Auto", "Manual", "Auto Channel")
                        .value("Auto"))
                .addField(ACTIVE_VFOS, fac.createQuantity()
                        .label("Active VFOs")
                        .description("Active VFOs (0-16, default = 1). To be any number besides 1, VFO mode MUST be set to Standard")
                        .definition(SWEHelper.getPropertyUri("ActiveVfos"))
                        .addAllowedInterval(1,16)
                        .value(1))
                .addField(OUTPUT_VFOS, fac.createQuantity()
                        .label("Output VFOs")
                        .description("Output VFOs: -1 (all), 0 – 15, default = 0")
                        .definition(SWEHelper.getPropertyUri("OutputVfos"))
                        .addAllowedInterval(-1.0,15)
                        .value(0))
                .addField(DSP_DECIMATION, fac.createQuantity()
                        .label("DSP Decimation")
                        .description("DSP Decimation: ≥ 1, default = 1")
                        .definition(SWEHelper.getPropertyUri("DspDecimation"))
                        .addAllowedInterval(1.0, Double.POSITIVE_INFINITY)
                        .value(1.0))
                .addField(OPTIMIZE_SHORT_BURSTS, fac.createBoolean()
                        .label("Optimize Short Bursts")
                        .description("Optimize Short Bursts: default=false")
                        .definition(SWEHelper.getPropertyUri("OptimizeShortBursts"))
                        .value(false))
                .build();
    }

    @Override
    protected boolean execCommand(DataBlock cmdData) throws CommandException
    {
        // RETRIEVE INPUTS FROM ADMIN PANEL CONTROL
        DataRecord commandData = commandDataStruct.copy();
        commandData.setData(cmdData);

        JsonObject data = new JsonObject();

        // UPDATE SPECTRUM CALCULATION IF UPDATED IN ADMIN PANEL
        Category oshSpectrumCalc = (Category) commandData.getField(SPECTRUM_CALC);
        String oshSpectrumCalcValue = oshSpectrumCalc.getValue();
        if (oshSpectrumCalcValue != null) {
            data.addProperty(KrakenSdrConstants.SPECTRUM_CALC, oshSpectrumCalcValue);
        }

        // UPDATE VFO MODE IF UPDATED IN ADMIN PANEL
        Category oshVfoMode = (Category) commandData.getField(VFO_MODE);
        String oshVfoModeValue = oshVfoMode.getValue();
        if (oshVfoModeValue != null) {
            data.addProperty(KrakenSdrConstants.VFO_MODE, oshVfoModeValue);
        }

        // UPDATE VFO DEFAULT SQUELCH MODE IF UPDATED IN ADMIN PANEL
        Category oshVfoSqlch = (Category) commandData.getField(VFO_DEFAULT_SQLCH);
        String oshVfoSqlchValue = oshVfoSqlch.getValue();
        if (oshVfoSqlchValue != null) {
            data.addProperty(KrakenSdrConstants.VFO_DFLT_SQLCH_MODE, oshVfoSqlchValue);
        }

        // UPDATE ACTIVE VFOS IF UPDATED IN ADMIN PANEL
        Quantity oshActiveVfos = (Quantity) commandData.getField(ACTIVE_VFOS);
        double oshActiveVfosValue = oshActiveVfos.getValue();
        data.addProperty(KrakenSdrConstants.VFOS_ACTIVE, oshActiveVfosValue);

        // UPDATE OUTPUT VFOS IF UPDATED IN ADMIN PANEL
        Quantity oshOutputVfos = (Quantity) commandData.getField(OUTPUT_VFOS);
        double oshOutputVfosValue = oshOutputVfos.getValue();
        data.addProperty(KrakenSdrConstants.VFOS_OUTPUT, oshOutputVfosValue);

        // UPDATE DSP SLIDE DECIMATION IF UPDATED IN ADMIN PANEL
        Quantity oshDspDec = (Quantity) commandData.getField(DSP_DECIMATION);
        double oshDspDecValue = oshDspDec.getValue();
        data.addProperty(KrakenSdrConstants.DSP_DECIMATION, oshDspDecValue);

        // UPDATE OPTIMIZE SHORT BURST OPTION IF UPDATED IN ADMIN PANEL
        Boolean oshOSB = (Boolean) commandData.getField(OPTIMIZE_SHORT_BURSTS);
        boolean oshOSBValue = oshOSB.getValue();
        data.addProperty(KrakenSdrConstants.ENBL_OPT_SHORT_BURST, oshOSBValue);

        parentSensor.updateKrakenSettings(data);
        return true;
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandDataStruct;
    }
}
