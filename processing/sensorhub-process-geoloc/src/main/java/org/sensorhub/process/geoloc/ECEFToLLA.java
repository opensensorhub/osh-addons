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
import org.sensorhub.api.processing.OSHProcessInfo;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.helper.GeoPosHelper;


/**
 * <p>
 * Process for converting 3D coordinates from ECEF (4978) to LLA (4979) 
 * </p>
 *
 * @author Alex Robin
 * @since Sep 2, 2015
 */
public class ECEFToLLA extends ExecutableProcessImpl
{
    public static final OSHProcessInfo INFO = new OSHProcessInfo("geoloc:ECEF2LLA", "ECEF to LLA",
        "ECEF to geographic coordinates conversion", ECEFToLLA.class);
    
    protected Vector ecefLoc;
    protected Vector llaLoc;
    protected GeoTransforms transforms;
    protected Vect3d ecef;
    protected Vect3d lla;

    
    public ECEFToLLA()
    {
        super(INFO);
        GeoPosHelper sweHelper = new GeoPosHelper();
        
        // create ECEF input
        ecefLoc = sweHelper.newLocationVectorECEF(null);
        inputData.add("ecefLoc", ecefLoc);
        
        // create LLA output
        llaLoc = sweHelper.newLocationVectorLLA(null);
        outputData.add("llaLoc", llaLoc);
    }

    
    @Override
    public void init() throws ProcessException
    {
        super.init();
        transforms = new GeoTransforms(Ellipsoid.WGS84);
        ecef = new Vect3d();
        lla = new Vect3d();
    }
   
    
    @Override
    public void execute() throws ProcessException
    {
    	DataBlock ecefData = ecefLoc.getData();
        ecef.x = ecefData.getDoubleValue(0);
    	ecef.y = ecefData.getDoubleValue(1);
    	ecef.z = ecefData.getDoubleValue(2);
    	
        transforms.ECEFtoLLA(ecef, lla);
    	
        DataBlock llaData = llaLoc.getData();
        llaData.setDoubleValue(1, Math.toDegrees(lla.x));
        llaData.setDoubleValue(0, Math.toDegrees(lla.y));        
        llaData.setDoubleValue(2, lla.z);
    }
}
