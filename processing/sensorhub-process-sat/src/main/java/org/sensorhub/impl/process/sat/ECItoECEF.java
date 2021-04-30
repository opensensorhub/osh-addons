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
 * Converts ECI to ECEF (4978) ECI coordinates
 * </p>
 * 
 * @author Alex Robin
 * @date Sep 2, 2015
 */
public class ECItoECEF extends ExecutableProcessImpl
{
    public static final OSHProcessInfo INFO = new OSHProcessInfo("ECI2ECEF", "ECI to ECEF", "ECI to ECEF coordinates conversion", ECItoECEF.class);
    Vector eciLoc;
    Vector ecefLoc;
    Time utcTime;
    GeoTransforms transforms;
    Vect3d ecef;
    Vect3d eci;
    

    public ECItoECEF()
    {
        super(INFO);
        GeoPosHelper sweHelper = new GeoPosHelper();
        
        // create ECI input
        eciLoc = sweHelper.newLocationVectorECEF(null);
        eciLoc.setReferenceFrame(SWEConstants.REF_FRAME_ECI_J2000);
        inputData.add("eciLoc", eciLoc);
        
        // create time input
        utcTime = sweHelper.newTimeStampIsoUTC();
        inputData.add(utcTime);
        
        // create ECEFoutput
        ecefLoc = sweHelper.newLocationVectorECEF(null);
        outputData.add("ecefLoc", ecefLoc);
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
                
        DataBlock eciData = eciLoc.getData();
        eci.x = eciData.getDoubleValue(0);
        eci.y = eciData.getDoubleValue(1);        
        eci.z = eciData.getDoubleValue(2);
                
        transforms.ECItoECEF(time, eci, ecef, false);
        
        DataBlock ecefData = ecefLoc.getData();
        ecefData.setDoubleValue(0, ecef.x);
        ecefData.setDoubleValue(1, ecef.y);
        ecefData.setDoubleValue(2, ecef.z);
    }
}