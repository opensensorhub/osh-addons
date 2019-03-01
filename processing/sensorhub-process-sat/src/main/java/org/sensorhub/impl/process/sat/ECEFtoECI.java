/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.process.sat;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.Time;
import net.opengis.swe.v20.Vector;
import org.sensorhub.algo.geoloc.Ellipsoid;
import org.sensorhub.algo.geoloc.GeoTransforms;
import org.sensorhub.algo.vecmath.Vect3d;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEConstants;
import org.vast.swe.helper.GeoPosHelper;


/**
 * <p>
 * Converts ECEF (4978) to ECI coordinates
 * </p>
 * 
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @date Sep 2, 2015
 */
public class ECEFtoECI extends ExecutableProcessImpl
{
    public static final OSHProcessInfo INFO = new OSHProcessInfo("ECEF2ECI", "ECEF to ECI", "ECEF to ECI coordinates conversion", ECEFtoECI.class);
    Vector ecefLoc;
    Vector eciLoc;
    Time utcTime;
    GeoTransforms transforms;
    Vect3d ecef;
    Vect3d eci;
    

    public ECEFtoECI()
    {
        super(INFO);
        GeoPosHelper sweHelper = new GeoPosHelper();
        
        // create ECEF input
        ecefLoc = sweHelper.newLocationVectorECEF(null);
        inputData.add("ecefLoc", ecefLoc);
        
        // create time input
        utcTime = sweHelper.newTimeStampIsoUTC();
        inputData.add(utcTime);
        
        // create ECI output
        eciLoc = sweHelper.newLocationVectorECEF(null);
        eciLoc.setReferenceFrame(SWEConstants.REF_FRAME_ECI_J2000);
        outputData.add("eciLoc", eciLoc);
    }


    @Override
    public void init() throws ProcessException
    {
        transforms = new GeoTransforms(Ellipsoid.WGS84);
        ecef = new Vect3d();
        eci = new Vect3d();
    }


    @Override
    public void execute() throws ProcessException
    {
        double time = utcTime.getData().getDoubleValue(); 
                
        DataBlock ecefData = ecefLoc.getData();
        ecef.x = ecefData.getDoubleValue(0);
        ecef.y = ecefData.getDoubleValue(1);
        ecef.z = ecefData.getDoubleValue(2);
        
        transforms.ECEFtoECI(time, ecef, eci, false);
        
        DataBlock eciData = eciLoc.getData();
        eciData.setDoubleValue(0, eci.x);
        eciData.setDoubleValue(1, eci.y);        
        eciData.setDoubleValue(2, eci.z);
    }
}