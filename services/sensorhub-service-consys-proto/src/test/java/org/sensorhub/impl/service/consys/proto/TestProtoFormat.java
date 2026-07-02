/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

Author: Ian Patterson <ian.patterson@georobotix.us>

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.consys.proto;

import static org.junit.Assert.*;
import org.junit.Test;
import org.vast.swe.SWEHelper;


/**
 * {@link ProtoFormat#isCompatible} / {@link ProtoFormat#canEncode}: swe+proto is
 * advertised only for structures the writer + codec can actually round-trip, so
 * an unsupported datastream doesn't list a format that then 500s on encode.
 */
public class TestProtoFormat
{
    @Test
    public void advertisesSupportedStructures()
    {
        var swe = new SWEHelper();
        assertTrue(ProtoFormat.canEncode(
            swe.createRecord().addField("a", swe.createQuantity().build()).build()));

        // a fixed-size array is supported
        assertTrue(ProtoFormat.canEncode(swe.createRecord()
            .addField("arr", swe.createArray().withFixedSize(3)
                .withElement("v", swe.createQuantity().build()))
            .build()));
    }


    @Test
    public void rejectsNonFlatArray()
    {
        var swe = new SWEHelper();
        // a fixed array whose element record hides a DataChoice → DataBlockList,
        // not flat-addressable: the writer would emit it but encode would 500, so
        // it must not be advertised
        var rec = swe.createRecord()
            .addField("rows", swe.createArray().withFixedSize(2)
                .withElement("row", swe.createRecord()
                    .addField("c", swe.createChoice()
                        .addItem("a", swe.createCount().build())
                        .addItem("b", swe.createQuantity().build()).build())
                    .build()))
            .build();
        assertFalse(ProtoFormat.canEncode(rec));
    }
}
