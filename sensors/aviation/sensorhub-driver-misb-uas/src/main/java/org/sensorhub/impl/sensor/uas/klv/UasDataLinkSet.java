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
import java.util.List;

/**
 * Encompasses a UAS Data Link set allowing for extraction of elements from the set
 * for further decoding.
 *
 * @author Nick Garay
 * @since Feb. 6, 2020
 */
public class UasDataLinkSet extends AbstractDataSet {

    public static final TagSet UAS_LOCAL_SET = new TagSet("06 0E 2B 34 02 0B 01 01 0E 01 03 01 01 00 00 00", "UAS Local Set", "Universal Label");

    private static final Logger logger = LoggerFactory.getLogger(UasDataLinkSet.class);

    private static final int NUM_DESIGNATOR_BYTES = 16;

    /**
     * The checksum as computed with the data received
     */
    private final short computedChecksum;

    /**
     * The checksum as reported by the received data set
     */
    private final short reportedChecksum;

    /**
     * Holds a decoded latitude value referenced as an offset by other fields in the UAS Local Set
     */
    private double frameCenterLatitude;

    /**
     * Holds a decoded longitude value referenced as an offset by other fields in the UAS Local Set
     */
    private double frameCenterLongitude;

    /**
     * The timestamp of this UasDataLinkSet
     */
    private double precisionTimeStamp;

    static {

        // UAS_LOCAL_SET_UNIVERSAL_LABEL Tags
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 02 03 01 00 00 00", 0x01, Encoding.UINT16, "Checksum", "MISB ST0601.16", "Checksum used to detect errors within a UAS Datalink LS packet"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 03 07 02 01 01 01 05 00 00", 0x02, Encoding.UINT64, "Precision Time Stamp", "MISB ST0601.16", "Timestamp for all metadata in this Local Set; used to coordinate with Motion Imagery", "microseconds"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 04 01 03 00 00 00", 0x03, Encoding.UTF8, "Mission ID", "MISB ST0601.16", "Descriptive mission identifier to distinguish event or sortie"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 04 01 02 00 00 00", 0x04, Encoding.UTF8, "Platform Tail Number", "MISB ST0601.16", "Identifier of platform as posted"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 07 07 01 10 01 06 00 00 00", 0x05, Encoding.UINT16, "Platform Heading Angle", "MISB ST0601.16", "Aircraft heading angle", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 07 07 01 10 01 05 00 00 00", 0x06, Encoding.INT16, "Platform Pitch Angle", "MISB ST0601.16", "Aircraft pitch angle", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 07 07 01 10 01 04 00 00 00", 0x07, Encoding.INT16, "Platform Roll Angle", "MISB ST0601.16", "Platform roll angle", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 0A 00 00 00", 0x08, Encoding.UINT8, "Platform True Airspeed", "MISB ST0601.16", "True airspeed (TAS) of platform", "m/s"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 0B 00 00 00", 0x09, Encoding.UINT8, "Platform Indicated Airspeed", "MISB ST0601.16", "Indicated airspeed (IAS) of platform", "m/s"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 01 01 20 01 00 00 00 00", 0x0A, Encoding.UTF8, "Platform Designation", "MISB ST0601.16", "Model name for the platform"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 04 20 01 02 01 01 00 00", 0x0B, Encoding.UTF8, "Image Source Sensor", "MISB ST0601.16", "Name of currently active sensor"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 07 01 01 01 00 00 00 00", 0x0C, Encoding.UTF8, "Image Coordinate System", "MISB ST0601.16", "Name of the image coordinate system used"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 03 07 01 02 01 02 04 02 00", 0x0D, Encoding.INT32, "Sensor Latitude", "MISB ST0601.16", "Sensor latitude", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 03 07 01 02 01 02 06 02 00", 0x0E, Encoding.INT32, "Sensor Longitude", "MISB ST0601.16", "Sensor longitude", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 07 01 02 01 02 02 00 00", 0x0F, Encoding.UINT16, "Sensor True Altitude", "MISB ST0601.16", "Altitude of sensor as measured from Mean Sea Level (MSL)", "m"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 02 04 20 02 01 01 08 00 00", 0x10, Encoding.UINT16, "Sensor Horizontal Field of View", "MISB ST0601.16", "Horizontal field of view of selected imaging sensor", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 07 04 20 02 01 01 0A 01 00", 0x11, Encoding.UINT16, "Sensor Vertical Field of View", "MISB ST0601.16", "Vertical field of view of selected imaging sensor", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 02 04 00 00 00", 0x12, Encoding.UINT32, "Sensor Relative Azimuth Angle", "MISB ST0601.16", "Relative rotation angle of sensor to platform longitudinal axis", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 02 05 00 00 00", 0x13, Encoding.INT32, "Sensor Relative Elevation Angle", "MISB ST0601.16", "Relative elevation angle of sensor to platform longitudinal-transverse plane", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 02 06 00 00 00", 0x14, Encoding.UINT32, "Sensor Relative Roll Angle", "MISB ST0601.16", "Relative roll angle of sensor to aircraft platform", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 07 01 08 01 01 00 00 00", 0x15, Encoding.UINT32, "Slant Range", "MISB ST0601.16", "Slant range in meters", "m"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 07 01 09 02 01 00 00 00", 0x16, Encoding.UINT16, "Target Width", "MISB ST0601.16", "Target width within sensor field of view", "m"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 07 01 02 01 03 02 00 00", 0x17, Encoding.INT32, "Frame Center Latitude", "MISB ST0601.16", "Terrain latitude of frame center", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 07 01 02 01 03 04 00 00", 0x18, Encoding.INT32, "Frame Center Longitude", "MISB ST0601.16", "Terrain longitude of frame center", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 0A 07 01 02 01 03 16 00 00", 0x19, Encoding.UINT16, "Frame Center Elevation", "MISB ST0601.16", "Terrain elevation at frame center relative to Mean Sea Level (MSL)", "m"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 03 07 01 02 01 03 07 01 00", 0x1A, Encoding.INT16, "Offset Corner Latitude Point 1", "MISB ST0601.16", "Frame latitude offset for upper left corner", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 03 07 01 02 01 03 0B 01 00", 0x1B, Encoding.INT16, "Offset Corner Longitude Point 1", "MISB ST0601.16", "Frame longitude offset for upper left corner", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 03 07 01 02 01 03 08 01 00", 0x1C, Encoding.INT16, "Offset Corner Latitude Point 2", "MISB ST0601.16", "Frame latitude offset for upper right corner", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 03 07 01 02 01 03 0C 01 00", 0x1D, Encoding.INT16, "Offset Corner Longitude Point 2", "MISB ST0601.16", "Frame longitude offset for upper right corner", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 03 07 01 02 01 03 09 01 00", 0x1E, Encoding.INT16, "Offset Corner Latitude Point 3", "MISB ST0601.16", "Frame latitude offset for lower right corner", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 03 07 01 02 01 03 0D 01 00", 0x1F, Encoding.INT16, "Offset Corner Longitude Point 3", "MISB ST0601.16", "Frame longitude offset for lower right corner", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 03 07 01 02 01 03 0A 01 00", 0x20, Encoding.INT16, "Offset Corner Latitude Point 4", "MISB ST0601.16", "Frame latitude offset for lower left corner", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 03 07 01 02 01 03 0E 01 00", 0x21, Encoding.INT16, "Offset Corner Longitude Point 4", "MISB ST0601.16", "Frame longitude offset for lower left corner", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 0C 00 00 00", 0x22, Encoding.UINT8, "Icing Detected", "MISB ST0601.16", "Flag for icing detected at aircraft location", "code"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 0D 00 00 00", 0x23, Encoding.UINT16, "Wind Direction", "MISB ST0601.16", "Wind direction at aircraft location", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 0E 00 00 00", 0x24, Encoding.UINT8, "Wind Speed", "MISB ST0601.16", "Wind speed at aircraft location", "m/s"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 0F 00 00 00", 0x25, Encoding.UINT16, "Static Pressure", "MISB ST0601.16", "Static pressure at aircraft location", "mbar"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 10 00 00 00", 0x26, Encoding.UINT16, "Density Altitude", "MISB ST0601.16", "Density altitude at aircraft location", "m"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 11 00 00 00", 0x27, Encoding.INT8, "Outside Air Temperature", "MISB ST0601.16", "Temperature outside of aircraft", "degC"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 03 02 00 00 00", 0x28, Encoding.INT32, "Target Location Latitude", "MISB ST0601.16", "Calculated target latitude", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 03 03 00 00 00", 0x29, Encoding.INT32, "Target Location Longitude", "MISB ST0601.16", "Calculated target longitude", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 03 04 00 00 00", 0x2A, Encoding.UINT16, "Target Location Elevation", "MISB ST0601.16", "Calculated target elevation", "m"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 03 05 00 00 00", 0x2B, Encoding.UINT8, "Target Track Gate Width", "MISB ST0601.16", "Tracking gate width (x value) of tracked target within field of view", "Pixels"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 03 06 00 00 00", 0x2C, Encoding.UINT8, "Target Track Gate Height", "MISB ST0601.16", "Tracking gate height (y value) of tracked target within field of view", "Pixels"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 03 07 00 00 00", 0x2D, Encoding.UINT16, "Target Error Estimate - CE90", "MISB ST0601.16", "Circular error 90 (CE90) is the estimated error distance in the horizontal direction", "m"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 03 08 00 00 00", 0x2E, Encoding.UINT16, "Target Error Estimate - LE90", "MISB ST0601.16", "Lateral error 90 (LE90) is the estimated error distance in the vertical (or lateral) direction", "m"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 03 01 00 00 00", 0x2F, Encoding.UINT8, "Generic Flag Data", "MISB ST0601.16", "Generic metadata flags"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 02 03 01 01 0E 01 03 03 02 00 00 00", 0x30, Encoding.SET, "Security Local Set", "MISB ST0601.16", "MISB ST 0102 local let Security Metadata items"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 01 00 00 00", 0x31, Encoding.UINT16, "Differential Pressure", "MISB ST0601.16", "Differential pressure at aircraft location", "mbar"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 02 00 00 00", 0x32, Encoding.INT16, "Platform Angle of Attack", "MISB ST0601.16", "Platform attack angle", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 03 00 00 00", 0x33, Encoding.INT16, "Platform Vertical Speed", "MISB ST0601.16", "Vertical speed of the aircraft relative to zenith", "m/s"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 04 00 00 00", 0x34, Encoding.INT16, "Platform Sideslip Angle", "MISB ST0601.16", "Angle between the platform longitudinal axis and relative wind", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 02 02 00 00 00", 0x35, Encoding.UINT16, "Airfield Barometric Pressure", "MISB ST0601.16", "Local pressure at airfield of known height", "mbar"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 02 03 00 00 00", 0x36, Encoding.UINT16, "Airfield Elevation", "MISB ST0601.16", "Elevation of airfield corresponding to Airfield Barometric Pressure", "m"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 09 00 00 00", 0x37, Encoding.UINT8, "Relative Humidity", "MISB ST0601.16", "Relative humidity at aircraft location", "%"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 05 00 00 00", 0x38, Encoding.UINT8, "Platform Ground Speed", "MISB ST0601.16", "Speed projected to the ground of an airborne platform passing overhead", "m/s"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 06 00 00 00", 0x39, Encoding.UINT32, "Ground Range", "MISB ST0601.16", "Horizontal distance from ground position of aircraft relative to nadir, and target of interest", "m"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 07 00 00 00", 0x3A, Encoding.UINT16, "Platform Fuel Remaining", "MISB ST0601.16", "Remaining fuel on airborne platform", "kg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 04 01 01 00 00 00", 0x3B, Encoding.UTF8, "Platform Call Sign", "MISB ST0601.16", "Call sign of platform or operating unit"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 12 00 00 00", 0x3C, Encoding.UINT16, "Weapon Load", "MISB ST0601.16", "Current weapons stored on aircraft"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 13 00 00 00", 0x3D, Encoding.UINT8, "Weapon Fired", "MISB ST0601.16", "Indication when a particular weapon is released"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 02 02 01 00 00 00", 0x3E, Encoding.UINT16, "Laser PRF Code", "MISB ST0601.16", "A laser's Pulse Repetition Frequency (PRF) code used to mark a target"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 02 02 02 00 00 00", 0x3F, Encoding.UINT8, "Sensor Field of View Name", "MISB ST0601.16", "Sensor field of view names"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 08 00 00 00", 0x40, Encoding.UINT16, "Platform Magnetic Heading", "MISB ST0601.16", "Aircraft magnetic heading angle", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 02 03 03 00 00 00", 0x41, Encoding.UINT8, "UAS Datalink LS Version Number", "MISB ST0601.16", "Version number of the UAS Datalink LS document used to generate KLV metadata"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 02 05 01 01 0E 01 03 03 14 00 00 00", 0x42, Encoding.NONE, "Deprecated", "MISB ST0601.16", "This item has been deprecated."));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 14 00 00 00", 0x43, Encoding.INT32, "Alternate Platform Latitude", "MISB ST0601.16", "Alternate platform latitude", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 15 00 00 00", 0x44, Encoding.INT32, "Alternate Platform Longitude", "MISB ST0601.16", "Alternate platform longitude", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 16 00 00 00", 0x45, Encoding.UINT16, "Alternate Platform Altitude", "MISB ST0601.16", "Altitude of alternate platform as measured from Mean Sea Level (MSL)", "m"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 17 00 00 00", 0x46, Encoding.UTF8, "Alternate Platform Name", "MISB ST0601.16", "Name of alternate platform connected to UAS"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 18 00 00 00", 0x47, Encoding.UINT16, "Alternate Platform Heading", "MISB ST0601.16", "Heading angle of alternate platform connected to UAS", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 07 02 01 02 07 01 00 00", 0x48, Encoding.UINT64, "Event Start Time - UTC", "MISB ST0601.16", "Start time of scene, project, event, mission, editing event, license, publication, etc.", "microseconds"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 02 0B 01 01 0E 01 03 01 02 00 00 00", 0x49, Encoding.SET, "RVT Local Set", "MISB ST0601.16", "RVT Local Set metadata items"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 02 0B 01 01 0E 01 03 03 06 00 00 00", 0x4A, Encoding.SET, "VMTI Local Set", "MISB ST0601.16", "VMTI Local Set metadata items"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 02 01 82 47 00 00", 0x4B, Encoding.UINT16, "Sensor Ellipsoid Height", "MISB ST0601.16", "Sensor ellipsoid height as measured from the reference WGS84 ellipsoid", "m"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 02 01 82 48 00 00", 0x4C, Encoding.UINT16, "Alternate Platform Ellipsoid Height", "MISB ST0601.16", "Alternate platform ellipsoid height as measured from the reference WGS84 Ellipsoid", "m"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 03 21 00 00 00", 0x4D, Encoding.UINT8, "Operational Mode", "MISB ST0601.16", "Indicates the mode of operations of the event portrayed"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 02 03 48 00 00 00", 0x4E, Encoding.UINT16, "Frame Center Height Above Ellipsoid", "MISB ST0601.16", "Frame center ellipsoid height as measured from the reference WGS84 ellipsoid", "m"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 02 02 7E 00 00 00", 0x4F, Encoding.INT16, "Sensor North Velocity", "MISB ST0601.16", "Northing velocity of the sensor or platform", "m/s"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 02 02 7F 00 00 00", 0x50, Encoding.INT16, "Sensor East Velocity", "MISB ST0601.16", "Easting velocity of the sensor or platform", "m/s"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 02 05 01 01 0E 01 03 02 08 00 00 00", 0x51, Encoding.DLP, "Image Horizon Pixel Pack", "MISB ST0601.16", "Location of earth-sky horizon in the Imagery"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 03 07 01 02 01 03 07 01 00", 0x52, Encoding.INT32, "Corner Latitude Point 1 (Full)", "MISB ST0601.16", "Frame latitude for upper left corner", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 03 07 01 02 01 03 0B 01 00", 0x53, Encoding.INT32, "Corner Longitude Point 1 (Full)", "MISB ST0601.16", "Frame longitude for upper left corner", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 03 07 01 02 01 03 08 01 00", 0x54, Encoding.INT32, "Corner Latitude Point 2 (Full)", "MISB ST0601.16", "Frame latitude for upper right corner", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 03 07 01 02 01 03 0C 01 00", 0x55, Encoding.INT32, "Corner Longitude Point 2 (Full)", "MISB ST0601.16", "Frame longitude for upper right corner", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 03 07 01 02 01 03 09 01 00", 0x56, Encoding.INT32, "Corner Latitude Point 3 (Full)", "MISB ST0601.16", "Frame latitude for lower right corner", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 03 07 01 02 01 03 0D 01 00", 0x57, Encoding.INT32, "Corner Longitude Point 3 (Full)", "MISB ST0601.16", "Frame longitude for lower right corner", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 03 07 01 02 01 03 0A 01 00", 0x58, Encoding.INT32, "Corner Latitude Point 4 (Full)", "MISB ST0601.16", "Frame latitude for lower left corner", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 03 07 01 02 01 03 0E 01 00", 0x59, Encoding.INT32, "Corner Longitude Point 4 (Full)", "MISB ST0601.16", "Frame longitude for lower left corner", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 07 07 01 10 01 05 00 00 00", 0x5A, Encoding.INT32, "Platform Pitch Angle (Full)", "MISB ST0601.16", "Aircraft pitch angle", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 07 07 01 10 01 04 00 00 00", 0x5B, Encoding.INT32, "Platform Roll Angle (Full)", "MISB ST0601.16", "Platform roll angle", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 02 00 00 00", 0x5C, Encoding.INT32, "Platform Angle of Attack (Full)", "MISB ST0601.16", "Platform attack angle", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 04 00 00 00", 0x5D, Encoding.INT32, "Platform Sideslip Angle (Full)", "MISB ST0601.16", "Angle between the platform longitudinal axis and relative wind", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 04 05 03 00 00 00", 0x5E, Encoding.BYTE, "MIIS Core Identifier", "MISB ST0601.16", "MISB ST 1204 MIIS Core Identifier binary value"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 02 0B 01 01 0E 01 03 03 0D 00 00 00", 0x5F, Encoding.SET, "SAR Motion Imagery Local Set", "MISB ST0601.16", "MISB ST 1206 SAR Motion Imagery Metadata Local Set metadata items"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 07 01 09 02 01 00 00 00", 0x60, Encoding.IMAPB, "Target Width Extended", "MISB ST0601.16", "Target width within sensor field of view", "m"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 02 0B 01 01 0E 01 03 03 0C 00 00 00", 0x61, Encoding.SET, "Range Image Local Set", "MISB ST0601.16", "MISB ST 1002 Range Imaging Local Set metadata items"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 02 0B 01 01 0E 01 03 03 01 00 00 00", 0x62, Encoding.SET, "Geo-Registration Local Set", "MISB ST0601.16", "MISB ST 1601 Geo-Registration Local Set metadata items"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 02 0B 01 01 0E 01 03 03 02 00 00 00", 0x63, Encoding.SET, "Composite Imaging Local Set", "MISB ST0601.16", "MISB ST 1602 Composite Imaging Local Set metadata items"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 02 0B 01 01 0E 01 03 03 03 00 00 00", 0x64, Encoding.SET, "Segment Local Set", "MISB ST0601.16", "MISB ST 1607 Local Set metadata items, used to enable metadata sharing"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 02 0B 01 01 0E 01 03 03 03 01 00 00", 0x65, Encoding.SET, "Amend Local Set", "MISB ST0601.16", "MISB ST 1607 Amend Local Set metadata items, used to provide metadata corrections"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 02 05 01 01 0E 01 03 03 21 00 00 00", 0x66, Encoding.FLP, "SDCC-FLP", "MISB ST0601.16", "MISB ST 1010 Floating Length Pack (FLP) metadata item, providing Standard Deviation and Cross Correlation (SDCC) metadata"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 10 00 00 00", 0x67, Encoding.IMAPB, "Density Altitude Extended", "MISB ST0601.16", "Density altitude above MSL at aircraft location", "m"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 02 01 82 47 00 00", 0x68, Encoding.IMAPB, "Sensor Ellipsoid Height Extended", "MISB ST0601.16", "Sensor ellipsoid height extended as measured from the reference WGS84 ellipsoid", "m"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 02 01 82 48 00 00", 0x69, Encoding.IMAPB, "Alternate Platform Ellipsoid Height Extended", "MISB ST0601.16", "Alternate platform ellipsoid height extended as measured from the reference WGS84 ellipsoid", "m"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 04 03 03 00 00 00", 0x6A, Encoding.UTF8, "Stream Designator", "MISB ST0601.16", "A second designation given to a sortie"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 04 03 03 00 00 00", 0x6B, Encoding.UTF8, "Operational Base", "MISB ST0601.16", "Name of the operational base hosting the platform"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 04 03 03 00 00 00", 0x6C, Encoding.UTF8, "Broadcast Source", "MISB ST0601.16", "Name of the source, where the Motion Imagery is first broadcast"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 30 00 00 00", 0x6D, Encoding.IMAPB, "Range To Recovery Location", "MISB ST0601.16", "Distance from current position to airframe recovery position", "km"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 31 00 00 00", 0x6E, Encoding.UINT, "Time Airborne", "MISB ST0601.16", "Number of seconds aircraft has been airborne", "s"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 32 00 00 00", 0x6F, Encoding.UINT, "Propulsion Unit Speed", "MISB ST0601.16", "The speed the engine (or electric motor) is rotating at", "RPM"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 33 00 00 00", 0x70, Encoding.IMAPB, "Platform Course Angle", "MISB ST0601.16", "Direction the aircraft is moving relative to True North", "deg"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 34 00 00 00", 0x71, Encoding.IMAPB, "Altitude AGL", "MISB ST0601.16", "Above Ground Level (AGL) height above the ground/water", "m"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 35 00 00 00", 0x72, Encoding.IMAPB, "Radar Altimeter", "MISB ST0601.16", "Height above the ground/water as reported by a RADAR altimeter", "m"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 02 05 01 01 0E 01 03 01 01 00 00 00", 0x73, Encoding.DLP, "Control Command", "MISB ST0601.16", "Record of command from GCS to Aircraft"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 02 05 01 01 0E 01 03 02 11 00 00 00", 0x74, Encoding.DLP, "Control Command Verification List", "MISB ST0601.16", "Acknowledgement of one or more control commands were received by the platform"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 02 0A 09 00 00", 0x75, Encoding.IMAPB, "Sensor Azimuth Rate", "MISB ST0601.16", "The rate the sensors azimuth angle is changing", "deg/s"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 02 0A 0A 00 00", 0x76, Encoding.IMAPB, "Sensor Elevation Rate", "MISB ST0601.16", "The rate the sensors elevation angle is changing", "deg/s"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 02 0A 0B 00 00", 0x77, Encoding.IMAPB, "Sensor Roll Rate", "MISB ST0601.16", "The rate the sensors roll angle is changing", "deg/s"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 02 0A 0C 00 00", 0x78, Encoding.IMAPB, "On-board MI Storage Percent Full", "MISB ST0601.16", "Amount of on-board Motion Imagery storage used as a percentage of the total storage", "%"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 02 05 01 01 0E 01 03 02 0A 00 00 00", 0x79, Encoding.DLP, "Active Wavelength List", "MISB ST0601.16", "List of wavelengths in Motion Imagery"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 02 04 01 01 0E 01 03 03 02 00 00 00", 0x7A, Encoding.VLP, "Country Codes", "MISB ST0601.16", "Country codes which are associated with the platform and its operation"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 02 0A 0D 00 00", 0x7B, Encoding.UINT, "Number of NAVSATs in View", "MISB ST0601.16", "Count of navigation satellites in view of platform", "count"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 02 0A 0E 00 00", 0x7C, Encoding.UINT, "Positioning Method Source", "MISB ST0601.16", "Source of the navigation positioning information. (e.g., NAVSAT-GPS, NAVSAT-Galileo, INS)"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 37 00 00 00", 0x7D, Encoding.UINT, "Platform Status", "MISB ST0601.16", "Enumeration of operational modes of the platform (e.g., in-route, RTB)"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 02 0A 0F 00 00", 0x7E, Encoding.UINT, "Sensor Control Mode", "MISB ST0601.16", "Enumerated value for the current sensor control operational status"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 02 05 01 01 0E 01 03 02 10 00 00 00", 0x7F, Encoding.DLP, "Sensor Frame Rate Pack", "MISB ST0601.16", "Values used to compute the frame rate of the Motion Imagery at the sensor"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 02 04 01 01 0E 01 03 02 01 00 00 00", 0x80, Encoding.VLP, "Wavelengths List", "MISB ST0601.16", "List of wavelength bands provided by sensor(s)"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 04 03 03 00 00 00", 0x81, Encoding.UTF8, "Target ID", "MISB ST0601.16", "Alpha-numeric identification of a target"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 02 04 01 01 0E 01 03 01 01 00 00 00", 0x82, Encoding.VLP, "Airbase Locations", "MISB ST0601.16", "Geographic location of the take-off site and recovery site"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 01 38 00 00 00", 0x83, Encoding.UINT, "Take-off Time", "MISB ST0601.16", "Time when aircraft became airborne", "microseconds"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 02 0A 13 00 00", 0x84, Encoding.IMAPB, "Transmission Frequency", "MISB ST0601.16", "Radio frequency used to transmit the Motion Imagery", "MHz"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 02 0A 14 00 00", 0x85, Encoding.UINT, "On-board MI Storage Capacity", "MISB ST0601.16", "The total capacity of on-board Motion Imagery storage", "GB"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 02 0A 15 00 00", 0x86, Encoding.IMAPB, "Zoom Percentage", "MISB ST0601.16", "For a variable zoom system, the percentage of zoom", "%"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 04 03 03 00 00 00", 0x87, Encoding.UTF8, "Communications Method", "MISB ST0601.16", "Type of communications used with platform"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 02 0A 00 00 00", 0x88, Encoding.INT, "Leap Seconds", "MISB ST0601.16", "Number of leap seconds to adjust Precision Time Stamp (Tag 2) to UTC", "s"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 02 0A 17 00 00", 0x89, Encoding.INT, "Correction Offset", "MISB ST0601.16", "Post-flight time adjustment to correct Precision Time Stamp (Tag 2) as needed", "microseconds"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 02 04 01 01 0E 01 03 01 02 00 00 00", 0x8A, Encoding.VLP, "Payload List", "MISB ST0601.16", "List of payloads available on the Platform"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 01 02 0A 1A 00 00", 0x8B, Encoding.BYTE, "Active Payloads", "MISB ST0601.16", "List of currently active payloads from the payload list (Tag 138)"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 02 04 01 01 0E 01 03 01 03 00 00 00", 0x8C, Encoding.VLP, "Weapons Stores", "MISB ST0601.16", "List of weapon stores and status"));
        TagRegistry.getInstance().registerTag(new Tag(UAS_LOCAL_SET, "06 0E 2B 34 02 04 01 01 0E 01 03 01 04 00 00 00", 0x8D, Encoding.VLP, "Waypoint List", "MISB ST0601.16", "List of waypoints and their status"));
    }

    /**
     * Constructor
     *
     * @param length  the length in bytes of the set
     * @param payload the content of the set as raw bytes
     */
    public UasDataLinkSet(int length, byte[] payload) {

        this.tagSet = UAS_LOCAL_SET;

        this.length = length;
        this.payload = payload;

        this.reportedChecksum = (short) ((payload[length - 2] & 0xFF) << 8 | (payload[length - 1] & 0xFF));
        this.computedChecksum = computeChecksum();

        this.designator = readSetDesignator();
        this.embeddedDataLength = decodeLength();
    }

    /**
     * Retrieves the precision time stamp for this UasDataLinkSet.  This method should be invoked
     * after {@link UasDataLinkSet#decode()}, otherwise timestamp reported will be 0.0
     *
     * @return the associated reported precission time stamp
     */
    public double getPrecisionTimeStamp() {
        return precisionTimeStamp;
    }

    /**
     * Compute the checksum for the entire <code>{@link UasDataLinkSet}</code>
     *
     * @return the computed checksum
     */
    private short computeChecksum() {

        short checksum = 0;

        for (int idx = 0; idx < length - 2; ++idx) {

            checksum += (payload[idx] & 0x00FF) << (8 * ((idx + 1) % 2));
        }

        return checksum;
    }

    /**
     * Validates the checksum
     *
     * @return true if the {@link UasDataLinkSet#reportedChecksum} equals the {@link UasDataLinkSet#computedChecksum}
     * from {@link UasDataLinkSet#computeChecksum()}
     */
    public boolean validateChecksum() {

        return this.reportedChecksum == this.computedChecksum;
    }

    /**
     * Reads the set header from the stream. The header should be
     * {@link UasDataLinkSet#UAS_LOCAL_SET} in order for the set to be
     * considered for further processing.
     *
     * @return The set header.
     */
    private String readSetDesignator() {

        StringBuilder tagBuilder = new StringBuilder();

        for (int index = 0; index < NUM_DESIGNATOR_BYTES; ++index) {

            byte value = payload[position++];

            if ((value & 0x00FF) < 0x10) {

                tagBuilder.append('0');
            }

            tagBuilder.append(Integer.toHexString(value));

            if (index < NUM_DESIGNATOR_BYTES - 1) {

                tagBuilder.append(' ');
            }
        }

        return tagBuilder.toString();
    }

    /**
     * Validates the {@link UasDataLinkSet#designator} is in the set of <code>acceptedDesignators</code>
     * provided by the caller
     *
     * @param acceptedDesignators a collection of designator as 16 bytes as a hex string, each byte
     *                            separated by a space
     * @return true if this sets designator is in the set of accepted designators provided
     */
    public boolean validateDesignator(List<String> acceptedDesignators) {

        boolean isValidDesignator = false;

        for (String target : acceptedDesignators) {

            if (target.equalsIgnoreCase(designator)) {

                isValidDesignator = true;
                break;
            }
        }

        return isValidDesignator;
    }

    @Override
    public HashMap<Tag, Object> decode() {

        HashMap<Tag, Object> valuesMap = new HashMap<>();

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

                    case 0x02: // "Precision Time Stamp", "Timestamp for all metadata in this Local Set; used to coordinate with Motion Imagery", "microseconds"
                        precisionTimeStamp = convertToTimeInMillis((long) value) / 1000.0;
                        valuesMap.put(tag, precisionTimeStamp);
                        break;

                    case 0x41: // "UAS Datalink LS Version Number", "Version number of the UAS Datalink LS document used to generate KLV metadata"
                    case 0x38: // "Platform Ground Speed", "Speed projected to the ground of an airborne platform passing overhead", "m/s"
                    case 0x0A: // "Platform Designation", "Model name for the platform"
                    case 0x04: // "Platform Tail Number", "Identifier of platform as posted"
                    case 0x0B: // "Image Source Sensor", "Name of currently active sensor"
                    case 0x0C: // "Image Coordinate System", "Name of the image coordinate system used"
                        valuesMap.put(tag, value);
                        break;

                    case 0x30: // "Security Local Set", "MISB ST 0102 local let Security Metadata items"
                        AbstractDataSet securitySet = new SecurityLocalSet(((byte[]) value).length, (byte[]) value);
                        valuesMap.putAll(securitySet.decode());
                        break;

                    case 0x0D: // "Sensor Latitude", "Sensor latitude", "deg"
                        valuesMap.put(tag, convertToDouble((int) value, 180.0, 4294967294.0, 0.0));
                        break;

                    case 0x0E: // "Sensor Longitude", "Sensor longitude", "deg"
                        valuesMap.put(tag, convertToDouble((int) value, 360.0, 4294967294.0, 0.0));
                        break;

                    case 0x0F: // "Sensor True Altitude", "Altitude of sensor as measured from Mean Sea Level (MSL)", "m"
                    case 0x19: // "Frame Center Elevation", "Terrain elevation at frame center relative to Mean Sea Level (MSL)", "m"
                        valuesMap.put(tag, convertToDouble((int) value, 19900.0, 65535.0, -900.0));
                        break;

                    case 0x10: // "Sensor Horizontal Field of View", "Horizontal field of view of selected imaging sensor", "deg"
                    case 0x11: // "Sensor Vertical Field of View", "Vertical field of view of selected imaging sensor", "deg"
                        valuesMap.put(tag, convertToDouble((int) value, 180.0, 65535.0, 0.0));
                        break;

                    case 0x17: // "Frame Center Latitude", "Terrain latitude of frame center", "deg"
                        frameCenterLatitude = convertToDouble((int) value, 180.0, 4294967294.0, 0.0);
                        valuesMap.put(tag, frameCenterLatitude);
                        break;

                    case 0x18: // "Frame Center Longitude", "Terrain longitude of frame center", "deg"
                        frameCenterLongitude = convertToDouble((int) value, 360.0, 4294967294.0, 0.0);
                        valuesMap.put(tag, frameCenterLongitude);
                        break;

                    case 0x1A: // "Offset Corner Latitude Point 1", "Frame latitude offset for upper right corner", "deg"
                    case 0x1C: // "Offset Corner Latitude Point 2", "Frame latitude offset for lower right corner", "deg"
                    case 0x1E: // "Offset Corner Latitude Point 3", "Frame latitude offset for lower left corner", "deg"
                        valuesMap.put(tag, convertToDouble((short) value, 0.15, 65534.0, frameCenterLatitude));
                        break;

                    case 0x1B: // "Offset Corner Longitude Point 1", "Frame longitude offset for upper right corner", "deg"
                    case 0x1D: // "Offset Corner Longitude Point 2", "Frame longitude offset for lower right corner", "deg"
                    case 0x1F: // "Offset Corner Longitude Point 3", "Frame longitude offset for lower left corner", "deg"
                        valuesMap.put(tag, convertToDouble((short) value, 0.15, 65534.0, frameCenterLongitude));
                        break;

                    case 0x20: // "Offset Corner Latitude Point 4", "Frame latitude offset for upper left corner", "deg"
                        valuesMap.put(tag, convertToDouble((short) value, 0.15, 65534.0, frameCenterLatitude));
                        frameCenterLatitude = 0.0;
                        break;

                    case 0x21: // "Offset Corner Longitude Point 4", "Frame longitude offset for upper left corner", "deg"
                        valuesMap.put(tag, convertToDouble((short) value, 0.15, 65534.0, frameCenterLongitude));
                        frameCenterLongitude = 0.0;
                        break;

                    case 0x15: // "Slant Range", "Slant range in meters", "m"
                        valuesMap.put(tag, convertToDouble((int) value, 5000000.0, 4294967295.0, 0.0));
                        break;

                    case 0x12: // "Sensor Relative Azimuth Angle", "Relative rotation angle of sensor to platform longitudinal axis", "deg"
                    case 0x13: // "Sensor Relative Elevation Angle", "Relative elevation angle of sensor to platform longitudinal-transverse plane", "deg"
                    case 0x14: // "Sensor Relative Roll Angle", "Relative roll angle of sensor to aircraft platform", "deg"
                        valuesMap.put(tag, convertToDouble((int) value, 360.0, 4294967295.0, 0.0));
                        break;

                    case 0x05: // "Platform Heading Angle", "Aircraft heading angle", "deg"
                        valuesMap.put(tag, convertToDouble((int) value, 360.0, 65535.0, 0.0));
                        break;

                    case 0x06: // "Platform Pitch Angle", "Aircraft pitch angle", "deg"
                        valuesMap.put(tag, convertToDouble((short) value, 40.0, 65534.0, 0.0));
                        break;

                    case 0x07: // "Platform Roll Angle", "Platform roll angle", "deg"
                        valuesMap.put(tag, convertToDouble((short) value, 100.0, 65534.0, 0.0));
                        break;
                        
                    case 0x4A: // "Video Moving Target Indicator and Track Metadata", "MISB ST 0903.4 local let VMTI Metadata items"
                        AbstractDataSet vmtiSet = new VmtiLocalSet(((byte[]) value).length, (byte[]) value);
                        valuesMap.putAll(vmtiSet.decode());
                        break;

                    default:
                        logger.trace("Unsupported tag: {}", tag.getLocalSetTag());
                        break;
                }

            } else {

                logger.error("Unknown UAS Data Link Set tag: \n \t{}", dataElement.toJsonString());
            }
        }

        return valuesMap;
    }
}
