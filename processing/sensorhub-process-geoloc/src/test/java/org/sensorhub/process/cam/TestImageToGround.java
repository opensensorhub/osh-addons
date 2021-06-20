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
import org.sensorhub.algo.vecmath.Vect3d;
import org.vast.process.ProcessException;


public class TestImageToGround
{
    ImageToGround process = new ImageToGround();
    
    
    void setupParams(
        double groundAlt,
        double platformLat, double platformLon, double platformAlt,
        double platformHeading, double platformPitch, double platformRoll,
        double camYaw, double camPitch, double camRoll,
        double focalLength, int imgWidth, int imgHeight) throws ProcessException
    {
        
        var params = process.getParameterList();
        
        var groundHeight = params.getComponent("groundHeight");
        groundHeight.assignNewDataBlock();
        groundHeight.getData().setDoubleValue(groundAlt);
        
        var platformLoc = params.getComponent("platformLocation");
        platformLoc.assignNewDataBlock();
        platformLoc.getData().setDoubleValue(0, platformLat);
        platformLoc.getData().setDoubleValue(1, platformLon);
        platformLoc.getData().setDoubleValue(2, platformAlt);
        
        var platformAtt = params.getComponent("platformAttitude");
        platformAtt.assignNewDataBlock();
        platformAtt.getData().setDoubleValue(0, platformHeading);
        platformAtt.getData().setDoubleValue(1, platformPitch);
        platformAtt.getData().setDoubleValue(2, platformRoll);
        
        var camOrient = params.getComponent("camOrientation");
        camOrient.assignNewDataBlock();
        camOrient.getData().setDoubleValue(0, camYaw);
        camOrient.getData().setDoubleValue(1, camPitch);
        camOrient.getData().setDoubleValue(2, camRoll);
        
        var camMatrix = params.getComponent("camMatrix");
        camMatrix.assignNewDataBlock();
        camMatrix.getData().setDoubleValue(0, focalLength);
        camMatrix.getData().setDoubleValue(2, imgWidth/2);
        camMatrix.getData().setDoubleValue(4, focalLength);
        camMatrix.getData().setDoubleValue(5, imgHeight/2);
        camMatrix.getData().setDoubleValue(8, 1.0);
        
        process.init();
    }
    
    
    void setInputPixel(double x, double y)
    {
        var imgPos = process.getInputList().getComponent("imgCoords");
        imgPos.assignNewDataBlock();
        imgPos.getData().setDoubleValue(0, x);
        imgPos.getData().setDoubleValue(1, y);
    }
    
    
    Vect3d getOutputLLA()
    {
        var lla = process.getOutputList().getComponent("groundLocation");
        return new Vect3d(
            lla.getData().getDoubleValue(1),
            lla.getData().getDoubleValue(0),
            lla.getData().getDoubleValue(2));
    }
    
    
    @Test
    public void testFixedParamsLookingDown() throws ProcessException
    {
        // looking straight down
        // LLA loc = 0,0,100
        setupParams(
            0.0,               // ground altitude
            0.0, 0.0, 100.0,   // platform location
            0.0, 0.0, 0.0,     // platform attitude
            0.0, -90.0, 0.0,   // camera orientation
            320, 640, 480);    // focal & img dims
                
        setInputPixel(320, 240);
        process.execute();        
        var lla = getOutputLLA();
        assertEquals(0.0, lla.y, 1e-8);
        assertEquals(0.0, lla.x, 1e-8);
        assertEquals(0.0, lla.z, 1e-8);
        
        // looking straight down
        // LLA loc = 45,10,100
        setupParams(
            0.0,                // ground altitude
            45.0, 10.0, 100.0,  // platform location
            0.0, 0.0, 0.0,      // platform attitude
            0.0, -90.0, 0.0,    // camera orientation
            320, 640, 480);     // focal & img dims
                
        setInputPixel(320, 240);
        process.execute();        
        lla = getOutputLLA();
        assertEquals(45.0, lla.y, 1e-8);
        assertEquals(10.0, lla.x, 1e-8);
        assertEquals(0.0, lla.z, 1e-8);
    }
    
    
    @Test
    public void testFixedParamsLookingEast() throws ProcessException
    {
        // platform roll=-45°
        // LLA loc = 0,0,100
        setupParams(
            0.0,                // ground altitude
            0.0, 0.0, 100.0,    // platform location
            0.0, 0.0, -45.0,    // platform attitude
            0.0, -90.0, 0.0,    // camera orientation
            320, 640, 480);     // focal & img dims
        
        setInputPixel(320, 240);
        process.execute();
        var lla = getOutputLLA();
        double expectedDLon = 100/4e7*360;
        assertEquals(0.0, lla.y, 1e-8);
        assertEquals(0.0+expectedDLon, lla.x, 1e-4);
        assertEquals(0.0, lla.z, 1e-8);
        
        // platform roll=-45°
        // LLA loc = 45,10,100
        setupParams(
            0.0,                // ground altitude
            45.0, 10.0, 100.0,  // platform location
            0.0, 0.0, -45.0,    // platform attitude
            0.0, -90.0, 0.0,    // camera orientation
            320, 640, 480);     // focal & img dims
                
        setInputPixel(320, 240);
        process.execute();        
        lla = getOutputLLA();
        expectedDLon = 100/4e7*360/Math.cos(Math.toRadians(45.0));
        assertEquals(45.0, lla.y, 1e-8);
        assertEquals(10.0+expectedDLon, lla.x, 1e-4);
        assertEquals(0.0, lla.z, 1e-8);
        
        // platform heading=90°, pitch=-45°
        // LLA loc = 0,0,100
        setupParams(
            0.0,                // ground altitude
            0.0, 0.0, 100.0,    // platform location
            90.0, -45.0, 0.0,   // platform attitude
            0.0, 0.0, 0.0,      // camera orientation
            320, 640, 480);     // focal & img dims
        
        setInputPixel(320, 240);
        process.execute();
        lla = getOutputLLA();
        expectedDLon = 100/4e7*360;
        assertEquals(0.0, lla.y, 1e-8);
        assertEquals(0.0+expectedDLon, lla.x, 1e-4);
        assertEquals(0.0, lla.z, 1e-8);
        
        // camera yaw=90, pitch=-45
        // LLA loc = 0,0,100
        setupParams(
            0.0,                // ground altitude
            0.0, 0.0, 100.0,    // platform location
            0.0, 0.0, 0.0,      // platform attitude
            90.0, -45.0, 0.0,   // camera orientation
            320, 640, 480);     // focal & img dims
                
        setInputPixel(320, 240);
        process.execute();        
        lla = getOutputLLA();
        expectedDLon = 100/4e7*360;
        assertEquals(0.0, lla.y, 1e-8);
        assertEquals(0.0+expectedDLon, lla.x, 1e-4);
        assertEquals(0.0, lla.z, 1e-8);
    }
    
    
    @Test
    public void testFixedParamsExtremePixels() throws ProcessException
    {
        // pixel at max X
        // LLA loc = 0,0,100
        setupParams(
            0.0,                // ground altitude
            0.0, 0.0, 100.0,    // platform location
            0.0, 0.0, 0.0,      // platform attitude
            0.0, -90.0, 0.0,    // camera orientation
            320, 640, 480);     // focal & img dims
                
        setInputPixel(640, 240);
        process.execute();        
        var lla = getOutputLLA();
        var expectedDLon = 100/4e7*360;
        assertEquals(0.0, lla.y, 1e-8);
        assertEquals(0.0+expectedDLon, lla.x, 1e-4);
        assertEquals(0.0, lla.z, 1e-8);
        
        // pixel at min X
        // LLA loc = 0,0,100
        setupParams(
            0.0,                // ground altitude
            0.0, 0.0, 100.0,    // platform location
            0.0, 0.0, 0.0,      // platform attitude
            0.0, -90.0, 0.0,    // camera orientation
            320, 640, 480);     // focal & img dims
                
        setInputPixel(0, 240);
        process.execute();        
        lla = getOutputLLA();
        expectedDLon = -100/4e7*360;
        assertEquals(0.0, lla.y, 1e-8);
        assertEquals(0.0+expectedDLon, lla.x, 1e-4);
        assertEquals(0.0, lla.z, 1e-8);
    }
}
