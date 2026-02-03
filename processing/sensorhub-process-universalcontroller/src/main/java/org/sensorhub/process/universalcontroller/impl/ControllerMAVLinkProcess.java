package org.sensorhub.process.universalcontroller.impl;

import com.botts.impl.sensor.universalcontroller.helpers.UniversalControllerComponent;
import net.opengis.swe.v20.*;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.sensorhub.process.universalcontroller.helpers.AbstractControllerTaskingProcess;
import org.vast.process.ProcessException;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

public class ControllerMAVLinkProcess extends AbstractControllerTaskingProcess {

    Quantity takeoffAltitude;
    Vector landingLocation;
    Vector velocity;
    DataRecord heading;
    DataChoice choice;
    float curYaw = 0.0f;

    public static final OSHProcessInfo INFO = new OSHProcessInfo(
            "controllermavlinkprocess",
            "Process to send MAVLink commands",
            null,
            ControllerMAVLinkProcess.class);

    public ControllerMAVLinkProcess() {
        super(INFO);

        GeoPosHelper geo = new GeoPosHelper();

        outputData.add("processCommand", choice =
                fac.createChoice().build());

        outputData.add("takeoffAltitude", takeoffAltitude = fac.createQuantity()
                        .label("Take-Off Altitude")
                        .definition(SWEHelper.getPropertyUri("AltitudeAboveGround"))
                        .uomCode("m")
                        .dataType(DataType.FLOAT)
                        .build());

        outputData.add("landingLocation", landingLocation = geo.createLocationVectorLatLon()
                .label("Landing Location")
                .definition(SWEHelper.getPropertyUri("PlatformLocation"))
                .description("Landing location or NaN to land at the current location")
                .build());

        outputData.add("velocity", velocity = geo.newVelocityVector(
                SWEHelper.getPropertyUri("PlatformVelocity"),
                SWEConstants.REF_FRAME_ENU,
                "m/s"));

        outputData.add("heading", heading = fac.createRecord()
                        .label("Heading")
                        .definition(SWEHelper.getPropertyUri("Heading"))
                            .addField("yaw", fac.createQuantity()
                                    .label("Yaw Angle")
                                    .definition(SWEHelper.getPropertyUri("Yaw"))
                                    .uomCode("deg")
                                    .dataType(DataType.FLOAT)
                                    .value(curYaw)
                                    .build())
                            .addField("yawRate", fac.createQuantity()
                                    .label("Yaw Rate")
                                    .definition(SWEHelper.getPropertyUri("YawRate"))
                                    .uomCode("deg/s")
                                    .dataType(DataType.FLOAT)
                                    .build())
                        .build());

    }

    @Override
    public void updateOutputs() throws ProcessException {
        // x-y velocity
        // rx-ry heading
        // dpad up and down
        // a takeoff
        // b land

        // TODO: Might have error of continuous landing command if 0,0,0 is sent repeatedly. May need to switch all outputs to just be a single DataChoice output.

        float currentX = fac.getComponentValueInput(UniversalControllerComponent.X_AXIS);
        float currentY = fac.getComponentValueInput(UniversalControllerComponent.Y_AXIS);
        float currentRX = fac.getComponentValueInput(UniversalControllerComponent.RX_AXIS);
        float currentRY = fac.getComponentValueInput(UniversalControllerComponent.RY_AXIS);
        float currentDPad = fac.getComponentValueInput(UniversalControllerComponent.D_PAD);
        boolean isAPressed = fac.getComponentValueInput(UniversalControllerComponent.A_BUTTON) == 1.0f;
        boolean isBPressed = fac.getComponentValueInput(UniversalControllerComponent.B_BUTTON) == 1.0f;

        // Takeoff
        if(isAPressed) {
            takeoffAltitude.getData().setFloatValue(5.0f);
        } else {
            takeoffAltitude.getData().setFloatValue(0.0f);
        }

        // Land
        if(isBPressed) {
            landingLocation.setData(null);
        } else {
            // Copied indexes from MAVLinkNavControl
            landingLocation.getData().setFloatValue(0, 0.0f);
            landingLocation.getData().setFloatValue(1, 0.0f);
            landingLocation.getData().setFloatValue(2, 0.0f);
        }

        // Velocity
        // TODO: Add sensitivity modifiers
        velocity.getData().setFloatValue(0, currentX * 3.0f);
        velocity.getData().setFloatValue(2, -currentY * 3.0f);

        if(currentDPad == UniversalControllerComponent.DPadDirection.UP.getValue()) {
            velocity.getData().setFloatValue(1, 3.0f);
        } else if (currentDPad == UniversalControllerComponent.DPadDirection.DOWN.getValue()) {
            velocity.getData().setFloatValue(1, -3.0f);
        } else {
            velocity.getData().setFloatValue(1, 0.0f);
        }

        // Heading
        curYaw += currentRX;
        if(curYaw >= 360.0f) {
            curYaw = Math.min(360.0f, curYaw);
        } else if (curYaw <= 0.0f) {
            curYaw = Math.max(0.0f, curYaw);
        }

        heading.getData().setFloatValue(0, curYaw);
        heading.getData().setFloatValue(1, -currentRY);
    }
}
