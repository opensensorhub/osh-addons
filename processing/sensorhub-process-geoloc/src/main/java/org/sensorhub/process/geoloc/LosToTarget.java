/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.process.geoloc;

import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Vector;
import org.sensorhub.algo.geoloc.GeoTransforms;
import org.sensorhub.algo.geoloc.NadirPointing;
import org.sensorhub.algo.vecmath.Mat3d;
import org.sensorhub.algo.vecmath.Vect3d;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.vast.process.DataQueue;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;


/**
 * <p>
 * Computes a target location knowing the observer location, as well as the
 * direction and distance to the target.
 * </p><p>
 * For instance, this process can be used to geolocate a target pointed by a
 * range finder that also outputs the inclination/elevation of the laser beam.
 * </p>
 *
 * @author Alex Robin
 * @since June 21, 2021
 */
public class LosToTarget extends ExecutableProcessImpl
{
    public static final OSHProcessInfo INFO = new OSHProcessInfo("geoloc:LosToTarget", "LoS To Target Location",
        "Compute target location knowing the observer location, as well as the direction and distance to the target",
        LosToTarget.class);
    
    protected Vector observerLocInput;
    protected Vector losDirInput;
    protected Quantity targetDistInput;
    protected Vector targetLocOutput;
            
    protected GeoTransforms geoConv = new GeoTransforms();
    protected NadirPointing nadirPointing = new NadirPointing();
    
    protected boolean lastSensorPosSet = false;
    protected Vect3d lastSensorPosEcef = new Vect3d();
    protected Vect3d lla = new Vect3d();
    protected Mat3d ecefRot = new Mat3d();
    
    protected DataRecord sensorLocInput;
    protected DataComponent rangeMeasInput;    
    protected DataQueue sensorLocQueue;
    protected DataQueue rangeMeasQueue;
    
    
    public LosToTarget()
    {
        super(INFO);
        
        GeoPosHelper swe = new GeoPosHelper();
        
        // inputs
        inputData.add("observerLocation", observerLocInput = swe.createLocationVectorLLA()
            .definition(SWEHelper.getPropertyUri("ObserverLocation"))
            .label("Observer Location")
            .description("Geographic location of the observer")
            .build());
        
        inputData.add("losDirection", losDirInput = swe.createVector()
            .definition(GeoPosHelper.DEF_ORIENTATION_EULER)
            .label("LOS Direction")
            .description("Direction of the line of sight")
            .refFrame(SWEConstants.REF_FRAME_NED)
            .dataType(DataType.FLOAT)
            .addCoordinate("azimuth", swe.createQuantity()
                .definition(GeoPosHelper.DEF_AZIMUTH_ANGLE)
                .label("Azimuth Angle")
                .description("Line-of-sight azimuth from true north, measured clockwise")
                .uomCode("deg")
                .axisId("Z")
                .build())
            .addCoordinate("elevation", swe.createQuantity()
                .definition(GeoPosHelper.DEF_ELEVATION_ANGLE)
                .label("Elevation Angle")
                .description("Line-of-sight elevation from the local horizontal plane (positive when pointing up)")
                .uomCode("deg")
                .axisId("Y")
                .build())
            .build());
        
        inputData.add("targetDistance", targetDistInput = swe.createQuantity()
            .definition(SWEHelper.getPropertyUri("Distance"))
            .label("Target Distance")
            .description("Distance to target")
            .uomCode("m")
            .build());
        
        // outputs
        outputData.add("targetLocation", targetLocOutput = swe.createLocationVectorLLA()
            .definition(SWEHelper.getPropertyUri("FeatureOfInterestLocation"))
            .label("Target Location")
            .build());
    }
    
    
    @Override
    public void execute() throws ProcessException
    {
        // get observer location and convert to ECEF
        var obsLocData = observerLocInput.getData();
        lla.set(
            Math.toRadians(obsLocData.getDoubleValue(1)),
            Math.toRadians(obsLocData.getDoubleValue(0)),
            obsLocData.getDoubleValue(2));
        geoConv.LLAtoECEF(lla, lastSensorPosEcef);
        
        // get target distance
        var range = targetDistInput.getData().getDoubleValue();
        
        // get LOS direction and 
        var losDirData = losDirInput.getData();
        var az = Math.toRadians(losDirData.getDoubleValue(0));
        var inc = Math.toRadians(losDirData.getDoubleValue(1));
        getLogger().debug("LOS: range={}, az={}, inc={}", range, az, inc);
        
        // convert LOS to ECEF        
        Vect3d los = new Vect3d(range, 0.0, 0.0);
        los.rotateY(inc);
        los.rotateZ(az);
        nadirPointing.getRotationMatrixNEDToECEF(lastSensorPosEcef, ecefRot);
        los.rotate(ecefRot);
        
        // simply translate to get target location
        los.add(lastSensorPosEcef);
        
        // convert back to LLA
        geoConv.ECEFtoLLA(los, lla);
        
        // write to output
        var targetLocData = targetLocOutput.getData();
        targetLocData.setDoubleValue(0, Math.toDegrees(lla.y));
        targetLocData.setDoubleValue(1, Math.toDegrees(lla.x));
        targetLocData.setDoubleValue(2, lla.z);
    }
}
