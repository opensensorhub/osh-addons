/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.mpegts;

/**
 * A simple data structure to hold data buffers with their timestamp computed from the TransportStream
 *
 * @author Nick Garay
 * @since Apr. 1, 2020
 */
public class DataBufferRecord {

    /**
     * The timestamp associated for the data
     */
    double presentationTimeStamp;

    /**
     * The data buffer to be stored
     */
    byte[] dataBuffer;

    /**
     * Constructor
     *
     * @param presentationTimeStamp  The presentationTimeStamp associated for the data
     * @param dataBuffer The data buffer to be stored
     */
    public DataBufferRecord(double presentationTimeStamp, byte[] dataBuffer) {

        this.presentationTimeStamp = presentationTimeStamp;
        this.dataBuffer = dataBuffer;
    }

    /**
     * Returns the timestamp associated with this data record
     *
     * @return timestamp value
     */
    public double getPresentationTimestamp() {
        return presentationTimeStamp;
    }

    /**
     * Returns the data buffer associated with this data record as a byte array
     *
     * @return the data byte array
     */
    public byte[] getDataBuffer() {
        return dataBuffer;
    }
}
