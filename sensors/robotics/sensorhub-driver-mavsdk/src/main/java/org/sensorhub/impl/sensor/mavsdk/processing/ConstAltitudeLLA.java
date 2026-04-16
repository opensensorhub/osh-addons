package org.sensorhub.impl.sensor.mavsdk.processing;

import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataType;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

public class ConstAltitudeLLA extends ExecutableProcessImpl {

    public static final OSHProcessInfo INFO = new OSHProcessInfo("constAltitudeLLA", "Constant Altitude LLA", null, ConstAltitudeLLA.class);

    DataComponent locationInput;
    DataComponent locationOutput;
    DataComponent llaVectorOutput;
    DataComponent altitudeParam;
    DataComponent returnToStartParam;
    DataComponent hoverSecondsParam;

    public ConstAltitudeLLA() {
        super(INFO);
        GeoPosHelper fac = new GeoPosHelper();

        paramData.add("altitude", altitudeParam = fac.createQuantity().dataType(DataType.DOUBLE).uom("m").definition(SWEHelper.getPropertyUri("AltitudeAGL")).build());
        paramData.add("returnToStart", returnToStartParam = fac.createBoolean().build());
        paramData.add("hoverSeconds", hoverSecondsParam = fac.createCount().value(0).build());

        inputData.add("locationInput", locationInput = fac.createLocationVectorLLA()
                .definition(SWEHelper.getPropertyUri("FeatureOfInterestLocation"))
                .label("Target Location")
                .build());

        outputData.add("locationOutput", locationOutput = fac.createRecord()
                .label("Target Location")
                .description("This should be used with UnmannedControlLocation")
                .addField( "locationVectorLLA", llaVectorOutput = fac.createVector()
                        .addCoordinate("Latitude", fac.createQuantity()
                                .uom("deg"))
                        .addCoordinate("Longitude", fac.createQuantity()
                                .uom("deg"))
                        .addCoordinate("AltitudeAGL", fac.createQuantity()
                                .uom("m")
                                .value(30))
                        .build())
                .addField( "returnToStart", fac.createBoolean().value(false))
                .addField( "hoverSeconds", fac.createCount().value(0))
                .build());
    }

    @Override
    public void execute() throws ProcessException {
        double lat = locationInput.getData().getDoubleValue(0);
        double lon = locationInput.getData().getDoubleValue(1);
        double alt = altitudeParam.getData().getDoubleValue(0);
        boolean returnToStart = returnToStartParam.getData().getBooleanValue();
        int hoverSeconds = hoverSecondsParam.getData().getIntValue();

        locationOutput.renewDataBlock();
        llaVectorOutput.getData().setDoubleValue(0, lat);
        llaVectorOutput.getData().setDoubleValue(1, lon);
        llaVectorOutput.getData().setDoubleValue(2, alt);
        locationOutput.getData().setBooleanValue(3, returnToStart);
        locationOutput.getData().setIntValue(4, hoverSeconds);
    }
}