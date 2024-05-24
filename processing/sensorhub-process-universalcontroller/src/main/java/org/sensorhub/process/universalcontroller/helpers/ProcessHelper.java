package org.sensorhub.process.universalcontroller.helpers;

import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataRecord;
import org.vast.process.ExecutableProcessImpl;
import org.vast.sensorML.AggregateProcessImpl;
import org.vast.sensorML.SMLStaxBindings;
import org.vast.sensorML.SMLUtils;
import org.vast.sensorML.SimpleProcessImpl;

import java.io.OutputStream;
import java.util.ArrayList;

public class ProcessHelper extends SMLUtils {
    AggregateProcessImpl process;
    ArrayList<SimpleProcessImpl> processComponents;
    public ProcessHelper() {
        super(V2_0);

        process = new AggregateProcessImpl();
        processComponents = new ArrayList<>();
    }
    public ProcessHelper(SMLStaxBindings staxBindings) {
        super(staxBindings);
    }

    // TODO: Implement all

    /**
     * Prints XML process description to output stream
     *
     * @param outputStream
     */
    public void writeXML(OutputStream outputStream) {}

    /**
     * Adds output to aggregate process
     *
     * @param output DataRecord that describes output
     */
    public void addOutput(DataRecord output) {}

    /**
     * Adds input to aggregate process
     *
     * @param input DataRecord that describes input
     */
    public void addInput(DataRecord input) {}

    /**
     * Adds process to aggregate process
     *
     * @param process Class of process
     */
    public void addProcess(ExecutableProcessImpl process) {}

    /**
     * Adds datasource to aggregate process
     *
     * @param systemUID System UID of datasource
     */
    public void addDataSource(String systemUID) {}

    /**
     * Adds control stream to aggregate process
     *
     * @param systemUID System UID of control stream
     * @param inputName Name of control stream input
     */
    public void addControlStream(String systemUID, String inputName) {}

    /**
     * Adds connection to link inputs to outputs or vice-versa
     *
     * @param source String of source of connection
     * @param destination String of destination of connection
     */
    public void addConnection(String source, String destination) {}
}
