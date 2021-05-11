/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.uas.klv;

import org.sensorhub.misb.stanag4609.klv.AbstractDataSet;
import org.sensorhub.misb.stanag4609.klv.Element;
import org.sensorhub.misb.stanag4609.tags.Encoding;
import org.sensorhub.misb.stanag4609.tags.Tag;
import org.sensorhub.misb.stanag4609.tags.TagRegistry;
import org.sensorhub.misb.stanag4609.tags.TagSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Encompasses a Video Moving Target Indicator (VMTI) set allowing for 
 * extraction of elements from the set for further decoding.
 *
 * @author Alex Robin
 * @since May 10, 2021
 */
public class VmtiLocalSet extends AbstractDataSet {

    private static final Logger logger = LoggerFactory.getLogger(VmtiLocalSet.class);

    public static final TagSet VMTI_LOCAL_SET = new TagSet("06 0E 2B 34 02 0B 01 01 0E 01 03 03 06 00 00 00", "VMTI Local Set Dictionary", "Universal Label");
    
    
    private int numTargets;
    
    
    static {

        // VMTI_LOCAL_SET_UNIVERSAL_LABEL Tags
        // note: universal designators were deprecated in ST0903.5
        TagRegistry.getInstance().registerTag(new Tag(VMTI_LOCAL_SET, "-", 1, Encoding.UINT16, "Checksum", "MISB ST0601.16", "Checksum used to detect errors within a UAS Datalink LS packet"));
        TagRegistry.getInstance().registerTag(new Tag(VMTI_LOCAL_SET, "-", 2, Encoding.UINT64, "Precision Time Stamp", "MISB ST0601.16", "Timestamp for all metadata in this Local Set; used to coordinate with Motion Imagery", "microseconds"));        
        TagRegistry.getInstance().registerTag(new Tag(VMTI_LOCAL_SET, "-", 3, Encoding.UTF8, "VMTI System Name / Description", "MISB ST0903.5", "Timestamp for all metadata in this Local Set; used to coordinate with Motion Imagery"));
        TagRegistry.getInstance().registerTag(new Tag(VMTI_LOCAL_SET, "-", 4, Encoding.UINT, "VMTI LS Version Number", "MISB ST0903.5", "Version number of the VMTI LS used to generate the VMTI metadata"));
        TagRegistry.getInstance().registerTag(new Tag(VMTI_LOCAL_SET, "-", 5, Encoding.UINT, "Total Number of Targets Detected", "MISB ST0903.5", "Total number of targets detected in a frame. 0 represents no targets detected"));
        TagRegistry.getInstance().registerTag(new Tag(VMTI_LOCAL_SET, "-", 6, Encoding.UINT, "Number of Reported Targets", "MISB ST0903.5", "Number of targets reported following a culling process"));
        TagRegistry.getInstance().registerTag(new Tag(VMTI_LOCAL_SET, "-", 7, Encoding.UINT, "Motion Imagery Frame Number", "MISB ST0903.5", "Frame number identifying detected targets. Use Precision Time Stamp when available."));
        TagRegistry.getInstance().registerTag(new Tag(VMTI_LOCAL_SET, "-", 8, Encoding.UINT, "Frame Width", "MISB ST0903.5", "Width of the Motion Imagery frame in pixels"));
        TagRegistry.getInstance().registerTag(new Tag(VMTI_LOCAL_SET, "-", 9, Encoding.UINT, "Frame Height", "MISB ST0903.5", "Height of the Motion Imagery frame in pixels"));
        TagRegistry.getInstance().registerTag(new Tag(VMTI_LOCAL_SET, "-", 10, Encoding.UTF8, "VMTI Source Sensor", "MISB ST0903.5", "String of VMTI source sensor. E.g. 'EO Nose', 'EO Zoom (DLTV)'"));
        TagRegistry.getInstance().registerTag(new Tag(VMTI_LOCAL_SET, "-", 11, Encoding.IMAPB, "VMTI Horizontal FOV", "MISB ST0903.5", "Horizontal field of view of imaging sensor input to VMTI process", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(VMTI_LOCAL_SET, "-", 12, Encoding.IMAPB, "VMTI Vertical FOV", "MISB ST0903.5", "Vertical field of view of imaging sensor input to VMTI process", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(VMTI_LOCAL_SET, "-", 13, Encoding.BINARY, "MIIS ID", "MISB ST0903.5", "A Motion Imagery Identification System (MIIS) Core Identifier conformant with MISB ST 1204"));
        TagRegistry.getInstance().registerTag(new Tag(VMTI_LOCAL_SET, "-", 101, Encoding.SERIES, "VTarget Series", "MISB ST0903.5", "VTarget Packs ordered as a Series"));
        TagRegistry.getInstance().registerTag(new Tag(VMTI_LOCAL_SET, "-", 102, Encoding.SERIES, "Algorithm Series", "MISB ST0903.5", "Series of one or more Algorithm LS"));
        TagRegistry.getInstance().registerTag(new Tag(VMTI_LOCAL_SET, "-", 103, Encoding.SERIES, "Ontology Series", "MISB ST0903.5", "Series of one or more Ontology LS"));
        
    }

    /**
     * Constructor
     *
     * @param length  the length in bytes of the set
     * @param payload the content of the set as raw bytes
     */
    public VmtiLocalSet(int length, byte[] payload) {

        this.tagSet = VMTI_LOCAL_SET;

        this.length = length;
        this.payload = payload;

        this.designator = VMTI_LOCAL_SET.getDesignator();
        this.embeddedDataLength = 0;
    }

    @Override
    public HashMap<Tag, Object> decode() {

        HashMap<Tag, Object> valuesMap = new LinkedHashMap<>();

        // Extract decoded elements from the set
        while (hasMoreElements()) {

            // Read data from next element in set
            Element dataElement = getNextElement();

            // Get the element tag
            Tag tag = dataElement.getTag();
            
            if (TagSet.UNKNOWN != tag.getMemberOf()) {

                // Decode element data values as raw data, these values get converted
                // to correct type based on tag specific formulas below
                Object value = dataElement.unpackData();

                // Map data to corresponding output in correct data format
                switch (tag.getLocalSetTag()) {

                    case 0x01: // "Checksum"
                        break;

                    case 0x02: // "Precision Time Stamp"
                        var precisionTimeStamp = convertToTimeInMillis((long) value) / 1000.0;
                        valuesMap.put(tag, precisionTimeStamp);
                        break;

                    case 3: // VMTI System Name
                    case 4: // VMTI LS Version Number
                    case 5: // Total Number of Targets Detected
                    case 6: // Number of Reported Targets
                    case 7: // Motion Imagery Frame Number
                    case 8: // Frame Width
                    case 9: // Frame Height
                    case 10: // VMTI Source Sensor
                        valuesMap.put(tag, value);
                        if (tag.getLocalSetTag() == 5 || tag.getLocalSetTag() == 6)
                            numTargets = ((Long)value).intValue();
                        break;
                        
                    case 11: // VMTI Horizontal FOV
                    case 12: // VMTI Horizontal FOV
                        var fov = reverse_imapb(0, 180, 2, (byte[])value);
                        valuesMap.put(tag, fov);
                        break;
                        
                    case 101:
                        var targetSeries = new VmtiTargetPack.Series(((byte[]) value).length, (byte[]) value, numTargets);
                        valuesMap.put(tag, targetSeries.decodeSeries());
                        break;
                        
                    default:
                        logger.error("Unsupported tag: {}", tag.getLocalSetTag());
                        break;
                }

            } else {

                logger.info("Unknown VMTI Set tag: \n \t{}", dataElement.toJsonString());
            }
            
            logger.debug("Tag {}: {} = {}", tag.getLocalSetTag(), tag.getName(), valuesMap.get(tag));
        }

        return valuesMap;
    }
    
}
