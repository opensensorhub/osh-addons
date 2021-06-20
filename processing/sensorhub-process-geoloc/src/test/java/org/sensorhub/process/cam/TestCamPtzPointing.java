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

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.vast.process.ProcessException;


public class TestCamPtzPointing
{
    CamPtzGeoPointing process = new CamPtzGeoPointing();
    
    
    void setupParams(
        double camLat, double camLon, double camAlt,
        double camHeading, double camPitch, double camRoll,
        double minFov, double maxFov) throws ProcessException
    {
        
        var params = process.getParameterList();
        
        var camLoc = params.getComponent("camLocation");
        camLoc.assignNewDataBlock();
        camLoc.getData().setDoubleValue(0, camLat);
        camLoc.getData().setDoubleValue(1, camLon);
        camLoc.getData().setDoubleValue(2, camAlt);
        
        var camOrient = params.getComponent("camOrientation");
        camOrient.assignNewDataBlock();
        camOrient.getData().setDoubleValue(0, camHeading);
        camOrient.getData().setDoubleValue(1, camPitch);
        camOrient.getData().setDoubleValue(2, camRoll);
        
        var fovRange = params.getComponent("camFovRange");
        fovRange.getData().setDoubleValue(0, minFov);
        fovRange.getData().setDoubleValue(1, maxFov);
        
        process.init();
    }
    
    
    void setTargetInput(double lat, double lon, double alt, double size)
    {
        var targetLoc = process.getInputList().getComponent("targetLocation");
        targetLoc.assignNewDataBlock();
        targetLoc.getData().setDoubleValue(0, lat);
        targetLoc.getData().setDoubleValue(1, lon);
        targetLoc.getData().setDoubleValue(2, alt);
        
        var targetSize = process.getInputList().getComponent("targetSize");
        targetSize.assignNewDataBlock();
        targetSize.getData().setDoubleValue(size);
    }
    
    
    double[] getOutputPtz()
    {
        var ptz = process.getOutputList().getComponent("ptz");
        return new double[] {
            ptz.getData().getDoubleValue(0),
            ptz.getData().getDoubleValue(1),
            ptz.getData().getDoubleValue(2)
        };
    }
    
    
    @Test
    public void testFixedParamsCamLookingNorth() throws ProcessException
    {
        // camera at LLA=0,0,0, looking north/horizontal
        setupParams(
            0.0, 0.0, 0.0,     // cam location
            0.0, 0.0, 0.0,     // cam orientation
            10, 45);           // fov range
        
        // point north
        setTargetInput(0.001, 0.0, 0.0, 10.0);
        process.execute();        
        var ptz = getOutputPtz();
        assertEquals(0.0, ptz[0], 1e-8);
        assertEquals(-5e-4, ptz[1], 1e-8);
        assertEquals(1.0, ptz[2], 1e-8);
        
        // point east
        setTargetInput(0.0, 0.001, 0.0, 10.0);
        process.execute();        
        ptz = getOutputPtz();
        assertEquals(90.0, ptz[0], 1e-8);
        assertEquals(-5e-4, ptz[1], 1e-8);
        assertEquals(1.0, ptz[2], 1e-8);
        
        // point south                
        setTargetInput(-0.001, 0.0, 0.0, 10.0);
        process.execute();        
        ptz = getOutputPtz();
        assertEquals(180.0, ptz[0], 1e-8);
        assertEquals(-5e-4, ptz[1], 1e-8);
        assertEquals(1.0, ptz[2], 1e-8);
        
        // point west                
        setTargetInput(0.0, -0.001, 0.0, 10.0);
        process.execute();        
        ptz = getOutputPtz();
        assertEquals(-90.0, ptz[0], 1e-8);
        assertEquals(-5e-4, ptz[1], 1e-8);
        assertEquals(1.0, ptz[2], 1e-8);
    }
    
    
    @Test
    public void testFixedParamsCamLookingEast() throws ProcessException
    {
        // camera at LLA=0,0,0, looking north/horizontal
        setupParams(
            0.0, 0.0, 0.0,     // cam location
            90.0, 0.0, 0.0,    // cam orientation
            10, 45);           // fov range
        
        // point north
        setTargetInput(0.001, 0.0, 0.0, 10.0);
        process.execute();        
        var ptz = getOutputPtz();
        assertEquals(-90.0, ptz[0], 1e-8);
        assertEquals(-5e-4, ptz[1], 1e-8);
        assertEquals(1.0, ptz[2], 1e-8);
        
        // point east
        setTargetInput(0.0, 0.001, 0.0, 10.0);
        process.execute();        
        ptz = getOutputPtz();
        assertEquals(0.0, ptz[0], 1e-8);
        assertEquals(-5e-4, ptz[1], 1e-8);
        assertEquals(1.0, ptz[2], 1e-8);
        
        // point south                
        setTargetInput(-0.001, 0.0, 0.0, 10.0);
        process.execute();        
        ptz = getOutputPtz();
        assertEquals(90.0, ptz[0], 1e-8);
        assertEquals(-5e-4, ptz[1], 1e-8);
        assertEquals(1.0, ptz[2], 1e-8);
        
        // point west                
        setTargetInput(0.0, -0.001, 0.0, 10.0);
        process.execute();        
        ptz = getOutputPtz();
        assertEquals(180.0, ptz[0], 1e-8);
        assertEquals(-5e-4, ptz[1], 1e-8);
        assertEquals(1.0, ptz[2], 1e-8);
    }
    
    
    @Test
    public void testFixedParamsLookingUp() throws ProcessException
    {
        setupParams(
            0.0, 0.0, 0.0,     // cam location
            0.0, 0.0, 0.0,     // cam orientation
            10, 45);           // fov range
                
        // point north/up
        setTargetInput(0.001, 0.0, 110.0, 10.0);
        process.execute();        
        var ptz = getOutputPtz();
        assertEquals(0.0, ptz[0], 1e-8);
        assertEquals(44.85, ptz[1], 1e-3);
        assertEquals(1.0, ptz[2], 1e-8);
    }        
}
