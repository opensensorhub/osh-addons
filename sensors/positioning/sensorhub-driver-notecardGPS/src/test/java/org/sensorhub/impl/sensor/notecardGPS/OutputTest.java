/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.notecardGPS;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.TextEncoding;
import org.junit.Test;

import static org.junit.Assert.*;

public class OutputTest extends TestBase {
    @Test
    public void getRecordDescription() {
        assertTrue(output.getRecordDescription() instanceof DataRecord);

        // Verify that the output's record description has the expected properties.
        var recordDescription = (DataRecord) output.getRecordDescription();
//        assertEquals(notecardGPSOutput.SENSOR_OUTPUT_NAME, recordDescription.getName());
//        assertEquals(notecardGPSOutput.SENSOR_OUTPUT_LABEL, recordDescription.getLabel());
//        assertEquals(notecardGPSOutput.SENSOR_OUTPUT_DESCRIPTION, recordDescription.getDescription());

        // Verify that the record description contains the expected fields.
        assertNotNull(recordDescription.getField("sampleTime"));
        assertEquals("Sample Time", recordDescription.getField("sampleTime").getLabel());
        assertEquals("Time of data collection", recordDescription.getField("sampleTime").getDescription());

        assertNotNull(recordDescription.getField("data"));
        assertEquals("Example Data", recordDescription.getField("data").getLabel());
    }

    @Test
    public void getRecommendedEncoding() {
        assertTrue(output.getRecommendedEncoding() instanceof TextEncoding);
    }

    @Test
    public void setData() {
//        sensor.stopProcessing();

        // Set some sample data.
        long sampleTime = System.currentTimeMillis();
        String data = "Test Data";
//        output.setData(sampleTime, data);

        // Get the latest record and pair it with the record description for data access.
        DataBlock latestRecord = output.getLatestRecord();
        DataComponent recordDescription = output.getRecordDescription().copy();
        recordDescription.setData(latestRecord);

        // Verify that the latest record contains the expected data.
        assertEquals(sampleTime / 1000d, recordDescription.getComponent("sampleTime").getData().getDoubleValue(), 0.001);
        assertEquals(data, recordDescription.getComponent("data").getData().getStringValue());
    }
}
