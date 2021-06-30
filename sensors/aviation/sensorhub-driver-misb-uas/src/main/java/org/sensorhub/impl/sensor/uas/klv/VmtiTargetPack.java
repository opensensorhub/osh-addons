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
import org.sensorhub.misb.stanag4609.klv.AbstractSeries;
import org.sensorhub.misb.stanag4609.klv.Element;
import org.sensorhub.misb.stanag4609.klv.ImapB;
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
public class VmtiTargetPack extends AbstractDataSet {

    public static final TagSet VTARGET_PACK_LOCAL_SET = new TagSet("-", "VTarget Pack Local Set Dictionary", "Local Label");
    
    private static final Logger logger = LoggerFactory.getLogger(VmtiTargetPack.class);
    
    private static final ImapB LOC_OFFSET_IMAPB_FUNC = new ImapB(-19.2, 19.2, 3);
    private static final ImapB HAE_IMAPB_FUNC = new ImapB(-900, 19000, 2);
    
    
    public static class Series extends AbstractSeries {

        public Series(int length, byte[] payload, int numElts) {
            super(length, payload, numElts);
        }

        @Override
        protected HashMap<Tag, Object> decodeElement(byte[] payload) {
            var targetPackSet = new VmtiTargetPack(payload.length, payload);
            return targetPackSet.decode();
        }        
    }
    
    
    static {

        TagRegistry.getInstance().registerTag(new Tag(VTARGET_PACK_LOCAL_SET, "-", 0, Encoding.UINT, "Target ID", "MISB ST0903.5", "Mandatory BER-OID encoded first value in a VTarget Pack"));
        TagRegistry.getInstance().registerTag(new Tag(VTARGET_PACK_LOCAL_SET, "-", 1, Encoding.UINT, "Target Centroid", "MISB ST0903.5", "Defines the position of the target within the Motion Imagery frame in pixels"));
        TagRegistry.getInstance().registerTag(new Tag(VTARGET_PACK_LOCAL_SET, "-", 2, Encoding.UINT, "Boundary Top Left", "MISB ST0903.5", "Position in pixels of the top left corner of the target bounding box within the Motion Imagery Frame"));
        TagRegistry.getInstance().registerTag(new Tag(VTARGET_PACK_LOCAL_SET, "-", 3, Encoding.UINT, "Boundary Bottom Right", "MISB ST0903.5", "Position in pixels of the bottom right corner of the target bounding box within the Motion Imagery Frame"));
        TagRegistry.getInstance().registerTag(new Tag(VTARGET_PACK_LOCAL_SET, "-", 4, Encoding.UINT, "Target Priority", "MISB ST0903.5", "Priority or validity of target based on criteria within the VMTI system"));
        TagRegistry.getInstance().registerTag(new Tag(VTARGET_PACK_LOCAL_SET, "-", 5, Encoding.UINT, "Target Confidence Level", "MISB ST0903.5", "Confidence level of target based on criteria within the VMTI system"));
        TagRegistry.getInstance().registerTag(new Tag(VTARGET_PACK_LOCAL_SET, "-", 6, Encoding.UINT, "Target History", "MISB ST0903.5", "Number of previous times the same target detected"));
        TagRegistry.getInstance().registerTag(new Tag(VTARGET_PACK_LOCAL_SET, "-", 7, Encoding.UINT, "Percentage of Target Pixels", "MISB ST0903.5", "Percentage of pixels within the bounding box detected to be target pixels rather than background pixels"));
        TagRegistry.getInstance().registerTag(new Tag(VTARGET_PACK_LOCAL_SET, "-", 8, Encoding.UINT, "Target Color", "MISB ST0903.5", "Dominant color of the target"));
        TagRegistry.getInstance().registerTag(new Tag(VTARGET_PACK_LOCAL_SET, "-", 9, Encoding.UINT, "Target Intensity", "MISB ST0903.5", "Dominant Intensity of the target"));
        TagRegistry.getInstance().registerTag(new Tag(VTARGET_PACK_LOCAL_SET, "-", 10, Encoding.IMAPB, "Target Location Offset Lat", "MISB ST0903.5", "Latitude offset for target from frame center latitude (used with MISB ST 0601)"));
        TagRegistry.getInstance().registerTag(new Tag(VTARGET_PACK_LOCAL_SET, "-", 11, Encoding.IMAPB, "Target Location Offset Lon", "MISB ST0903.5", "Longitude offset for target from frame center latitude (used with MISB ST 0601)"));
        TagRegistry.getInstance().registerTag(new Tag(VTARGET_PACK_LOCAL_SET, "-", 12, Encoding.IMAPB, "Target Hae", "MISB ST0903.5", "Height of target in meters above WGS84 Ellipsoid"));
        TagRegistry.getInstance().registerTag(new Tag(VTARGET_PACK_LOCAL_SET, "-", 13, Encoding.IMAPB, "Boundary Top Left Lat Offset", "MISB ST0903.5", "Latitude offset for top left corner of target bounding box"));
        TagRegistry.getInstance().registerTag(new Tag(VTARGET_PACK_LOCAL_SET, "-", 14, Encoding.IMAPB, "Boundary Top Left Lon Offset", "MISB ST0903.5", "Longitude offset for top left corner of target bounding box"));
        TagRegistry.getInstance().registerTag(new Tag(VTARGET_PACK_LOCAL_SET, "-", 15, Encoding.IMAPB, "Boundary Bottom Right Lat Offset", "MISB ST0903.5", "Latitude offset for bottom right corner of target bounding box"));
        TagRegistry.getInstance().registerTag(new Tag(VTARGET_PACK_LOCAL_SET, "-", 16, Encoding.IMAPB, "Boundary Bottom Right Lon Offset", "MISB ST0903.5", "Longitude offset for bottom right corner of target bounding box"));
        TagRegistry.getInstance().registerTag(new Tag(VTARGET_PACK_LOCAL_SET, "-", 17, Encoding.UINT, "Target Location", "MISB ST0903.5", "Location of the target (latitude, longitude, & height above WGS84 Ellipsoid), with sigma and rho values"));
        TagRegistry.getInstance().registerTag(new Tag(VTARGET_PACK_LOCAL_SET, "-", 18, Encoding.SERIES, "Target Boundary Series", "MISB ST0903.5", "Boundary around the target"));
        TagRegistry.getInstance().registerTag(new Tag(VTARGET_PACK_LOCAL_SET, "-", 19, Encoding.UINT, "Centroid Pix Row", "MISB ST0903.5", "Specifies the row in pixels of the target centroid within the Motion Imagery Frame"));
        TagRegistry.getInstance().registerTag(new Tag(VTARGET_PACK_LOCAL_SET, "-", 20, Encoding.UINT, "Centroid Pix Col", "MISB ST0903.5", "Specifies the column in pixels of the target centroid within the Motion Imagery Frame"));
        TagRegistry.getInstance().registerTag(new Tag(VTARGET_PACK_LOCAL_SET, "-", 21, Encoding.DLP, "FPA Index", "MISB ST0903.5", "Specifies the column and the row of a sensor Focal Plane Array (FPA) in a twodimensional array of FPAs"));
        TagRegistry.getInstance().registerTag(new Tag(VTARGET_PACK_LOCAL_SET, "-", 22, Encoding.UINT, "Algorithm ID", "MISB ST0903.5", "Identifier indicating which algorithm in Algorithm Series detected this target"));
        TagRegistry.getInstance().registerTag(new Tag(VTARGET_PACK_LOCAL_SET, "-", 101, Encoding.UINT, "vMask", "MISB ST0903.5", "Local Set to include a mask for delineating the perimeter of the target"));
        TagRegistry.getInstance().registerTag(new Tag(VTARGET_PACK_LOCAL_SET, "-", 102, Encoding.SET, "vObject", "MISB ST0903.5", "Local Set to specify the class or type of a target"));
        TagRegistry.getInstance().registerTag(new Tag(VTARGET_PACK_LOCAL_SET, "-", 103, Encoding.SET, "vFeature", "MISB ST0903.5", "Local Set to include features about the target"));
        TagRegistry.getInstance().registerTag(new Tag(VTARGET_PACK_LOCAL_SET, "-", 104, Encoding.SET, "vTracker", "MISB ST0903.5", "Local Set to include track information about the target"));
        TagRegistry.getInstance().registerTag(new Tag(VTARGET_PACK_LOCAL_SET, "-", 105, Encoding.SET, "vChip", "MISB ST0903.5", "Local Set to include underlying pixel values for the target"));
        TagRegistry.getInstance().registerTag(new Tag(VTARGET_PACK_LOCAL_SET, "-", 106, Encoding.SERIES, "vChipSeries", "MISB ST0903.5", "Series of one or more VChip LS"));
        TagRegistry.getInstance().registerTag(new Tag(VTARGET_PACK_LOCAL_SET, "-", 107, Encoding.SERIES, "vObjectSeries", "MISB ST0903.5", "Series of one or more VObject LS"));
        
    }

    /**
     * Constructor
     *
     * @param length  the length in bytes of the set
     * @param payload the content of the set as raw bytes
     */
    public VmtiTargetPack(int length, byte[] payload) {

        this.tagSet = VTARGET_PACK_LOCAL_SET;

        this.length = length;
        this.payload = payload;

        this.designator = VTARGET_PACK_LOCAL_SET.getDesignator();
        this.embeddedDataLength = 0;
    }

    @Override
    public HashMap<Tag, Object> decode() {

        HashMap<Tag, Object> valuesMap = new LinkedHashMap<>();
        
        // first decode mandatory target ID
        int targetID = decodeOID();
        Tag tag = TagRegistry.getInstance().getByTagSetAndId(VTARGET_PACK_LOCAL_SET, (byte)0);
        valuesMap.put(tag, targetID);        
        if (logger.isTraceEnabled())
            logger.trace("Tag {}: {} = {}", tag.getLocalSetTag(), tag.getName(), valuesMap.get(tag));
        
        // Extract decoded elements from the set
        while (hasMoreElements()) {

            // Read data from next element in set
            Element dataElement = getNextElement();

            // Get the element tag
            tag = dataElement.getTag();
            
            if (TagSet.UNKNOWN != tag.getMemberOf()) {

                // Decode element data values as raw data, these values get converted
                // to correct type based on tag specific formulas below
                Object value = dataElement.unpackData();

                // Map data to corresponding output in correct data format
                switch (tag.getLocalSetTag()) {

                    case 1:  // Target Centroid
                    case 2:  // Boundary Top Left
                    case 3:  // Boundary Bottom Right
                    case 4:  // Target Priority
                    case 5:  // Target Confidence Level
                    case 6:  // Target History
                    case 7:  // Percentage of Target Pixels
                    case 8:  // Target Color
                    case 9:  // Target Intensity
                    case 19: // Centroid Pix Row
                    case 20: // Centroid Pix Col
                        valuesMap.put(tag, value);
                        break;
                        
                    // imap values
                    case 10: // Target Location Offset Lat
                    case 11: // Target Location Offset Lon
                    case 13: // Boundary Top Left Lat Offset
                    case 14: // Boundary Top Left Lon Offset
                    case 15: // Boundary Bottom Right Lat Offset
                    case 16: // Boundary Bottom Right Lon Offset
                        var offsetDeg = LOC_OFFSET_IMAPB_FUNC.intToDouble((byte[])value);
                        valuesMap.put(tag, offsetDeg);
                        break;
                        
                    case 12: // Target Hae
                        var hae = HAE_IMAPB_FUNC.intToDouble((byte[])value);
                        valuesMap.put(tag, hae);
                        break;
                        
                    default:
                        logger.trace("Unsupported tag: {}", tag.getLocalSetTag());
                        break;
                }

            } else {

                logger.error("Unknown VMTI Set tag: \n \t{}", dataElement.toJsonString());
            }
            
            if (logger.isTraceEnabled())
                logger.trace("Tag {}: {} = {}", tag.getLocalSetTag(), tag.getName(), valuesMap.get(tag));
        }

        return valuesMap;
    }
    
    
    private int decodeOID() {
        return decodeLength();
    }
    
}
