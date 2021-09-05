/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.uas.outputs;

import net.opengis.swe.v20.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

/**
 * Factory class for creating MISB ST0601 metadata SWE Common data elements
 *
 * @author Nick Garay
 * @since Oct. 6, 2020
 */
public class UasHelper extends GeoPosHelper {

    private static final Logger logger = LoggerFactory.getLogger(UasHelper.class);

    public static final String MISB_ST0601_DEF_URI_PREFIX = SWEConstants.SML_ONTOLOGY_ROOT + "misb0601/property/";
    public static final String MISB_ST0903_DEF_URI_PREFIX = SWEConstants.SML_ONTOLOGY_ROOT + "misb0903/property/";
    
    
    public Quantity createPlatformGroundSpeed() {

        return createQuantity()
                .label("Platform Ground Speed")
                .description("Speed of the airborne platform relative to the ground")
                .definition(SWEHelper.getPropertyUri("GroundSpeed"))
                .uomCode("m/s")
                .build();
    }

    public Quantity createSlantRange() {

        return createQuantity()
                .label("Slant Range")
                .description("Slant range in meters")
                .definition(MISB_ST0601_DEF_URI_PREFIX + "SlantRange")
                .uomCode("m")
                .build();
    }

    public Text createImageCoordinateSystem() {

        return createText()
                .label("Image Coordinate System")
                .description("Name of the image coordinate system used")
                .definition(MISB_ST0601_DEF_URI_PREFIX + "ImageCoordinateSystem")
                .build();
    }

    public Text createImageSourceSensor() {

        return createText()
                .label("Image Source Sensor")
                .description("Name of currently active sensor")
                .definition(MISB_ST0601_DEF_URI_PREFIX + "ImageSourceSensor")
                .build();
    }

    public DataRecord createImageFrame() {

        return createRecord()
                .addField("center",
                        newLocationVectorLLA(MISB_ST0601_DEF_URI_PREFIX + "FrameCenterLocation"))
                .addField("urc",
                        newLocationVectorLatLon(MISB_ST0601_DEF_URI_PREFIX + "FrameUpperRightCornerLocation"))
                .addField("lrc",
                        newLocationVectorLatLon(MISB_ST0601_DEF_URI_PREFIX + "FrameLowerRightCornerLocation"))
                .addField("llc",
                        newLocationVectorLatLon(MISB_ST0601_DEF_URI_PREFIX + "FrameLowerLeftCornerLocation"))
                .addField("ulc",
                        newLocationVectorLatLon(MISB_ST0601_DEF_URI_PREFIX + "FrameUpperLeftCornerLocation"))
                .build();
    }

    public DataRecord createSensorParams() {

        return createRecord()
                .addField("hfov", createQuantity()
                        .label("Sensor Horizontal Field of View")
                        .description("Horizontal field of view of imaging sensor")
                        .definition(MISB_ST0601_DEF_URI_PREFIX + "HorizontalFov")
                        .uomCode("deg")
                        .build())
                .addField("vfov", createQuantity()
                        .label("Sensor Vertical Field of View")
                        .description("Vertical field of view of imaging sensor")
                        .definition(MISB_ST0601_DEF_URI_PREFIX + "VerticalFov")
                        .uomCode("deg")
                        .build())
                .build();
    }

    public Text createDataLinkVersion() {

        return createText()
                .label("UAS Datalink LS Version Number")
                .description("Version number of the UAS Datalink LS document used to generate KLV metadata")
                .definition(MISB_ST0601_DEF_URI_PREFIX + "UasDataLinkLSVersionNumber")
                .build();
    }

    public Text createPlatformDesignation() {

        return createText()
                .label("Platform Designation")
                .description("Model name for the platform")
                .definition(MISB_ST0601_DEF_URI_PREFIX + "PlatformDesignation")
                .build();
    }

    public Text createPlatformTailNumber() {

        return createText()
                .label("Platform Tail Number")
                .description("Identifier of platform as posted")
                .definition(MISB_ST0601_DEF_URI_PREFIX + "PlatformTailNumber")
                .build();
    }

    public Vector createSensorAttitude() {

        return createEulerOrientationYPR("deg")
                .definition(SWEConstants.DEF_SENSOR_ORIENT)
                .refFrame("#PLATFORM_FRAME")
                .localFrame("#GIMBAL_FRAME")
                .dataType(DataType.FLOAT)
                .build();
    }

    public Vector createPlatformHPR() {

        return createEulerOrientationNED("deg")
                .definition(SWEConstants.DEF_PLATFORM_ORIENT)
                .localFrame("#PLATFORM_FRAME")
                .dataType(DataType.FLOAT)
                .build();
    }

    public Vector createPlatformLocation() {

        return createLocationVectorLLA_MSL()
                .definition(SWEConstants.DEF_PLATFORM_LOC)
                .localFrame("#PLATFORM_FRAME")
                .build();
    }

    public Vector createSensorLocation() {

        return createLocationVectorLLA_MSL()
                .definition(SWEConstants.DEF_SENSOR_LOC)
                .localFrame("#SENSOR_FRAME")
                .build();
    }

    /*
     * Creates security data record structure according to MISB ST 0102.12
     */
    public DataRecord createSecurityDataRecord() {

        logger.debug("Creating security data record");

        DataRecord securityDataStruct = createRecord()
                .name("security")
                .label("Security Local Set")
                .description("MISB ST 0102 local let Security Metadata items")
                .definition(SWEHelper.getPropertyUri("SecurityLocalSet"))
        .addField("securityClassification", createCategory()
                .name("securityClassification")
                .label("Security Classification")
                .description("The overall security classification of the Motion Imagery Data in " +
                        "accordance with U.S. and NATO classification guidance. ")
                .definition(SWEHelper.getPropertyUri("SecurityClassification"))
                .addAllowedValues("UNCLASSIFIED", "RESTRICTED", "CONFIDENTIAL", "SECRET", "TOP SECRET")
                .build())
        .addField("releaseInstructions", createText()
                .name("releaseInstructions")
                .label("Classifying Country and Releasing Instructions Country Coding Method")
                .description("Identifies the country coding method for the Classifying Country" +
                        " and Releasing Instructions")
                .definition(SWEHelper.getPropertyUri("ReleaseInstructions"))
                .build())
        .addField("classifyingCountry", createText()
                .name("classifyingCountry")
                .label("Classifying Country")
                .description("A value for the classifying country code preceded by a double slash " +
                        "\"//.\" ")
                .definition(SWEHelper.getPropertyUri("ClassifyingCountry"))
                .build())
        .addField("sciShiInformation", createText()
                .name("sciShiInformation")
                .label("Security-SCI/SHI Information")
                .description("Sensitive Compartmented Information (SCI) / Special Handling Instructions " +
                        "(SHI) Information")
                .definition(SWEHelper.getPropertyUri("Security-SCI-SHI"))
                .build())
        .addField("caveats", createText()
                .name("caveats")
                .label("Caveats")
                .description("Pertinent caveats (or code words) from each category of the appropriate security" +
                        " entity register.")
                .definition(SWEHelper.getPropertyUri("Caveats"))
                .build())
        .addField("releasingInstructions", createText()
                .name("releasingInstructions")
                .label("Releasing Instructions")
                .description("A list of country codes to indicate the countries to which the Motion " +
                        "Imagery Data is releasable. ")
                .definition(SWEHelper.getPropertyUri("ReleasingInstructions"))
                .build())
        .addField("classifiedBy", createText()
                .name("classifiedBy")
                .label("Classified By")
                .description("Name and type of authority used to classify the Motion Imagery Data.")
                .definition(SWEHelper.getPropertyUri("ClassifiedBy"))
                .build())
        .addField("derivedFrom", createText()
                .name("derivedFrom")
                .label("Derived From")
                .description("Information about the original source of data from which the classification was " +
                        "derived.")
                .definition(SWEHelper.getPropertyUri("DerivedFrom"))
                .build())
        .addField("classificationReason", createText()
                .name("classificationReason")
                .label("Classification Reason")
                .description("The reason for classification or a citation from a document.")
                .definition(SWEHelper.getPropertyUri("ClassificationReason"))
                .build())
        .addField("declassificationDate", createText()
                .name("declassificationDate")
                .label("Declassification Date")
                .description("A date when the classified material may be automatically declassified.")
                .definition(SWEHelper.getPropertyUri("DeclassificationDate"))
                .build())
        .addField("classificationMarkingSystem", createText()
                .name("classificationMarkingSystem")
                .label("Classification and Marking System")
                .description("Classification or marking system used in the Security Metadata " +
                        "set as determined by the appropriate security entity for the country originating the data.")
                .definition(SWEHelper.getPropertyUri("ClassificationMarkingSystem"))
                .build())
        .addField("objectCountryCodingMethod", createText()
                .name("objectCountryCodingMethod")
                .label("Object Country Coding Method")
                .description("Identifies the coding method for the Object Country Code metadata.")
                .definition(SWEHelper.getPropertyUri("ObjectCountryCodingMethod"))
                .build())
        .addField("objectCountryCodingCodes", createText()
                .name("objectCountryCodingCodes")
                .label("Object Country Coding Method")
                .description("A value identifying the country (or countries), which is the object of " +
                        "the Motion Imagery Data.")
                .definition(SWEHelper.getPropertyUri("ObjectCountryCodingCodes"))
                .build())
        .addField("classificationComments", createText()
                .name("classificationComments ")
                .label("Classification Comments ")
                .description("Security related comments and format changes necessary in the future.")
                .definition(SWEHelper.getPropertyUri("ClassificationComments"))
                .build())
        .addField("version", createText()
                .name("version")
                .label("Version")
                .description("Indicates the version number of MISB ST 0102 referenced.")
                .definition(SWEHelper.getPropertyUri("Version"))
                .build())
        .addField("classifyingCountryCodingMethodVersionDate", createText()
                .name("classifyingCountryCodingMethodVersionDate")
                .label("Classifying Country and Releasing Instructions Country Coding Method Version Date ")
                .description("The effective date (promulgation date) of the source " +
                        "(FIPS 10-4, ISO 3166, GENC 2.0, or STANAG 1059) used for the Classifying Country and Releasing " +
                        "Instructions Country Coding Method.")
                .definition(SWEHelper.getPropertyUri("ClassifyingCountryCodingMethodVersionDate"))
                .build())
        .addField("objectCountryCodingMethodVersionDate", createText()
                .name("objectCountryCodingMethodVersionDate")
                .label("Object Country Coding Method Version Date")
                .description("The effective date (promulgation date) of the source " +
                        "(FIPS 10-4, ISO 3166, GENC 2.0, or STANAG 1059) used for the Object Country Coding Method.")
                .definition(SWEHelper.getPropertyUri("ObjectCountryCodingMethodVersionDate"))
                .build())
                .build();

        logger.debug("Security data record created");

        return securityDataStruct;
    }

    public Time createTimeStamp() {

        return createTime()
                .asSamplingTimeIsoUTC()
                .label("Precision Time Stamp")
                .build();
    }
}
