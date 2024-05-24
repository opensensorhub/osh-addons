package org.sensorhub.process.universalcontroller;

import org.junit.Assert;
import org.junit.Test;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.processing.IProcessModule;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.processing.*;
import org.sensorhub.process.universalcontroller.impl.ControllerPTZProcess;
import org.vast.sensorML.*;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class TestGamepadPtzProcess {

    GamepadPtz ptzProcess = new GamepadPtz();
    static ModuleRegistry registry;

    protected static void runProcess(IProcessModule<?> process) throws Exception
    {
        AtomicInteger counter = new AtomicInteger();

        for (IStreamingDataInterface output: process.getOutputs().values())
            output.registerListener(e -> {
                //System.out.println(e.getTimeStamp() + ": " + ((DataEvent)e).getRecords()[0].getAtomCount());
                counter.incrementAndGet();
            });

        process.start();

        long t0 = System.currentTimeMillis();
        while (counter.get() < 85-12+1)
        {
            if (System.currentTimeMillis() - t0 >= 100000L)
                Assert.fail("No data received before timeout");
            Thread.sleep(100);
        }

        System.out.println();
    }
    protected static IProcessModule<?> createSMLProcess(String smlUrl) throws Exception
    {
        SMLProcessConfig processCfg = new SMLProcessConfig();
        processCfg.autoStart = false;
        processCfg.name = "SensorML Process #1";
        processCfg.moduleClass = SMLProcessImpl.class.getCanonicalName();
        processCfg.sensorML = smlUrl;

        @SuppressWarnings("unchecked")
        IProcessModule<SMLProcessConfig> process = (IProcessModule<SMLProcessConfig>)registry.loadModule(processCfg);
        process.init();
        return process;
    }

    @Test
    public void testGamepadPtzProcess() throws Exception
    {
        ControllerPTZProcess p = new ControllerPTZProcess();
        p.init();
        System.out.println(p.getProcessInfo().getUri());

        SMLUtils smlHelper = new SMLUtils(SMLUtils.V2_0);

        SimpleProcessImpl simple = new SimpleProcessImpl();
        simple.setExecutableImpl(p);

        // serialize
//        AggregateProcessImpl wp = new AggregateProcessImpl();
//        for (AbstractSWEIdentifiable output: p.getOutputList()) {
//            wp.addOutput(output.getId(), (DataComponent) output);
//        }
        //smlHelper.makeProcessExecutable(wp, false);
        //wp.setExecutableImpl(p);
        // set type
//        wp.setUniqueIdentifier(UUID.randomUUID().toString());

//        // gamepad source
//        SimpleProcess gamepadSource = new SimpleProcessImpl();
//        gamepadSource.setUniqueIdentifier("urn:osh:sensor:universalcontroller");
//        Reference sourceRef = new ReferenceImpl();
//        sourceRef.setHref("urn:osh:process:datasource:datastream");
//        gamepadSource.setTypeOf(sourceRef);
//        wp.addComponent("gamepadsource", gamepadSource);
//
//        // process component
//        wp.addComponent("ptzprocess", simple);
//
//        // axiscam commandstream
////        CommandStreamSink ptzDestination = new CommandStreamSink();
//        SimpleProcess ptzDestination = new SimpleProcessImpl();
//        ptzDestination.setUniqueIdentifier("urn:axis:cam:00408CA0FF1C");
//        Reference sinkRef = new ReferenceImpl();
//        sinkRef.setHref("urn:osh:process:datasink:commandstream");
//        ptzDestination.setTypeOf(sinkRef);
//        wp.addComponent("axiscam", ptzDestination);
//
//        // inputs and outputs
////        wp.addOutput("ptz", p.getOutputList().getComponent(0));
////        wp.addInput("pov", p.getInputList().getComponent(0));
//
//        // connections
//        LinkImpl inputToProcess = new LinkImpl();
//        inputToProcess.setSource("components/gamepadsource/outputs/gamepads/gamepad0/pov");
//        inputToProcess.setDestination("components/ptzprocess/inputs/pov");
//
//        LinkImpl outputToCommand = new LinkImpl();
//        outputToCommand.setSource("components/ptzprocess/outputs/ptz");
//        outputToCommand.setDestination("components/axiscam/inputs/ptz");
//
//        // add 2 connections
//        wp.addConnection(inputToProcess);
//        wp.addConnection(outputToCommand);

        smlHelper.writeProcess(System.out, simple, true);

        p.execute();
    }

}
