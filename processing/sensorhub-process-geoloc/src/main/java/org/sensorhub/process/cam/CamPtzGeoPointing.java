/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.process.cam;

import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.QuantityRange;
import net.opengis.swe.v20.Vector;
import org.sensorhub.algo.geoloc.GeoTransforms;
import org.sensorhub.algo.geoloc.NadirPointing;
import org.sensorhub.algo.vecmath.Mat3d;
import org.sensorhub.algo.vecmath.Vect3d;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;


/**
 * <p>
 * Process for controlling a PTZ camera (or other sensing or actuator system
 * that can be oriented along 2 axes: yaw and pitch) to point at a particular
 * geographic location.
 * </p>
 *
 * @author Alex Robin
 * @since June 9, 2021
 */
public class CamPtzGeoPointing extends ExecutableProcessImpl
{
    public static final OSHProcessInfo INFO = new OSHProcessInfo("PtzGeoPointing", "PTZ Geo-Pointing",
        "Compute PTZ values to point to a given geographic location", CamPtzGeoPointing.class);
    
    protected Vector targetLocInput;
    protected Quantity targetSizeInput;
    protected DataRecord ptzOutput;
    protected Vector camLocationParam;
    protected Vector camOrientParam;
    protected QuantityRange camFovRangeParam;
    
    protected GeoTransforms geoConv = new GeoTransforms();
    protected NadirPointing nadirPointing = new NadirPointing();
    
    protected Vect3d latestCamLocECEF = new Vect3d();
    protected Vect3d latestCamRotNED = new Vect3d();
    protected Vect3d targetLocECEF = new Vect3d();
    protected Mat3d rotNEDToECEF = new Mat3d();
    protected Mat3d rotCamToNED = new Mat3d();
    protected Mat3d rotCamToECEF = new Mat3d();
    protected Vect3d targetLocLLA = new Vect3d();
    protected double minFov, maxFov;
    
    
    public CamPtzGeoPointing()
    {
        super(INFO);
        
        GeoPosHelper swe = new GeoPosHelper();
        
        // inputs
        inputData.add("targetLocation", targetLocInput = swe.createLocationVectorLLA()
            .definition(SWEHelper.getPropertyUri("FeatureOfInterestLocation"))
            .label("Target Location")
            .build());
        
        inputData.add("targetSize", targetSizeInput = swe.createQuantity()
            .definition(SWEHelper.getPropertyUri("Width"))
            .label("Target Size")
            .description("Approximate size of target (used to determine zoom level)")
            .uomCode("m")
            .build());
        
        // outputs
        outputData.add("ptz", ptzOutput = swe.createRecord()
            .addField("pan", swe.createQuantity()
                .definition(SWEHelper.getPropertyUri("PanAngle"))
                .label("Pan")
                .uomCode("deg")
                .dataType(DataType.FLOAT))
            .addField("tilt", swe.createQuantity()
                .definition(SWEHelper.getPropertyUri("TiltAngle"))
                .label("Tilt")
                .uomCode("deg")
                .dataType(DataType.FLOAT))
            .addField("zoom", swe.createQuantity()
                .definition(SWEHelper.getPropertyUri("ZoomFactor"))
                .label("Zoom Factor")
                .uomCode("1")
                .dataType(DataType.SHORT))
            .build());
        
        // params
        paramData.add("camLocation", camLocationParam = swe.createLocationVectorLLA()
            .label("Camera Location")
            .description("Geographic location of camera")
            .fill(Double.NaN)
            .build());
        
        paramData.add("camOrientation", camOrientParam = swe.createEulerOrientationNED("deg")
            .label("Camera Orientation")
            .description("Orientation of camera w.r.t. NED local reference frame")
            .fill(Double.NaN)
            .build());
        
        paramData.add("camFovRange", camFovRangeParam = swe.createQuantityRange()
            .definition(SWEHelper.getPropertyUri("FieldOfView"))
            .label("FOV Range")
            .description("Camera field of view at min and max zoom positions "
                + "(Note that max FOV is at zoomfactor = 0.0, min FOV at zoomfactor = 1.0)")
            .uomCode("deg")
            .value(Double.NaN, Double.NaN)
            .build());
    }
    
    
    protected void readPositionParams() throws ProcessException
    {
        // read camera location
        try
        {   
            var llaData = camLocationParam.getData();
            if (llaData != null)
            {
                var lat = latestCamLocECEF.y = Math.toRadians(llaData.getDoubleValue(0));
                var lon = latestCamLocECEF.x = Math.toRadians(llaData.getDoubleValue(1));
                var alt = latestCamLocECEF.z = llaData.getDoubleValue(2);
                getLogger().debug("Latest camera location = [{},{},{}]" , lat, lon, alt);
                
                // convert to ECEF
                geoConv.LLAtoECEF(latestCamLocECEF, latestCamLocECEF);
                
                // also compute NED to ECEF matrix
                nadirPointing.getRotationMatrixNEDToECEF(latestCamLocECEF, rotNEDToECEF);
            }
        }
        catch (Exception e)
        {
            reportError("Invalid camera location", e);
        }
        
        // read camera orientation
        try
        {   
            var yprData = camOrientParam.getData();
            if (yprData != null)
            {
                var heading = Math.toRadians(yprData.getDoubleValue(0));
                var pitch = Math.toRadians(yprData.getDoubleValue(1));
                var roll = Math.toRadians(yprData.getDoubleValue(2));
                getLogger().debug("Latest camera orientation = [{},{},{}]" , heading, pitch, roll);
                
                // compute camera to NED matrix
                // that is the transpose of R_heading * R_pitch * R_roll
                rotCamToNED.setIdentity();
                rotCamToNED.rotateX(-roll);
                rotCamToNED.rotateY(-pitch);
                rotCamToNED.rotateZ(-heading);
            }
        }
        catch (Exception e)
        {
            reportError("Invalid camera orientation", e);
        }
    }
    
    
    @Override
    public void init() throws ProcessException
    {
        super.init();
        
        minFov = Math.toRadians(camFovRangeParam.getData().getDoubleValue(0));
        maxFov = Math.toRadians(camFovRangeParam.getData().getDoubleValue(1));
        
        readPositionParams();
    }
    
    
    @Override
    public void execute() throws ProcessException
    {        
        try
        {
            readPositionParams();
            
            // wait until camera location has been received
            var llaData = camLocationParam.getData();
            if (Double.isNaN(llaData.getDoubleValue(0)))
                return;
                
            // get target coordinates input
            var lat = targetLocInput.getData().getDoubleValue(0);
            var lon = targetLocInput.getData().getDoubleValue(1);
            var alt = targetLocInput.getData().getDoubleValue(2);
            
            // convert to radians and then ECEF
            targetLocLLA.y = Math.toRadians(lat);
            targetLocLLA.x = Math.toRadians(lon);
            targetLocLLA.z = alt;
            geoConv.LLAtoECEF(targetLocLLA, targetLocECEF);
            
            // compute LOS from camera to target
            Vect3d los = targetLocECEF.sub(latestCamLocECEF);
            double dist = los.norm();
            los.scale(1./dist); // normalize
            
            // transform LOS to camera frame
            rotCamToECEF.set(rotNEDToECEF);
            rotCamToECEF.mul(rotCamToNED);
            rotCamToECEF.transpose();
            los.rotate(rotCamToECEF);
            
            // compute PTZ values
            double pan = Math.toDegrees(Math.atan2(los.y, los.x));
            if (pan <= -180)
            	pan += 360.0;
            else if (pan > 180)
                pan -= 360;
            double xyProj = Math.sqrt(los.x*los.x + los.y*los.y);
            double tilt = -Math.toDegrees(Math.atan2(los.z, xyProj));
            
            // compute zoom value
            // set FOV to 10% more than the target size 
            var targetSize = targetSizeInput.getData().getDoubleValue() * 1.1;
            getLogger().debug("Distance to target = {}", dist);
            double fov = 2*Math.atan(targetSize / 2 / dist) * 1.1;
            double zoom = 1.0 - (fov - minFov) / (maxFov - minFov);
            zoom = Math.min(Math.max(zoom, 0.), 1.);
                        
            // send to PTZ output
            getLogger().debug("Computed PTZ = [{},{},{}]", pan, tilt, zoom);
            ptzOutput.getData().setDoubleValue(0, pan);
            ptzOutput.getData().setDoubleValue(1, tilt);
            ptzOutput.getData().setDoubleValue(2, zoom);
        }
        catch (Exception e)
        {
            reportError("Error computing PTZ position", e);
        }
    }
}
