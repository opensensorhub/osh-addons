package org.sensorhub.impl.process.universalcontroller.helpers;

import net.opengis.gml.v32.impl.ReferenceImpl;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.sensorml.v20.IOPropertyList;
import net.opengis.sensorml.v20.SimpleProcess;
import net.opengis.sensorml.v20.impl.SettingsImpl;
import net.opengis.swe.v20.AbstractSWEIdentifiable;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.IProcessExec;
import org.vast.process.ProcessException;
import org.vast.sensorML.*;
import org.vast.xml.XMLWriterException;

import java.io.OutputStream;
import java.util.ArrayList;

public class ProcessHelper extends SMLUtils {
    AggregateProcessImpl aggregateProcess;
    ReferenceImpl controlType;
    ReferenceImpl sourceType;
    public ProcessHelper() {
        super(V2_0);

        controlType = new ReferenceImpl("urn:osh:process:datasink:commandstream");
        sourceType = new ReferenceImpl("urn:osh:process:datasource:stream");

        aggregateProcess = new AggregateProcessImpl();
    }
    public ProcessHelper(SMLStaxBindings staxBindings) {
        super(staxBindings);
    }

    /**
     * Prints XML process description to output stream
     *
     * @param outputStream
     */
    public void writeXML(OutputStream outputStream) throws XMLWriterException {
        writeProcess(outputStream, aggregateProcess, true);
    }

    /**
     * Adds output to aggregate process
     *
     * @param output DataRecord that describes output
     */
    public void addOutput(DataRecord output) {
        aggregateProcess.addOutput(output.getName(), output);
    }

    public void addOutput(String name, DataRecord output) {
        aggregateProcess.addOutput(name, output);
    }

    /**
     * Adds output list to aggregate process
     *
     * @param outputs List of outputs from a process
     */
    public void addOutputList(IOPropertyList outputs) {
        for (AbstractSWEIdentifiable output : outputs) {
            DataComponent outputData = (DataComponent) output;
            aggregateProcess.addOutput(outputData.getName(), outputData);
        }
    }

    /**
     * Adds input to aggregate process
     *
     * @param input DataRecord that describes input
     */
    public void addInput(DataRecord input) {
        aggregateProcess.addInput(input.getName(), input);
    }

    public void addInput(String name, DataRecord input) {
        aggregateProcess.addInput(name, input);
    }

    /**
     * Adds process to aggregate process
     *
     * @param process Class of process
     */
    public void addProcess(String name, ExecutableProcessImpl process) throws ProcessException {
        process.init();
        SimpleProcessImpl execProcess = new SimpleProcessImpl();
        execProcess.setExecutableImpl(process);

        aggregateProcess.addComponent(name, execProcess);
    }

    /**
     * Adds datasource to aggregate process
     *
     * @param systemUID System UID of datasource
     */
    public void addDataSource(String name, String systemUID) {
        SimpleProcessImpl source = new SimpleProcessImpl();
        source.setTypeOf(sourceType);
        SettingsImpl settings = new SettingsImpl();
        settings.addSetValue("parameters/producerURI", systemUID);
        source.setConfiguration(settings);

        aggregateProcess.addComponent(name, source);
    }

    /**
     * Adds control stream to aggregate process
     *
     * @param systemUID System UID of control stream
     * @param inputName Name of control stream input
     */
    public void addControlStream(String name, String systemUID, String inputName) {
        SimpleProcessImpl control = new SimpleProcessImpl();
        control.setTypeOf(controlType);
        SettingsImpl settings = new SettingsImpl();
        settings.addSetValue("parameters/systemUID", systemUID);
        settings.addSetValue("parameters/inputName", inputName);

        control.setConfiguration(settings);

        aggregateProcess.addComponent(name, control);
    }

    /**
     * Adds connection to link inputs to outputs or vice-versa
     *
     * @param source String of source of connection
     * @param destination String of destination of connection
     */
    public void addConnection(String source, String destination) {
        aggregateProcess.addConnection(new LinkImpl(source, destination));
    }
}
