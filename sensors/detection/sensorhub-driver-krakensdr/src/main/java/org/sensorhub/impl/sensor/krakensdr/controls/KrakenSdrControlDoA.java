package org.sensorhub.impl.sensor.krakensdr.controls;

import com.google.gson.JsonObject;
import net.opengis.swe.v20.*;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.sensorhub.impl.sensor.krakensdr.KrakenSdrConstants;
import org.sensorhub.impl.sensor.krakensdr.KrakenSdrDriver;
import org.vast.swe.SWEHelper;

public class KrakenSdrControlDoA extends AbstractSensorControl<KrakenSdrDriver> {
    private DataRecord commandDataStruct;
    private static final String ANTENNA_SPACING_M = "antennaSpacingMeters";
    private static final String ANTENNA_ARRANGEMENT = "antennaArrangement";
    private static final String DOA_ALGORITHM = "doaAlgorithm";
    private static final String DECORRELATION_MTHD = "decorrelationMethod";
    private static final String ULA_DIRECTION = "ulaDirection";
    private static final String ARRAY_OFFSET = "arrayOffset";
    private static final String EXPECTED_NUM_SRCS = "expectedNumOfSrcs";


    // CONSTRUCTOR
    public KrakenSdrControlDoA(KrakenSdrDriver krakenSdrDriver) {
        super("doaControl", krakenSdrDriver);
    }

    // INITIALIZE CONTROL
    public void doInit(){
        SWEHelper fac = new SWEHelper();
        // The Master Control Data Structure is a Choice of individual controls for the KrakenSDR
        commandDataStruct = fac.createRecord()
                .name("doaControl")
                .label("DoA Configuration Control")
                .description("Data Record for the DoA Configuration Settings")
                .definition(SWEHelper.getPropertyUri("DoaControl"))
                .addField(ANTENNA_ARRANGEMENT, fac.createCategory()
                        .label("Antenna Arrangement")
                        .description("The Arrangement must be UCA or ULA")
                        .definition(SWEHelper.getPropertyUri("AntennaArrangement"))
                        .addAllowedValues("UCA", "ULA")
                        .value("UCA"))
                .addField(ANTENNA_SPACING_M, fac.createQuantity()
                        .uom("m")
                        .label("Antenna Array Radius")
                        .description("Current spacing of the Antenna Array")
                        .definition(SWEHelper.getPropertyUri("AntennaSpacingMeters"))
                        .value(0.21))
                .addField(DOA_ALGORITHM, fac.createCategory()
                        .label("DoA Algorithm")
                        .description("Algorithm used to obtain the DoA")
                        .definition(SWEHelper.getPropertyUri("DoaAlgorithm"))
                        .addAllowedValues("Bartlett", "Capon", "MEM", "TNA", "MUSIC", "ROOT-MUSIC")
                        .value("MUSIC"))
                .addField(DECORRELATION_MTHD, fac.createCategory()
                        .label("Decorrelation Method")
                        .description("Decorrelation method used to assist in dealing with correlated signals")
                        .definition(SWEHelper.getPropertyUri("DecorrelationMethod"))
                        .addAllowedValues("Off", "FBA", "TOEP", "FBSS", "FBTOEP")
                        .value("Off"))
                .addField(ULA_DIRECTION, fac.createCategory()
                        .label("ULA Direction")
                        .description("Determines how Uniform Linear Array (ULA) antennas are oriented relative to reference heading")
                        .definition(SWEHelper.getPropertyUri("UlaDirection"))
                        .addAllowedValues("Both", "Forward", "Backward")
                        .value("Both"))
                .addField(ARRAY_OFFSET, fac.createQuantity()
                        .uom("deg")
                        .label("Array Offset")
                        .description("Array offset in degrees")
                        .definition(SWEHelper.getPropertyUri("ArrayOffset"))
                        .value(0.0))
                .addField(EXPECTED_NUM_SRCS, fac.createCategory()
                        .label("Expected Number of Sources")
                        .description("Number of signal sources expected by the DoA algorithm")
                        .definition(SWEHelper.getPropertyUri("ExpectedNumberOfSources"))
                        .addAllowedValues("1", "2", "3", "4")
                        .value("1"))
                .build();
    }

    @Override
    protected boolean execCommand(DataBlock cmdData) throws CommandException
    {
        // RETRIEVE INPUTS FROM ADMIN PANEL CONTROL
        DataRecord commandData = commandDataStruct.copy();
        commandData.setData(cmdData);

        JsonObject data = new JsonObject();

        // UPDATE ANTENNA Arrangement IF UPDATED IN ADMIN PANEL
        Category oshAntArrangement = (Category) commandData.getField(ANTENNA_ARRANGEMENT);
        String oshAntArrangementValue = oshAntArrangement.getValue();
        if (oshAntArrangementValue != null) {
            data.addProperty(KrakenSdrConstants.ANT_ARRGMNT, oshAntArrangementValue);
        }

        // UPDATE ANTENNA SPACING IF UPDATED IN ADMIN PANEL
        Quantity oshAntennaSpacing = (Quantity) commandData.getField(ANTENNA_SPACING_M);
        double oshAntennaSpacingValue = oshAntennaSpacing.getValue();
        if(oshAntennaSpacingValue != 0.0){
            data.addProperty(KrakenSdrConstants.ANT_SPACING_MTRS, oshAntennaSpacingValue);
        }

        // UPDATE DOA ALGORITHM IF UPDATED IN ADMIN PANEL
        Category oshDoaAlgo = (Category) commandData.getField(DOA_ALGORITHM);
        String oshDoaAlgoValue = oshDoaAlgo.getValue();
        if (oshDoaAlgoValue != null) {
            data.addProperty(KrakenSdrConstants.DOA_ALGORITHM, oshDoaAlgoValue);
        }

        // UPDATE DECORRELATION METHOD IF UPDATED IN ADMIN PANEL
        Category oshDecorrelationMethod = (Category) commandData.getField(DECORRELATION_MTHD);
        String oshDecorrelationMethodValue = oshDecorrelationMethod.getValue();
        if (oshDecorrelationMethodValue != null) {
            data.addProperty(KrakenSdrConstants.DOA_DECORRELATION, oshDecorrelationMethodValue);
        }

        // UPDATE ULA DIRECTION IF UPDATED IN ADMIN PANEL
        Category oshUlaDir = (Category) commandData.getField(ULA_DIRECTION);
        String oshUlaDirValue = oshUlaDir.getValue();
        if (oshUlaDirValue != null) {
            data.addProperty(KrakenSdrConstants.ULA_DIR, oshUlaDirValue);
        }

        // UPDATE ARRAY OFFSET IF UPDATED IN ADMIN PANEL
        Quantity oshArrayOffset = (Quantity) commandData.getField(ARRAY_OFFSET);
        data.addProperty(KrakenSdrConstants.ARRAY_OFFSET, oshArrayOffset.getValue());

        // UPDATE EXPECTED NUMBER OF SOURCES IF UPDATED IN ADMIN PANEL
        Category oshExpNumSrcs = (Category) commandData.getField(EXPECTED_NUM_SRCS);
        String oshExpNumSrcsValue = oshExpNumSrcs.getValue();
        if (oshExpNumSrcsValue != null) {
            data.addProperty(KrakenSdrConstants.EXPECTED_NUM_SRCS, Integer.parseInt(oshExpNumSrcsValue));
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
