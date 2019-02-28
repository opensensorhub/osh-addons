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
import net.opengis.swe.v20.Vector;
import org.sensorhub.algo.geoloc.Ellipsoid;
import org.sensorhub.algo.geoloc.GeoTransforms;
import org.sensorhub.algo.vecmath.Vect3d;
import org.vast.process.SMLException;
import org.vast.sensorML.ExecutableProcessImpl;
import org.vast.swe.helper.GeoPosHelper;


/**
 * <p>
 * Process for converting 3D coordinates from LLA (4979) to ECEF (4978)
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 2, 2015
 */
public class LLAToECEF_Process extends ExecutableProcessImpl
{
    private Vector ecefLoc, llaLoc;
    private GeoTransforms transforms;
    private Vect3d ecef, lla;

    
    public LLAToECEF_Process()
    {
        GeoPosHelper sweHelper = new GeoPosHelper();
        
        // create LLA input
        llaLoc = sweHelper.newLocationVectorLLA(null);
        inputData.add("llaLoc", llaLoc);
        
        // create ECEF output
        ecefLoc = sweHelper.newLocationVectorECEF(null);
        outputData.add("ecefLoc", ecefLoc);
    }

    
    @Override
    public void init() throws SMLException
    {
        transforms = new GeoTransforms(Ellipsoid.WGS84);
        ecef = new Vect3d();
        lla = new Vect3d();
    }
   
    
    @Override
    public void execute() throws SMLException
    {
        DataBlock llaData = llaLoc.getData();
        lla.x = Math.toRadians(llaData.getDoubleValue(1));
        lla.y = Math.toRadians(llaData.getDoubleValue(0));        
        lla.z = llaData.getDoubleValue(2);
        
        transforms.LLAtoECEF(lla, ecef);
        
        DataBlock ecefData = ecefLoc.getData();
        ecefData.setDoubleValue(0, ecef.x);
        ecefData.setDoubleValue(1, ecef.y);
        ecefData.setDoubleValue(2, ecef.z);
    }
}
