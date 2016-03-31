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

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Vector;
import org.sensorhub.algo.geoloc.RayIntersectEllipsoid;
import org.sensorhub.algo.vecmath.Vect3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.process.SMLException;
import org.vast.sensorML.ExecutableProcessImpl;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;


/**
 * <p>
 * Computes intersection of a 3D ray with a sphere which axes are
 * aligned with the axes of the referential of the ray. This process outputs
 * coordinates of the intersect point expressed in the same frame.
 * </p>

 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 13, 2015
 */
public class RayIntersectSphere_Process extends ExecutableProcessImpl
{
    private Logger log = LoggerFactory.getLogger(RayIntersectSphere_Process.class);
    
    private Vector rayOrigin, rayDirection, intersection;
    private Quantity sphereRadius;
    private RayIntersectEllipsoid rie;
    private Vect3d origin, dir, intersect;
    private double radius;
    

    public RayIntersectSphere_Process()
    {
        GeoPosHelper sweHelper = new GeoPosHelper();
        
        //// INPUTS ////
        // ray origin in reference frame
        rayOrigin = sweHelper.newLocationVectorXYZ(null, SWEConstants.NIL_UNKNOWN, "m");
        inputData.add("rayOrigin", rayOrigin);
        
        // ray direction in reference frame
        rayDirection = sweHelper.newLocationVectorXYZ(null, SWEConstants.NIL_UNKNOWN, "1");
        inputData.add("rayDirection", rayDirection);
        
        //// PARAMETERS ////
        // sphere radius
        sphereRadius = sweHelper.newQuantity(SWEHelper.getPropertyUri("Radius"), "Sphere Radius", null, "m");
        sphereRadius.createDataBlock();
        paramData.add("sphereRadius", sphereRadius);        
        
        //// OUTPUTS ////
        intersection = sweHelper.newLocationVectorECEF(null);
        outputData.add("intersection", intersection);
    }

    
    @Override
    public void init() throws SMLException
    {
        this.origin = new Vect3d();
        this.dir = new Vect3d();
        this.intersect = new Vect3d();
        
        // instantiate ellipsoid intersection algorithm
        rie = new RayIntersectEllipsoid(radius, radius, radius);
    }
    
    
    @Override
    public void execute() throws SMLException
    {
        // get ray origin input
        DataBlock originData = rayOrigin.getData();
        origin.x = originData.getDoubleValue(0);
        origin.y = originData.getDoubleValue(1);
        origin.z = originData.getDoubleValue(2);
        
        // get ray direction input
        DataBlock dirData = rayDirection.getData();
        dir.x = dirData.getDoubleValue(0);
        dir.y = dirData.getDoubleValue(1);
        dir.z = dirData.getDoubleValue(2);
        
        boolean ok = rie.computeIntersection(origin, dir, intersect);
        if (!ok)
            log.debug("No intersection found");
        
        // set intersection point output
        DataBlock intersectData = intersection.getData();
        intersectData.setDoubleValue(0, intersect.x);
        intersectData.setDoubleValue(1, intersect.y);
        intersectData.setDoubleValue(2, intersect.z);
    }
}
