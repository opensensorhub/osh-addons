/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.process.cam;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.Matrix;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Vector;
import org.sensorhub.algo.geoloc.Ellipsoid;
import org.sensorhub.algo.geoloc.EllipsoidIntersect;
import org.sensorhub.algo.geoloc.GeoTransforms;
import org.sensorhub.algo.geoloc.NadirPointing;
import org.sensorhub.algo.vecmath.Mat3d;
import org.sensorhub.algo.vecmath.Vect3d;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.sensorhub.process.vecmath.VecMathHelper;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.process.ProcessInfo;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;
import org.vast.swe.helper.RasterHelper;


/**
 * <p>
 * Transforms pixel coordinates to a geographic location on the ground taking
 * into account the full camera model and intersecting with the earth ellipsoid.
 * </p>
 *
 * @author Alex Robin
 * @since Jun 11, 2021
 */
public class ImageToGround extends ExecutableProcessImpl
{
    public static final OSHProcessInfo INFO = new OSHProcessInfo("geoloc:ImageToGround", "Image to Ground", "Compute ground location of a pixel knowing its image coordinates", ImageToGround.class);
    
    protected Vector imgCoords;
    protected Vector groundLocation;
    
    protected Quantity heightAdjParam;
    protected Vector platformLocParam;
    protected Vector platformAttParam;
    protected Vector camOrientParam;
    protected Matrix camMatrixParam;
    protected DataRecord camDistortParam;
    
    protected GeoTransforms geoConv = new GeoTransforms();
    protected NadirPointing nadirPointing = new NadirPointing(geoConv);
    protected EllipsoidIntersect rie;
    protected double heightAdj;
    protected double fx, fy, cx, cy;
    protected double k1, k2, k3, p1, p2;
    protected Mat3d rotCam0 = new Mat3d();
    protected Mat3d rotCamToPlatform = new Mat3d();
    protected Mat3d rotPlatformToECEF = new Mat3d();
    protected Mat3d rotPlatformToNED = new Mat3d();
    protected Vect3d platformLocECEF = new Vect3d();
    
    protected Vect3d lookDir = new Vect3d();
    protected Vect3d intersect = new Vect3d();
    

    public ImageToGround()
    {
        this(INFO);
    }
    
    
    public ImageToGround(ProcessInfo info)
    {
        super(info);
        var swe = new RasterHelper();
        var geo = new GeoPosHelper();
        
        // inputs
        inputData.add("imgCoords", imgCoords = swe.createGridCoordinates2D()
            .label("Image Coordinates")
            .build());
        
        // outputs
        outputData.add("groundLocation", groundLocation = geo.createLocationVectorLLA()
            .label("Target Location")
            .build());
        
        // parameters
        // initialize with NaN so we can detect if real data has been set or received
        paramData.add("groundHeight", heightAdjParam = swe.createQuantity()
            .definition(SWEHelper.getPropertyUri("HeightAboveEllipsoid"))
            .label("Ground Altitude")
            .description("Altitude of terrain w.r.t. the WGS84 ellipsoid")
            .uom("m")
            .value(Double.NaN)
            .build());
        
        paramData.add("platformLocation", platformLocParam = geo.createLocationVectorLLA()
            .localFrame("PLATFORM_FRAME")
            .label("Platform Location")
            .description("Geographic location of platform carrying the camera")
            .fill(Double.NaN)
            .build());
        
        paramData.add("platformAttitude", platformAttParam = geo.createEulerOrientationNED("deg")
            .localFrame("PLATFORM_FRAME")
            .label("Platform Attitude")
            .description("Orientation of platform w.r.t. the NED local reference frame")
            .fill(Double.NaN)
            .build());
        
        paramData.add("camOrientation", camOrientParam = geo.createEulerOrientationYPR("deg")
            .localFrame("CAMERA_FRAME")
            .refFrame("PLATFORM_FRAME")
            .label("Camera Orientation")
            .description("Orientation of camera w.r.t. the platform reference frame")
            .fill(Double.NaN)
            .build());
        
        paramData.add("camMatrix", camMatrixParam = swe.createMatrix()
            .definition(SWEHelper.getPropertyUri("CameraMatrix"))
            .label("Camera Matrix")
            .size(3, 3, true)
            .withElement("param", swe.createQuantity()
                .uomCode("1"))
            .build());
        
        paramData.add("camDistort", camDistortParam = swe.createRecord()
            .label("Distortion coefficients")
            .addField("k1", swe.createQuantity()
                .definition("http://sensorml.com/ont/csm/property/DISTOR_RAD1")
                .description("2nd order radial distortion coefficient")
                .uomCode("1"))
            .addField("k2", swe.createQuantity()
                .definition("http://sensorml.com/ont/csm/property/DISTOR_RAD2")
                .description("4th order radial distortion coefficient")
                .uomCode("1"))
            .addField("k3", swe.createQuantity()
                .definition("http://sensorml.com/ont/csm/property/DISTOR_RAD3")
                .description("6th order radial distortion coefficient")
                .uomCode("1"))
            .addField("p1", swe.createQuantity()
                .definition("http://sensorml.com/ont/csm/property/DECEN_LENS1")
                .description("1st tangential-decentering coefficient")
                .uomCode("1"))
            .addField("p2", swe.createQuantity()
                .definition("http://sensorml.com/ont/csm/property/DECEN_LENS2")
                .description("2nd tangential-decentering coefficient")
                .uomCode("1"))
            .build());
    }

    
    @Override
    public void init() throws ProcessException
    {
        super.init();
        
        // instantiate RIE algo
        double heightOffset = heightAdjParam.getData().getDoubleValue();
        this.rie = new EllipsoidIntersect(Ellipsoid.WGS84, heightOffset);
        
        // set default camera orientation =  = camera pointing forward
        rotCam0.setZero();
        rotCam0.setElement(0, 2, -1); // x_pltf = -z_cam
        rotCam0.setElement(1, 0,  1); // y_pltf = +x_cam
        rotCam0.setElement(2, 1, -1); // z_pltf = -y_cam
        
        // read platform and camera position params (if set to fixed values)        
        readPositionParams();
    }
    
    
    /*
     * Override to handle case where pixel input is fixed and we have to wait for
     * params
     */
    protected void consumeParamData() throws InterruptedException
    {
        if (!getInputConnections().isEmpty())
            consumeData(paramConnections, false);
        else
            consumeData(paramConnections, true);
    }
    
    
    
    protected void readPositionParams() throws ProcessException
    {
        // read platform location
        try
        {   
            var llaData = platformLocParam.getData();
            if (llaData != null)
            {
                platformLocECEF.y = Math.toRadians(llaData.getDoubleValue(0));
                platformLocECEF.x = Math.toRadians(llaData.getDoubleValue(1));
                platformLocECEF.z = llaData.getDoubleValue(2);
                
                // convert to ECEF
                geoConv.LLAtoECEF(platformLocECEF, platformLocECEF);
            }
        }
        catch (Exception e)
        {
            reportError("Invalid platform location", e);
        }
        
        // read platform orientation
        try
        {   
            var yprData = platformAttParam.getData();
            if (yprData != null)
            {
                var heading = Math.toRadians(yprData.getDoubleValue(0));
                var pitch = Math.toRadians(yprData.getDoubleValue(1));
                var roll = Math.toRadians(yprData.getDoubleValue(2));
                
                // get NED to ECEF matrix
                nadirPointing.getRotationMatrixNEDToECEF(platformLocECEF, rotPlatformToECEF);
                
                // combine with platform to NED matrix
                // that is the transpose of R_heading * R_pitch * R_roll
                rotPlatformToNED.setIdentity();
                rotPlatformToNED.rotateX(-roll);
                rotPlatformToNED.rotateY(-pitch);
                rotPlatformToNED.rotateZ(-heading);
                rotPlatformToECEF.mul(rotPlatformToNED);
            }
        }
        catch (Exception e)
        {
            reportError("Invalid platform orientation", e);
        }
        
        // read camera orientation
        try
        {   
            var yprData = camOrientParam.getData();            
            if (yprData != null)
            {
                var yaw = Math.toRadians(yprData.getDoubleValue(0));
                var pitch = Math.toRadians(yprData.getDoubleValue(1));
                var roll = Math.toRadians(yprData.getDoubleValue(2));
                                
                // compute gimbal orientation rotation
                // that is the transpose of R_yaw * R_pitch * R_roll
                rotCamToPlatform.setIdentity();
                rotCamToPlatform.rotateX(-roll);
                rotCamToPlatform.rotateY(-pitch);
                rotCamToPlatform.rotateZ(-yaw);
                
                // set relative to default camera orientation
                rotCamToPlatform.mul(rotCam0);
            }
        }
        catch (Exception e)
        {
            reportError("Invalid camera orientation", e);
        }
        
        // read camera matrix
        try
        {   
            var camMatrix = new Mat3d();         
            VecMathHelper.toMat3d(camMatrixParam.getData(), camMatrix);
            fx = camMatrix.m00;
            fy = camMatrix.m11;
            cx = camMatrix.m02;
            cy = camMatrix.m12;
        }
        catch (Exception e)
        {
            reportError("Invalid camera matrix", e);
        }
        
        // read camera distortion coefficients
        try
        {   
            int idx = 0;
            k1 = camDistortParam.getData().getDoubleValue(idx++);
            k2 = camDistortParam.getData().getDoubleValue(idx++);
            k3 = camDistortParam.getData().getDoubleValue(idx++);
            p1 = camDistortParam.getData().getDoubleValue(idx++);
            p2 = camDistortParam.getData().getDoubleValue(idx++);
        }
        catch (Exception e)
        {
            reportError("Invalid camera matrix", e);
        }
    }
    
    
    @Override
    public void execute() throws ProcessException
    {
        readPositionParams();
        
        // wait until platform location has been received
        var llaData = platformLocParam.getData();
        if (Double.isNaN(llaData.getDoubleValue(0)))
            return;
            
        // get pixel coordinates input
        var x = imgCoords.getData().getDoubleValue(0);
        var y = imgCoords.getData().getDoubleValue(1);
        
        // compute ground location of pixel
        var ok = toGroundLocation(x, y, intersect);
        if (!ok)
            getLogger().debug("No intersection found");
        
        // set ground location output
        DataBlock intersectData = groundLocation.getData();
        intersectData.setDoubleValue(0, Math.toDegrees(intersect.y));
        intersectData.setDoubleValue(1, Math.toDegrees(intersect.x));
        intersectData.setDoubleValue(2, intersect.z);
    }
    
    
    protected boolean toGroundLocation(double x, double y, Vect3d result)
    {
        // compute look direction
        lookDir.x = (x - cx) / fx;
        lookDir.y = ((480-y) - cy) / fy;
        lookDir.z = -1.0;
        
        // apply distortions
        var x2 = lookDir.x * lookDir.x;
        var y2 = lookDir.y * lookDir.y;
        var r2 = x2 + y2;
        var r4 = r2 * r2;
        var r6 = r4 * r2;
        var xy = lookDir.x * lookDir.y;
        lookDir.x = lookDir.x * (1 + k1*r2 + k2*r4 + k3*r6 + 2*p1*xy + p2*(r2+2*x2));
        lookDir.y = lookDir.y * (1 + k1*r2 + k2*r4 + k3*r6 + 2*p2*xy + p1*(r2+2*y2));
        
        // transform look dir to ECEF
        rotCamToPlatform.mul(lookDir, lookDir);
        rotPlatformToECEF.mul(lookDir, lookDir);
        
        // intersect with ellipsoid
        lookDir.normalize();
        boolean ok = rie.computeIntersection(platformLocECEF, lookDir, result);
        if (!ok)
        {
            result.set(Double.NaN, Double.NaN, Double.NaN);
            return false;
        }
        
        geoConv.ECEFtoLLA(result, result);
        return true;
    }
}
