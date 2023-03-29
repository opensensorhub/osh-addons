/*
 *  The contents of this file are subject to the Mozilla Public License, v. 2.0.
 *  If a copy of the MPL was not distributed with this file, You can obtain one
 *  at http://mozilla.org/MPL/2.0/.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the License.
 *
 *  Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 */
package org.sensorhub.impl.ros.output;

import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataStream;
import org.sensorhub.api.sensor.ISensorModule;
import org.slf4j.Logger;

/**
 * Provides base functionality for defining and publishing sensor video outputs
 * from ROS nodes.  This class needs to be extended and definitions provided for
 * {@link #defineRecordStructure} and {@link #onNewMessage} to process ROS
 * messages and generate SWE Common data records from the received samples/observations.
 *
 * @param <SensorType> {@link ISensorModule} based output class
 * @author Nicolas Garay
 * @since Mar 20, 2023
 */
public abstract class RosVideoOutput<SensorType extends ISensorModule<?>> extends BaseRosOutput<SensorType> {


    /**
     * SWE Common Data Stream
     */
    protected DataStream dataStream;

    /**
     * SWE Common Data Encoding
     */
    protected DataEncoding dataEncoding;

    /**
     * Constructor
     *
     * @param name         Name of the output
     * @param parentSensor Parent sensor owning this output definition
     * @param log          Instance of the logger to use
     */
    protected RosVideoOutput(String name, SensorType parentSensor, Logger log) {
        super(name, parentSensor, log);
    }

    /**
     * Retrieves the composed record description
     *
     * @return the composed record description
     */
    @Override
    public DataComponent getRecordDescription() {

        return dataStream.getElementType();
    }

    /**
     * Retrieves the data encoding for records produced by this output class
     *
     * @return The data encoding
     */
    @Override
    public DataEncoding getRecommendedEncoding() {

        return dataEncoding;
    }
}
