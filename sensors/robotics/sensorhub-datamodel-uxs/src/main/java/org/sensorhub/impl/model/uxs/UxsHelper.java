/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Sensia Software LLC. Portions created by the Initial
Developer are Copyright (C) 2022 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.model.uxs;

import org.vast.swe.SWEConstants;
import org.vast.swe.SWEBuilders.DataRecordBuilder;
import org.vast.swe.SWEBuilders.QuantityBuilder;
import org.vast.swe.helper.GeoPosHelper;
import net.opengis.swe.v20.DataType;


/**
 * <p>
 * Helper classes to create system metadata, datastreams and command channels
 * compatible with the common UxS data model. 
 * </p>
 *
 * @author Alex Robin
 * @since March 2022
 */
public class UxsHelper extends GeoPosHelper
{
    public static final String DEF_PLATFORM_ID = getPropertyUri("PlatformID");
    public static final String DEF_PLATFORM_TYPE = getPropertyUri("PlatformType");
    public static final String DEF_DEPTH_BELOW_MSL = getPropertyUri("DepthBelowMSL");
    public static final String DEF_DEPTH_BELOW_SURFACE = getPropertyUri("DepthBelowSurface");
    public static final String DEF_SPEED_OVER_GROUND = getPropertyUri("SpeedOverGround");
    public static final String DEF_COURSE_OVER_GROUND = getPropertyUri("CourseOverGround");
    
    public static final String CMD_URI_PREFIX = "urn:ogc:uxs:messages:";
    
    public enum HeightType
    {
        ALT_ABOVE_ELLIPSOID,
        ALT_ABOVE_MSL,
        ALT_ABOVE_SURFACE,
        DEPTH_BELOW_MSL,
        DEPTH_BELOW_SURFACE
    }
    
    
    // Vehicle Info
    
    public DataRecordBuilder createVehicleInfo(String platformTypeCodespace)
    {
        return createRecord()
            .addField("id", createText()
                .definition(DEF_PLATFORM_ID))
            .addField("name", createText())
            .addField("type", createCategory()
                .definition(DEF_PLATFORM_TYPE)
                .codeSpace(platformTypeCodespace)
                .label("Type")
                .description("Type of watercraft"));
    }
    
    
    // Position Telemetry
    
    /**
     * Create standard mechanical state record of an aerial vehicle (UAV)
     * @return A builder to set other options and build the final record 
     */
    public DataRecordBuilder createUavMechanicalState()
    {
        return createRecord()
            .label("UAV Estimated State")
            .addField("location", createLocationVectorLLA()
                .label("Platform Location"))
            .addField("attitude", createEulerOrientationNED("deg")
                .label("Platform Orientation"))
            .addField("velocity", createVelocityVectorNED("m/s")
                .label("Ground Velocity"));
    }
    
    
    /**
     * Create standard state record of a ground vehicle (UGV)
     * @return A builder to set other options and build the final record 
     */
    public DataRecordBuilder createUgvMechanicalState()
    {
        return createRecord()
            .label("UGV Estimated State")
            .addField("location", createLocationVectorLatLon()
                .label("Platform Location"))
            .addField("heading", createHeadingAngle("deg"))
            .addField("speed", createSpeedOverGround("km/h"));
    }
    
    
    /**
     * Create standard state record of a surface water vehicle (USV)
     * @return A builder to set other options and build the final record 
     */
    public DataRecordBuilder createUsvMechanicalState()
    {
        return createRecord()
            .label("USV Estimated State")
            .addField("location", createLocationVectorLatLon()
                .label("Platform Location"))
            .addField("heading", createHeadingAngle("deg"))
            .addField("course", createCourseOverGround("deg"))
            .addField("speed", createSpeedOverGround("[kn_i]"));
    }
    
    
    /**
     * Create standard state record for an underwater vehicle (UUV)
     * @return A builder to set other options and build the final record 
     */
    public DataRecordBuilder createUuvMechanicalState()
    {
        return createRecord()
            .label("UUV Estimated State")
            .addField("location", createLocationVectorLLA()
                .label("Platform Location"))
            .addField("depth", createDepthBelowSurface("m"))
            .addField("attitude", createEulerOrientationNED("deg")
                .label("Platform Orientation"))
            .addField("velocity", createVelocityVectorNED("m/s")
                .label("Ground Velocity"));
    }
    
    
    public QuantityBuilder createHeadingAngle(String uom)
    {
        checkUom(uom, ANGLE_UNIT);
        
        var q = createQuantity()
            .definition(DEF_HEADING_TRUE)
            .refFrame(SWEConstants.REF_FRAME_NED)
            .axisId("Z")
            .label("Heading")
            .description("Heading angle from true north, measured clockwise")
            .uomCode(uom)
            .dataType(DataType.FLOAT);
        
        if ("deg".equals(uom))
        {
            q.addAllowedInterval(0, 360);
            q.significantFigures(6);
        }
        else if ("rad".equals(uom))
        {
            q.addAllowedInterval(-Math.PI, Math.PI);
            q.significantFigures(6);
        }
        
        return q;
    }
    
    
    public QuantityBuilder createCourseOverGround(String uom)
    {
        checkUom(uom, ANGLE_UNIT);
        
        var q = createQuantity()
            .definition(DEF_COURSE_OVER_GROUND)
            .refFrame(SWEConstants.REF_FRAME_NED)
            .axisId("Z")
            .label("Course Over Ground")
            .description("Course direction from true north, measured clockwise")
            .uom(uom);
        
        if ("deg".equals(uom))
        {
            q.addAllowedInterval(0, 360);
            q.significantFigures(6);
        }
        else if ("rad".equals(uom))
        {
            q.addAllowedInterval(-Math.PI, Math.PI);
            q.significantFigures(6);
        }
        
        return q;
    }
    
    
    public QuantityBuilder createSpeedOverGround(String uom)
    {
        checkUom(uom, SPEED_UNIT);
        
        return createQuantity()
            .definition(DEF_SPEED_OVER_GROUND)
            .label("Speed Over Ground")
            .description("Speed relative to the surface of the earth (or other planetary body)")
            .uom(uom)
            .dataType(DataType.FLOAT)
            .addAllowedInterval(0, Double.POSITIVE_INFINITY);
    }
    
    
    public QuantityBuilder createDepthBelowMSL(String uom)
    {
        checkUom(uom, DISTANCE_UNIT);
        
        return createQuantity()
            .definition(DEF_DEPTH_BELOW_MSL)
            .label("Depth")
            .description("Depth below mean sea level")
            .uomCode(uom);
    }
    
    
    public QuantityBuilder createDepthBelowSurface(String uom)
    {
        checkUom(uom, DISTANCE_UNIT);
        
        return createQuantity()
            .definition(DEF_DEPTH_BELOW_SURFACE)
            .label("Depth")
            .description("Depth below instantaneous water surface")
            .uomCode(uom);
    }
    
    
    // Mission
    
    public DataRecordBuilder createUavMission()
    {
        return createRecord()
            .label("Mission Info")
            .addField("mission_id", createText())
            .addField("waypoints", createArray()
                .label("Waypoints")
                .withElement("wpt", createRecord()
                    .label("Waypoint")
                    .addField("name", createText()
                        .description("Name of waypoint"))
                    .addField("type", createCategory()
                        .description("Type of waypoint")
                        .addAllowedValues("HOVER", "LOITER"))
                    .addField("location", createLocationVectorLLA()
                        .description("Geographic location of waypoint"))
                    .addField("heading", createHeadingAngle("deg")
                        .description("Desired heading at waypoint"))
                    .addField("speed", createSpeedOverGround("m/s")
                        .description("Desired speed to go to waypoint"))
            )
        );
    }
    
    
    // Commands
    
    public DataRecordBuilder createUavGoToWaypointCommand()
    {
        return createGoToWaypointCommand("m/s", HeightType.ALT_ABOVE_ELLIPSOID);
    }
    
    
    public DataRecordBuilder createUgvGoToWaypointCommand()
    {
        return createGoToWaypointCommand("km/h", null);
    }

    
    public DataRecordBuilder createUsvGoToWaypointCommand()
    {
        return createGoToWaypointCommand("[kn_i]", null);
    }

    
    public DataRecordBuilder createUuvGoToWaypointCommand()
    {
        return createGoToWaypointCommand("[kn_i]", HeightType.DEPTH_BELOW_MSL);
    }
    
    
    public DataRecordBuilder createGoToWaypointCommand(String speedUnit, HeightType heightType)
    {
        var cmd = createRecord()
            .definition(CMD_URI_PREFIX + "WaypointCommand")
            .label("Go To Waypoint")
            .description("The waypoint command requests the vehicle to go to a specified waypoint with a desired travel speed");
        
        // use either 3D ellipsoidal location or lat/lon with separate height/depth field
        String locLabel = "Waypoint location";
        if (heightType == HeightType.ALT_ABOVE_ELLIPSOID)
        {
            cmd.addField("location", createLocationVectorLLA()
                .label(locLabel));
        }
        else if (heightType != null)
        {
            cmd.addField("location", createLocationVectorLatLon()
                .label(locLabel));
            
            switch (heightType)
            {
                case ALT_ABOVE_MSL:
                    cmd.addField("altitude", createQuantity()
                        .definition(DEF_ALTITUDE_MSL)
                        .refFrame(SWEConstants.EPSG_URI_PREFIX + "5714")
                        .label("MSL Height")
                        .uomCode("m"));
                    break;
                    
                case DEPTH_BELOW_MSL:
                    cmd.addField("depth", createQuantity()
                        .definition(DEF_DEPTH_BELOW_MSL)
                        .refFrame(SWEConstants.EPSG_URI_PREFIX + "5715")
                        .label("MSL Depth")
                        .uomCode("m"));
                    break;
                    
                case ALT_ABOVE_SURFACE:
                    cmd.addField("altitude", createQuantity()
                        .definition(DEF_DEPTH_BELOW_SURFACE)
                        .refFrame(SWEConstants.EPSG_URI_PREFIX + "5829")
                        .label("Instantaneous Water Level height")
                        .uomCode("m"));
                    break;
                    
                case DEPTH_BELOW_SURFACE:
                    cmd.addField("depth", createQuantity()
                        .definition(DEF_DEPTH_BELOW_SURFACE)
                        .refFrame(SWEConstants.EPSG_URI_PREFIX + "5831")
                        .label("Instantaneous Water Level Depth")
                        .uomCode("m"));
                    break;
                    
                default:
            }
        }
        
        cmd.addField("velocity", createQuantity()
            .definition(DEF_SPEED_OVER_GROUND)
            .label("Travel Speed")
            .description("Speed used to reach the waypoint")
            .uomCode(speedUnit));
        
        return cmd;
    }

}
