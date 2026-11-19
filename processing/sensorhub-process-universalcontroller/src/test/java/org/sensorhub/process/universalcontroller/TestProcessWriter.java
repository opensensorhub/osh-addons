package org.sensorhub.process.universalcontroller;

import org.junit.Test;
import org.sensorhub.impl.process.universalcontroller.helpers.ProcessHelper;
import org.sensorhub.impl.process.universalcontroller.ControllerPTZProcess;
import org.sensorhub.impl.process.universalcontroller.PrimaryControllerSelector;

import static org.junit.Assert.assertEquals;

public class TestProcessWriter {

    public TestProcessWriter() throws Exception {
        testGamepadPtzProcess();
    }

    @Test
    public void testGamepadPtzProcess() throws Exception
    {
        ProcessHelper processHelper = new ProcessHelper();

        processHelper.addOutputList(new PrimaryControllerSelector().getOutputList());

        processHelper.addDataSource("gamepadsource", "urn:osh:sensor:universalcontroller");

        processHelper.addProcess("primaryselector", new PrimaryControllerSelector());
        processHelper.addProcess("axisprocess", new ControllerPTZProcess());
        processHelper.addProcess("dahuaprocess", new ControllerPTZProcess());

        processHelper.addControlStream("axisrpan", "urn:axis:cam:00408CA0FF1C", "ptzControl");
        processHelper.addControlStream("axisrtilt", "urn:axis:cam:00408CA0FF1C", "ptzControl");
        processHelper.addControlStream("axisrzoom", "urn:axis:cam:00408CA0FF1C", "ptzControl");
        processHelper.addControlStream("dahuarpan", "urn:dahua:cam:1G0215CGAK00046", "ptzControl");
        processHelper.addControlStream("dahuartilt", "urn:dahua:cam:1G0215CGAK00046", "ptzControl");
        processHelper.addControlStream("dahuarzoom", "urn:dahua:cam:1G0215CGAK00046", "ptzControl");

        processHelper.addConnection("components/gamepadsource/outputs/UniversalControllerOutput/primaryControlStreamIndex",
                "components/primaryselector/inputs/gamepadRecord/primaryControlStreamIndex");
        processHelper.addConnection("components/gamepadsource/outputs/UniversalControllerOutput/numControlStreams",
                "components/primaryselector/inputs/gamepadRecord/numControlStreams");
        processHelper.addConnection("components/gamepadsource/outputs/UniversalControllerOutput/numGamepads",
                "components/primaryselector/inputs/gamepadRecord/numGamepads");
        processHelper.addConnection("components/gamepadsource/outputs/UniversalControllerOutput/gamepads",
                "components/primaryselector/inputs/gamepadRecord/gamepads");

        processHelper.addConnection("components/primaryselector/outputs/componentRecord",
                "components/axisprocess/inputs/componentRecord");
        processHelper.addConnection("components/primaryselector/outputs/componentRecord",
                "components/dahuaprocess/inputs/componentRecord");

        processHelper.addConnection("components/axisprocess/outputs/rpan",
                "components/axiscamcontrolrpan/inputs/ptzControl/rpan");
        processHelper.addConnection("components/axisprocess/outputs/rtilt",
                "components/axiscamcontrolrtilt/inputs/ptzControl/rtilt");
        processHelper.addConnection("components/axisprocess/outputs/rzoom",
                "components/axiscamcontrolrzoom/inputs/ptzControl/rzoom");

        processHelper.addConnection("components/dahuaprocess/outputs/rpan",
                "components/dahuacontrolrpan/inputs/ptzControl/rpan");
        processHelper.addConnection("components/dahuaprocess/outputs/rtilt",
                "components/dahuacontrolrtilt/inputs/ptzControl/rtilt");
        processHelper.addConnection("components/dahuaprocess/outputs/rzoom",
                "components/dahuacontrolrzoom/inputs/ptzControl/rzoom");

        processHelper.addConnection("components/primaryselector/outputs/componentRecord",
                "outputs/componentRecord");

        processHelper.writeXML(System.out);
    }

}
