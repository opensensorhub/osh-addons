/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2025 the Initial Developer. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package com.botts.process.helpers;

import com.google.gson.stream.JsonWriter;
import net.opengis.gml.v32.impl.ReferenceImpl;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.sensorml.v20.AggregateProcess;
import net.opengis.sensorml.v20.IOPropertyList;
import net.opengis.sensorml.v20.impl.SettingsImpl;
import net.opengis.swe.v20.AbstractSWEIdentifiable;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.slf4j.Logger;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.sensorML.*;
import org.vast.xml.XMLWriterException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class ProcessHelper extends SMLUtils {

    SMLJsonBindings jsonBindings = new SMLJsonBindings();
    public ProcessHelper() {
        super(V2_0);
    }

    /**
     * Prints XML process description of process to output stream
     *
     * @param outputStream
     */
    public void writeProcessXML(AbstractProcess process, OutputStream outputStream) throws XMLWriterException {
        writeProcess(outputStream, process, true);
    }

    /**
     * Prints JSON process description of process to output stream
     * @param process
     *
     * @param outputStream
     * @throws IOException
     */
    public void writeProcessJSON(AbstractProcess process, OutputStream outputStream) throws IOException {
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(outputStream));
        writer.setIndent("");
        jsonBindings.writeDescribedObject(writer, process);
        writer.flush();
    }

    public ProcessChainBuilder createProcessChain() {
        return new ProcessChainBuilder();
    }

    public class ProcessChainBuilder {
        ReferenceImpl controlType = new ReferenceImpl("urn:osh:process:datasink:commandstream");
        ReferenceImpl sourceType = new ReferenceImpl("urn:osh:process:datasource:stream");
        ProcessHelper helper;
        AggregateProcessImpl aggregateProcess;

        ProcessChainBuilder() {
            helper = new ProcessHelper();
            aggregateProcess = new AggregateProcessImpl();
        }

        public ProcessChainBuilder uid(String uid) {
            aggregateProcess.setUniqueIdentifier(uid);
            return this;
        }

        public ProcessChainBuilder name(String name) {
            aggregateProcess.setName(name);
            return this;
        }

        public ProcessChainBuilder description(String description) {
            aggregateProcess.setDescription(description);
            return this;
        }

        /**
         * Adds output to aggregate process
         *
         * @param output DataRecord that describes output
         */
        public ProcessChainBuilder addOutput(DataRecord output) {
            aggregateProcess.addOutput(output.getName(), output);
            return this;
        }

        public ProcessChainBuilder addOutput(String name, DataRecord output) {
            aggregateProcess.addOutput(name, output);
            return this;
        }

        /**
         * Adds output list to aggregate process
         *
         * @param outputs List of outputs from a process
         */
        public ProcessChainBuilder addOutputList(IOPropertyList outputs) {
            for (AbstractSWEIdentifiable output : outputs) {
                DataComponent outputData = (DataComponent) output;
                aggregateProcess.addOutput(outputData.getName(), outputData);
            }
            return this;
        }

        /**
         * Adds input to aggregate process
         *
         * @param input DataRecord that describes input
         */
        public ProcessChainBuilder addInput(DataRecord input) {
            aggregateProcess.addInput(input.getName(), input);
            return this;
        }

        public ProcessChainBuilder addInput(String name, DataRecord input) {
            aggregateProcess.addInput(name, input);
            return this;
        }

        /**
         * Adds process to aggregate process
         *
         * @param process Class of process
         */
        public ProcessChainBuilder addProcess(String name, ExecutableProcessImpl process) throws ProcessException {
            process.init();
            SimpleProcessImpl execProcess = new SimpleProcessImpl();
            execProcess.setExecutableImpl(process);

            aggregateProcess.addComponent(name, execProcess);
            return this;
        }

        /**
         * Adds datasource to aggregate process
         *
         * @param systemUID System UID of datasource
         */
        public ProcessChainBuilder addDataSource(String name, String systemUID) {
            SimpleProcessImpl source = new SimpleProcessImpl();
            source.setTypeOf(sourceType);
            SettingsImpl settings = new SettingsImpl();
            settings.addSetValue("parameters/producerURI", systemUID);
            source.setConfiguration(settings);

            aggregateProcess.addComponent(name, source);
            return this;
        }

        /**
         * Adds control stream to aggregate process
         *
         * @param systemUID System UID of control stream
         * @param inputName Name of control stream input
         */
        public ProcessChainBuilder addControlStream(String name, String systemUID, String inputName) {
            SimpleProcessImpl control = new SimpleProcessImpl();
            control.setTypeOf(controlType);
            SettingsImpl settings = new SettingsImpl();
            settings.addSetValue("parameters/systemUID", systemUID);
            settings.addSetValue("parameters/inputName", inputName);

            control.setConfiguration(settings);

            aggregateProcess.addComponent(name, control);
            return this;
        }

        /**
         * Adds connection to link inputs to outputs or vice-versa
         *
         * @param source String of source of connection
         * @param destination String of destination of connection
         */
        public ProcessChainBuilder addConnection(String source, String destination) {
            aggregateProcess.addConnection(new LinkImpl(source, destination));
            return this;
        }

        public AggregateProcess build() {
            return aggregateProcess;
        }

    }

}